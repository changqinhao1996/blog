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

public class OpenAiProviderTest {

    private OpenAiProvider provider;
    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        provider = new OpenAiProvider();
        ReflectionTestUtils.setField(provider, "apiKey", "sk-test-key");
        ReflectionTestUtils.setField(provider, "model", "gpt-4o-mini");

        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(provider, "restTemplate", restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    // --- complete() — happy path ---

    @Test
    public void complete_happyPath_returnsTextFromFirstChoice() {
        String responseJson = "{"
                + "\"id\":\"chatcmpl-123\","
                + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
                + "\"content\":\"This is the OpenAI response.\"}}]"
                + "}";
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = provider.complete("Hello", 300);

        assertEquals("This is the OpenAI response.", result);
        mockServer.verify();
    }

    @Test
    public void complete_sendsModelAndMaxTokensInBody() {
        String responseJson = "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}";
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(content().string(containsString("\"model\":\"gpt-4o-mini\"")))
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
        // mockServer.expect not called — any HTTP request would cause an
        // unexpected-request failure.
        assertNull(provider.complete("anything", 300));
    }

    @Test
    public void complete_nullApiKey_returnsNull() {
        ReflectionTestUtils.setField(provider, "apiKey", null);
        assertNull(provider.complete("anything", 300));
    }

    @Test
    public void complete_serverError_returnsNull() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withServerError());

        assertNull(provider.complete("Hello", 300));
    }

    @Test
    public void complete_clientError_returnsNull() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertNull(provider.complete("Hello", 300));
    }

    @Test
    public void complete_emptyChoicesArray_returnsNull() {
        String responseJson = "{\"choices\":[]}";
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        assertNull(provider.complete("Hello", 300));
    }

    @Test
    public void complete_missingMessageNode_returnsNull() {
        // choices present but no message.content field — should not NPE
        String responseJson = "{\"choices\":[{\"index\":0}]}";
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        // missing path returns empty string via asText(), but the code only
        // returns it if not isMissingNode — so this case yields null.
        String result = provider.complete("Hello", 300);
        assertNull(result);
    }

    @Test
    public void complete_malformedJson_returnsNull() {
        mockServer.expect(requestTo("https://api.openai.com/v1/chat/completions"))
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
    public void name_returnsOpenAI() {
        assertEquals("OpenAI", provider.name());
    }
}
