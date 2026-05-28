# MySQL 9 Side-by-Side Setup (for the `vector` profile)

This guide installs MySQL 9.x via Homebrew **alongside** the existing
MySQL 8.4 (`/usr/local/mysql`, port `3306`). MySQL 9 runs on port `3307`
with its own data directory, so the two never collide.

> **Why MySQL 9?** Only 9.0+ has the native `VECTOR` data type and the
> `STRING_TO_VECTOR()` / `VECTOR_TO_STRING()` functions that the blog's
> vector-search feature depends on.

---

## 1. Install MySQL 9.x

```bash
# Homebrew installs into /opt/homebrew on Apple Silicon (won't touch /usr/local)
brew install mysql
/opt/homebrew/opt/mysql/bin/mysql --version    # confirm 9.x.x
```

## 2. Create a private data directory + config on port 3307

```bash
mkdir -p ~/.mysql9/data
cat > ~/.mysql9/my.cnf <<'EOF'
[mysqld]
port     = 3307
datadir  = /Users/qinhaochang/.mysql9/data
socket   = /tmp/mysql9.sock
basedir  = /opt/homebrew/opt/mysql

[client]
port     = 3307
socket   = /tmp/mysql9.sock
EOF
```

## 3. Initialize and start

```bash
# One-time initialise (no root password)
/opt/homebrew/opt/mysql/bin/mysqld \
  --defaults-file=$HOME/.mysql9/my.cnf --initialize-insecure

# Start in background
/opt/homebrew/opt/mysql/bin/mysqld \
  --defaults-file=$HOME/.mysql9/my.cnf &
```

To stop later: `mysqladmin -P 3307 -h 127.0.0.1 -u root shutdown`.

## 4. Create the database and verify vector support

```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root <<'SQL'
ALTER USER 'root'@'localhost' IDENTIFIED BY 'cy960720';
CREATE DATABASE blog_v9 CHARACTER SET utf8mb4;

-- Sanity checks the professor will appreciate:
SELECT VERSION();                                   -- expect 9.x.x
SELECT VECTOR_DIM(STRING_TO_VECTOR('[1, 2, 3]'));   -- expect 3
SQL
```

## 5. Set the Voyage AI key

The blog uses Voyage AI (`voyage-3-lite`, 512-dim) to generate embeddings.
Get a free key at <https://www.voyageai.com> and:

```bash
export VOYAGE_API_KEY=pa-...
```

(Without this key the embedding code is a graceful no-op — saves still
succeed and the search falls back to keyword matching.)

## 6. Start the blog against MySQL 9

```bash
cd /Users/qinhaochang/Desktop/blog
mvn spring-boot:run -Drun.profiles=vector
```

> **Note (Spring Boot 1.5.x):** the profile flag is `-Drun.profiles=vector`.
> The `-Dspring-boot.run.profiles=...` form only works on Spring Boot 2.x and
> is **silently ignored** here — the app would start on the default `dev`
> profile against MySQL 8.4. To also run on a non-default HTTP port (e.g. so it
> can coexist with a `dev` instance already on 8080), add
> `-Drun.jvmArguments="-Dserver.port=8081"`.

On first start, JPA's `ddl-auto: update` will create `t_blog` and friends in
`blog_v9`. Hibernate **cannot** synthesise the `VECTOR(512)` DDL, so the next
step adds the column manually.

## 7. Apply the schema migration (one-time)

```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root -p blog_v9 \
  < docs/sql/V1__add_embedding_column.sql
```

## 8. Backfill embeddings for any existing blogs

```bash
# Log in as admin at http://localhost:8080/admin/login, then visit:
open http://localhost:8080/admin/blogs/backfill-embeddings
```

This iterates published blogs and runs
`UPDATE t_blog SET embedding = STRING_TO_VECTOR(?) WHERE id = ?` for each.

## 9. Verify the integration end-to-end

```sql
-- Count how many blogs now have a real vector stored
SELECT COUNT(*) FROM t_blog WHERE embedding IS NOT NULL;

-- Confirm dimension (Voyage `voyage-3-lite` = 512)
SELECT id, title, VECTOR_DIM(embedding) FROM t_blog
  WHERE embedding IS NOT NULL LIMIT 5;

-- Peek at the actual stored vector
SELECT SUBSTRING(VECTOR_TO_STRING(embedding), 1, 80) FROM t_blog
  WHERE embedding IS NOT NULL LIMIT 1;

-- Round-trip proof — both functions in one query
SELECT VECTOR_DIM(STRING_TO_VECTOR(VECTOR_TO_STRING(embedding)))
  FROM t_blog WHERE embedding IS NOT NULL LIMIT 1;
```

UI demo:
- Open <http://localhost:8080/>
- Search a concept whose words do **not** appear literally in any blog title
  (e.g. "machine learning" when the only relevant blog is "Training a Neural
  Net") — the semantic search returns it.
- Open any blog detail page — scroll to **Related articles** for the 5
  nearest neighbours.

## 10. Switch back to MySQL 8.4

Just omit the profile flag — the default `dev` profile points at
`localhost:3306`:

```bash
mvn spring-boot:run
```

Nothing on 8.4 was changed by any of these steps.

---

## Cheat sheet

| Action | Command |
|---|---|
| Start MySQL 9 | `mysqld --defaults-file=~/.mysql9/my.cnf &` |
| Stop MySQL 9 | `mysqladmin -P 3307 -h 127.0.0.1 -u root -p shutdown` |
| Run blog on MySQL 9 | `mvn spring-boot:run -Drun.profiles=vector` |
| Run blog on MySQL 9 (alt port) | `mvn spring-boot:run -Drun.profiles=vector -Drun.jvmArguments="-Dserver.port=8081"` |
| Run blog on MySQL 8.4 | `mvn spring-boot:run` |
| Connect to MySQL 9 | `mysql -P 3307 -h 127.0.0.1 -u root -p blog_v9` |
| Connect to MySQL 8.4 | `mysql -P 3306 -h 127.0.0.1 -u root -p blog` |
