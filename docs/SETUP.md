# Setup Guide — Branch 0.0.5

Everything from 0.0.4 **plus OpenAI integration as the primary AI provider**. OpenAI is tried first for every AI call (summary, tags, description, suggest-tags endpoint, RAG `/ask` answer). Claude is the fallback. Both providers can be configured independently or together.

## What's new on this branch

| Change | Where |
|---|---|
| `LlmProvider` interface — single-method abstraction over a chat-completion call | `service/llm/LlmProvider.java` |
| `OpenAiProvider` — `@Order(1)`, tries `https://api.openai.com/v1/chat/completions` | `service/llm/OpenAiProvider.java` |
| `ClaudeProvider` — `@Order(2)`, extracted from the old `AiServiceImpl.callClaudeApi` | `service/llm/ClaudeProvider.java` |
| `AiServiceImpl` — refactored to inject `List<LlmProvider>` and iterate by `@Order` | `service/AiServiceImpl.java` |
| `openai` config block | `application.yml` |
| `OPENAI_API_KEY` / `OPENAI_MODEL` env vars | runtime |

How the fallback chain works for every AI call:

```
prompt assembled by AiServiceImpl
       ↓
OpenAiProvider.complete()
   ├─ configured?       → no  → skip, try next
   ├─ HTTP 200 + content → yes → return text
   ├─ HTTP error / null  → fall through to next provider
ClaudeProvider.complete()
   ├─ configured?       → no  → skip
   ├─ HTTP 200 + content → yes → return text
   ├─ HTTP error / null  → return null (graceful)
       ↓
AiServiceImpl returns the first non-empty response,
or null if no provider succeeds.
```

The chain is fully extensible — to add a new provider (Gemini, Mistral, etc.) drop a new `@Service @Order(N) class XyzProvider implements LlmProvider`. No change to `AiServiceImpl`.

---

## 1. Prerequisites

Same as 0.0.4:

| Software | Version |
|---|---|
| Java JDK | 1.8 (Java 8) |
| Apache Maven | 3.x |
| **MySQL 9.0+** | Required for `VECTOR(512)` |
| Git | 2.x |

**API keys (at least ONE required for AI features to actually return responses):**

| Key | Where to get | Used for | Required? |
|---|---|---|---|
| **`OPENAI_API_KEY`** | [platform.openai.com](https://platform.openai.com/api-keys) | Primary provider for ALL AI calls (summary, tags, description, RAG answer) | At least one of the two |
| **`CLAUDE_API_KEY`** | [console.anthropic.com](https://console.anthropic.com) → API Keys | Fallback provider — used when OpenAI is absent or its call fails | At least one of the two |
| **`VOYAGE_API_KEY`** | [voyageai.com](https://www.voyageai.com) → Sign up → API keys | Embedding for vector retrieval (unrelated to OpenAI/Claude) | **Yes** for any vector feature including `/ask` |

> **Without OPENAI_API_KEY**: the system silently skips OpenAI and falls through to Claude. Behaviour is identical to 0.0.4.
> **Without CLAUDE_API_KEY but with OPENAI_API_KEY**: everything still works; OpenAI handles every AI call.
> **Without either**: AI features degrade gracefully — summary/tags/description stay empty, `/ask` returns "Could not generate an answer".

---

## 2. Clone and switch branch

```bash
git clone https://github.com/changqinhao1996/blog.git ~/Desktop/blog
cd ~/Desktop/blog
git checkout 0.0.5
```

---

## 3. Install MySQL 9 side-by-side on port 3307

```bash
brew install mysql
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

## 5. Export API keys

```bash
# Primary AI provider (recommended on 0.0.5)
export OPENAI_API_KEY=sk-...

# Optional: fallback AI provider
export CLAUDE_API_KEY=sk-ant-api03-...

# Required for /ask and semantic search
export VOYAGE_API_KEY=pa-...
```

Recommended pattern — gitignored file you can source:

```bash
cat > ~/Desktop/blog/.env <<'EOF'
export OPENAI_API_KEY=sk-...
export CLAUDE_API_KEY=sk-ant-api03-...
export VOYAGE_API_KEY=pa-...
EOF
chmod 600 ~/Desktop/blog/.env
echo ".env" >> .gitignore     # only if not already
source .env
```

### Optional model override

By default OpenAI uses `gpt-4o-mini` (fast + cheap). Override with:

```bash
export OPENAI_MODEL=gpt-4o            # or any other Chat Completions model id
```

---

## 6. Start the app on the vector profile

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

```bash
/opt/homebrew/opt/mysql/bin/mysql -P 3307 -h 127.0.0.1 -u root -p blog_v9 \
    < docs/sql/V1__add_embedding_column.sql
```

---

## 8. Add content and backfill embeddings

```bash
curl -s -c cookies.txt -X POST http://localhost:8081/admin/login \
     --data-urlencode 'username=admin' --data-urlencode 'password=password' -o /dev/null
curl --max-time 300 -b cookies.txt http://localhost:8081/admin/blogs/backfill-embeddings
```

---

## 9. Try the RAG endpoint

Open **http://localhost:8081/ask** and ask:

```
How does the attention mechanism in transformers differ from a regular neural network?
```

Server log will show **which provider answered** — that's how you confirm OpenAI is being tried first:

```
EmbeddingServiceImpl : Embedding generated (dim=512)
AiServiceImpl        : AI response from OpenAI (1024 chars)    ← OpenAI succeeded
AiServiceImpl        : RAG answer generated (1024 chars, 5 sources)
```

If OpenAI is not configured or fails, you'll see the fallback in action:

```
AiServiceImpl        : AI response from Claude (1024 chars)    ← Claude fallback
```

---

## 10. How to verify the fallback chain

Quick experiments you can run after publishing a couple of blogs:

| Setup | Expected behaviour |
|---|---|
| Both keys set | OpenAI answers; log shows `AI response from OpenAI` |
| `unset OPENAI_API_KEY`, restart | Claude answers; log shows `AI response from Claude` |
| `unset CLAUDE_API_KEY`, restart with OpenAI | OpenAI answers; Claude is silently skipped |
| Both keys unset | `/ask` shows "Could not generate an answer. Check that CLAUDE_API_KEY is set." (the existing error message is kept for backward compat) |
| Override `OPENAI_MODEL=invalid` | OpenAI returns 4xx, AiService falls back to Claude automatically |

---

## API keys summary

| Key | Required? | Used for |
|---|---|---|
| **`OPENAI_API_KEY`** | At least one of the two | Primary AI provider for all generation tasks |
| **`CLAUDE_API_KEY`** | At least one of the two | Fallback AI provider |
| **`VOYAGE_API_KEY`** | Yes for vector / RAG features | Embedding the question (and blog content) |

---

## Run the tests

```bash
mvn -o test -Dtest='*Test'
```

Expected: **355 tests passing** (+31 over 0.0.4 — 13 `OpenAiProviderTest` + 11 `ClaudeProviderTest` + the refactored `AiServiceImplTest`).

The tests cover:
- OpenAI and Claude HTTP integration in isolation (vendor-specific tests under `service/llm`)
- AiService orchestration: tries OpenAI first, falls back to Claude on null/throw/blank, stops after first success, handles unconfigured providers, prompt assembly, RAG `max_tokens=800` vs summary `max_tokens=300`

---

## What's on the other branches

| Branch | Adds |
|---|---|
| 0.0.1 | Base blog (no AI) |
| 0.0.2 | Claude auto-summary / tagging / description |
| 0.0.3 | MySQL 9 vector search + related articles |
| 0.0.4 | "Ask my blog" RAG endpoint |
| **0.0.5 (this branch)** | OpenAI integration — primary AI provider, with Claude as fallback |
