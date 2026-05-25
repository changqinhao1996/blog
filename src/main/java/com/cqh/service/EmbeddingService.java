package com.cqh.service;

public interface EmbeddingService {

    /**
     * Generate a vector embedding for the given text using a hosted embedding model.
     *
     * @param text any text (typically blog title + content, or a search query)
     * @return the embedding as a JSON-array string ready for STRING_TO_VECTOR(),
     *         e.g. "[0.12,-0.34,...]", or {@code null} if generation fails or
     *         the service is not configured.
     */
    String embed(String text);
}
