package com.cqh.interceptor;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.Assert.*;

public class LoginInterceptorTest {

    private LoginInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() {
        interceptor = new LoginInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testPreHandleWithLoggedInUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "admin");
        request.setSession(session);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue("Should allow access when user is in session", result);
    }

    @Test
    public void testPreHandleWithNoUser() throws Exception {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse("Should block access when no user in session", result);
        assertEquals("/admin", response.getRedirectedUrl());
    }

    @Test
    public void testPreHandleRedirectsToAdmin() throws Exception {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(302, response.getStatus());
    }
}
