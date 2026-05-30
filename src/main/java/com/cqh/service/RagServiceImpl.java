package com.cqh.service;

import com.cqh.po.Blog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagServiceImpl implements RagService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Default number of blog posts to retrieve as context when topK <= 0. */
    static final int DEFAULT_TOP_K = 5;
    /** Hard cap so a runaway request can't stuff the prompt with the whole site. */
    static final int MAX_TOP_K = 10;

    // VectorSearchService is only present under the `vector` profile (MySQL 9).
    // Marking the dependency optional lets the RAG endpoint return a friendly
    // error under the default profile instead of failing to start the context.
    @Autowired(required = false) private VectorSearchService vectorSearchService;

    @Autowired private AiService aiService;

    @Override
    public RagAnswer ask(String question, int topK) {
        if (question == null || question.trim().isEmpty()) {
            return RagAnswer.error("Please enter a question.");
        }
        if (vectorSearchService == null) {
            return RagAnswer.error(
                    "Semantic retrieval is not available — start the app with the 'vector' "
                  + "profile (MySQL 9) so embeddings can be searched.");
        }

        int k = (topK <= 0) ? DEFAULT_TOP_K : Math.min(topK, MAX_TOP_K);

        List<Blog> sources;
        try {
            sources = vectorSearchService.semanticSearch(question, k);
        } catch (Exception e) {
            logger.warn("Vector retrieval failed for question '{}': {}", question, e.getMessage());
            return RagAnswer.error("Vector retrieval failed. See server logs for details.");
        }
        if (sources == null || sources.isEmpty()) {
            return RagAnswer.error(
                    "No relevant blog posts found for your question. Try a different question, "
                  + "or publish more posts so the index has something to draw from.");
        }

        String answer;
        try {
            answer = aiService.answerQuestion(question, sources);
        } catch (Exception e) {
            logger.warn("Claude call failed for question '{}': {}", question, e.getMessage());
            return RagAnswer.error("Could not reach the language model. See server logs.");
        }
        if (answer == null || answer.trim().isEmpty()) {
            return RagAnswer.error(
                    "Could not generate an answer. Check that CLAUDE_API_KEY is set.");
        }

        logger.info("RAG ask: question='{}' k={} sourcesFound={} answerChars={}",
                question, k, sources.size(), answer.length());
        return RagAnswer.success(answer, sources);
    }
}
