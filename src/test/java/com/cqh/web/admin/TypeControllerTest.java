package com.cqh.web.admin;

import com.cqh.interceptor.LoginInterceptor;
import com.cqh.po.Type;
import com.cqh.po.User;
import com.cqh.service.TypeService;
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class TypeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TypeService typeService;

    @InjectMocks
    private TypeController typeController;

    private MockHttpSession session;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(typeController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .addInterceptors(new LoginInterceptor())
                .build();

        User user = new User();
        user.setUsername("admin");
        session = new MockHttpSession();
        session.setAttribute("user", user);
    }

    // --- GET /admin/types (list) ---

    @Test
    public void types_returnsListView() throws Exception {
        when(typeService.listType(any(Pageable.class)))
                .thenReturn(new PageImpl<>(new ArrayList<Type>()));

        mockMvc.perform(get("/admin/types").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/types"))
                .andExpect(model().attributeExists("page"));

        verify(typeService).listType(any(Pageable.class));
    }

    @Test
    public void types_withoutSession_redirectsToAdmin() throws Exception {
        mockMvc.perform(get("/admin/types"))
                .andExpect(status().is3xxRedirection());
    }

    // --- GET /admin/types/input (new form) ---

    @Test
    public void input_returnsEmptyForm() throws Exception {
        mockMvc.perform(get("/admin/types/input").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/types-input"))
                .andExpect(model().attributeExists("type"));
    }

    // --- GET /admin/types/{id}/input (edit form) ---

    @Test
    public void editInput_returnsFormWithType() throws Exception {
        Type type = new Type();
        type.setId(1L);
        type.setName("Technology");
        when(typeService.getType(1L)).thenReturn(type);

        mockMvc.perform(get("/admin/types/1/input").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/types-input"))
                .andExpect(model().attribute("type", type));

        verify(typeService).getType(1L);
    }

    // --- POST /admin/types (create) ---

    @Test
    public void post_withValidType_redirectsWithSuccess() throws Exception {
        when(typeService.getTypeByName("Technology")).thenReturn(null);
        Type saved = new Type();
        saved.setId(1L);
        saved.setName("Technology");
        when(typeService.saveType(any(Type.class))).thenReturn(saved);

        mockMvc.perform(post("/admin/types").session(session)
                        .param("name", "Technology"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/types"))
                .andExpect(flash().attributeExists("message"));

        verify(typeService).saveType(any(Type.class));
    }

    @Test
    public void post_withDuplicateName_returnsFormWithError() throws Exception {
        Type existing = new Type();
        existing.setId(1L);
        existing.setName("Technology");
        when(typeService.getTypeByName("Technology")).thenReturn(existing);

        mockMvc.perform(post("/admin/types").session(session)
                        .param("name", "Technology"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/types-input"));

        verify(typeService, never()).saveType(any(Type.class));
    }

    @Test
    public void post_withBlankName_returnsFormWithError() throws Exception {
        mockMvc.perform(post("/admin/types").session(session)
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/types-input"));

        verify(typeService, never()).saveType(any(Type.class));
    }

    @Test
    public void post_saveReturnsNull_setsFailureMessage() throws Exception {
        when(typeService.getTypeByName("NewType")).thenReturn(null);
        when(typeService.saveType(any(Type.class))).thenReturn(null);

        mockMvc.perform(post("/admin/types").session(session)
                        .param("name", "NewType"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/types"))
                .andExpect(flash().attributeExists("message"));

        verify(typeService).saveType(any(Type.class));
    }

    // --- POST /admin/types/{id} (update) ---

    @Test
    public void editPost_withValidType_redirectsWithSuccess() throws Exception {
        when(typeService.getTypeByName("Updated")).thenReturn(null);
        Type updated = new Type();
        updated.setId(1L);
        updated.setName("Updated");
        when(typeService.updateType(eq(1L), any(Type.class))).thenReturn(updated);

        mockMvc.perform(post("/admin/types/1").session(session)
                        .param("name", "Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/types"))
                .andExpect(flash().attributeExists("message"));

        verify(typeService).updateType(eq(1L), any(Type.class));
    }

    @Test
    public void editPost_withDuplicateName_returnsFormWithError() throws Exception {
        Type existing = new Type();
        existing.setId(2L);
        existing.setName("Technology");
        when(typeService.getTypeByName("Technology")).thenReturn(existing);

        mockMvc.perform(post("/admin/types/1").session(session)
                        .param("name", "Technology"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/types-input"));

        verify(typeService, never()).updateType(anyLong(), any(Type.class));
    }

    @Test
    public void editPost_withBlankName_returnsFormWithError() throws Exception {
        mockMvc.perform(post("/admin/types/1").session(session)
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/types-input"));

        verify(typeService, never()).updateType(anyLong(), any(Type.class));
    }

    @Test
    public void editPost_updateReturnsNull_setsFailureMessage() throws Exception {
        when(typeService.getTypeByName("Updated")).thenReturn(null);
        when(typeService.updateType(eq(1L), any(Type.class))).thenReturn(null);

        mockMvc.perform(post("/admin/types/1").session(session)
                        .param("name", "Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/types"))
                .andExpect(flash().attributeExists("message"));
    }

    // --- GET /admin/types/{id}/delete ---

    @Test
    public void delete_redirectsWithSuccess() throws Exception {
        doNothing().when(typeService).deleteType(1L);

        mockMvc.perform(get("/admin/types/1/delete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/types"))
                .andExpect(flash().attributeExists("message"));

        verify(typeService).deleteType(1L);
    }
}
