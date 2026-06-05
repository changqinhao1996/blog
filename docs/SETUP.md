# Setup Guide — Branch 0.0.2

The base blog **plus Claude-powered AI features**: auto-summary, auto-tagging, and auto-description on publish.

## What you get on this branch

Everything from 0.0.1, plus three AI features that fire automatically when you publish a blog:

| Feature | When it fires | Endpoint / location |
|---|---|---|
| **Auto-summary** | On publish, if no summary exists | Stored in `t_blog.summary`, shown on list pages |
| **Auto-tagging** | On publish, if no tags selected | Claude picks tags from your existing tag set |
| **Auto-description** | On publish, if description left blank | Stored in `t_blog.description`, shown as preview text |
| **Suggest-tags REST endpoint** | Always available | `POST /admin/blogs/suggest-tags` |

All AI calls **degrade gracefully** — if no `CLAUDE_API_KEY` is set, publish still works, the AI step is just skipped.

---

## 1. Prerequisites

Same as 0.0.1:

| Software | Version |
|---|---|
| Java JDK | **1.8 (Java 8)** |
| Apache Maven | 3.x |
| MySQL | 8.x |
| Git | 2.x |

**New for 0.0.2:** an **Anthropic Claude API key**. Get one at [console.anthropic.com](https://console.anthropic.com) → API Keys.

---

## 2. Clone and switch branch

```bash
git clone https://github.com/changqinhao1996/blog.git ~/Desktop/blog
cd ~/Desktop/blog
git checkout 0.0.2
```

---

## 3. Create the database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE blog DEFAULT CHARACTER SET utf8mb4;
EXIT;
```

Edit `src/main/resources/application-dev.yml` and adjust `spring.datasource.username` / `.password` to your local MySQL.

---

## 4. Get and export your Claude API key

```bash
export CLAUDE_API_KEY=sk-ant-api03-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

> **Without this key:** the app still runs and you can publish blogs — the AI fields just stay empty. So you can skip this step if you only want to test the base blog features.

To keep the key out of your shell history, store it in a gitignored file you can `source`:

```bash
cat > ~/Desktop/blog/.env <<'EOF'
export CLAUDE_API_KEY=sk-ant-api03-...
EOF
chmod 600 ~/Desktop/blog/.env
echo ".env" >> .gitignore     # only if not already ignored
source .env
```

### Optional Claude config

The defaults are fine; override only if you want a different model or token budget:

```bash
export CLAUDE_MODEL=claude-sonnet-4-6     # default
# claude.max-tokens defaults to 300 (set in application.yml)
```

---

## 5. Run the app

```bash
cd ~/Desktop/blog
source .env                                # if you used the file pattern
mvn spring-boot:run
```

Wait for:

```
Started BlogApplication in N seconds
Tomcat started on port(s): 8080 (http)
```

---

## 6. Try the AI features

1. Open http://localhost:8080/admin and log in (admin user — seed one in `t_user` if you haven't, see 0.0.1's SETUP.md).
2. Click **Publish**, write a short post (Markdown). Leave the **Description** field blank and don't select any tags.
3. Click **Publish**.
4. Re-open the post — it should now have:
   - An auto-generated **summary** on the public list page.
   - Auto-assigned **tags** (picked by Claude from your existing tags).
   - An auto-generated one-sentence **description**.
5. Server log will show lines like:

```
AiServiceImpl    : AI summary generated successfully
BlogServiceImpl  : AI summary generated for blog: <title>
AiServiceImpl    : AI description generated successfully (~123 chars)
```

---

## 7. Stop the app

`Ctrl+C` in the terminal running `mvn spring-boot:run`.

---

## API keys summary

| Key | Where to get | Required? | Used for |
|---|---|---|---|
| **`CLAUDE_API_KEY`** | [console.anthropic.com](https://console.anthropic.com) | Optional (graceful degradation if missing) | Summary, tags, description generation |

No Voyage key on this branch — Voyage AI is only used starting at 0.0.3 for vector embeddings.

---

## Run the tests

```bash
mvn -o test -Dtest='*Test'
```

Expected: ~278 tests passing (vs ~200 on 0.0.1 — the extra 78 cover the AI features). All HTTP calls to Claude are mocked, so no API key needed for tests.

---

## What's on the other branches

| Branch | Adds |
|---|---|
| 0.0.1 | Base blog (no AI) |
| **0.0.2 (this branch)** | Claude auto-summary / tagging / description |
| 0.0.3 | MySQL 9 vector search (semantic search + related articles) |
| 0.0.4 | "Ask my blog" RAG endpoint at `/ask` |
