package com.cqh.service;

import com.cqh.po.Blog;
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
        ReflectionTestUtils.setField(aiService, "ragMaxTokens", 800);

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

    // --- answerQuestion (RAG) tests ---

    private static Blog blog(long id, String title, String content) {
        Blog b = new Blog();
        b.setId(id);
        b.setTitle(title);
        b.setContent(content);
        return b;
    }

    @Test
    public void answerQuestion_happyPath_buildsCitedAnswer() throws Exception {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":"
                + "\"Backprop is the algorithm [1].\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<Blog> ctx = Arrays.asList(blog(1L, "Neural Nets", "Backprop and gradient descent."));
        String result = aiService.answerQuestion("How does backprop work?", ctx);

        assertEquals("Backprop is the algorithm [1].", result);
        mockServer.verify();
    }

    @Test
    public void answerQuestion_buildsPromptWithSourceMarkersAndQuestion() throws Exception {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                // verify the JSON body contains both source markers and the question
                .andExpect(content().string(org.hamcrest.Matchers.containsString("--- Source [1]: Title A ---")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("--- Source [2]: Title B ---")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Question: what is X")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("max_tokens\":800")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<Blog> ctx = Arrays.asList(
                blog(1L, "Title A", "content A"),
                blog(2L, "Title B", "content B")
        );
        String result = aiService.answerQuestion("what is X", ctx);

        assertEquals("ok", result);
        mockServer.verify();
    }

    @Test
    public void answerQuestion_nullQuestion_returnsNull() {
        List<Blog> ctx = Arrays.asList(blog(1L, "T", "C"));
        assertNull(aiService.answerQuestion(null, ctx));
    }

    @Test
    public void answerQuestion_blankQuestion_returnsNull() {
        List<Blog> ctx = Arrays.asList(blog(1L, "T", "C"));
        assertNull(aiService.answerQuestion("   ", ctx));
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
    public void answerQuestion_noApiKey_returnsNull() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");
        List<Blog> ctx = Arrays.asList(blog(1L, "T", "C"));
        assertNull(aiService.answerQuestion("q", ctx));
    }

    @Test
    public void answerQuestion_apiError_returnsNull() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());
        List<Blog> ctx = Arrays.asList(blog(1L, "T", "C"));
        assertNull(aiService.answerQuestion("q", ctx));
    }

    @Test
    public void answerQuestion_handlesNullTitleAndContent() throws Exception {
        // A blog with null title/content should not NPE — the prompt builder
        // substitutes safe defaults.
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Blog b = new Blog(); b.setId(99L); // title and content left null
        String result = aiService.answerQuestion("q", Arrays.asList(b));

        assertEquals("ok", result);
    }
}
