# Blog

A personal blogging platform built with Spring Boot. It provides a public-facing
site for readers and a secured admin console for managing content. The
application is enhanced with **Claude AI** for automatic article summaries and
intelligent tag suggestions.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Data Model](#data-model)
- [Functions](#functions)
- [AI Features](#ai-features)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Testing](#testing)

---

## Tech Stack

| Layer        | Technology                                      |
|--------------|-------------------------------------------------|
| Language     | Java 8                                          |
| Framework    | Spring Boot 1.5.7 (Web, Data JPA, AOP, Thymeleaf)|
| Persistence  | Spring Data JPA / Hibernate, MySQL 8            |
| View         | Thymeleaf 3 + Layout Dialect                    |
| Markdown     | CommonMark (heading-anchor + GFM tables)        |
| AI           | Anthropic Claude API (Messages API)             |
| Build        | Maven                                           |
| Testing      | JUnit 4, Mockito, Spring MockMvc                |

---

## Architecture

The application follows a classic layered Spring MVC architecture:

```
                    HTTP Request
                         |
                         v
   +----------------------------------------------+
   |  Interceptor Layer   (LoginInterceptor)       |  -> guards /admin/**
   +----------------------------------------------+
                         |
                         v
   +----------------------------------------------+
   |  Controller Layer    (web / web.admin)        |  -> request mapping, view selection
   +----------------------------------------------+
                         |
                         v
   +----------------------------------------------+
   |  Service Layer       (service)                |  -> business logic, transactions
   |    BlogService, TagService, TypeService,      |
   |    CommentService, UserService, AiService ----+--> Claude API (HTTPS)
   +----------------------------------------------+
                         |
                         v
   +----------------------------------------------+
   |  Repository Layer    (dao)                    |  -> Spring Data JPA
   +----------------------------------------------+
                         |
                         v
                    MySQL Database
```

**Cross-cutting concerns:**

- **`LogAspect`** (AOP) — logs every controller request: URL, client IP, invoked
  method, and arguments.
- **`ControllerExceptionHandler`** — global `@ControllerAdvice` that renders a
  friendly error page; `NotFoundException` maps to a 404 view.
- **`LoginInterceptor` / `WebConfig`** — intercepts all `/admin/**` routes and
  redirects unauthenticated users to the login page.
- **Graceful AI degradation** — every Claude call is wrapped in try/catch; an AI
  failure never blocks saving a blog post.

---

## Project Structure

```
src/main/java/com/cqh/
├── BlogApplication.java          # Spring Boot entry point
├── NotFoundException.java        # custom 404 exception
├── aspect/
│   └── LogAspect.java            # AOP request logging
├── handler/
│   └── ControllerExceptionHandler.java
├── interceptor/
│   ├── LoginInterceptor.java     # admin auth guard
│   └── WebConfig.java            # interceptor registration
├── dao/                          # Spring Data JPA repositories
│   ├── BlogRepository.java
│   ├── CommentRepository.java
│   ├── TagRepository.java
│   ├── TypeRepository.java
│   └── UserRepository.java
├── po/                           # JPA entities
│   ├── Blog.java
│   ├── Comment.java
│   ├── Tag.java
│   ├── Type.java
│   └── User.java
├── vo/
│   └── BlogQuery.java            # search/filter form object
├── service/                      # business logic
│   ├── BlogService(Impl)
│   ├── CommentService(Impl)
│   ├── TagService(Impl)
│   ├── TypeService(Impl)
│   ├── UserService(Impl)
│   └── AiService(Impl)           # Claude API integration
├── util/
│   ├── MarkdownUtils.java        # Markdown -> HTML
│   ├── MD5Utils.java             # password hashing
│   └── MyBeanUtils.java          # null-property helper
└── web/
    ├── IndexController, AboutShowController,
    │   ArchiveShowController, TagShowController,
    │   TypeShowController, CommentController   # public pages
    └── admin/
        ├── LoginController, BlogController,
            TagController, TypeController       # admin console

src/main/resources/
├── application.yml               # base config + Claude settings
├── application-dev.yml            # dev profile (datasource, logging)
├── application-pro.yml            # production profile
├── i18n/                          # message bundles
├── static/                        # css, js, images, libs
└── templates/                     # Thymeleaf views (+ admin/, error/)
```

---

## Data Model

| Entity    | Description                                                              |
|-----------|--------------------------------------------------------------------------|
| `Blog`    | An article. Holds title, Markdown `content`, `firstPicture`, flags (published, recommend, appreciation, comment-enabled), `views`, timestamps, a manual `description`, and an AI-generated `summary`. Linked to `Type`, `User`, `Tag`s, and `Comment`s. |
| `Type`    | A category. One `Type` has many `Blog`s.                                 |
| `Tag`     | A label. Many-to-many with `Blog`.                                       |
| `Comment` | A reader comment. Self-referencing for threaded replies.                 |
| `User`    | An admin account (username + MD5-hashed password).                       |

The schema is auto-managed by Hibernate (`ddl-auto: update`).

---

## Functions

### Public Site

| Route                | Function                                                       |
|----------------------|----------------------------------------------------------------|
| `GET /`              | Home page — paginated list of blogs + recommended sidebar      |
| `POST /search`       | Full-text search across blog title and content                 |
| `GET /blog/{id}`     | Article detail — renders Markdown to HTML, increments view count|
| `GET /types/{id}`    | Blogs filtered by category                                     |
| `GET /tags/{id}`     | Blogs filtered by tag                                          |
| `GET /archives`      | Blogs grouped by year                                          |
| `GET /about`         | About page                                                    |
| `GET /comments/{blogId}` / `POST /comments` | View and post threaded comments    |

### Admin Console (`/admin/**`, login-protected)

| Route                       | Function                                            |
|-----------------------------|-----------------------------------------------------|
| `POST /admin/login` / `GET /admin/logout` | Authentication                        |
| `GET /admin/blogs`          | Manage blogs — list, paginate, filter               |
| `GET /admin/blogs/input`    | New blog form                                       |
| `GET /admin/blogs/{id}/input` | Edit blog form                                    |
| `POST /admin/blogs`         | Create or update a blog                             |
| `GET /admin/blogs/{id}/delete` | Delete a blog                                    |
| `/admin/types/**`           | Full CRUD for categories                            |
| `/admin/tags/**`            | Full CRUD for tags                                  |

---

## AI Features

The application integrates the **Anthropic Claude API** (`AiService` /
`AiServiceImpl`) to assist content authoring. All AI behavior is optional and
degrades gracefully — if the API key is missing or a call fails, the blog still
saves normally.

### 1. Automatic Blog Summary

- **When:** an admin creates and **publishes** a new blog (drafts are skipped),
  and the blog has no summary yet.
- **How:** `BlogServiceImpl.saveBlog()` sends the article content to Claude,
  which returns a concise 2–3 sentence summary stored in `Blog.summary`.
- **Where it shows:** the summary is used as the preview text on the public
  blog-card lists — **home page**, **category pages**, **tag pages**, and
  **search results**. If no summary exists, the page falls back to the manual
  `description` field.

### 2. AI Auto-Tagging

- **When:** an admin saves a blog **without selecting any tags**.
- **How:** `BlogController.post()` passes the content plus the list of all
  existing tags to Claude, which picks 1–5 relevant tags **from that list only**
  (it never invents new tags). Returned names are validated against real tags
  before being attached.
- **Fallback:** if there are no tags in the system, or the AI call fails, the
  blog is simply saved without tags.

### 3. Tag Suggestion Endpoint

- **`POST /admin/blogs/suggest-tags`** — returns a JSON array of suggested tag
  names for a given `content` parameter. Designed for an AJAX "suggest tags"
  action in the editor.

### Design Principles

- **Non-blocking** — every Claude call is wrapped in try/catch; AI errors are
  logged but never abort a save.
- **Idempotent summaries** — a summary is only generated when one does not
  already exist, so re-saving never overwrites it.
- **Tag safety** — the AI can only choose from existing tags.
- **Configuration-driven** — the feature is fully disabled when no API key is set.

---

## Configuration

`src/main/resources/application.yml`:

```yaml
claude:
  api-key: ${CLAUDE_API_KEY:}              # empty -> AI features disabled
  model:   ${CLAUDE_MODEL:claude-sonnet-4-6}
  max-tokens: 300
```

The Claude API key is supplied via the `CLAUDE_API_KEY` environment variable so
no secret is committed to source control.

Datasource and logging live in the profile-specific files
(`application-dev.yml`, `application-pro.yml`); the active profile is `dev` by
default.

---

## Running the Application

**Prerequisites:** JDK 8, Maven, MySQL 8 with a database named `blog`.

```bash
# 1. Set the Claude API key (optional — AI features are skipped without it)
export CLAUDE_API_KEY=sk-ant-...

# 2. Configure the datasource in application-dev.yml (url / username / password)

# 3. Build and run
mvn spring-boot:run
```

The site is then available at `http://localhost:8080/` and the admin console at
`http://localhost:8080/admin/login`. Hibernate creates/updates the schema
automatically on first run.

---

## Testing

The project has a comprehensive JUnit 4 + Mockito test suite covering entities,
utilities, services, controllers, interceptors, and AOP.

```bash
mvn test
```

- **Service tests** use Mockito to mock repositories; `AiServiceImplTest` uses
  `MockRestServiceServer` to stub the Claude HTTP API.
- **Controller tests** use standalone `MockMvc` to exercise routes, views, and
  the AI auto-tagging branches end-to-end.
