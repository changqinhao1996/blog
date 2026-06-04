package com.cqh.service;

import com.cqh.config.AiGuardrailProperties;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class ModerationServiceImplTest {

    private ModerationServiceImpl moderationService;
    private AiServiceImpl aiService;
    private AiGuardrailProperties props;
    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        aiService = new AiServiceImpl();
        ReflectionTestUtils.setField(aiService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "model", "claude-sonnet-4-6");
        ReflectionTestUtils.setField(aiService, "maxTokens", 300);
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(aiService, "restTemplate", restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);

        props = new AiGuardrailProperties();
        moderationService = new ModerationServiceImpl();
        ReflectionTestUtils.setField(moderationService, "aiService", aiService);
        ReflectionTestUtils.setField(moderationService, "props", props);
    }

    @Test
    public void allow_whenModelReturnsAllow() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(jsonPath("$.system").exists())
                .andRespond(withSuccess("{\"content\":[{\"type\":\"text\",\"text\":\"ALLOW\"}]}",
                        MediaType.APPLICATION_JSON));

        ModerationService.ModerationResult result = moderationService.check("some content");

        assertTrue(result.isAllowed());
        mockServer.verify();
    }

    @Test
    public void block_whenModelReturnsBlock() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("{\"content\":[{\"type\":\"text\",\"text\":\"BLOCK: hateful content\"}]}",
                        MediaType.APPLICATION_JSON));

        ModerationService.ModerationResult result = moderationService.check("bad content");

        assertFalse(result.isAllowed());
        assertTrue(result.getReason().contains("hateful"));
    }

    @Test
    public void block_whenApiErrors_failClosed() {
        mockServer.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withServerError());

        ModerationService.ModerationResult result = moderationService.check("content");

        assertFalse(result.isAllowed());
    }

    @Test
    public void block_whenNotConfigured_failClosed() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");

        ModerationService.ModerationResult result = moderationService.check("content");

        assertFalse(result.isAllowed());
    }

    @Test
    public void allow_whenModerationDisabled() {
        props.setModerationEnabled(false);

        ModerationService.ModerationResult result = moderationService.check("content");

        assertTrue(result.isAllowed());
    }

    @Test
    public void allow_whenContentBlank() {
        ModerationService.ModerationResult result = moderationService.check("   ");

        assertTrue(result.isAllowed());
    }
}
