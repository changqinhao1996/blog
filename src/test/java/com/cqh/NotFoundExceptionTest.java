package com.cqh;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.Assert.*;

public class NotFoundExceptionTest {

    @Test
    public void testDefaultConstructor() {
        NotFoundException e = new NotFoundException();
        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    public void testMessageConstructor() {
        NotFoundException e = new NotFoundException("blog not found");
        assertEquals("blog not found", e.getMessage());
        assertNull(e.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("root cause");
        NotFoundException e = new NotFoundException("blog not found", cause);
        assertEquals("blog not found", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    public void testIsRuntimeException() {
        NotFoundException e = new NotFoundException();
        assertTrue(e instanceof RuntimeException);
    }

    @Test
    public void testResponseStatusAnnotation() {
        ResponseStatus annotation = NotFoundException.class.getAnnotation(ResponseStatus.class);
        assertNotNull("Should have @ResponseStatus annotation", annotation);
        assertEquals(HttpStatus.NOT_FOUND, annotation.value());
    }
}
