package com.cqh.service.llm;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class ClaudeProviderTest {

    private ClaudeProvider provider;
    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        provider = new ClaudeProvider();
        ReflectionTestUtils.setField(provider, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(provider, "model", "claude-sonnet-4-6");

        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(provider, "restTemplate", restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    // --- complete() — happy path ---

    @Test
    public void complete_happyPath_returnsTextFromContent() {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"This is the Claude response.\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = provider.complete("Hello", 300);

        assertEquals("This is the Claude response.", result);
        mockServer.verify();
    }

    @Test
    public void complete_sendsModelAndMaxTokensInBody() {
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(content().string(containsString("\"model\":\"claude-sonnet-4-6\"")))
                .andExpect(content().string(containsString("\"max_tokens\":800")))
                .andExpect(content().string(containsString("\"role\":\"user\"")))
                .andExpect(content().string(containsString("\"content\":\"my prompt\"")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        provider.complete("my prompt", 800);
        mockServer.verify();
    }

    // --- complete() — error paths ---

    @Test
    public void complete_noApiKey_returnsNullWithoutHttpCall() {
        ReflectionTestUtils.setField(provider, "apiKey", "");
        // No mockServer.expect; any HTTP would be unexpected.
        assertNull(provider.complete("anything", 300));
    }

    @Test
    public void complete_serverError_returnsNull() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());

        assertNull(provider.complete("Hello", 300));
    }

    @Test
    public void complete_clientError_returnsNull() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertNull(provider.complete("Hello", 300));
    }

    @Test
    public void complete_emptyContentArray_returnsNull() {
        String responseJson = "{\"content\":[]}";
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        assertNull(provider.complete("Hello", 300));
    }

    @Test
    public void complete_malformedJson_returnsNull() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("{not valid json", MediaType.APPLICATION_JSON));

        assertNull(provider.complete("Hello", 300));
    }

    // --- isConfigured / name ---

    @Test
    public void isConfigured_withKey_returnsTrue() {
        assertTrue(provider.isConfigured());
    }

    @Test
    public void isConfigured_blankKey_returnsFalse() {
        ReflectionTestUtils.setField(provider, "apiKey", "   ");
        assertFalse(provider.isConfigured());
    }

    @Test
    public void isConfigured_nullKey_returnsFalse() {
        ReflectionTestUtils.setField(provider, "apiKey", null);
        assertFalse(provider.isConfigured());
    }

    @Test
    public void name_returnsClaude() {
        assertEquals("Claude", provider.name());
    }
}
