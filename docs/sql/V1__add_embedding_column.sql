-- =====================================================================
-- V1 — Add embedding column to t_blog (MySQL 9.0+ only)
-- =====================================================================
-- Apply ONCE, after Spring Boot has created the t_blog table on the first
-- `mvn spring-boot:run -Dspring-boot.run.profiles=vector` run.
--
-- Hibernate cannot generate the VECTOR DDL, so this lives outside JPA.
-- =====================================================================

ALTER TABLE t_blog ADD COLUMN embedding VECTOR(512) NULL;

-- Speeds up the WHERE published = TRUE scan that VectorSearchService runs.
CREATE INDEX idx_blog_published ON t_blog(published);

-- Sanity checks
SELECT VERSION();                                    -- expect 9.x.x
SELECT VECTOR_DIM(STRING_TO_VECTOR('[1, 2, 3]'));    -- expect 3
SHOW COLUMNS FROM t_blog LIKE 'embedding';           -- expect VECTOR(512)
