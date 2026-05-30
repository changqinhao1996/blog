package com.cqh.service;

import com.cqh.po.Blog;

import java.util.List;

public interface AiService {

    /**
     * Generate a 2-3 sentence summary of the blog content.
     *
     * @param blogContent the raw blog content (markdown)
     * @return the generated summary, or null if generation fails
     */
    String generateSummary(String blogContent);

    /**
     * Suggest relevant tag names for the blog content from the existing tags.
     *
     * @param blogContent      the raw blog content (markdown)
     * @param existingTagNames list of all available tag names in the system
     * @return list of suggested tag names (subset of existingTagNames), or empty list on failure
     */
    List<String> suggestTagNames(String blogContent, List<String> existingTagNames);

    /**
     * Retrieval-Augmented Generation primitive: answer a question using ONLY
     * the supplied blog posts as context. The prompt instructs the model to
     * cite sources with bracketed numbers like [1] or [2,3] that match the
     * numbered sources in the prompt, and to admit when the posts do not
     * contain the answer rather than invent facts.
     *
     * @param question      the user's natural-language question
     * @param contextBlogs  the blog posts retrieved from vector search; their
     *                      title + content are sent verbatim to the model
     * @return the generated answer text, or null on any failure (graceful degradation)
     */
    String answerQuestion(String question, List<Blog> contextBlogs);
}
