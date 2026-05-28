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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.*;

@Service
public class BlogServiceImpl implements BlogService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired private BlogRepository blogRepository;

  @Autowired private AiService aiService;

  @Autowired(required = false) private EmbeddingService embeddingService;

  @PersistenceContext private EntityManager em;

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
    if (saved.isPublished() && (saved.getSummary() == null || saved.getSummary().isEmpty())) {
      try {
        String summary = aiService.generateSummary(saved.getContent());
        if (summary != null && !summary.isEmpty()) {
          saved.setSummary(summary);
          blogRepository.save(saved);
          logger.info("AI summary generated for blog: {}", saved.getTitle());
        }
      } catch (Exception e) {
        logger.warn("AI summary generation failed for blog '{}', saved without summary",
                saved.getTitle(), e);
      }
    }
    generateAndStoreEmbedding(saved);
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
    generateAndStoreEmbedding(saved);
    return saved;
  }

  @Transactional
  @Override
  public void deleteBlog(Long id) {
    blogRepository.delete(id);
  }

  /**
   * Generate an embedding for the blog and persist it to the t_blog.embedding
   * VECTOR column via {@code STRING_TO_VECTOR(?)}.
   *
   * <p>This is a no-op when:
   * <ul>
   *   <li>the blog is a draft (only published blogs are searchable),</li>
   *   <li>no {@link EmbeddingService} bean is wired (e.g. unit tests),</li>
   *   <li>the embedding API call fails (returns null), or</li>
   *   <li>the database is not MySQL 9+ (the UPDATE throws and we swallow it).</li>
   * </ul>
   * Embedding failures never block the save — the blog has already been
   * persisted by the caller.
   */
  void generateAndStoreEmbedding(Blog saved) {
    if (saved == null || saved.getId() == null || !saved.isPublished()) return;
    if (embeddingService == null) return;
    try {
      String text = (saved.getTitle() == null ? "" : saved.getTitle())
              + "\n\n"
              + (saved.getContent() == null ? "" : saved.getContent());
      String vec = embeddingService.embed(text);
      if (vec != null) {
        storeEmbedding(saved.getId(), vec);
        logger.info("Embedding stored (STRING_TO_VECTOR) for blog id={}", saved.getId());
      }
    } catch (Exception e) {
      logger.warn("Embedding generation/storage failed for blog '{}': {}",
              saved.getTitle(), e.getMessage());
    }
  }

  /**
   * Persist a pre-computed embedding string to the VECTOR column.
   *
   * <p>Annotated {@code @Transactional} because a native {@code executeUpdate()}
   * requires an active transaction. When invoked from {@code saveBlog}/
   * {@code updateBlog} it simply joins their transaction; when invoked from a
   * non-transactional caller (the admin backfill endpoint) the proxy opens a
   * short transaction just for this UPDATE.
   */
  @Transactional
  @Override
  public void storeEmbedding(Long id, String vec) {
    em.createNativeQuery(
            "UPDATE t_blog SET embedding = STRING_TO_VECTOR(?1) WHERE id = ?2")
        .setParameter(1, vec)
        .setParameter(2, id)
        .executeUpdate();
  }
}
