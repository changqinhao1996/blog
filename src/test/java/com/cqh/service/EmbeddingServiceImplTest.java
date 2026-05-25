package com.cqh.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class EmbeddingServiceImplTest {

    private EmbeddingServiceImpl service;
    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        service = new EmbeddingServiceImpl();
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "voyage-3-lite");
        RestTemplate rt = new RestTemplate();
        ReflectionTestUtils.setField(service, "restTemplate", rt);
        mockServer = MockRestServiceServer.createServer(rt);
    }

    @Test
    public void embed_returnsJsonArrayString() {
        String json = "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}";
        mockServer.expect(requestTo("https://api.voyageai.com/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        String result = service.embed("hello world");

        // Jackson's array toString — exactly what STRING_TO_VECTOR accepts
        assertEquals("[0.1,0.2,0.3]", result);
        mockServer.verify();
    }

    @Test
    public void embed_nullText_returnsNull() {
        assertNull(service.embed(null));
    }

    @Test
    public void embed_emptyText_returnsNull() {
        assertNull(service.embed("   "));
    }

    @Test
    public void embed_missingApiKey_returnsNullWithoutCallingApi() {
        ReflectionTestUtils.setField(service, "apiKey", "");
        // No mockServer.expect — proves no HTTP call was made
        String result = service.embed("hello");
        assertNull(result);
    }

    @Test
    public void embed_apiError_returnsNull() {
        mockServer.expect(requestTo("https://api.voyageai.com/v1/embeddings"))
                .andRespond(withServerError());

        String result = service.embed("hello");
        assertNull(result);
    }

    @Test
    public void embed_malformedResponse_returnsNull() {
        mockServer.expect(requestTo("https://api.voyageai.com/v1/embeddings"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        String result = service.embed("hello");
        assertNull(result);
    }

    @Test
    public void isConfigured_withKey_true() {
        assertTrue(service.isConfigured());
    }

    @Test
    public void isConfigured_withoutKey_false() {
        ReflectionTestUtils.setField(service, "apiKey", "");
        assertFalse(service.isConfigured());
    }

    @Test
    public void isConfigured_nullKey_false() {
        ReflectionTestUtils.setField(service, "apiKey", null);
        assertFalse(service.isConfigured());
    }
}
