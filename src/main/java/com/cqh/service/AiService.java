package com.cqh.service;

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
}
