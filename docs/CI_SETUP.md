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
| PR opened/updated against `main` or `0.0.*` | Runs full test suite on the PR's HEAD |
| Push to `main` | Runs tests as a final safety check after merge |
| New commit to an open PR | Cancels the previous still-running build |
| Tests fail | Surefire XML reports uploaded as artifacts (kept 7 days) for triage |

## Local verification

The same command runs locally:

```bash
mvn -B -ntp test
```

This needs JDK 8 on your `PATH`. Other JDK versions may compile but the
Spring Boot 1.5.7 runtime classes are Java-8 era and won't reliably run
on newer JDKs without extra flags.
