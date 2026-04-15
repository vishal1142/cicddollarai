# cicddollarai

This repository holds the **local full CI/CD stack** (Docker Compose, Jenkins as Code, bootstrap scripts) and the **Jenkins Global Pipeline Library** (`vars/*.groovy`).

## Layout

| Path | Purpose |
|------|--------|
| `docker-compose.cicd.yml` | Jenkins, SonarQube, Nexus, etc. |
| `jenkins-as-code/` | Controller image + CasC (`jenkins.yaml`), agent `Dockerfile.agent` |
| `scripts/` | `jenkins_bootstrap.py` (GitHub credential + pipeline job) |
| `secrets.env.example` | Copy to `secrets.env` locally (never commit `secrets.env`) |
| `fullcicdsetup.md` | Step-by-step setup notes |
| `vars/` | Shared library steps loaded as **`jenkinslibrary`** |

The sample Java application and its `Jenkinsfile` stay in **[vishal1142/java](https://github.com/vishal1142/java)** so you do not maintain two copies of the app.

## Jenkins library

- Default branch: **main**
- In CasC, this repo is registered as library **`jenkinslibrary`** (see `jenkins-as-code/jenkins.yaml`).

In the app `Jenkinsfile`:

```groovy
@Library('jenkinslibrary@main') _
```

Pass `credentialsId: 'github-pat'` into `gitCheckout([...])` when the application repo is private.

## Quick start

Clone this repo, copy `secrets.env.example` → `secrets.env`, fill values, then follow `fullcicdsetup.md` (build agent image, `docker compose`, run bootstrap).
