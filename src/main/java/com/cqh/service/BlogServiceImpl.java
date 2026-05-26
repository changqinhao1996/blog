package com.cqh.service;

import com.cqh.NotFoundException;
import com.cqh.dao.BlogRepository;
import com.cqh.po.Blog;
import com.cqh.po.Type;
import com.cqh.util.MarkdownUtils;
import com.cqh.util.MyBeanUtils;
import com.cqh.vo.BlogQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.util.*;

@Service
public class BlogServiceImpl implements BlogService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired private BlogRepository blogRepository;

  @Autowired private AiService aiService;

  @Override
  public Blog getBlog(Long id) {
    return blogRepository.findOne(id);
  }

  @Transactional
  @Override
  public Blog getAndConvert(Long id) {
    Blog blog = blogRepository.findOne(id);
    if (blog == null) {
      throw new NotFoundException("This blog does not exist");
    }
    Blog b = new Blog();
    BeanUtils.copyProperties(blog, b);
    String content = b.getContent();
    b.setContent(MarkdownUtils.markdownToHtmlExtensions(content));

    blogRepository.updateViews(id);
    return b;
  }

  @Override
  public Page<Blog> listBlog(Pageable pageable, BlogQuery blog) {
    return blogRepository.findAll(
        new Specification<Blog>() {
          @Override
          public Predicate toPredicate(Root<Blog> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
            List<Predicate> predicates = new ArrayList<>();
            if (!"".equals(blog.getTitle()) && blog.getTitle() != null) {
              predicates.add(cb.like(root.<String>get("title"), "%" + blog.getTitle() + "%"));
            }
            if (blog.getTypeId() != null) {
              predicates.add(cb.equal(root.<Type>get("type").get("id"), blog.getTypeId()));
            }
            if (blog.isRecommend()) {
              predicates.add(cb.equal(root.<Boolean>get("recommend"), blog.isRecommend()));
            }
            cq.where(predicates.toArray(new Predicate[predicates.size()]));
            return null;
          }
        },
        pageable);
  }

  @Override
  public Page<Blog> listBlog(Pageable pageable) {
    return blogRepository.findAll(pageable);
  }

  @Override
  public Page<Blog> listBlog(Long tagId, Pageable pageable) {
    return blogRepository.findAll(
        new Specification<Blog>() {
          @Override
          public Predicate toPredicate(Root<Blog> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
            Join join = root.join("tags");
            return cb.equal(join.get("id"), tagId);
          }
        },
        pageable);
  }

  @Override
  public Page<Blog> listBlog(String query, Pageable pageable) {
    return blogRepository.findByQuery(query, pageable);
  }

  @Override
  public List<Blog> listRecommendBlogTop(Integer size) {
    Sort sort = new Sort(Sort.Direction.DESC, "updateTime");
    Pageable pageable = new PageRequest(0, size, sort);
    return blogRepository.findTop(pageable);
  }

  @Override
  public Map<String, List<Blog>> archiveBlog() {
    List<String> years = blogRepository.findGroupYear();
    Map<String, List<Blog>> map = new HashMap<>();
    for (String year : years) {
      map.put(year, blogRepository.findByYear(year));
    }
    return map;
  }

  @Override
  public Long countBlog() {
    return blogRepository.count();
  }

  @Transactional
  @Override
  public Blog saveBlog(Blog blog) {
    if (blog.getId() == null) {
      blog.setCreateTime(new Date());
      blog.setUpdateTime(new Date());
      blog.setViews(0);
    } else {
      blog.setUpdateTime(new Date());
    }
    Blog saved = blogRepository.save(blog);
    boolean changed = false;
    if (saved.isPublished() && (saved.getSummary() == null || saved.getSummary().isEmpty())) {
      try {
        String summary = aiService.generateSummary(saved.getContent());
        if (summary != null && !summary.isEmpty()) {
          saved.setSummary(summary);
          changed = true;
          logger.info("AI summary generated for blog: {}", saved.getTitle());
        }
      } catch (Exception e) {
        logger.warn("AI summary generation failed for blog '{}', saved without summary",
                saved.getTitle(), e);
      }
    }
    if (saved.isPublished() && isBlank(saved.getDescription())) {
      try {
        String desc = aiService.generateDescription(saved.getContent());
        if (desc != null && !desc.isEmpty()) {
          saved.setDescription(desc);
          changed = true;
          logger.info("AI description generated for blog: {}", saved.getTitle());
        }
      } catch (Exception e) {
        logger.warn("AI description generation failed for blog '{}', saved without description",
                saved.getTitle(), e);
      }
    }
    if (changed) {
      blogRepository.save(saved);
    }
    return saved;
  }

  @Transactional
  @Override
  public Blog updateBlog(Long id, Blog blog) {
    Blog b = blogRepository.findOne(id);
    if (b == null) {
      throw new NotFoundException("This blog does not exist");
    }
    BeanUtils.copyProperties(blog, b, MyBeanUtils.getNullPropertyNames(blog));
    b.setUpdateTime(new Date());
    Blog saved = blogRepository.save(b);
    // If a published blog has no description (author cleared it or never set
    // it), generate one via Claude. Drafts are skipped.
    if (saved.isPublished() && isBlank(saved.getDescription())) {
      try {
        String desc = aiService.generateDescription(saved.getContent());
        if (desc != null && !desc.isEmpty()) {
          saved.setDescription(desc);
          blogRepository.save(saved);
          logger.info("AI description regenerated on update for blog: {}", saved.getTitle());
        }
      } catch (Exception e) {
        logger.warn("AI description regeneration failed for blog '{}'", saved.getTitle(), e);
      }
    }
    return saved;
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  @Transactional
  @Override
  public void deleteBlog(Long id) {
    blogRepository.delete(id);
  }
}
