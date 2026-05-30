package com.cqh.service;

/**
 * Retrieval-Augmented Generation entry point.
 *
 * <p>Chains the two AI building blocks already in the app:
 * <ol>
 *   <li>{@link VectorSearchService#semanticSearch} retrieves the top-K blog
 *       posts most semantically similar to the question.</li>
 *   <li>{@link AiService#answerQuestion} sends those posts to Claude as
 *       grounding context and returns an answer that cites them.</li>
 * </ol>
 */
public interface RagService {

    /**
     * Answer a free-form question, grounded in the top-K most relevant blog posts.
     *
     * @param question the user's natural-language question
     * @param topK     how many blog posts to retrieve as context (clamped to a
     *                 sane range; pass &lt;= 0 to use the default)
     * @return the answer plus the source posts, or an error result on failure
     */
    RagAnswer ask(String question, int topK);
}
