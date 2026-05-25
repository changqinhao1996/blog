package com.cqh.web;

import com.cqh.po.Blog;
import com.cqh.po.Tag;
import com.cqh.service.BlogService;
import com.cqh.service.TagService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class TagShowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TagService tagService;

    @Mock
    private BlogService blogService;

    @InjectMocks
    private TagShowController tagShowController;

    private Tag javaTag;
    private Tag springTag;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(tagShowController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        javaTag = new Tag();
        javaTag.setId(1L);
        javaTag.setName("Java");

        springTag = new Tag();
        springTag.setId(2L);
        springTag.setName("Spring");
    }

    @Test
    public void tags_withSpecificId_returnsTagsView() throws Exception {
        List<Tag> tags = Arrays.asList(javaTag, springTag);
        when(tagService.listTagTop(10000)).thenReturn(tags);

        Page<Blog> page = new PageImpl<>(new ArrayList<Blog>());
        when(blogService.listBlog(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/tags/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tags"))
                .andExpect(model().attributeExists("tags"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("activeTagId", 1L));

        verify(tagService).listTagTop(10000);
        verify(blogService).listBlog(eq(1L), any(Pageable.class));
    }

    @Test
    public void tags_withIdNegativeOne_usesFirstTagId() throws Exception {
        List<Tag> tags = Arrays.asList(javaTag, springTag);
        when(tagService.listTagTop(10000)).thenReturn(tags);

        Page<Blog> page = new PageImpl<>(new ArrayList<Blog>());
        when(blogService.listBlog(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/tags/-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tags"))
                .andExpect(model().attribute("activeTagId", 1L));

        verify(blogService).listBlog(eq(1L), any(Pageable.class));
    }

    @Test
    public void tags_withSecondTagId_returnsCorrectActiveTag() throws Exception {
        List<Tag> tags = Arrays.asList(javaTag, springTag);
        when(tagService.listTagTop(10000)).thenReturn(tags);

        Page<Blog> page = new PageImpl<>(new ArrayList<Blog>());
        when(blogService.listBlog(eq(2L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/tags/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("tags"))
                .andExpect(model().attribute("activeTagId", 2L));

        verify(blogService).listBlog(eq(2L), any(Pageable.class));
    }

    @Test
    public void tags_returnsPagedResults() throws Exception {
        List<Tag> tags = Arrays.asList(javaTag);
        when(tagService.listTagTop(10000)).thenReturn(tags);

        List<Blog> blogs = new ArrayList<>();
        Blog blog = new Blog();
        blog.setId(1L);
        blog.setTitle("Java Blog");
        blogs.add(blog);
        Page<Blog> page = new PageImpl<>(blogs);
        when(blogService.listBlog(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/tags/1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("page"));
    }
}
