package com.cqh.web;

import com.cqh.service.RagAnswer;
import com.cqh.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * "Ask my blog" RAG endpoint.
 *
 * <ul>
 *   <li>{@code GET  /ask} — render the blank question form.</li>
 *   <li>{@code POST /ask} — vector-retrieve the top-K relevant posts, send them
 *       to Claude as grounding context, and render the answer with citations.</li>
 * </ul>
 *
 * <p>Both the retrieval and the generation pieces already exist
 * ({@link com.cqh.service.VectorSearchService}, {@link com.cqh.service.AiService});
 * the chaining that turns "search + summarize" into actual RAG lives in
 * {@link RagService}.
 */
@Controller
public class AskController {

    private static final int DEFAULT_TOP_K = 5;

    @Autowired private RagService ragService;

    @GetMapping("/ask")
    public String askForm() {
        return "ask";
    }

    @PostMapping("/ask")
    public String ask(@RequestParam String question, Model model) {
        RagAnswer answer = ragService.ask(question, DEFAULT_TOP_K);
        model.addAttribute("question", question);
        model.addAttribute("ragAnswer", answer);
        return "ask";
    }
}
