package com.cqh.service;

import com.cqh.config.AiGuardrailProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class GuardedAiServiceTest {

    private GuardedAiService service;
    private AiServiceImpl delegate;
    private AiGuardrailProperties props;

    @Before
    public void setUp() {
        service = new GuardedAiService();
        delegate = mock(AiServiceImpl.class);
        props = new AiGuardrailProperties();
        ReflectionTestUtils.setField(service, "delegate", delegate);
        ReflectionTestUtils.setField(service, "props", props);
        ReflectionTestUtils.setField(service, "rateLimiter", new RateLimiter());
    }

    @Test
    public void generateSummary_sanitizesOutput() {
        when(delegate.generateSummary(anyString())).thenReturn("See https://spam.example now <b>x</b>");

        String result = service.generateSummary("content");

        assertNotNull(result);
        assertFalse(result.contains("https://"));
        assertFalse(result.contains("<b>"));
    }

    @Test
    public void generateSummary_truncatesInputBeforeDelegating() {
        props.setMaxInputChars(10);
        when(delegate.generateSummary(anyString())).thenReturn("ok");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append("a");

        service.generateSummary(sb.toString());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(delegate).generateSummary(captor.capture());
        assertEquals(10, captor.getValue().length());
    }

    @Test
    public void generateSummary_rateLimited_returnsNull() {
        props.setRateLimitPerMinute(1);
        when(delegate.generateSummary(anyString())).thenReturn("ok");

        assertNotNull(service.generateSummary("a"));
        assertNull(service.generateSummary("b")); // second call in the window is blocked
    }

    @Test
    public void generateSummary_blankAfterSanitize_returnsNull() {
        when(delegate.generateSummary(anyString())).thenReturn("https://only.example");

        assertNull(service.generateSummary("content"));
    }

    @Test
    public void disabled_isPurePassThrough() {
        props.setEnabled(false);
        when(delegate.generateSummary("raw http://x")).thenReturn("kept http://x");

        String result = service.generateSummary("raw http://x");

        assertEquals("kept http://x", result);           // not sanitized
        verify(delegate).generateSummary("raw http://x"); // not truncated
    }

    @Test
    public void suggestTagNames_delegatesAndReturnsList() {
        List<String> existing = Arrays.asList("Java", "Spring");
        when(delegate.suggestTagNames(anyString(), eq(existing))).thenReturn(Arrays.asList("Java"));

        List<String> result = service.suggestTagNames("content", existing);

        assertEquals(1, result.size());
        assertTrue(result.contains("Java"));
    }

    @Test
    public void suggestTagNames_rateLimited_returnsEmptyList() {
        props.setRateLimitPerMinute(1);
        List<String> existing = Arrays.asList("Java");
        when(delegate.suggestTagNames(anyString(), eq(existing))).thenReturn(Arrays.asList("Java"));

        assertFalse(service.suggestTagNames("a", existing).isEmpty());
        assertTrue(service.suggestTagNames("b", existing).isEmpty());
    }
}
