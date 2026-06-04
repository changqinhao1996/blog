package com.cqh.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class PromptGuardrailsTest {

    // --- wrapUntrusted ---

    @Test
    public void wrapUntrusted_addsDelimiters() {
        String wrapped = PromptGuardrails.wrapUntrusted("hello");
        assertTrue(wrapped.contains("<untrusted_content>"));
        assertTrue(wrapped.contains("</untrusted_content>"));
        assertTrue(wrapped.contains("hello"));
    }

    @Test
    public void wrapUntrusted_null_returnsEmpty() {
        assertEquals("", PromptGuardrails.wrapUntrusted(null));
    }

    // --- truncate ---

    @Test
    public void truncate_underLimit_unchanged() {
        assertEquals("abc", PromptGuardrails.truncate("abc", 10));
    }

    @Test
    public void truncate_overLimit_cut() {
        assertEquals("abc", PromptGuardrails.truncate("abcdef", 3));
    }

    @Test
    public void truncate_zeroLimit_unlimited() {
        assertEquals("abcdef", PromptGuardrails.truncate("abcdef", 0));
    }

    @Test
    public void truncate_null_returnsNull() {
        assertNull(PromptGuardrails.truncate(null, 5));
    }

    // --- sanitizeOutput ---

    @Test
    public void sanitizeOutput_null_returnsNull() {
        assertNull(PromptGuardrails.sanitizeOutput(null, 100));
    }

    @Test
    public void sanitizeOutput_stripsHtmlTags() {
        String result = PromptGuardrails.sanitizeOutput("Hello <script>alert(1)</script> world", 100);
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void sanitizeOutput_stripsBareUrls() {
        String result = PromptGuardrails.sanitizeOutput("Visit https://spam.example/path now", 100);
        assertFalse(result.contains("https://"));
        assertFalse(result.contains("spam.example"));
        assertTrue(result.contains("Visit"));
    }

    @Test
    public void sanitizeOutput_keepsMarkdownLinkLabel_dropsTarget() {
        String result = PromptGuardrails.sanitizeOutput("See [the docs](https://x.example) here", 100);
        assertTrue(result.contains("the docs"));
        assertFalse(result.contains("x.example"));
    }

    @Test
    public void sanitizeOutput_removesMarkdownImage() {
        String result = PromptGuardrails.sanitizeOutput("Look ![alt](https://img.example/a.png) ok", 100);
        assertFalse(result.contains("img.example"));
        assertTrue(result.contains("Look"));
        assertTrue(result.contains("ok"));
    }

    @Test
    public void sanitizeOutput_collapsesWhitespace() {
        assertEquals("a b c", PromptGuardrails.sanitizeOutput("a   b\n\nc", 100));
    }

    @Test
    public void sanitizeOutput_capsLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("a");
        String result = PromptGuardrails.sanitizeOutput(sb.toString(), 10);
        assertEquals(10, result.length());
    }
}
