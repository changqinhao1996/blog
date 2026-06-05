# Setup Guide — Branch 0.0.4

Everything from 0.0.3 **plus the "Ask my blog" Retrieval-Augmented Generation (RAG) endpoint** at `GET / POST /ask`.

## What you get on this branch

Everything from 0.0.3, plus:

| Feature | Where | Powered by |
|---|---|---|
| **Ask my blog** | `GET /ask` (form) + `POST /ask` (answer) | Vector retrieval **chained into** Claude generation |
| Nav menu "Ask AI" item | All public pages | Links to `/ask` |

How the chain works:

```
your question
  → Voyage embeds it (dim 512)
  → MySQL 9 ranks the top-5 blog posts by cosine similarity (VECTOR_TO_STRING)
  → those posts become numbered context in a Claude prompt
  → Claude answers, citing sources as [1], [2], ...
  → Claude refuses to answer if the posts don't cover the question (no hallucination)
```

Both halves degrade gracefully — missing `VOYAGE_API_KEY` or `CLAUDE_API_KEY` yields a friendly error, never a crash.

---

## 1. Prerequisites

Same as 0.0.3:

| Software | Version |
|---|---|
| Java JDK | 1.8 (Java 8) |
| Apache Maven | 3.x |
| **MySQL 9.0+** | Required for `VECTOR(512)` |
| Git | 2.x |

**Both API keys are required for /ask to actually answer:**

| Key | Where to get | Used for | Required? |
|---|---|---|---|
| **`VOYAGE_API_KEY`** | [voyageai.com](https://www.voyageai.com) → Sign up → API keys | Embedding the question (and blog content) into 512-dim vectors | **Yes** — without it the retrieval step returns nothing |
| **`CLAUDE_API_KEY`** | [console.anthropic.com](https://console.anthropic.com) → API Keys | Generating the grounded answer with citations | **Yes** — without it `/ask` returns a friendly "Could not generate an answer" message |

> Without either key the page still renders, the form still submits, no exception is thrown — but you won't get a real answer. This is intentional graceful degradation.

---

## 2. Clone and switch branch

```bash
git clone https://github.com/changqinhao1996/blog.git ~/Desktop/blog
cd ~/Desktop/blog
git checkout 0.0.4
```

---

## 3. Install MySQL 9 side-by-side on port 3307

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

/opt/homebrew/opt/mysql/bin/mysqld --defaults-file=$HOME/.mysql9/my.cnf --initialize-insecure
/opt/homebrew/opt/mysql/bin/mysqld --defaults-file=$HOME/.mysql9/my.cnf &
```

---

## 4. Create the database

```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root <<'SQL'
ALTER USER 'root'@'localhost' IDENTIFIED BY 'cy960720';
CREATE DATABASE blog_v9 CHARACTER SET utf8mb4;

SELECT VERSION();                                  -- expect 9.x.x
SELECT VECTOR_DIM(STRING_TO_VECTOR('[1, 2, 3]'));  -- expect 3
SQL
```

---

## 5. Export BOTH API keys

```bash
export VOYAGE_API_KEY=pa-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export CLAUDE_API_KEY=sk-ant-api03-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Recommended pattern — store both in a gitignored file:

```bash
cat > ~/Desktop/blog/.env <<'EOF'
export VOYAGE_API_KEY=pa-...
export CLAUDE_API_KEY=sk-ant-...
EOF
chmod 600 ~/Desktop/blog/.env
echo ".env" >> .gitignore     # only if not already ignored
source .env
```

---

## 6. Start the app on the vector profile

> **Critical:** Spring Boot 1.5 uses `-Drun.profiles=vector`. The `-Dspring-boot.run.profiles=...` form is silently ignored and defaults to `dev` against MySQL 8.4.

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

---

## 7. One-time schema migration

JPA creates the tables on first launch; Hibernate cannot generate `VECTOR(512)`. Apply it manually:

```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root -p blog_v9 \
    < docs/sql/V1__add_embedding_column.sql
```

---

## 8. Add content and backfill embeddings

`/ask` needs posts with vectors. Either publish a few via the admin UI (auto-embeds), or seed via SQL then run the backfill:

```bash
# log in
curl -s -c cookies.txt -X POST http://localhost:8081/admin/login \
     --data-urlencode 'username=admin' --data-urlencode 'password=password' -o /dev/null

# backfill (~1 min due to Voyage free-tier rate limit + retry/backoff)
curl --max-time 300 -b cookies.txt \
     http://localhost:8081/admin/blogs/backfill-embeddings
```

---

## 9. Try the RAG endpoint

Open **http://localhost:8081/ask** and try three deliberately different questions:

### 9.1  Multi-source synthesis
```
How does the attention mechanism in transformers differ from a regular neural network?
```
Expect: answer cites `[1]` and `[2]`; sources include both the Transformers post and the Neural Network post.

### 9.2  Single-source
```
What temperature water should I use for pour-over coffee?
```
Expect: answer cites `[1]`; the Coffee post is the top source. If the post doesn't give an exact figure, Claude will say so rather than invent one.

### 9.3  The honesty test (proves it's RAG, not "ask Claude anything")
```
What is the capital of Mongolia?
```
Expect: Claude says the blog doesn't cover Mongolia and refuses to answer — even though it *knows* the answer from training. That refusal is the defining safety property of RAG.

---

## 10. Same demo from the terminal

```bash
curl -s -X POST http://localhost:8081/ask \
  --data-urlencode 'question=How does attention work in transformers?' \
  | sed -n '/Answer/,/Sources/p' | sed 's/<[^>]*>//g' | tr -s '[:space:]'
```

Server log shows the full chain:
```
EmbeddingServiceImpl : Embedding generated (dim=512)
AiServiceImpl        : RAG answer generated (N chars, 5 sources)
RagServiceImpl       : RAG ask: question='...' k=5 sourcesFound=5 answerChars=N
```

---

## 11. Stop & switch back

```bash
pkill -f "run.profiles=vector"               # stop the vector instance
mvn spring-boot:run                          # default 'dev' profile → MySQL 8.4
mysqladmin -P 3307 -h 127.0.0.1 -u root -p shutdown    # optional, stop MySQL 9
```

---

## API keys summary

| Key | Required for | Without it, /ask says |
|---|---|---|
| **`VOYAGE_API_KEY`** | Embedding your question for retrieval | "No relevant blog posts found for your question" |
| **`CLAUDE_API_KEY`** | Generating the grounded answer | "Could not generate an answer. Check that CLAUDE_API_KEY is set." |

Both keys are required for a working `/ask`. The 0.0.3 features (semantic search, related articles) still work with just `VOYAGE_API_KEY`.

---

## Run the tests

```bash
mvn -o test -Dtest='*Test'
```

Expected: **324 tests passing**. All HTTP calls are mocked; no API keys needed for tests.

---

## What's on the other branches

| Branch | Adds |
|---|---|
| 0.0.1 | Base blog (no AI) |
| 0.0.2 | Claude auto-summary / tagging / description |
| 0.0.3 | MySQL 9 vector search + related articles |
| **0.0.4 (this branch)** | "Ask my blog" RAG endpoint — chains retrieval into Claude generation |
