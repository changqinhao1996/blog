package com.cqh.service;

import com.cqh.po.Blog;

import java.util.List;

public interface VectorSearchService {

    /**
     * Find published blogs most semantically similar to the given query text.
     *
     * @param query free-text query
     * @param topK  max number of blogs to return
     * @return up to {@code topK} blogs ranked by cosine similarity (best first).
     *         Empty list if the embedding service is unavailable or no blogs
     *         have embeddings yet.
     */
    List<Blog> semanticSearch(String query, int topK);

    /**
     * Find blogs related to the given blog, ranked by cosine similarity of
     * their embeddings. The blog with id {@code blogId} is excluded from the
     * results.
     *
     * @param blogId id of the source blog
     * @param topK   max number of related blogs to return
     * @return up to {@code topK} related blogs (best first), or an empty list
     *         if the source blog has no stored embedding.
     */
    List<Blog> relatedTo(Long blogId, int topK);
}
