package com.cqh.web.admin;

import com.cqh.interceptor.LoginInterceptor;
import com.cqh.po.Tag;
import com.cqh.po.User;
import com.cqh.service.TagService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class TagControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController tagController;

    private MockHttpSession session;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(tagController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .addInterceptors(new LoginInterceptor())
                .build();

        User user = new User();
        user.setUsername("admin");
        session = new MockHttpSession();
        session.setAttribute("user", user);
    }

    @Test
    public void tags_returnsListView() throws Exception {
        when(tagService.listTag(any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        mockMvc.perform(get("/admin/tags").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tags"))
                .andExpect(model().attributeExists("page"));
    }

    @Test
    public void input_returnsEmptyForm() throws Exception {
        mockMvc.perform(get("/admin/tags/input").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tags-input"))
                .andExpect(model().attributeExists("tag"));
    }

    @Test
    public void editInput_returnsFormWithTag() throws Exception {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("Java");
        when(tagService.getTag(1L)).thenReturn(tag);

        mockMvc.perform(get("/admin/tags/1/input").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tags-input"))
                .andExpect(model().attribute("tag", tag));
    }

    @Test
    public void post_withValidTag_redirectsWithSuccess() throws Exception {
        when(tagService.getTagByName("Spring")).thenReturn(null);
        Tag saved = new Tag();
        saved.setId(1L);
        saved.setName("Spring");
        when(tagService.saveTag(any(Tag.class))).thenReturn(saved);

        mockMvc.perform(post("/admin/tags").session(session)
                        .param("name", "Spring"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    public void post_withDuplicateName_returnsFormWithError() throws Exception {
        Tag existing = new Tag();
        existing.setId(1L);
        existing.setName("Java");
        when(tagService.getTagByName("Java")).thenReturn(existing);

        mockMvc.perform(post("/admin/tags").session(session)
                        .param("name", "Java"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tags-input"));
    }

    @Test
    public void delete_redirectsWithSuccess() throws Exception {
        doNothing().when(tagService).deleteTag(1L);

        mockMvc.perform(get("/admin/tags/1/delete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    public void tags_withoutSession_redirectsToAdmin() throws Exception {
        mockMvc.perform(get("/admin/tags"))
                .andExpect(status().is3xxRedirection());
    }
}
