package com.cqh.web;

import com.cqh.po.Blog;
import com.cqh.service.RagAnswer;
import com.cqh.service.RagService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Arrays;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AskControllerTest {

    @Mock private RagService ragService;
    @InjectMocks private AskController askController;
    private MockMvc mvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Configure a view resolver so the view name "ask" doesn't forward back to /ask
        // (standalone MockMvc otherwise builds a circular view path).
        InternalResourceViewResolver vr = new InternalResourceViewResolver();
        vr.setPrefix("/templates/");
        vr.setSuffix(".html");
        mvc = MockMvcBuilders.standaloneSetup(askController)
                .setViewResolvers(vr)
                .build();
    }

    private static Blog blog(long id, String title) {
        Blog b = new Blog();
        b.setId(id);
        b.setTitle(title);
        return b;
    }

    @Test
    public void getAsk_rendersForm() throws Exception {
        mvc.perform(get("/ask"))
                .andExpect(status().isOk())
                .andExpect(view().name("ask"));
    }

    @Test
    public void postAsk_withQuestion_invokesRagAndPassesAttributes() throws Exception {
        RagAnswer ans = RagAnswer.success(
                "Answer text [1].",
                Arrays.asList(blog(1L, "Neural Nets")));
        when(ragService.ask(eq("hello"), anyInt())).thenReturn(ans);

        mvc.perform(post("/ask").param("question", "hello"))
                .andExpect(status().isOk())
                .andExpect(view().name("ask"))
                .andExpect(model().attribute("question", "hello"))
                .andExpect(model().attributeExists("ragAnswer"));

        verify(ragService).ask("hello", 5);
    }

    @Test
    public void postAsk_errorResult_stillRendersTemplate() throws Exception {
        RagAnswer err = RagAnswer.error("not configured");
        when(ragService.ask(anyString(), anyInt())).thenReturn(err);

        mvc.perform(post("/ask").param("question", "anything"))
                .andExpect(status().isOk())
                .andExpect(view().name("ask"))
                .andExpect(model().attribute("question", "anything"));
    }

    @Test
    public void postAsk_missingQuestionParam_returns400() throws Exception {
        // Spring's @RequestParam without a default requires the param.
        mvc.perform(post("/ask"))
                .andExpect(status().isBadRequest());
    }
}
