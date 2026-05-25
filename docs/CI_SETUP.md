# CI Setup — Block Merging on Test Failures

This repo uses GitHub Actions to run `mvn test` on every pull request
(see `.github/workflows/test.yml`). Out of the box, the workflow only
**reports** pass/fail — it does **not** block merges by itself. GitHub
needs to be told to treat the check as required.

## One-time GitHub setup (admin)

After this workflow has run at least once on any branch (so the check
name `mvn test (JDK 8)` is registered in GitHub), enable the protection
rule:

1. Go to **Settings → Branches → Branch protection rules → Add rule**
   (or **Add classic branch protection rule**).
2. **Branch name pattern:** `main`
3. Check **Require status checks to pass before merging**.
4. Check **Require branches to be up to date before merging** (optional but
   recommended — forces PRs to be rebased on latest `main` before they merge).
5. In the search box under "Status checks that are required", select
   **`mvn test (JDK 8)`** (the job name from `test.yml`).
6. Click **Create**.

Optionally repeat for the `0.0.*` pattern if you want the same protection
on release branches.

After this, the **Merge pull request** button on any PR targeting `main`
will be greyed out until the `mvn test` check is green.

## What the workflow does

| Trigger | Action |
|---|---|
| PR opened/updated against `main` or `0.0.*` | Runs the unit-test suite on the PR's HEAD |
| Push to `main` | Runs tests as a final safety check after merge |
| New commit to an open PR | Cancels the previous still-running build |
| Tests fail | Surefire XML reports uploaded as artifacts (kept 7 days) for triage |

## What CI runs (and what it skips)

CI runs:

```bash
mvn -B -ntp test -Dtest='*Test'
```

That matches every class ending in `*Test` (singular) — all unit tests.
The pattern intentionally excludes `BlogApplicationTests` (the only class
ending in `*Tests`, plural). It's a full `@SpringBootTest` that boots the
entire ApplicationContext, which in turn asks Hibernate to validate every
JPQL query in `BlogRepository` — and one of those (`findGroupYear()`) uses
MySQL's `date_format()` function. Without a live MySQL server on the runner,
that validation fails. Substituting H2 doesn't help because H2 has no
`date_format()` function.

Every other test is a pure unit test using Mockito mocks or standalone
`MockMvc`, so they need no database and run cleanly in CI.

Surefire 2.18.1 (this project's version) doesn't support `!Pattern`
negation, so we filter by the naming convention instead. If the project
ever adds another `*Tests` (plural) class, decide explicitly whether it
needs a DB and either rename it to `*Test` or update the workflow.

## Local verification

Run the full suite (including `BlogApplicationTests`) when MySQL is up:

```bash
mvn -B -ntp test
```

Or replicate exactly what CI does:

```bash
mvn -B -ntp test -Dtest='*Test'
```

This needs JDK 8 on your `PATH`. Other JDK versions may compile but the
Spring Boot 1.5.7 runtime classes are Java-8 era and won't reliably run
on newer JDKs without extra flags.
