# Setup Guide — Branch 0.0.1

The base blog application. Just MySQL and Spring Boot — **no AI features, no API keys required.**

## What you get on this branch

- A personal blog with post CRUD, categories, tags, comments, search, and archives.
- Admin dashboard at `/admin` for managing posts.
- Public site at `/`.

---

## 1. Prerequisites

| Software | Version | Install on macOS |
|---|---|---|
| Java JDK | **1.8 (Java 8)** | `brew install --cask zulu@8`  *(or any JDK 8)* |
| Apache Maven | 3.x | `brew install maven` |
| MySQL | 8.x | Oracle `.dmg` or `brew install mysql@8.0` |
| Git | 2.x | Already on macOS |

> Spring Boot 1.5.7 will **not** run on JDK 9+. Stay on Java 8.

---

## 2. Clone the repo and switch to this branch

```bash
git clone https://github.com/changqinhao1996/blog.git ~/Desktop/blog
cd ~/Desktop/blog
git checkout 0.0.1
```

---

## 3. Create the database

Start MySQL 8 (however you installed it — Oracle's MySQL.app or `brew services start mysql`).

```bash
mysql -u root -p
```

```sql
CREATE DATABASE blog DEFAULT CHARACTER SET utf8mb4;
EXIT;
```

---

## 4. Configure database credentials

Open `src/main/resources/application-dev.yml` and make sure the `spring.datasource` block matches your local MySQL. The defaults in this branch are:

```yaml
url: jdbc:mysql://127.0.0.1:3306/blog?...
username: root
password: cy960720
```

Change `username` / `password` to whatever your local MySQL uses. The default active profile is `dev`, so this file is the one that takes effect.

---

## 5. Run the app

```bash
mvn spring-boot:run
```

Wait for the log line:

```
Started BlogApplication in N seconds
Tomcat started on port(s): 8080 (http)
```

JPA will auto-create the `t_blog`, `t_type`, `t_tag`, `t_comment`, `t_user` tables on first start (`ddl-auto: update`).

---

## 6. Open it

- **Public site:** http://localhost:8080
- **Admin dashboard:** http://localhost:8080/admin

There is no seed data. Log in to the admin dashboard and create a Category, a few Tags, then your first blog post.

> No default admin account exists either — you need to insert one into `t_user` manually before logging in:
> ```sql
> INSERT INTO t_user (username, password, nickname, email, type, avatar, create_time, update_time)
> VALUES ('admin', MD5('password'), 'Admin', 'you@example.com', 1, '/images/avatar.png', NOW(), NOW());
> ```
> Then log in as `admin` / `password`.

---

## 7. Stop the app

`Ctrl+C` in the terminal running `mvn spring-boot:run`.

---

## API keys on this branch

**None.** There are no AI features on 0.0.1. API keys (Voyage, Claude) become relevant starting at branch 0.0.2 — see that branch's `docs/SETUP.md`.

---

## Run the tests

```bash
mvn -o test -Dtest='*Test'
```

Expected: ~200 tests passing. (`BlogApplicationTests` is excluded by the `*Test` pattern because it requires a running MySQL — see `docs/CI_SETUP.md`.)

---

## What's on the other branches

| Branch | Adds |
|---|---|
| **0.0.1 (this branch)** | Base blog |
| **0.0.2** | Claude-powered auto-summary, auto-tagging, auto-description on publish |
| **0.0.3** | MySQL 9 vector search (semantic search + related articles) |
| **0.0.4** | "Ask my blog" RAG endpoint at `/ask` |

Each branch has its own `docs/SETUP.md` with the steps for the features added there.
