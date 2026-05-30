package com.cqh.service;

import com.cqh.po.Blog;

import java.util.Collections;
import java.util.List;

/**
 * Result of a Retrieval-Augmented Generation ask: the LLM-generated answer
 * plus the source blog posts that grounded it. On failure, {@link #getError()}
 * carries a user-friendly message and {@link #isSuccess()} returns false.
 */
public class RagAnswer {

    private final String answer;
    private final List<Blog> sources;
    private final String error;

    private RagAnswer(String answer, List<Blog> sources, String error) {
        this.answer = answer;
        this.sources = sources == null ? Collections.<Blog>emptyList() : sources;
        this.error = error;
    }

    public static RagAnswer success(String answer, List<Blog> sources) {
        return new RagAnswer(answer, sources, null);
    }

    public static RagAnswer error(String message) {
        return new RagAnswer(null, Collections.<Blog>emptyList(), message);
    }

    public String getAnswer()      { return answer; }
    public List<Blog> getSources() { return sources; }
    public String getError()       { return error; }
    public boolean isSuccess()     { return error == null && answer != null; }
}
