package com.cqh.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.*;

public class LogAspectTest {

    private LogAspect logAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        logAspect = new LogAspect();

        request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.cqh.web.IndexController");
        when(signature.getName()).thenReturn("index");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", "arg2"});
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void doBefore_logsRequestInfo() {
        logAspect.doBefore(joinPoint);

        verify(joinPoint, times(2)).getSignature();
        verify(joinPoint).getArgs();
    }

    @Test
    public void doBefore_withDifferentUrl() {
        request.setRequestURI("/blogs/1");
        request.setRemoteAddr("192.168.1.1");

        when(signature.getDeclaringTypeName()).thenReturn("com.cqh.web.IndexController");
        when(signature.getName()).thenReturn("blog");
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        logAspect.doBefore(joinPoint);

        verify(joinPoint, times(2)).getSignature();
    }

    @Test
    public void doBefore_withNoArgs() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        logAspect.doBefore(joinPoint);

        verify(joinPoint).getArgs();
    }

    @Test
    public void doAfter_executesWithoutError() {
        logAspect.doAfter();
        // doAfter is currently a no-op, just verify it doesn't throw
    }

    @Test
    public void doAfterReturn_logsResult() {
        logAspect.doAfterRuturn("test result");
        // verify it doesn't throw
    }

    @Test
    public void doAfterReturn_withNullResult() {
        logAspect.doAfterRuturn(null);
        // verify it doesn't throw with null
    }

    @Test
    public void doAfterReturn_withObjectResult() {
        Object result = new Object() {
            @Override
            public String toString() {
                return "custom object";
            }
        };
        logAspect.doAfterRuturn(result);
    }

    @Test
    public void doBefore_withNullArgs() {
        when(joinPoint.getArgs()).thenReturn(null);

        logAspect.doBefore(joinPoint);

        verify(joinPoint, times(2)).getSignature();
    }
}
