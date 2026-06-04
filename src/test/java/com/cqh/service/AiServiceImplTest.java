package com.cqh.service;

import com.cqh.config.AiGuardrailProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class AiServiceImplTest {

    private AiServiceImpl aiService;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        aiService = new AiServiceImpl();
        ReflectionTestUtils.setField(aiService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "model", "claude-sonnet-4-6");
        ReflectionTestUtils.setField(aiService, "maxTokens", 300);

        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(aiService, "restTemplate", restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    // --- generateSummary tests ---

    @Test
    public void generateSummary_returnsText() throws Exception {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"This is a summary.\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = aiService.generateSummary("Some blog content about Java.");

        assertNotNull(result);
        assertEquals("This is a summary.", result);
        mockServer.verify();
    }

    @Test
    public void generateSummary_nullContent_returnsNull() {
        String result = aiService.generateSummary(null);
        assertNull(result);
    }

    @Test
    public void generateSummary_emptyContent_returnsNull() {
        String result = aiService.generateSummary("   ");
        assertNull(result);
    }

    @Test
    public void generateSummary_apiError_returnsNull() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());

        String result = aiService.generateSummary("Some content");

        assertNull(result);
    }

    @Test
    public void generateSummary_noApiKey_returnsNull() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");

        String result = aiService.generateSummary("Some content");

        assertNull(result);
    }

    // --- suggestTagNames tests ---

    @Test
    public void suggestTagNames_returnsParsedTags() throws Exception {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"Java, Spring\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<String> existing = Arrays.asList("Java", "Spring", "Python", "React");
        List<String> result = aiService.suggestTagNames("A blog about Java Spring Boot", existing);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Java"));
        assertTrue(result.contains("Spring"));
        mockServer.verify();
    }

    @Test
    public void suggestTagNames_filtersNonExistingTags() throws Exception {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"Java, Unknown, Spring\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<String> existing = Arrays.asList("Java", "Spring");
        List<String> result = aiService.suggestTagNames("Some content", existing);

        assertEquals(2, result.size());
        assertTrue(result.contains("Java"));
        assertTrue(result.contains("Spring"));
        assertFalse(result.contains("Unknown"));
    }

    @Test
    public void suggestTagNames_nullContent_returnsEmptyList() {
        List<String> result = aiService.suggestTagNames(null, Arrays.asList("Java"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void suggestTagNames_emptyTagList_returnsEmptyList() {
        List<String> result = aiService.suggestTagNames("Some content", Collections.<String>emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void suggestTagNames_apiError_returnsEmptyList() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());

        List<String> result = aiService.suggestTagNames("Some content", Arrays.asList("Java"));

        assertTrue(result.isEmpty());
    }

    @Test
    public void suggestTagNames_noApiKey_returnsEmptyList() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");

        List<String> result = aiService.suggestTagNames("Some content", Arrays.asList("Java"));

        assertTrue(result.isEmpty());
    }

    // --- isConfigured tests ---

    @Test
    public void isConfigured_withApiKey_returnsTrue() {
        assertTrue(aiService.isConfigured());
    }

    @Test
    public void isConfigured_withEmptyKey_returnsFalse() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");
        assertFalse(aiService.isConfigured());
    }

    @Test
    public void isConfigured_withNullKey_returnsFalse() {
        ReflectionTestUtils.setField(aiService, "apiKey", null);
        assertFalse(aiService.isConfigured());
    }

    // --- generateDescription tests ---

    @Test
    public void generateDescription_returnsText() throws Exception {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"A short blog about Java.\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = aiService.generateDescription("blog content");
        assertEquals("A short blog about Java.", result);
        mockServer.verify();
    }

    @Test
    public void generateDescription_nullContent_returnsNull() {
        assertNull(aiService.generateDescription(null));
    }

    @Test
    public void generateDescription_emptyContent_returnsNull() {
        assertNull(aiService.generateDescription("   "));
    }

    @Test
    public void generateDescription_apiError_returnsNull() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());
        assertNull(aiService.generateDescription("content"));
    }

    @Test
    public void generateDescription_noApiKey_returnsNull() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");
        assertNull(aiService.generateDescription("content"));
    }

    @Test
    public void generateDescription_truncatesAt200Chars() throws Exception {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 250; i++) longText.append("a");
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"" + longText + "\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = aiService.generateDescription("content");
        assertNotNull(result);
        assertEquals(200, result.length());
        assertTrue(result.endsWith("..."));
    }

    // --- callClaudeApi tests ---

    @Test
    public void callClaudeApi_sendsCorrectHeaders() throws Exception {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"response\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = aiService.callClaudeApi("test message");

        assertEquals("response", result);
        mockServer.verify();
    }

    @Test
    public void callClaudeApi_emptyContentArray_returnsNull() throws Exception {
        String responseJson = "{\"content\":[]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = aiService.callClaudeApi("test message");

        assertNull(result);
    }

    // --- guardrail (Layer A) tests ---

    @Test
    public void generateSummary_withGuardrails_sendsSystemPromptAndDelimitsContent() throws Exception {
        AiGuardrailProperties props = new AiGuardrailProperties();
        props.setEnabled(true);
        ReflectionTestUtils.setField(aiService, "guardrailProperties", props);

        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"summary\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.system").exists())
                .andExpect(jsonPath("$.messages[0].content").value(containsString("<untrusted_content>")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = aiService.generateSummary("Some content");

        assertEquals("summary", result);
        mockServer.verify();
    }

    @Test
    public void generateSummary_withoutGuardrails_sendsNoSystemPrompt() throws Exception {
        // guardrailProperties left unset -> guardrails off -> legacy request shape
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"summary\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(jsonPath("$.system").doesNotExist())
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        aiService.generateSummary("Some content");

        mockServer.verify();
    }
}
