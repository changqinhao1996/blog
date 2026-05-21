package com.cqh.handler;

import com.cqh.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;

public class ControllerExceptionHandlerTest {

    private ControllerExceptionHandler handler;
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        handler = new ControllerExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/test");
    }

    @Test
    public void testHandlesGenericException() throws Exception {
        Exception ex = new RuntimeException("something went wrong");

        ModelAndView mv = handler.exceptionHander(request, ex);

        assertNotNull(mv);
        assertEquals("error/error", mv.getViewName());
        assertNotNull(mv.getModel().get("url"));
        assertSame(ex, mv.getModel().get("exception"));
    }

    @Test(expected = NotFoundException.class)
    public void testRethrowsExceptionWithResponseStatus() throws Exception {
        NotFoundException ex = new NotFoundException("not found");
        handler.exceptionHander(request, ex);
    }

    @Test
    public void testModelContainsUrlAndException() throws Exception {
        request.setRequestURI("/admin/blogs");
        Exception ex = new IllegalArgumentException("bad argument");

        ModelAndView mv = handler.exceptionHander(request, ex);

        assertNotNull(mv.getModel().get("url"));
        assertEquals(ex, mv.getModel().get("exception"));
    }
}
