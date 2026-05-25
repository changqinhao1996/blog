package com.cqh.web;

import com.cqh.po.Blog;
import com.cqh.po.Tag;
import com.cqh.po.Type;
import com.cqh.service.BlogService;
import com.cqh.service.TagService;
import com.cqh.service.TypeService;
import com.cqh.service.VectorSearchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class IndexControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BlogService blogService;

    @Mock
    private TypeService typeService;

    @Mock
    private TagService tagService;

    @Mock
    private VectorSearchService vectorSearchService;

    @InjectMocks
    private IndexController indexController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(indexController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    public void index_returnsPageWithModelAttributes() throws Exception {
        when(blogService.listBlog(any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));
        when(typeService.listTypeTop(anyInt()))
                .thenReturn(Collections.<Type>emptyList());
        when(tagService.listTagTop(anyInt()))
                .thenReturn(Collections.<Tag>emptyList());
        when(blogService.listRecommendBlogTop(anyInt()))
                .thenReturn(Collections.<Blog>emptyList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attributeExists("tags"))
                .andExpect(model().attributeExists("recommendBlogs"));
    }

    @Test
    public void search_noSemanticHits_fallsBackToKeyword() throws Exception {
        // Vector search returns empty → controller falls back to keyword listBlog
        when(vectorSearchService.semanticSearch(anyString(), anyInt()))
                .thenReturn(Collections.<Blog>emptyList());
        when(blogService.listBlog(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        mockMvc.perform(post("/search").param("query", "java"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("query", "java"));

        verify(blogService).listBlog(anyString(), any(Pageable.class));
    }

    @Test
    public void search_withSemanticHits_usesVectorResults() throws Exception {
        Blog hit1 = new Blog(); hit1.setId(1L); hit1.setTitle("Semantic Match");
        Blog hit2 = new Blog(); hit2.setId(2L); hit2.setTitle("Another Match");
        when(vectorSearchService.semanticSearch(eq("ml"), anyInt()))
                .thenReturn(Arrays.asList(hit1, hit2));

        mockMvc.perform(post("/search").param("query", "ml"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("query", "ml"));

        verify(vectorSearchService).semanticSearch(eq("ml"), anyInt());
        // Keyword fallback should NOT have been invoked
        verify(blogService, org.mockito.Mockito.never())
                .listBlog(anyString(), any(Pageable.class));
    }

    @Test
    public void blog_includesRelatedArticles() throws Exception {
        Blog blog = new Blog();
        blog.setId(1L);
        blog.setTitle("Test Blog");
        when(blogService.getAndConvert(1L)).thenReturn(blog);

        Blog related = new Blog();
        related.setId(2L);
        related.setTitle("Related");
        when(vectorSearchService.relatedTo(eq(1L), anyInt()))
                .thenReturn(Collections.singletonList(related));

        mockMvc.perform(get("/blog/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attributeExists("related"));

        verify(vectorSearchService).relatedTo(eq(1L), anyInt());
    }

    @Test
    public void footerNewblogs_returnsFragment() throws Exception {
        when(blogService.listRecommendBlogTop(anyInt()))
                .thenReturn(Collections.<Blog>emptyList());

        mockMvc.perform(get("/footer/newblog"))
                .andExpect(status().isOk())
                .andExpect(view().name("_fragments :: newblogList"))
                .andExpect(model().attributeExists("newblogs"));
    }
}
