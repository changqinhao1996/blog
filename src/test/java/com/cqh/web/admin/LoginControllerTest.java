package com.cqh.web.admin;

import com.cqh.po.User;
import com.cqh.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class LoginControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private LoginController loginController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(loginController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void loginPage_returnsLoginView() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    public void login_withValidCredentials_setsSessionAndReturnsIndex() throws Exception {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("secret");
        user.setAvatar("/images/admin.png");
        when(userService.checkUser("admin", "secret")).thenReturn(user);

        MvcResult result = mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"))
                .andReturn();

        User sessionUser = (User) result.getRequest().getSession().getAttribute("user");
        assertNotNull(sessionUser);
        assertNull(sessionUser.getPassword());
    }

    @Test
    public void login_withInvalidCredentials_redirectsWithMessage() throws Exception {
        when(userService.checkUser("admin", "wrong")).thenReturn(null);

        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    public void logout_clearsSessionAndRedirects() throws Exception {
        User user = new User();
        user.setUsername("admin");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);

        MvcResult result = mockMvc.perform(get("/admin/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();

        assertNull(result.getRequest().getSession().getAttribute("user"));
    }
}
