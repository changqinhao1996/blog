package com.cqh.web;

import com.cqh.po.Blog;
import com.cqh.po.Type;
import com.cqh.service.BlogService;
import com.cqh.service.TypeService;
import com.cqh.vo.BlogQuery;
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
public class TypeShowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TypeService typeService;

    @Mock
    private BlogService blogService;

    @InjectMocks
    private TypeShowController typeShowController;

    private Type techType;
    private Type lifeType;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(typeShowController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        techType = new Type();
        techType.setId(1L);
        techType.setName("Technology");

        lifeType = new Type();
        lifeType.setId(2L);
        lifeType.setName("Life");
    }

    @Test
    public void types_withSpecificId_returnsTypesView() throws Exception {
        List<Type> types = Arrays.asList(techType, lifeType);
        when(typeService.listTypeTop(10000)).thenReturn(types);

        Page<Blog> page = new PageImpl<>(new ArrayList<Blog>());
        when(blogService.listBlog(any(Pageable.class), any(BlogQuery.class))).thenReturn(page);

        mockMvc.perform(get("/types/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("types"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("activeTypeId", 1L));

        verify(typeService).listTypeTop(10000);
        verify(blogService).listBlog(any(Pageable.class), any(BlogQuery.class));
    }

    @Test
    public void types_withIdNegativeOne_usesFirstTypeId() throws Exception {
        List<Type> types = Arrays.asList(techType, lifeType);
        when(typeService.listTypeTop(10000)).thenReturn(types);

        Page<Blog> page = new PageImpl<>(new ArrayList<Blog>());
        when(blogService.listBlog(any(Pageable.class), any(BlogQuery.class))).thenReturn(page);

        mockMvc.perform(get("/types/-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("types"))
                .andExpect(model().attribute("activeTypeId", 1L));

        verify(blogService).listBlog(any(Pageable.class), any(BlogQuery.class));
    }

    @Test
    public void types_withSecondTypeId_returnsCorrectActiveType() throws Exception {
        List<Type> types = Arrays.asList(techType, lifeType);
        when(typeService.listTypeTop(10000)).thenReturn(types);

        Page<Blog> page = new PageImpl<>(new ArrayList<Blog>());
        when(blogService.listBlog(any(Pageable.class), any(BlogQuery.class))).thenReturn(page);

        mockMvc.perform(get("/types/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("types"))
                .andExpect(model().attribute("activeTypeId", 2L));
    }

    @Test
    public void types_setsTypesListInModel() throws Exception {
        List<Type> types = Arrays.asList(techType, lifeType);
        when(typeService.listTypeTop(10000)).thenReturn(types);

        Page<Blog> page = new PageImpl<>(new ArrayList<Blog>());
        when(blogService.listBlog(any(Pageable.class), any(BlogQuery.class))).thenReturn(page);

        mockMvc.perform(get("/types/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("types", types));
    }

    @Test
    public void types_returnsPagedBlogResults() throws Exception {
        List<Type> types = Arrays.asList(techType);
        when(typeService.listTypeTop(10000)).thenReturn(types);

        List<Blog> blogs = new ArrayList<>();
        Blog blog = new Blog();
        blog.setId(1L);
        blog.setTitle("Tech Blog");
        blogs.add(blog);
        Page<Blog> page = new PageImpl<>(blogs);
        when(blogService.listBlog(any(Pageable.class), any(BlogQuery.class))).thenReturn(page);

        mockMvc.perform(get("/types/1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("page"));
    }
}
