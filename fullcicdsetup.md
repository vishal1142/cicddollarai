# Full CI/CD stack — step-by-step setup

This document describes how the local CI/CD environment in this repository is put together: Docker Compose services, Jenkins as Code (CasC), shared pipeline library, and the optional Python bootstrap for Git credentials and a first pipeline job.

---

## 1. Prerequisites

Install and verify on your machine:

- **Docker Desktop** (Windows) with Linux containers, WSL2 backend recommended  
- **Docker Compose** v2 (`docker compose` in the terminal)  
- **Git** (to clone repos and for the Jenkins shared library working copy)  
- **Python 3.11+** (only if you use `scripts/jenkins_bootstrap.py`)

---

## 2. What the stack includes

| Service        | Container name     | Port  | Role |
|----------------|--------------------|-------|------|
| Jenkins (CasC)| `my-jenkins-server`| 8080, 50000 | CI controller + Docker cloud agents |
| SonarQube     | `my-sonarqube`     | 9000  | Code quality |
| PostgreSQL    | `sonar-postgres`   | (internal) | SonarQube database |
| Nexus 3       | `my-nexus`         | 8081  | Artifact repository |
| pgAdmin       | `pgadmin`          | 5050  | Optional DB UI |

All services share the Docker network `my-ci-network` defined in `docker-compose.cicd.yml`.

---

## 3. Jenkins “as code” image (`jenkins-as-code/`)

### 3.1 Dockerfile

- Base image: `jenkins/jenkins:lts-jdk17` (LTS + JDK 17 so plugin CLI requirements stay satisfied).  
- **Plugins** are installed at build time from `plugins.txt` via `jenkins-plugin-cli`.  
- **CasC** seed file is copied to `/usr/share/jenkins/ref/casc/jenkins.yaml` so a fresh volume still gets the reference config.  
- `CASC_JENKINS_CONFIG=/var/jenkins_home/casc` so live config is read from the Jenkins home `casc` folder (overridable by the bind mount in Compose).

### 3.2 `plugins.txt`

Core plugins pinned by name (versions resolved at build):

- `configuration-as-code`  
- `workflow-aggregator`  
- `git`  
- `docker-workflow`  
- `docker-plugin`  

### 3.3 `jenkins.yaml` (Configuration as Code)

Highlights:

- **Executors on controller:** `numExecutors: 0` — builds are not meant to run on the controller.  
- **Docker cloud:** agents with label `docker-agent`, image `jenkins/inbound-agent:jdk17`, host Docker socket mounted into the agent template so pipelines can use Docker on the host.  
- **Security:** local user `admin` / password `changeme` (change immediately in production or after first login).  
- **Global Pipeline Library** named `jenkinslibrary`, retrieved from `file:///var/jenkins_library` (must be a Git working copy on the host, bind-mounted — see next section).  
- **Jenkins URL** in CasC: `http://localhost:8080/`.

Pipelines that use the library should reference it consistently, for example: `@Library('jenkinslibrary@master')` in the `Jenkinsfile` (library name must match CasC).

---

## 4. Docker Compose (`docker-compose.cicd.yml`)

### 4.1 Build and run Jenkins

- Service **`ci-jenkins`** builds from `./jenkins-as-code` and tags the image as `my-jenkins-casc:latest`.  
- **`user: root`** is set for **local Docker Desktop on Windows** so the Jenkins process can use the mounted `docker.sock` (development convenience only; do not use this pattern blindly in production).  
- **Volumes:**  
  - `jenkins-data` → `/var/jenkins_home`  
  - Bind mount: `./jenkins-as-code/jenkins.yaml` → `/var/jenkins_home/casc/jenkins.yaml` (read-only) so you can edit CasC in the repo without rebuilding the image.  
  - `docker.sock` mounted for Docker-in-Docker-style agents on the host.  
  - **Shared library folder:** host path bind-mounted to `/var/jenkins_library` (read-only).  
    - In this repo the example path is `C:/Users/visha/Jenkins library` — **change this** on your PC to your actual clone of the shared library repo.

### 4.2 First run / CasC not applying

If you previously ran the stock Jenkins image against the same volume name, old data can prevent CasC from behaving as expected. One-time reset:

```powershell
docker compose -f docker-compose.cicd.yml down -v
```

Then bring the stack up again (this **deletes** named volumes including Jenkins data).

### 4.3 Start the full stack

From the repository root (path with a space must be quoted on Windows):

```powershell
cd "C:\Users\visha\full cicd"
docker compose -f docker-compose.cicd.yml build
docker compose -f docker-compose.cicd.yml up -d
docker compose -f docker-compose.cicd.yml ps
```

### 4.4 Optional: Jira

Jira is **not** part of `docker-compose.cicd.yml` in this setup; a separate `docker-compose.jira.yml` exists if you want Jira locally. Comments at the bottom of the CICD file list URLs and useful `docker` / `docker compose` commands.

---

## 5. Prepare the Jenkins shared library (host)

1. Clone your shared library repository to a folder on the host (example remote: `https://github.com/vishal1142/jenkinslibrary.git`).  
2. In `docker-compose.cicd.yml`, set the bind mount so the **left** side of the mapping is that folder and the **right** side stays `/var/jenkins_library:ro`.  
3. Ensure the folder is a valid Git repo (CasC uses `file:///var/jenkins_library` as the SCM URL).

If you prefer remote Git instead of a file URL, change the `globalLibraries` section in `jenkins.yaml` to use an `https` remote and appropriate credentials (not covered by the bootstrap script’s file-based library).

---

## 6. After containers are up — URLs and defaults

| Application | URL | Default access |
|-------------|-----|----------------|
| Jenkins | http://localhost:8080 | `admin` / `changeme` (change immediately) |
| SonarQube | http://localhost:9000 | Complete SonarQube’s first-run wizard in the UI |
| Nexus | http://localhost:8081 | Retrieve `admin` password from container logs on first start |
| pgAdmin | http://localhost:5050 | `admin@example.com` / `admin` (change for anything beyond local dev) |

SonarQube JDBC (inside Compose only): `jdbc:postgresql://sonar-db:5432/sonar`, user/password `sonar` / `sonar` as set in the compose file.

---

## 7. Secrets file and Jenkins bootstrap (optional automation)

### 7.1 Create `secrets.env`

1. Copy the example file:

   ```powershell
   copy secrets.env.example secrets.env
   ```

2. Edit `secrets.env` (this file is listed in `.gitignore` — do not commit it).  
3. Set at minimum:  
   - `JENKINS_PASSWORD` — your Jenkins `admin` password **or** a Jenkins API token (`JENKINS_API_TOKEN` is also supported by the script if you set it instead).  
   - `GITHUB_PAT` — a GitHub personal access token with appropriate repo scopes for the pipeline repo (private repos need read access to code).

Other variables (`JENKINS_URL`, `GITHUB_REPO_URL`, `JENKINS_JOB_NAME`, etc.) are documented in `secrets.env.example`.

### 7.2 Install Python dependency

```powershell
pip install -r scripts/requirements-bootstrap.txt
```

(`requests` is the only dependency.)

### 7.3 Run the bootstrap script

From the repo root:

```powershell
python scripts/jenkins_bootstrap.py --env-file secrets.env
```

What it does (via Jenkins script console API, with CSRF crumb):

1. Creates or replaces a **global** username/password credential with ID **`github-pat`** (GitHub user + PAT).  
2. Creates or replaces a **Pipeline** job (default name `java-ci`) from SCM pointing at **`GITHUB_REPO_URL`**, branch **`*/main`**, using **`JENKINSFILE_PATH`** (default `Jenkinsfile`).

Adjust env vars in `secrets.env` if your repo URL, job name, or branch differs.

---

## 8. Running pipelines

- Use **`agent { label 'docker-agent' }`** (or equivalent) so work runs on Docker agents, matching the CasC cloud definition.  
- Use the shared library name **`jenkinslibrary`** if you configured the global library as in `jenkins.yaml`.  
- For Git operations inside the job, reference the credential ID **`github-pat`** if your `Jenkinsfile` expects that ID (as created by the bootstrap script).

---

## 9. Quick troubleshooting

| Issue | What to try |
|-------|-------------|
| Jenkins UI shows old config | `docker compose -f docker-compose.cicd.yml down -v` once, then `up -d` again (loses Jenkins volume data). |
| Permission denied on `docker.sock` (Windows) | Confirm `user: root` on `ci-jenkins` for local dev, or fix socket permissions for the `jenkins` user. |
| Shared library not found | Check the host bind mount path exists and is a Git clone; check CasC `file:///var/jenkins_library` matches the mount. |
| Bootstrap script fails auth | Verify `JENKINS_URL`, and password or API token; ensure Jenkins is fully up. |
| Bootstrap says missing `GITHUB_PAT` | Set `GITHUB_PAT` in `secrets.env` or the environment. |

---

## 10. File map (this setup)

| Path | Purpose |
|------|---------|
| `docker-compose.cicd.yml` | Full stack: Jenkins build, Sonar, Postgres, Nexus, pgAdmin |
| `jenkins-as-code/Dockerfile` | Custom Jenkins image + plugins + CasC ref copy |
| `jenkins-as-code/plugins.txt` | Plugin list for `jenkins-plugin-cli` |
| `jenkins-as-code/jenkins.yaml` | Live CasC (also bind-mounted into the container) |
| `secrets.env.example` | Template for bootstrap secrets |
| `secrets.env` | Local secrets (gitignored) |
| `scripts/jenkins_bootstrap.py` | Credential + Pipeline job bootstrap |
| `scripts/requirements-bootstrap.txt` | Python deps for bootstrap |

This completes the step-by-step configuration for the full local CI/CD stack as defined in this repository.
