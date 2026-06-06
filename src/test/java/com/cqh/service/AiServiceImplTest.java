package com.cqh.service;

import com.cqh.po.Blog;
import com.cqh.service.llm.LlmProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Refactored on 0.0.5: tests the orchestration layer (prompt assembly +
 * provider fallback chain). Vendor-specific HTTP behaviour is covered by
 * OpenAiProviderTest and ClaudeProviderTest under com.cqh.service.llm.
 */
public class AiServiceImplTest {

    private AiServiceImpl aiService;
    private LlmProvider openAi;       // primary (mocked)
    private LlmProvider claude;       // fallback (mocked)

    @Before
    public void setUp() {
        aiService = new AiServiceImpl();
        ReflectionTestUtils.setField(aiService, "maxTokens", 300);
        ReflectionTestUtils.setField(aiService, "ragMaxTokens", 800);

        openAi = mock(LlmProvider.class);
        when(openAi.name()).thenReturn("OpenAI");
        when(openAi.isConfigured()).thenReturn(true);

        claude = mock(LlmProvider.class);
        when(claude.name()).thenReturn("Claude");
        when(claude.isConfigured()).thenReturn(true);

        // List order = @Order(1) OpenAI, @Order(2) Claude
        ReflectionTestUtils.setField(aiService, "providers", Arrays.asList(openAi, claude));
    }

    // ──── generateSummary ────

    @Test
    public void generateSummary_openAiSucceeds_returnsItsResponse() {
        when(openAi.complete(anyString(), eq(300))).thenReturn("Summary from OpenAI");

        String result = aiService.generateSummary("blog body");

        assertEquals("Summary from OpenAI", result);
        verify(openAi).complete(anyString(), eq(300));
        verify(claude, never()).complete(anyString(), anyInt());
    }

    @Test
    public void generateSummary_openAiReturnsNull_fallsBackToClaude() {
        when(openAi.complete(anyString(), eq(300))).thenReturn(null);
        when(claude.complete(anyString(), eq(300))).thenReturn("Summary from Claude");

        String result = aiService.generateSummary("blog body");

        assertEquals("Summary from Claude", result);
        verify(openAi).complete(anyString(), eq(300));
        verify(claude).complete(anyString(), eq(300));
    }

    @Test
    public void generateSummary_openAiThrows_fallsBackToClaude() {
        when(openAi.complete(anyString(), eq(300)))
                .thenThrow(new RuntimeException("network timeout"));
        when(claude.complete(anyString(), eq(300))).thenReturn("Summary from Claude");

        String result = aiService.generateSummary("blog body");

        assertEquals("Summary from Claude", result);
    }

    @Test
    public void generateSummary_bothProvidersFail_returnsNull() {
        when(openAi.complete(anyString(), eq(300))).thenReturn(null);
        when(claude.complete(anyString(), eq(300))).thenReturn(null);

        assertNull(aiService.generateSummary("blog body"));
    }

    @Test
    public void generateSummary_openAiNotConfigured_skipsToClaude() {
        when(openAi.isConfigured()).thenReturn(false);
        when(claude.complete(anyString(), eq(300))).thenReturn("Summary from Claude");

        String result = aiService.generateSummary("blog body");

        assertEquals("Summary from Claude", result);
        verify(openAi, never()).complete(anyString(), anyInt());
        verify(claude).complete(anyString(), eq(300));
    }

    @Test
    public void generateSummary_noProviderConfigured_returnsNull() {
        when(openAi.isConfigured()).thenReturn(false);
        when(claude.isConfigured()).thenReturn(false);

        assertNull(aiService.generateSummary("blog body"));
        verify(openAi, never()).complete(anyString(), anyInt());
        verify(claude, never()).complete(anyString(), anyInt());
    }

    @Test
    public void generateSummary_nullContent_returnsNull() {
        assertNull(aiService.generateSummary(null));
        verify(openAi, never()).complete(anyString(), anyInt());
    }

    @Test
    public void generateSummary_blankContent_returnsNull() {
        assertNull(aiService.generateSummary("   "));
        verify(openAi, never()).complete(anyString(), anyInt());
    }

    @Test
    public void generateSummary_promptContainsContent() {
        when(openAi.complete(anyString(), eq(300))).thenReturn("ok");
        ArgumentCaptor<String> promptCap = ArgumentCaptor.forClass(String.class);

        aiService.generateSummary("Some unique sentinel content xyzzy");

        verify(openAi).complete(promptCap.capture(), eq(300));
        assertTrue(promptCap.getValue().contains("Some unique sentinel content xyzzy"));
        assertTrue(promptCap.getValue().contains("Summarize"));
    }

    // ──── suggestTagNames ────

    @Test
    public void suggestTagNames_openAiSucceeds_returnsParsedAndFilteredTags() {
        when(openAi.complete(anyString(), eq(300))).thenReturn("Java, Unknown, Spring");

        List<String> existing = Arrays.asList("Java", "Spring", "Python");
        List<String> result = aiService.suggestTagNames("blog content", existing);

        assertEquals(2, result.size());
        assertTrue(result.contains("Java"));
        assertTrue(result.contains("Spring"));
        assertFalse(result.contains("Unknown"));
        verify(claude, never()).complete(anyString(), anyInt());
    }

    @Test
    public void suggestTagNames_openAiNull_fallsBackToClaude() {
        when(openAi.complete(anyString(), eq(300))).thenReturn(null);
        when(claude.complete(anyString(), eq(300))).thenReturn("Java");

        List<String> result = aiService.suggestTagNames("blog content",
                Arrays.asList("Java", "Spring"));

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0));
    }

    @Test
    public void suggestTagNames_emptyExistingTags_returnsEmpty() {
        List<String> result = aiService.suggestTagNames("blog", Collections.<String>emptyList());
        assertTrue(result.isEmpty());
        verify(openAi, never()).complete(anyString(), anyInt());
    }

    @Test
    public void suggestTagNames_nullExistingTags_returnsEmpty() {
        List<String> result = aiService.suggestTagNames("blog", null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void suggestTagNames_nullContent_returnsEmpty() {
        List<String> result = aiService.suggestTagNames(null, Arrays.asList("Java"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void suggestTagNames_noProvider_returnsEmpty() {
        when(openAi.isConfigured()).thenReturn(false);
        when(claude.isConfigured()).thenReturn(false);

        List<String> result = aiService.suggestTagNames("blog", Arrays.asList("Java"));
        assertTrue(result.isEmpty());
    }

    // ──── answerQuestion (RAG) ────

    @Test
    public void answerQuestion_usesRagMaxTokens_notDefault() {
        when(openAi.complete(anyString(), eq(800))).thenReturn("Answer text [1].");
        List<Blog> ctx = Arrays.asList(blog(1L, "T", "C"));

        String result = aiService.answerQuestion("q", ctx);

        assertEquals("Answer text [1].", result);
        verify(openAi).complete(anyString(), eq(800));   // RAG max-tokens
        verify(openAi, never()).complete(anyString(), eq(300));  // not summary max-tokens
    }

    @Test
    public void answerQuestion_promptContainsAllSourcesAndQuestion() {
        when(openAi.complete(anyString(), eq(800))).thenReturn("ok");
        ArgumentCaptor<String> promptCap = ArgumentCaptor.forClass(String.class);

        aiService.answerQuestion("what is X",
                Arrays.asList(blog(1L, "Title A", "content A"),
                              blog(2L, "Title B", "content B")));

        verify(openAi).complete(promptCap.capture(), eq(800));
        String prompt = promptCap.getValue();
        assertTrue(prompt.contains("--- Source [1]: Title A ---"));
        assertTrue(prompt.contains("--- Source [2]: Title B ---"));
        assertTrue(prompt.contains("Question: what is X"));
        assertTrue(prompt.contains("ONLY"));
    }

    @Test
    public void answerQuestion_openAiFails_fallsBackToClaudeWithSamePrompt() {
        when(openAi.complete(anyString(), eq(800))).thenReturn(null);
        when(claude.complete(anyString(), eq(800))).thenReturn("Claude answer [1].");

        String result = aiService.answerQuestion("q",
                Arrays.asList(blog(1L, "T", "C")));

        assertEquals("Claude answer [1].", result);
    }

    @Test
    public void answerQuestion_nullQuestion_returnsNull() {
        assertNull(aiService.answerQuestion(null, Arrays.asList(blog(1L, "T", "C"))));
        verify(openAi, never()).complete(anyString(), anyInt());
    }

    @Test
    public void answerQuestion_blankQuestion_returnsNull() {
        assertNull(aiService.answerQuestion("   ", Arrays.asList(blog(1L, "T", "C"))));
    }

    @Test
    public void answerQuestion_nullContext_returnsNull() {
        assertNull(aiService.answerQuestion("q", null));
    }

    @Test
    public void answerQuestion_emptyContext_returnsNull() {
        assertNull(aiService.answerQuestion("q", Collections.<Blog>emptyList()));
    }

    @Test
    public void answerQuestion_noProvider_returnsNull() {
        when(openAi.isConfigured()).thenReturn(false);
        when(claude.isConfigured()).thenReturn(false);
        assertNull(aiService.answerQuestion("q", Arrays.asList(blog(1L, "T", "C"))));
    }

    @Test
    public void answerQuestion_handlesNullTitleAndContent() {
        when(openAi.complete(anyString(), eq(800))).thenReturn("ok");
        Blog b = new Blog(); b.setId(99L);   // title + content null

        String result = aiService.answerQuestion("q", Arrays.asList(b));

        assertEquals("ok", result);
    }

    // ──── callLlm direct (fallback chain) ────

    @Test
    public void callLlm_tries_openAi_then_claude_in_order() {
        when(openAi.complete(anyString(), anyInt())).thenReturn(null);
        when(claude.complete(anyString(), anyInt())).thenReturn("from-claude");

        String result = aiService.callLlm("prompt", 300);

        assertEquals("from-claude", result);
        // Verify order: OpenAI called first, then Claude
        org.mockito.InOrder inOrder = inOrder(openAi, claude);
        inOrder.verify(openAi).complete("prompt", 300);
        inOrder.verify(claude).complete("prompt", 300);
    }

    @Test
    public void callLlm_stopsAfterFirstSuccess() {
        when(openAi.complete(anyString(), anyInt())).thenReturn("from-openai");

        String result = aiService.callLlm("prompt", 300);

        assertEquals("from-openai", result);
        verify(claude, never()).complete(anyString(), anyInt());
    }

    @Test
    public void callLlm_treatsBlankResponseAsFailure_andFallsBack() {
        when(openAi.complete(anyString(), anyInt())).thenReturn("   ");
        when(claude.complete(anyString(), anyInt())).thenReturn("real answer");

        String result = aiService.callLlm("prompt", 300);

        assertEquals("real answer", result);
    }

    @Test
    public void callLlm_nullProvidersList_returnsNull() {
        ReflectionTestUtils.setField(aiService, "providers", null);
        assertNull(aiService.callLlm("prompt", 300));
    }

    @Test
    public void callLlm_emptyProvidersList_returnsNull() {
        ReflectionTestUtils.setField(aiService, "providers",
                Collections.<LlmProvider>emptyList());
        assertNull(aiService.callLlm("prompt", 300));
    }

    // ──── isConfigured ────

    @Test
    public void isConfigured_oneProviderConfigured_returnsTrue() {
        when(openAi.isConfigured()).thenReturn(false);
        when(claude.isConfigured()).thenReturn(true);
        assertTrue(aiService.isConfigured());
    }

    @Test
    public void isConfigured_noProviderConfigured_returnsFalse() {
        when(openAi.isConfigured()).thenReturn(false);
        when(claude.isConfigured()).thenReturn(false);
        assertFalse(aiService.isConfigured());
    }

    @Test
    public void isConfigured_nullProvidersList_returnsFalse() {
        ReflectionTestUtils.setField(aiService, "providers", null);
        assertFalse(aiService.isConfigured());
    }

    // ──── helper ────
    private static Blog blog(long id, String title, String content) {
        Blog b = new Blog();
        b.setId(id);
        b.setTitle(title);
        b.setContent(content);
        return b;
    }
}
