package com.cqh.service;

import com.cqh.dao.BlogRepository;
import com.cqh.po.Blog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cosine-similarity search over the embeddings stored in t_blog.embedding.
 *
 * <p>MySQL 9 Community Edition exposes STRING_TO_VECTOR / VECTOR_TO_STRING /
 * VECTOR_DIM but NOT a server-side DISTANCE() function (HeatWave-only), so we
 * pull every embedding back into Java via VECTOR_TO_STRING(embedding) and
 * compute cosine similarity in process. For a personal blog (< 10 000 posts)
 * this is fast enough that the cost is dominated by network/render.
 */
@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @PersistenceContext
    private EntityManager em;

    @Autowired private EmbeddingService embeddingService;
    @Autowired private BlogRepository blogRepository;

    @Override
    public List<Blog> semanticSearch(String query, int topK) {
        if (query == null || query.trim().isEmpty() || topK <= 0) {
            return Collections.emptyList();
        }
        String qVec = embeddingService.embed(query);
        if (qVec == null) {
            return Collections.emptyList();
        }
        try {
            double[] q = parse(qVec);
            return rank(q, topK, null);
        } catch (Exception e) {
            logger.warn("Semantic search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Blog> relatedTo(Long blogId, int topK) {
        if (blogId == null || topK <= 0) {
            return Collections.emptyList();
        }
        try {
            // Pull THIS blog's embedding back from the VECTOR column as a JSON-array string.
            Object row;
            try {
                row = em.createNativeQuery(
                        "SELECT VECTOR_TO_STRING(embedding) FROM t_blog WHERE id = ?1")
                    .setParameter(1, blogId)
                    .getSingleResult();
            } catch (Exception e) {
                // No row, or VECTOR functions not available (e.g. running on MySQL 8.4)
                logger.debug("relatedTo: cannot read embedding for blog id={}: {}",
                        blogId, e.getMessage());
                return Collections.emptyList();
            }
            if (row == null) {
                return Collections.emptyList();
            }
            double[] q = parse((String) row);
            return rank(q, topK, blogId);
        } catch (Exception e) {
            logger.warn("relatedTo failed for blog id={}: {}", blogId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Rank all published blogs (optionally excluding one id) by cosine similarity. */
    @SuppressWarnings("unchecked")
    private List<Blog> rank(double[] q, int topK, Long excludeId) {
        // VECTOR_TO_STRING(embedding) returns the JSON-array form of the stored vector.
        List<Object[]> rows;
        try {
            rows = em.createNativeQuery(
                    "SELECT id, VECTOR_TO_STRING(embedding) FROM t_blog " +
                    "WHERE embedding IS NOT NULL AND published = TRUE"
            ).getResultList();
        } catch (Exception e) {
            // e.g. VECTOR functions not supported in the connected MySQL
            logger.warn("Vector scan failed (is MySQL 9.0+?): {}", e.getMessage());
            return Collections.emptyList();
        }

        List<Scored> scored = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long id = ((Number) r[0]).longValue();
            if (excludeId != null && excludeId.equals(id)) continue;
            try {
                double[] v = parse((String) r[1]);
                scored.add(new Scored(id, cosine(q, v)));
            } catch (Exception ignore) {
                // Skip rows with malformed vectors rather than failing the whole search.
            }
        }
        scored.sort(Comparator.comparingDouble((Scored s) -> s.score).reversed());

        List<Blog> out = new ArrayList<>(Math.min(topK, scored.size()));
        for (int i = 0; i < scored.size() && out.size() < topK; i++) {
            Blog b = blogRepository.findOne(scored.get(i).id);
            if (b != null) out.add(b);
        }
        return out;
    }

    /** Parse "[v1, v2, ...]" (with or without whitespace) into a double[]. */
    static double[] parse(String json) {
        if (json == null) throw new IllegalArgumentException("null vector string");
        String body = json.trim();
        if (body.startsWith("[")) body = body.substring(1);
        if (body.endsWith("]"))   body = body.substring(0, body.length() - 1);
        if (body.isEmpty())       return new double[0];
        String[] parts = body.split(",");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Double.parseDouble(parts[i].trim());
        }
        return out;
    }

    /** Cosine similarity, with a tiny epsilon to avoid divide-by-zero. */
    static double cosine(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "vector dim mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-12);
    }

    private static final class Scored {
        final Long id;
        final double score;
        Scored(Long id, double score) { this.id = id; this.score = score; }
    }

    // Used by tests via package-private setter style — but Spring autowires via field.
    void setEntityManager(EntityManager em) { this.em = em; }
    void setEmbeddingService(EmbeddingService s) { this.embeddingService = s; }
    void setBlogRepository(BlogRepository r) { this.blogRepository = r; }
    // Quiet "field never read" in checkers
    @SuppressWarnings("unused") private void _useFields() {
        Objects.requireNonNull(em);
        Objects.requireNonNull(embeddingService);
        Objects.requireNonNull(blogRepository);
    }
}
