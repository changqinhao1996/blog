package com.cqh.web;

import com.cqh.service.BlogService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.cqh.po.Blog;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class ArchiveShowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BlogService blogService;

    @InjectMocks
    private ArchiveShowController archiveShowController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(archiveShowController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void archives_returnsArchivesView() throws Exception {
        Map<String, List<Blog>> archiveMap = new HashMap<>();
        archiveMap.put("2024", new ArrayList<Blog>());
        when(blogService.archiveBlog()).thenReturn(archiveMap);
        when(blogService.countBlog()).thenReturn(5L);

        mockMvc.perform(get("/archives"))
                .andExpect(status().isOk())
                .andExpect(view().name("archives"))
                .andExpect(model().attributeExists("archiveMap"))
                .andExpect(model().attributeExists("blogCount"));

        verify(blogService).archiveBlog();
        verify(blogService).countBlog();
    }

    @Test
    public void archives_emptyArchive_returnsView() throws Exception {
        when(blogService.archiveBlog()).thenReturn(new HashMap<String, List<Blog>>());
        when(blogService.countBlog()).thenReturn(0L);

        mockMvc.perform(get("/archives"))
                .andExpect(status().isOk())
                .andExpect(view().name("archives"))
                .andExpect(model().attribute("blogCount", 0L));
    }

    @Test
    public void archives_multipleYears_setsArchiveMap() throws Exception {
        Map<String, List<Blog>> archiveMap = new HashMap<>();
        List<Blog> blogs2023 = new ArrayList<>();
        Blog b1 = new Blog();
        b1.setId(1L);
        b1.setTitle("Blog 2023");
        blogs2023.add(b1);
        archiveMap.put("2023", blogs2023);

        List<Blog> blogs2024 = new ArrayList<>();
        Blog b2 = new Blog();
        b2.setId(2L);
        b2.setTitle("Blog 2024");
        blogs2024.add(b2);
        archiveMap.put("2024", blogs2024);

        when(blogService.archiveBlog()).thenReturn(archiveMap);
        when(blogService.countBlog()).thenReturn(2L);

        mockMvc.perform(get("/archives"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("blogCount", 2L));

        verify(blogService).archiveBlog();
    }
}
