package com.cqh.interceptor;

import org.junit.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.Assert.*;

public class WebConfigTest {

    @Test
    public void testWebConfigCreation() {
        WebConfig config = new WebConfig();
        assertNotNull(config);
    }

    @Test
    public void testAddInterceptorsDoesNotThrow() {
        WebConfig config = new WebConfig();
        InterceptorRegistry registry = new InterceptorRegistry();
        config.addInterceptors(registry);
    }
}
