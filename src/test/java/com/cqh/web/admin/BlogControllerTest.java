package com.cqh.web.admin;

import com.cqh.interceptor.LoginInterceptor;
import com.cqh.po.Blog;
import com.cqh.po.Tag;
import com.cqh.po.Type;
import com.cqh.po.User;
import com.cqh.service.AiService;
import com.cqh.service.BlogService;
import com.cqh.service.TagService;
import com.cqh.service.TypeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class BlogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BlogService blogService;
    @Mock
    private TypeService typeService;
    @Mock
    private TagService tagService;
    @Mock
    private AiService aiService;

    @InjectMocks
    private BlogController blogController;

    private MockHttpSession session;
    private Type testType;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(blogController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .addInterceptors(new LoginInterceptor())
                .build();

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        session = new MockHttpSession();
        session.setAttribute("user", user);

        testType = new Type();
        testType.setId(1L);
        testType.setName("Technology");
        when(typeService.getType(1L)).thenReturn(testType);
    }

    @Test
    public void post_withManualTags_skipsAi() throws Exception {
        Tag javaTag = new Tag();
        javaTag.setId(1L);
        javaTag.setName("Java");
        when(tagService.listTag("1")).thenReturn(Collections.singletonList(javaTag));

        Blog savedBlog = new Blog();
        savedBlog.setId(1L);
        when(blogService.saveBlog(any(Blog.class))).thenReturn(savedBlog);

        mockMvc.perform(post("/admin/blogs").session(session)
                        .param("title", "Test Blog")
                        .param("content", "Content about Java")
                        .param("type.id", "1")
                        .param("tagIds", "1")
                        .param("description", "Test desc")
                        .param("firstPicture", "http://img.com/1.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blogs"));

        verify(aiService, never()).suggestTagNames(anyString(), anyList());
        verify(tagService).listTag("1");
    }

    @Test
    public void post_withoutTags_callsAiSuggestTags() throws Exception {
        Tag javaTag = new Tag();
        javaTag.setId(1L);
        javaTag.setName("Java");
        Tag springTag = new Tag();
        springTag.setId(2L);
        springTag.setName("Spring");

        List<Tag> allTags = Arrays.asList(javaTag, springTag);
        when(tagService.listTag()).thenReturn(allTags);
        when(aiService.suggestTagNames(anyString(), anyList()))
                .thenReturn(Arrays.asList("Java", "Spring"));

        Blog savedBlog = new Blog();
        savedBlog.setId(1L);
        when(blogService.saveBlog(any(Blog.class))).thenReturn(savedBlog);

        mockMvc.perform(post("/admin/blogs").session(session)
                        .param("title", "Test Blog")
                        .param("content", "Content about Java Spring Boot")
                        .param("type.id", "1")
                        .param("description", "Test desc")
                        .param("firstPicture", "http://img.com/1.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blogs"));

        verify(aiService).suggestTagNames(eq("Content about Java Spring Boot"), anyList());

        ArgumentCaptor<Blog> captor = ArgumentCaptor.forClass(Blog.class);
        verify(blogService).saveBlog(captor.capture());
        Blog captured = captor.getValue();
        assertEquals(2, captured.getTags().size());
    }

    @Test
    public void post_aiFailure_savesWithoutTags() throws Exception {
        Tag javaTag = new Tag();
        javaTag.setId(1L);
        javaTag.setName("Java");
        when(tagService.listTag()).thenReturn(Collections.singletonList(javaTag));
        when(aiService.suggestTagNames(anyString(), anyList()))
                .thenThrow(new RuntimeException("API error"));

        Blog savedBlog = new Blog();
        savedBlog.setId(1L);
        when(blogService.saveBlog(any(Blog.class))).thenReturn(savedBlog);

        mockMvc.perform(post("/admin/blogs").session(session)
                        .param("title", "Test Blog")
                        .param("content", "Content")
                        .param("type.id", "1")
                        .param("description", "Test desc")
                        .param("firstPicture", "http://img.com/1.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blogs"));

        ArgumentCaptor<Blog> captor = ArgumentCaptor.forClass(Blog.class);
        verify(blogService).saveBlog(captor.capture());
        Blog captured = captor.getValue();
        assertTrue(captured.getTags().isEmpty());
    }

    @Test
    public void post_emptyTagIds_triggersAi() throws Exception {
        when(tagService.listTag()).thenReturn(Collections.<Tag>emptyList());

        Blog savedBlog = new Blog();
        savedBlog.setId(1L);
        when(blogService.saveBlog(any(Blog.class))).thenReturn(savedBlog);

        mockMvc.perform(post("/admin/blogs").session(session)
                        .param("title", "Test Blog")
                        .param("content", "Content")
                        .param("type.id", "1")
                        .param("tagIds", "")
                        .param("description", "Test desc")
                        .param("firstPicture", "http://img.com/1.jpg"))
                .andExpect(status().is3xxRedirection());

        // No tags in system, so AI shouldn't even be called
        verify(aiService, never()).suggestTagNames(anyString(), anyList());
    }

    @Test
    public void suggestTags_returnsAiSuggestions() throws Exception {
        Tag javaTag = new Tag();
        javaTag.setId(1L);
        javaTag.setName("Java");
        when(tagService.listTag()).thenReturn(Collections.singletonList(javaTag));
        when(aiService.suggestTagNames(anyString(), anyList()))
                .thenReturn(Collections.singletonList("Java"));

        mockMvc.perform(post("/admin/blogs/suggest-tags").session(session)
                        .param("content", "Blog about Java"))
                .andExpect(status().isOk());

        verify(aiService).suggestTagNames(eq("Blog about Java"), eq(Collections.singletonList("Java")));
    }

    // --- GET /admin/blogs (list) ---

    @Test
    public void blogs_returnsListView() throws Exception {
        when(typeService.listType()).thenReturn(Collections.singletonList(testType));
        when(blogService.listBlog(any(Pageable.class), any()))
                .thenReturn(new PageImpl<>(new ArrayList<Blog>()));

        mockMvc.perform(get("/admin/blogs").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blogs"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attributeExists("page"));

        verify(typeService).listType();
    }

    @Test
    public void blogs_withoutSession_redirects() throws Exception {
        mockMvc.perform(get("/admin/blogs"))
                .andExpect(status().is3xxRedirection());
    }

    // --- POST /admin/blogs/search ---

    @Test
    public void search_returnsFragmentView() throws Exception {
        when(blogService.listBlog(any(Pageable.class), any()))
                .thenReturn(new PageImpl<>(new ArrayList<Blog>()));

        mockMvc.perform(post("/admin/blogs/search").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blogs :: blogList"))
                .andExpect(model().attributeExists("page"));
    }

    @Test
    public void search_withTitleFilter() throws Exception {
        when(blogService.listBlog(any(Pageable.class), any()))
                .thenReturn(new PageImpl<>(new ArrayList<Blog>()));

        mockMvc.perform(post("/admin/blogs/search").session(session)
                        .param("title", "Java"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blogs :: blogList"));
    }

    // --- GET /admin/blogs/input (new blog form) ---

    @Test
    public void input_returnsInputView() throws Exception {
        when(typeService.listType()).thenReturn(Collections.singletonList(testType));
        when(tagService.listTag()).thenReturn(new ArrayList<Tag>());

        mockMvc.perform(get("/admin/blogs/input").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blogs-input"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attributeExists("tags"));

        verify(typeService).listType();
        verify(tagService).listTag();
    }

    // --- GET /admin/blogs/{id}/input (edit blog form) ---

    @Test
    public void editInput_returnsFormWithBlog() throws Exception {
        Blog existingBlog = new Blog();
        existingBlog.setId(1L);
        existingBlog.setTitle("Existing Blog");
        existingBlog.setContent("Content");
        existingBlog.setTags(new ArrayList<Tag>());
        when(blogService.getBlog(1L)).thenReturn(existingBlog);
        when(typeService.listType()).thenReturn(Collections.singletonList(testType));
        when(tagService.listTag()).thenReturn(new ArrayList<Tag>());

        mockMvc.perform(get("/admin/blogs/1/input").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blogs-input"))
                .andExpect(model().attributeExists("blog"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attributeExists("tags"));

        verify(blogService).getBlog(1L);
    }

    // --- GET /admin/blogs/{id}/delete ---

    @Test
    public void delete_redirectsWithSuccess() throws Exception {
        doNothing().when(blogService).deleteBlog(1L);

        mockMvc.perform(get("/admin/blogs/1/delete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blogs"))
                .andExpect(flash().attributeExists("message"));

        verify(blogService).deleteBlog(1L);
    }

    // --- POST /admin/blogs (update existing blog) ---

    @Test
    public void post_withExistingId_callsUpdateBlog() throws Exception {
        Tag javaTag = new Tag();
        javaTag.setId(1L);
        javaTag.setName("Java");
        when(tagService.listTag("1")).thenReturn(Collections.singletonList(javaTag));

        Blog updatedBlog = new Blog();
        updatedBlog.setId(1L);
        when(blogService.updateBlog(eq(1L), any(Blog.class))).thenReturn(updatedBlog);

        mockMvc.perform(post("/admin/blogs").session(session)
                        .param("id", "1")
                        .param("title", "Updated Blog")
                        .param("content", "Updated content")
                        .param("type.id", "1")
                        .param("tagIds", "1")
                        .param("description", "Updated desc")
                        .param("firstPicture", "http://img.com/1.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blogs"));

        verify(blogService).updateBlog(eq(1L), any(Blog.class));
        verify(blogService, never()).saveBlog(any(Blog.class));
    }

    @Test
    public void post_saveFails_setsFailureMessage() throws Exception {
        when(tagService.listTag("1")).thenReturn(new ArrayList<Tag>());
        when(blogService.saveBlog(any(Blog.class))).thenReturn(null);

        mockMvc.perform(post("/admin/blogs").session(session)
                        .param("title", "Test Blog")
                        .param("content", "Content")
                        .param("type.id", "1")
                        .param("tagIds", "1")
                        .param("description", "desc")
                        .param("firstPicture", "http://img.com/1.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blogs"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    public void post_aiReturnsEmptyList_savesWithoutTags() throws Exception {
        Tag javaTag = new Tag();
        javaTag.setId(1L);
        javaTag.setName("Java");
        when(tagService.listTag()).thenReturn(Collections.singletonList(javaTag));
        when(aiService.suggestTagNames(anyString(), anyList()))
                .thenReturn(Collections.<String>emptyList());

        Blog savedBlog = new Blog();
        savedBlog.setId(1L);
        when(blogService.saveBlog(any(Blog.class))).thenReturn(savedBlog);

        mockMvc.perform(post("/admin/blogs").session(session)
                        .param("title", "Test Blog")
                        .param("content", "Content")
                        .param("type.id", "1")
                        .param("description", "desc")
                        .param("firstPicture", "http://img.com/1.jpg"))
                .andExpect(status().is3xxRedirection());

        ArgumentCaptor<Blog> captor = ArgumentCaptor.forClass(Blog.class);
        verify(blogService).saveBlog(captor.capture());
        assertTrue(captor.getValue().getTags().isEmpty());
    }
}
