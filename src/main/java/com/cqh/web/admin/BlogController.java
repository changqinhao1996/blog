package com.cqh.web.admin;

import com.cqh.po.Blog;
import com.cqh.po.Tag;
import com.cqh.po.Type;
import com.cqh.po.User;
import com.cqh.service.AiService;
import com.cqh.service.BlogService;
import com.cqh.service.TagService;
import com.cqh.service.TypeService;
import com.cqh.vo.BlogQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class BlogController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String INPUT = "admin/blogs-input";
  private static final String LIST = "admin/blogs";
  private static final String REDIRECT_LIST = "redirect:/admin/blogs";

  @Autowired private BlogService blogService;
  @Autowired private TypeService typeService;
  @Autowired private TagService tagService;
  @Autowired private AiService aiService;

  @GetMapping("/blogs")
  public String blogs(
      @PageableDefault(
              size = 8,
              sort = {"updateTime"},
              direction = Sort.Direction.DESC)
          Pageable pageable,
      BlogQuery blog,
      Model model) {
    model.addAttribute("types", typeService.listType());
    model.addAttribute("page", blogService.listBlog(pageable, blog));
    return LIST;
  }

  @PostMapping("/blogs/search")
  public String search(
      @PageableDefault(
              size = 8,
              sort = {"updateTime"},
              direction = Sort.Direction.DESC)
          Pageable pageable,
      BlogQuery blog,
      Model model) {
    model.addAttribute("page", blogService.listBlog(pageable, blog));
    return "admin/blogs :: blogList";
  }

  @GetMapping("/blogs/input")
  public String input(Model model) {
    setTypeAndTag(model);
    model.addAttribute("blog", new Blog());
    return INPUT;
  }

  private void setTypeAndTag(Model model) {
    model.addAttribute("types", typeService.listType());
    model.addAttribute("tags", tagService.listTag());
  }

  @GetMapping("/blogs/{id}/input")
  public String editInput(@PathVariable Long id, Model model) {
    setTypeAndTag(model);
    Blog blog = blogService.getBlog(id);
    blog.init();
    model.addAttribute("blog", blog);
    return INPUT;
  }

  @PostMapping("/blogs")
  public String post(Blog blog, RedirectAttributes attributes, HttpSession session) {
    blog.setUser((User) session.getAttribute("user"));
    blog.setType(typeService.getType(blog.getType().getId()));

    // AI auto-tagging: if admin didn't select tags, ask AI to suggest them
    if (blog.getTagIds() == null || blog.getTagIds().trim().isEmpty()) {
      List<Tag> allTags = tagService.listTag();
      if (!allTags.isEmpty()) {
        try {
          List<String> allNames = allTags.stream()
                  .map(Tag::getName).collect(Collectors.toList());
          List<String> suggested = aiService.suggestTagNames(blog.getContent(), allNames);
          if (!suggested.isEmpty()) {
            List<Tag> matched = allTags.stream()
                    .filter(t -> suggested.contains(t.getName()))
                    .collect(Collectors.toList());
            blog.setTags(matched);
            logger.info("AI auto-tagged blog '{}' with: {}", blog.getTitle(), suggested);
          } else {
            blog.setTags(new ArrayList<>());
          }
        } catch (Exception e) {
          logger.warn("AI tag suggestion failed, saving without tags", e);
          blog.setTags(new ArrayList<>());
        }
      } else {
        blog.setTags(new ArrayList<>());
      }
    } else {
      blog.setTags(tagService.listTag(blog.getTagIds()));
    }

    Blog b;
    if (blog.getId() == null) {
      b = blogService.saveBlog(blog);
    } else {
      b = blogService.updateBlog(blog.getId(), blog);
    }

    if (b == null) {
      attributes.addFlashAttribute("message", "Operation failed");
    } else {
      attributes.addFlashAttribute("message", "Operation successful");
    }
    return REDIRECT_LIST;
  }

  @PostMapping("/blogs/suggest-tags")
  @ResponseBody
  public List<String> suggestTags(@RequestParam String content) {
    List<Tag> allTags = tagService.listTag();
    List<String> names = allTags.stream()
            .map(Tag::getName).collect(Collectors.toList());
    return aiService.suggestTagNames(content, names);
  }

  @GetMapping("/blogs/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes attributes) {
    blogService.deleteBlog(id);
    attributes.addFlashAttribute("message", "Deletion successful");
    return REDIRECT_LIST;
  }
}
