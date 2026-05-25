package com.cqh.web;

import com.cqh.NotFoundException;
import com.cqh.po.Blog;
import com.cqh.service.BlogService;
import com.cqh.service.TagService;
import com.cqh.service.TypeService;
import com.cqh.service.VectorSearchService;
import com.cqh.vo.BlogQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private TypeService typeService;

    @Autowired
    private TagService tagService;

    @Autowired(required = false)
    private VectorSearchService vectorSearchService;

    @GetMapping("/")
    public String index(@PageableDefault(size = 8, sort = {"updateTime"}, direction = Sort.Direction.DESC) Pageable pageable,
                        Model model) {
        model.addAttribute("page",blogService.listBlog(pageable));
        model.addAttribute("types", typeService.listTypeTop(6));
        model.addAttribute("tags", tagService.listTagTop(10));
        model.addAttribute("recommendBlogs", blogService.listRecommendBlogTop(8));
        return "index";
    }


    @PostMapping("/search")
    public String search(@PageableDefault(size = 8, sort = {"updateTime"}, direction = Sort.Direction.DESC) Pageable pageable,
                         @RequestParam String query, Model model) {
        // Try semantic (vector) search first; fall back to legacy keyword search
        // when no embeddings exist yet or the embedding service is unavailable.
        List<Blog> hits = (vectorSearchService == null)
                ? java.util.Collections.<Blog>emptyList()
                : vectorSearchService.semanticSearch(query, 20);
        if (hits != null && !hits.isEmpty()) {
            model.addAttribute("page", new PageImpl<>(hits, pageable, hits.size()));
        } else {
            model.addAttribute("page", blogService.listBlog("%" + query + "%", pageable));
        }
        model.addAttribute("query", query);
        return "search";
    }

    @GetMapping("/blog/{id}")
    public String blog(@PathVariable Long id,Model model) {
        model.addAttribute("blog", blogService.getAndConvert(id));
        // "Related articles" — k-nearest neighbours by embedding cosine similarity.
        // Falls back to an empty list when vector search isn't configured.
        List<Blog> related = (vectorSearchService == null)
                ? java.util.Collections.<Blog>emptyList()
                : vectorSearchService.relatedTo(id, 5);
        model.addAttribute("related", related);
        return "blog";
    }

    @GetMapping("/footer/newblog")
    public String newblogs(Model model) {
        model.addAttribute("newblogs", blogService.listRecommendBlogTop(3));
        return "_fragments :: newblogList";
    }

}
