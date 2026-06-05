# Setup Guide — Branch 0.0.3

The Claude AI features from 0.0.2 **plus MySQL 9 vector search** for semantic search and related articles.

> For an exhaustive runbook on the MySQL 9 install, see `docs/MYSQL_9_SETUP.md`. This file is the shorter step-by-step.

## What you get on this branch

Everything from 0.0.2, plus:

| Feature | Where it shows | Powered by |
|---|---|---|
| **Semantic search** | `POST /search` (the search box on the homepage) | Voyage AI embed query → MySQL 9 cosine ranking |
| **Related articles** | Bottom of any blog detail page | MySQL 9 `VECTOR_TO_STRING` + Java cosine |
| **Embedding on publish** | Automatic on `POST /admin/blogs` | Voyage AI → `STRING_TO_VECTOR()` in `t_blog.embedding` |
| **Backfill endpoint** | `GET /admin/blogs/backfill-embeddings` | Embeds every published post that lacks a vector |

Everything is opt-in via the **`vector` Spring profile** — the default `dev` profile still uses MySQL 8.4 and behaves like 0.0.2.

---

## 1. Prerequisites

Same as 0.0.2, plus:

| Software | Version | Notes |
|---|---|---|
| **MySQL 9.0+** | 9.0+ | **Required** for the `VECTOR` type. Installs side-by-side with your existing MySQL 8.x. |
| Java JDK | 1.8 (Java 8) | Same as 0.0.2 |
| Apache Maven | 3.x | |
| Git | 2.x | |

**API keys needed:**

| Key | Where to get | Required? |
|---|---|---|
| `VOYAGE_API_KEY` | [voyageai.com](https://www.voyageai.com) → Sign up → API keys | **Yes** for any vector feature |
| `CLAUDE_API_KEY` | [console.anthropic.com](https://console.anthropic.com) → API Keys | Optional (only used by the 0.0.2 AI features) |

---

## 2. Clone and switch branch

```bash
git clone https://github.com/changqinhao1996/blog.git ~/Desktop/blog
cd ~/Desktop/blog
git checkout 0.0.3
```

---

## 3. Install MySQL 9 side-by-side on port 3307

Homebrew installs MySQL 9 to `/opt/homebrew` so it doesn't touch any existing `/usr/local/mysql` 8.x. We give it its own port, data dir, and socket.

```bash
brew install mysql
/opt/homebrew/opt/mysql/bin/mysql --version    # confirm 9.x.x

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

# One-time initialise (no root password)
/opt/homebrew/opt/mysql/bin/mysqld --defaults-file=$HOME/.mysql9/my.cnf --initialize-insecure

# Start in background
/opt/homebrew/opt/mysql/bin/mysqld --defaults-file=$HOME/.mysql9/my.cnf &
```

To stop later: `mysqladmin -P 3307 -h 127.0.0.1 -u root -p shutdown`.

---

## 4. Create the database and confirm VECTOR works

```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root <<'SQL'
ALTER USER 'root'@'localhost' IDENTIFIED BY 'cy960720';
CREATE DATABASE blog_v9 CHARACTER SET utf8mb4;

-- Sanity checks
SELECT VERSION();                                  -- expect 9.x.x
SELECT VECTOR_DIM(STRING_TO_VECTOR('[1, 2, 3]'));  -- expect 3
SQL
```

If the password in `application-vector.yml` is different from `cy960720`, update one or the other so they match.

---

## 5. Export API keys

```bash
export VOYAGE_API_KEY=pa-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export CLAUDE_API_KEY=sk-ant-api03-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx    # optional
```

To keep them out of shell history, save them in a gitignored file and source it:

```bash
cat > ~/Desktop/blog/.env <<'EOF'
export VOYAGE_API_KEY=pa-...
export CLAUDE_API_KEY=sk-ant-...
EOF
chmod 600 ~/Desktop/blog/.env
echo ".env" >> .gitignore     # only if not already
source .env
```

**Without `VOYAGE_API_KEY`:** publishing still works, but no embedding is generated → semantic search falls back to keyword `LIKE` and related-articles shows nothing.

---

## 6. Start the app on the vector profile

> **Critical:** Spring Boot 1.5.x uses **`-Drun.profiles=vector`**, NOT `-Dspring-boot.run.profiles=vector` (which is silently ignored and defaults to `dev` against MySQL 8.4).

```bash
cd ~/Desktop/blog
source .env
mvn spring-boot:run \
    -Drun.profiles=vector \
    -Drun.jvmArguments="-Dserver.port=8081"
```

Wait for:
```
The following profiles are active: vector
Tomcat started on port(s): 8081 (http)
```

Port 8081 lets the vector instance run beside a normal `dev` instance on 8080.

---

## 7. One-time schema migration (add VECTOR(512) column)

JPA creates the relational tables on first launch, but Hibernate **cannot generate the `VECTOR` DDL**. Apply it manually:

```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root -p blog_v9 \
    < docs/sql/V1__add_embedding_column.sql
```

Confirm:
```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root -p blog_v9 \
    -e "SHOW COLUMNS FROM t_blog LIKE 'embedding';"
# expect: embedding   vector(512)   YES   NULL
```

---

## 8. Add content and backfill embeddings

Either publish posts via the admin UI (each publish auto-embeds), or seed sample posts via SQL and then trigger the backfill endpoint:

```bash
# Log in first
curl -s -c cookies.txt -X POST http://localhost:8081/admin/login \
     --data-urlencode 'username=admin' --data-urlencode 'password=password' -o /dev/null

# Backfill (may take ~1 min — Voyage free tier ~3 req/min, with auto retry/backoff)
curl --max-time 300 -b cookies.txt \
     http://localhost:8081/admin/blogs/backfill-embeddings
# Expect: Backfill complete — considered=N, written=N, skipped(draft)=0, failed=0
```

---

## 9. Verify

### Database layer
```sql
SELECT COUNT(*) FROM t_blog WHERE embedding IS NOT NULL;            -- expect N
SELECT id, title, VECTOR_DIM(embedding) FROM t_blog
  WHERE embedding IS NOT NULL LIMIT 5;                              -- dim should be 512
SELECT SUBSTRING(VECTOR_TO_STRING(embedding),1,80) FROM t_blog
  WHERE embedding IS NOT NULL LIMIT 1;                              -- prints [v1,v2,...]
```

### Application layer
- Open http://localhost:8081/ — search a concept that's *not* a literal title word (e.g. "machine learning" when your post is "Training a Neural Net"). Semantic match returned ✓
- Open any blog detail page → scroll to **Related articles** → 5 nearest neighbours by cosine ✓

---

## 10. Stop & switch back

```bash
pkill -f "run.profiles=vector"               # stop the vector instance
mvn spring-boot:run                          # default 'dev' profile → MySQL 8.4 → behaves like 0.0.2
```

MySQL 9 keeps running idly. Stop it with:
```bash
mysqladmin -P 3307 -h 127.0.0.1 -u root -p shutdown
```

---

## API keys summary

| Key | Required for | What breaks without it |
|---|---|---|
| **`VOYAGE_API_KEY`** | Semantic search, related articles, embedding on publish, backfill | Search falls back to keyword `LIKE`; related-articles shows nothing |
| **`CLAUDE_API_KEY`** | The 0.0.2 AI features (summary, tags, description) | Those fields stay empty on publish; vector features unaffected |

---

## Run the tests

```bash
mvn -o test -Dtest='*Test'
```

Expected: ~309 tests passing. All HTTP calls (Voyage, Claude) are mocked, so no keys needed for tests.

---

## What's on the other branches

| Branch | Adds |
|---|---|
| 0.0.1 | Base blog (no AI) |
| 0.0.2 | Claude auto-summary / tagging / description |
| **0.0.3 (this branch)** | MySQL 9 vector search + related articles |
| 0.0.4 | "Ask my blog" RAG endpoint at `/ask` (chains retrieval + Claude) |
