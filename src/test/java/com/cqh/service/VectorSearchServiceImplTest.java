package com.cqh.service;

import com.cqh.dao.BlogRepository;
import com.cqh.po.Blog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class VectorSearchServiceImplTest {

    @Mock private EntityManager em;
    @Mock private EmbeddingService embeddingService;
    @Mock private BlogRepository blogRepository;
    @Mock private Query scanQuery;
    @Mock private Query singleQuery;

    private VectorSearchServiceImpl service;

    private Blog blog1, blog2, blog3;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new VectorSearchServiceImpl();
        service.setEntityManager(em);
        service.setEmbeddingService(embeddingService);
        service.setBlogRepository(blogRepository);

        blog1 = new Blog(); blog1.setId(1L); blog1.setTitle("Java");
        blog2 = new Blog(); blog2.setId(2L); blog2.setTitle("Python");
        blog3 = new Blog(); blog3.setId(3L); blog3.setTitle("Go");

        when(blogRepository.findOne(1L)).thenReturn(blog1);
        when(blogRepository.findOne(2L)).thenReturn(blog2);
        when(blogRepository.findOne(3L)).thenReturn(blog3);
    }

    // ---------- parse + cosine pure-math tests ----------

    @Test
    public void parse_handlesWhitespaceAndBrackets() {
        double[] v = VectorSearchServiceImpl.parse("[1.0, 2.5, -3.0]");
        assertArrayEquals(new double[]{1.0, 2.5, -3.0}, v, 1e-9);
    }

    @Test
    public void parse_handlesNoBrackets() {
        double[] v = VectorSearchServiceImpl.parse("0.5,0.5");
        assertArrayEquals(new double[]{0.5, 0.5}, v, 1e-9);
    }

    @Test
    public void cosine_identicalVectors_returns1() {
        double[] a = {1, 2, 3};
        assertEquals(1.0, VectorSearchServiceImpl.cosine(a, a), 1e-9);
    }

    @Test
    public void cosine_orthogonalVectors_returns0() {
        double[] a = {1, 0};
        double[] b = {0, 1};
        assertEquals(0.0, VectorSearchServiceImpl.cosine(a, b), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cosine_dimMismatch_throws() {
        VectorSearchServiceImpl.cosine(new double[]{1, 2}, new double[]{1, 2, 3});
    }

    // ---------- semanticSearch ----------

    @Test
    public void semanticSearch_nullQuery_returnsEmpty() {
        assertTrue(service.semanticSearch(null, 5).isEmpty());
    }

    @Test
    public void semanticSearch_embeddingServiceReturnsNull_returnsEmpty() {
        when(embeddingService.embed(anyString())).thenReturn(null);
        assertTrue(service.semanticSearch("java", 5).isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void semanticSearch_ranksByCosineSimilarity() {
        // Query "java" embeds to [1, 0]
        when(embeddingService.embed("java")).thenReturn("[1.0, 0.0]");

        // 3 stored blogs — blog1 perfect match, blog3 partial, blog2 orthogonal
        Object[] row1 = new Object[]{1L, "[1.0, 0.0]"};     // cosine = 1.0
        Object[] row2 = new Object[]{2L, "[0.0, 1.0]"};     // cosine = 0.0
        Object[] row3 = new Object[]{3L, "[0.7, 0.3]"};     // cosine ~ 0.92
        List rows = Arrays.asList(row1, row2, row3);

        when(em.createNativeQuery(anyString())).thenReturn(scanQuery);
        when(scanQuery.getResultList()).thenReturn(rows);

        List<Blog> result = service.semanticSearch("java", 10);

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(1L), result.get(0).getId()); // best match
        assertEquals(Long.valueOf(3L), result.get(1).getId()); // second
        assertEquals(Long.valueOf(2L), result.get(2).getId()); // worst
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void semanticSearch_respectsTopK() {
        when(embeddingService.embed(anyString())).thenReturn("[1.0, 0.0]");
        List rows = Arrays.asList(
                new Object[]{1L, "[1.0, 0.0]"},
                new Object[]{2L, "[0.9, 0.1]"},
                new Object[]{3L, "[0.5, 0.5]"});
        when(em.createNativeQuery(anyString())).thenReturn(scanQuery);
        when(scanQuery.getResultList()).thenReturn(rows);

        List<Blog> result = service.semanticSearch("q", 2);
        assertEquals(2, result.size());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void semanticSearch_scanFailure_returnsEmpty() {
        when(embeddingService.embed(anyString())).thenReturn("[1.0]");
        when(em.createNativeQuery(anyString())).thenReturn(scanQuery);
        when(scanQuery.getResultList()).thenThrow(new RuntimeException("VECTOR not supported"));

        assertTrue(service.semanticSearch("q", 5).isEmpty());
    }

    // ---------- relatedTo ----------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void relatedTo_excludesSelfAndRanks() {
        // The source blog's embedding
        when(em.createNativeQuery("SELECT VECTOR_TO_STRING(embedding) FROM t_blog WHERE id = ?1"))
                .thenReturn(singleQuery);
        when(singleQuery.setParameter(eq(1), eq(1L))).thenReturn(singleQuery);
        when(singleQuery.getSingleResult()).thenReturn("[1.0, 0.0]");

        // The scan rows (includes self id=1)
        List rows = Arrays.asList(
                new Object[]{1L, "[1.0, 0.0]"},   // self
                new Object[]{2L, "[0.0, 1.0]"},   // orthogonal
                new Object[]{3L, "[0.9, 0.1]"});  // close
        when(em.createNativeQuery(
                "SELECT id, VECTOR_TO_STRING(embedding) FROM t_blog " +
                "WHERE embedding IS NOT NULL AND published = TRUE"))
                .thenReturn(scanQuery);
        when(scanQuery.getResultList()).thenReturn(rows);

        List<Blog> result = service.relatedTo(1L, 5);

        // Self excluded → 2 left, blog3 closer to self than blog2
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(3L), result.get(0).getId());
        assertEquals(Long.valueOf(2L), result.get(1).getId());
    }

    @Test
    public void relatedTo_noEmbeddingForBlog_returnsEmpty() {
        when(em.createNativeQuery(anyString())).thenReturn(singleQuery);
        when(singleQuery.setParameter(anyInt(), eq(1L))).thenReturn(singleQuery);
        when(singleQuery.getSingleResult())
                .thenThrow(new RuntimeException("no row"));

        assertTrue(service.relatedTo(1L, 5).isEmpty());
    }

    @Test
    public void relatedTo_nullId_returnsEmpty() {
        assertTrue(service.relatedTo(null, 5).isEmpty());
    }

    @Test
    public void relatedTo_topKZero_returnsEmpty() {
        assertTrue(service.relatedTo(1L, 0).isEmpty());
    }
}
