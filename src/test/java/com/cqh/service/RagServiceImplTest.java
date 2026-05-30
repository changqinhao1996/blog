package com.cqh.service;

import com.cqh.po.Blog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RagServiceImplTest {

    @Mock private VectorSearchService vectorSearchService;
    @Mock private AiService aiService;
    @InjectMocks private RagServiceImpl ragService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private static Blog blog(long id, String title, String content) {
        Blog b = new Blog();
        b.setId(id);
        b.setTitle(title);
        b.setContent(content);
        return b;
    }

    // --- happy path ---

    @Test
    public void ask_happyPath_returnsAnswerAndSources() {
        List<Blog> retrieved = Arrays.asList(
                blog(1L, "Neural Networks", "Backprop and gradient descent..."),
                blog(2L, "Transformers", "Attention mechanism...")
        );
        when(vectorSearchService.semanticSearch("machine learning", 5)).thenReturn(retrieved);
        when(aiService.answerQuestion(eq("machine learning"), eq(retrieved)))
                .thenReturn("Both posts describe deep learning [1][2].");

        RagAnswer ans = ragService.ask("machine learning", 5);

        assertTrue("expected success", ans.isSuccess());
        assertEquals("Both posts describe deep learning [1][2].", ans.getAnswer());
        assertEquals(2, ans.getSources().size());
        assertNull(ans.getError());
    }

    // --- input validation ---

    @Test
    public void ask_nullQuestion_returnsErrorWithoutCallingDownstream() {
        RagAnswer ans = ragService.ask(null, 5);
        assertFalse(ans.isSuccess());
        assertNotNull(ans.getError());
        verifyZeroInteractions(vectorSearchService, aiService);
    }

    @Test
    public void ask_blankQuestion_returnsError() {
        RagAnswer ans = ragService.ask("   ", 5);
        assertFalse(ans.isSuccess());
        verifyZeroInteractions(vectorSearchService, aiService);
    }

    // --- missing vector profile ---

    @Test
    public void ask_vectorServiceNotWired_returnsFriendlyError() {
        // Simulate the default (non-vector) profile where VectorSearchService
        // is not in the context — @Autowired(required=false) leaves it null.
        RagServiceImpl bare = new RagServiceImpl();
        ReflectionTestUtils.setField(bare, "aiService", aiService);
        // vectorSearchService intentionally left null

        RagAnswer ans = bare.ask("anything", 5);

        assertFalse(ans.isSuccess());
        assertNotNull(ans.getError());
        assertTrue("error should mention vector/retrieval/profile",
                ans.getError().toLowerCase().contains("vector")
                || ans.getError().toLowerCase().contains("retrieval")
                || ans.getError().toLowerCase().contains("profile"));
        verifyZeroInteractions(aiService);
    }

    // --- retrieval results ---

    @Test
    public void ask_noSourcesFound_returnsErrorWithoutCallingClaude() {
        when(vectorSearchService.semanticSearch(anyString(), anyInt()))
                .thenReturn(Collections.<Blog>emptyList());

        RagAnswer ans = ragService.ask("obscure question", 5);

        assertFalse(ans.isSuccess());
        assertNotNull(ans.getError());
        verify(aiService, never()).answerQuestion(anyString(), anyList());
    }

    @Test
    public void ask_vectorReturnsNull_returnsError() {
        when(vectorSearchService.semanticSearch(anyString(), anyInt())).thenReturn(null);

        RagAnswer ans = ragService.ask("q", 5);

        assertFalse(ans.isSuccess());
        verify(aiService, never()).answerQuestion(anyString(), anyList());
    }

    @Test
    public void ask_vectorThrows_returnsFriendlyError() {
        when(vectorSearchService.semanticSearch(anyString(), anyInt()))
                .thenThrow(new RuntimeException("DB down"));

        RagAnswer ans = ragService.ask("q", 5);

        assertFalse(ans.isSuccess());
        assertNotNull(ans.getError());
        verify(aiService, never()).answerQuestion(anyString(), anyList());
    }

    // --- LLM responses ---

    @Test
    public void ask_aiReturnsNull_returnsError() {
        when(vectorSearchService.semanticSearch(anyString(), anyInt()))
                .thenReturn(Arrays.asList(blog(1L, "T", "C")));
        when(aiService.answerQuestion(anyString(), anyList())).thenReturn(null);

        RagAnswer ans = ragService.ask("q", 5);

        assertFalse(ans.isSuccess());
        assertNotNull(ans.getError());
    }

    @Test
    public void ask_aiReturnsBlank_returnsError() {
        when(vectorSearchService.semanticSearch(anyString(), anyInt()))
                .thenReturn(Arrays.asList(blog(1L, "T", "C")));
        when(aiService.answerQuestion(anyString(), anyList())).thenReturn("   ");

        RagAnswer ans = ragService.ask("q", 5);

        assertFalse(ans.isSuccess());
    }

    @Test
    public void ask_aiThrows_returnsFriendlyError() {
        when(vectorSearchService.semanticSearch(anyString(), anyInt()))
                .thenReturn(Arrays.asList(blog(1L, "T", "C")));
        when(aiService.answerQuestion(anyString(), anyList()))
                .thenThrow(new RuntimeException("Claude 500"));

        RagAnswer ans = ragService.ask("q", 5);

        assertFalse(ans.isSuccess());
        assertNotNull(ans.getError());
    }

    // --- topK clamping ---

    @Test
    public void ask_zeroTopK_usesDefault() {
        when(vectorSearchService.semanticSearch("q", RagServiceImpl.DEFAULT_TOP_K))
                .thenReturn(Arrays.asList(blog(1L, "T", "C")));
        when(aiService.answerQuestion(anyString(), anyList())).thenReturn("ok");

        ragService.ask("q", 0);

        verify(vectorSearchService).semanticSearch("q", RagServiceImpl.DEFAULT_TOP_K);
    }

    @Test
    public void ask_negativeTopK_usesDefault() {
        when(vectorSearchService.semanticSearch("q", RagServiceImpl.DEFAULT_TOP_K))
                .thenReturn(Arrays.asList(blog(1L, "T", "C")));
        when(aiService.answerQuestion(anyString(), anyList())).thenReturn("ok");

        ragService.ask("q", -7);

        verify(vectorSearchService).semanticSearch("q", RagServiceImpl.DEFAULT_TOP_K);
    }

    @Test
    public void ask_largeTopK_clampedToMax() {
        when(vectorSearchService.semanticSearch("q", RagServiceImpl.MAX_TOP_K))
                .thenReturn(Arrays.asList(blog(1L, "T", "C")));
        when(aiService.answerQuestion(anyString(), anyList())).thenReturn("ok");

        ragService.ask("q", 1000);

        verify(vectorSearchService).semanticSearch("q", RagServiceImpl.MAX_TOP_K);
    }

    // --- DTO behavior sanity ---

    @Test
    public void ragAnswer_successFactory_hasExpectedFields() {
        Blog b = blog(1L, "T", "C");
        RagAnswer ans = RagAnswer.success("answer", Arrays.asList(b));
        assertTrue(ans.isSuccess());
        assertEquals("answer", ans.getAnswer());
        assertEquals(1, ans.getSources().size());
        assertNull(ans.getError());
    }

    @Test
    public void ragAnswer_errorFactory_hasExpectedFields() {
        RagAnswer ans = RagAnswer.error("nope");
        assertFalse(ans.isSuccess());
        assertEquals("nope", ans.getError());
        assertNull(ans.getAnswer());
        assertTrue(ans.getSources().isEmpty());
    }
}
