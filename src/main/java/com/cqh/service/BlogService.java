package com.cqh.service;

import com.cqh.po.Blog;
import com.cqh.vo.BlogQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface BlogService {

    Blog getBlog(Long id);

    Blog getAndConvert(Long id);

    Page<Blog> listBlog(Pageable pageable,BlogQuery blog);

    Page<Blog> listBlog(Pageable pageable);

    Page<Blog> listBlog(Long tagId,Pageable pageable);

    Page<Blog> listBlog(String query,Pageable pageable);

    List<Blog> listRecommendBlogTop(Integer size);

    Map<String,List<Blog>> archiveBlog();

    Long countBlog();

    Blog saveBlog(Blog blog);

    Blog updateBlog(Long id,Blog blog);

    void deleteBlog(Long id);

    /**
     * Persist a pre-computed embedding to t_blog.embedding via STRING_TO_VECTOR().
     * Runs in its own transaction so it can be called safely from non-transactional
     * callers (e.g. the admin backfill endpoint).
     */
    void storeEmbedding(Long id, String vec);
}
