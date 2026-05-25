# Vector Search Integration — Implementation Plan

> **Goal:** Add vector-based semantic search and "related articles" to the
> blog application, using MySQL's native `VECTOR` data type and
> `STRING_TO_VECTOR()` function.

This plan is designed to be **incremental** — each phase is independently
shippable, reuses the existing `AiService` graceful-degradation pattern, and
adds the minimum number of new files.

---

## 1. What We Are Building

Two user-visible features, both powered by the same vector pipeline:

| Feature | Where | How |
|---|---|---|
| **Semantic search** | Public site search box (`POST /search`) | Embed the query, return blogs whose embeddings are nearest. |
| **"Related articles"** | Bottom of blog detail page (`GET /blog/{id}`) | For the current blog's embedding, return its k-nearest neighbours. |

Both features replace or supplement today's `LIKE '%query%'` keyword search,
which only matches literal substrings and misses synonyms or paraphrases.

---

## 2. Prerequisites

### 2.1 MySQL 9.0+ (REQUIRED for the headline feature)

The current environment is **MySQL 8.4** — it does **not** support the `VECTOR`
type or `STRING_TO_VECTOR()`. These were introduced in **MySQL 9.0** (Innovation
Release, July 2024).

**Action:** upgrade the local/dev MySQL to **9.0 Community Innovation** (or
later). Production deployment will need the same.

> If upgrading is not possible, see **Appendix A** for an 8.4-compatible
> fallback (stores embeddings as `JSON`, does distance math in Java) — it does
> **not** satisfy the professor's `STRING_TO_VECTOR()` requirement but proves
> the same concept.

### 2.2 An embedding provider

Claude does **not** offer a hosted embedding API. Pick one:

| Provider | Model | Dim | Notes |
|---|---|---|---|
| **Voyage AI** *(recommended)* | `voyage-3-lite` | **512** | Anthropic's recommended partner; free tier covers academic-scale traffic. |
| OpenAI | `text-embedding-3-small` | 1536 | Largest ecosystem, paid only. |
| Local | `all-MiniLM-L6-v2` via a tiny FastAPI sidecar | 384 | Zero cost, no API key, more moving parts. |

The rest of this plan assumes **Voyage `voyage-3-lite` (dim = 512)**. Swap
endpoints and dimension constant to change.

---

## 3. Architecture

```
                     New blog saved (published)
                                |
                                v
              +-------------------------------------+
              | BlogServiceImpl.saveBlog()          |
              |   - existing: AI summary, tags      |
              |   - NEW: EmbeddingService.embed()   |---> Voyage API
              +-------------------------------------+
                                |
                                v   stored via STRING_TO_VECTOR(?)
                       +--------------------+
                       | t_blog.embedding   |  VECTOR(512)
                       +--------------------+
                                ^
                                |
              Search / Related --+
                                |
              +-------------------------------------+
              | VectorSearchService                 |
              |   - embed query                     |
              |   - read all embeddings via         |
              |     VECTOR_TO_STRING(...)           |
              |   - rank by cosine similarity       |
              +-------------------------------------+
```

**Why compute similarity in Java (not SQL)?**
MySQL 9.0 Community has the `VECTOR` *storage* and the `STRING_TO_VECTOR` /
`VECTOR_TO_STRING` / `VECTOR_DIM` *helper* functions, but the `DISTANCE()`
function for nearest-neighbour search is **HeatWave-only**. So the Community
Edition pattern is:

1. Persist embeddings in a `VECTOR` column (using `STRING_TO_VECTOR`).
2. Read them back (using `VECTOR_TO_STRING`) into Java.
3. Compute cosine similarity in Java with a 5-line loop.

For a typical personal blog (< 10 000 posts) this is plenty fast.

---

## 4. Data Model Change

Add **one** column to `Blog` / `t_blog`:

```java
// src/main/java/com/cqh/po/Blog.java
@Column(columnDefinition = "VECTOR(512)")
private String embedding;   // stored as VECTOR, exchanged as String via STRING_TO_VECTOR
public String getEmbedding() { ... }
public void setEmbedding(String embedding) { ... }
```

JPA does not natively understand `VECTOR`, so we treat it as a `String` at the
Java layer and let MySQL convert on read/write via `STRING_TO_VECTOR()` /
`VECTOR_TO_STRING()` in native queries. **`ddl-auto: update` will not create the
column** because Hibernate cannot synthesise the `VECTOR(512)` DDL; run this
once by hand:

```sql
ALTER TABLE t_blog ADD COLUMN embedding VECTOR(512) NULL;
```

---

## 5. New Files (only 3)

### 5.1 `EmbeddingService` interface

```java
// src/main/java/com/cqh/service/EmbeddingService.java
public interface EmbeddingService {
    /** Returns the embedding as a JSON-array string like "[0.12, -0.34, ...]",
     *  or null on failure / when not configured. */
    String embed(String text);
}
```

### 5.2 `EmbeddingServiceImpl` (Voyage AI HTTP client)

Mirrors `AiServiceImpl` exactly:

- Reads `voyage.api-key` and `voyage.model` from `application.yml`.
- `POST https://api.voyageai.com/v1/embeddings` with `{ "input": [text], "model": "voyage-3-lite" }`.
- Parses `data[0].embedding` into a `double[]`, formats as `"[v1, v2, ...]"`.
- Wraps everything in try/catch — returns `null` on failure (never throws).

### 5.3 `VectorSearchService` + `Impl`

```java
public interface VectorSearchService {
    List<Blog> semanticSearch(String query, int topK);
    List<Blog> relatedTo(Long blogId, int topK);
}
```

Implementation outline:

```java
public List<Blog> semanticSearch(String query, int topK) {
    String qVec = embeddingService.embed(query);
    if (qVec == null) return Collections.emptyList();          // graceful

    // native query — reads stored VECTOR as a JSON string
    List<Object[]> rows = entityManager.createNativeQuery(
        "SELECT id, VECTOR_TO_STRING(embedding) FROM t_blog " +
        "WHERE embedding IS NOT NULL AND published = true"
    ).getResultList();

    double[] q = parse(qVec);
    return rows.stream()
        .map(r -> new Scored((Long) r[0], cosine(q, parse((String) r[1]))))
        .sorted(Comparator.comparingDouble(Scored::score).reversed())
        .limit(topK)
        .map(s -> blogRepository.findOne(s.id))
        .collect(Collectors.toList());
}
```

`parse()` reads `"[0.1, 0.2, ...]"` into a `double[]`; `cosine()` is a trivial
dot-product / norm. Both are ~10 lines.

---

## 6. Hook Points in Existing Code

### 6.1 Generate embedding on save

In `BlogServiceImpl.saveBlog()`, immediately after the existing AI summary
block, add:

```java
if (saved.isPublished() && (saved.getEmbedding() == null)) {
    try {
        String vec = embeddingService.embed(
            saved.getTitle() + "\n\n" + saved.getContent());
        if (vec != null) {
            // STRING_TO_VECTOR is required because JPA can't bind a VECTOR literal
            entityManager.createNativeQuery(
                "UPDATE t_blog SET embedding = STRING_TO_VECTOR(?1) WHERE id = ?2")
                .setParameter(1, vec)
                .setParameter(2, saved.getId())
                .executeUpdate();
            logger.info("Embedding stored for blog id={}", saved.getId());
        }
    } catch (Exception e) {
        logger.warn("Embedding generation failed for blog '{}'", saved.getTitle(), e);
    }
}
```

This **directly demonstrates the professor's `STRING_TO_VECTOR()` requirement**.

Also call the same block from `updateBlog()` so edits keep the embedding in
sync.

### 6.2 Wire semantic search into the public site

In `IndexController.search()` (line 42 of `IndexController.java`), replace the
single call to `blogService.listBlog(query, pageable)` with:

```java
List<Blog> hits = vectorSearchService.semanticSearch(query, 20);
if (hits.isEmpty()) {
    // fallback: legacy keyword search (so search still works without embeddings)
    model.addAttribute("page", blogService.listBlog(query, pageable));
} else {
    model.addAttribute("page", new PageImpl<>(hits, pageable, hits.size()));
}
```

### 6.3 Wire "related articles" into the blog detail page

In `IndexController.blog()` (line 50), add:

```java
model.addAttribute("related",
    vectorSearchService.relatedTo(id, 5));
```

Then in `templates/blog.html`, append a small `<section th:if="${!related.isEmpty()}">`
loop near the comments area.

---

## 7. Configuration

Append to `application.yml`:

```yaml
voyage:
  api-key: ${VOYAGE_API_KEY:}
  model:   ${VOYAGE_MODEL:voyage-3-lite}
  dimension: 512
```

Like the Claude integration, a missing key disables the feature silently.

---

## 8. Backfill Script (one-shot)

Existing blogs have `embedding = NULL`. Add a small dev-only endpoint or a
`CommandLineRunner` that iterates all published blogs and calls the same
embedding logic:

```java
@PostMapping("/admin/embeddings/backfill")
public String backfill() {
    blogService.listBlog(new PageRequest(0, 1000)).forEach(b -> {
        String vec = embeddingService.embed(b.getTitle() + "\n\n" + b.getContent());
        if (vec != null) { /* UPDATE t_blog SET embedding = STRING_TO_VECTOR(?) ... */ }
    });
    return "redirect:/admin/blogs";
}
```

Run it **once** after deploying Phase 1.

---

## 9. Testing Strategy

Mirror the existing test style (JUnit 4 + Mockito + `MockRestServiceServer`):

| Test class | Coverage |
|---|---|
| `EmbeddingServiceImplTest` | Happy path, null/empty input, API error, no API key, malformed response. Use `MockRestServiceServer` like `AiServiceImplTest`. |
| `VectorSearchServiceImplTest` | Cosine math on hand-built vectors, ranking order, graceful fallback when embedding service returns null, `topK` truncation. Mock the `EntityManager` query. |
| `BlogServiceImplTest` (extend) | `saveBlog_published_storesEmbedding`, `saveBlog_embeddingFailure_stillSaves`, `saveBlog_draft_skipsEmbedding`. |
| `IndexControllerTest` (extend) | `search_withSemanticHits_usesVectorResults`, `search_noSemanticHits_fallsBackToKeyword`. |

Target: keep the project's existing pattern of `~25–30` test methods per
service/controller test class.

---

## 10. Phased Rollout

Each phase compiles and ships on its own.

| Phase | Scope | Effort |
|---|---|---|
| **0** | Upgrade local MySQL → 9.0+. Run the `ALTER TABLE` to add `embedding VECTOR(512)`. | 30 min |
| **1** | Add `EmbeddingService` + impl. Wire into `BlogServiceImpl.saveBlog()`. Add tests. Manually publish a blog and verify with `SELECT id, VECTOR_DIM(embedding) FROM t_blog;` | 2–3 hrs |
| **2** | Add `VectorSearchService` + impl with cosine similarity. Add tests. | 2–3 hrs |
| **3** | Hook semantic search into `IndexController.search()`. Visible in UI. | 1 hr |
| **4** | Hook "related articles" into the blog detail page + template. | 1 hr |
| **5** | Backfill endpoint, run once for existing blogs. | 30 min |

**Total: roughly one weekend of work**, and every phase is demoable.

---

## 11. Demo Script for Your Professor

After Phase 3, this is a clean 5-minute walkthrough:

1. **Show the schema** — `SHOW COLUMNS FROM t_blog LIKE 'embedding';` → highlight `VECTOR(512)`.
2. **Show the write path** — `git grep STRING_TO_VECTOR` → point at the line in `BlogServiceImpl`.
3. **Show the read path** — `git grep VECTOR_TO_STRING` → point at `VectorSearchServiceImpl`.
4. **Live demo** — search for a concept that does **not** appear literally in any blog (e.g. search "machine learning" when the only relevant post is titled "Training a Neural Net"). The keyword search returns nothing; the semantic search returns the post.
5. **Show the row** — `SELECT id, title, VECTOR_DIM(embedding) FROM t_blog LIMIT 5;` → proves the data really is a vector.

---

## Appendix A — MySQL 8.4 Fallback (if you cannot upgrade)

Replace the `VECTOR(512)` column with `JSON` and skip `STRING_TO_VECTOR()`
entirely:

```sql
ALTER TABLE t_blog ADD COLUMN embedding JSON NULL;
```

Java side: store the embedding as the same `"[0.1, 0.2, ...]"` string (JSON
array). Read it back with a normal JPA query and parse with Jackson. Everything
else in this plan is identical.

**Trade-off:** you keep all the architectural value (semantic search, related
articles), but you lose the headline `STRING_TO_VECTOR()` call. Use this only
if upgrading MySQL is blocked.

---

## Appendix B — Files Touched, At a Glance

**New files (3):**
- `src/main/java/com/cqh/service/EmbeddingService.java`
- `src/main/java/com/cqh/service/EmbeddingServiceImpl.java`
- `src/main/java/com/cqh/service/VectorSearchService.java` (+ `Impl`)

**Modified files (5):**
- `src/main/java/com/cqh/po/Blog.java` — add `embedding` field + getter/setter
- `src/main/java/com/cqh/service/BlogServiceImpl.java` — generate embedding on save/update
- `src/main/java/com/cqh/web/IndexController.java` — use vector search + related
- `src/main/resources/application.yml` — add `voyage.*` config block
- `src/main/resources/templates/blog.html` — "Related articles" widget

**Schema migration (1):**
- `ALTER TABLE t_blog ADD COLUMN embedding VECTOR(512) NULL;`

**Test files (4):**
- `EmbeddingServiceImplTest` (new)
- `VectorSearchServiceImplTest` (new)
- `BlogServiceImplTest` (extend with 3 cases)
- `IndexControllerTest` (extend with 2 cases)
