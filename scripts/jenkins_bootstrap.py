#!/usr/bin/env python3
"""
One-shot bootstrap for local Jenkins:
  1) Global username/password credential for GitHub HTTPS (id: github-pat)
  2) Pipeline job from SCM (default: vishal1142/java, branch main)

Prereqs:
  pip install -r scripts/requirements-bootstrap.txt

Usage (PowerShell):
  cd "c:\\Users\\visha\\full cicd"
  copy secrets.env.example secrets.env
  # edit secrets.env — set GITHUB_PAT and JENKINS_PASSWORD if you changed it
  python scripts/jenkins_bootstrap.py

Or load env from a file (same keys as secrets.env.example):
  python scripts/jenkins_bootstrap.py --env-file secrets.env
"""

from __future__ import annotations

import argparse
import os
import sys
import textwrap


def _require_requests():
    try:
        import requests  # noqa: WPS433
    except ImportError as e:
        print("Missing dependency: pip install -r scripts/requirements-bootstrap.txt", file=sys.stderr)
        raise SystemExit(1) from e
    return requests


def _groovy_double_escape(value: str) -> str:
    """Escape for Groovy double-quoted string literals."""
    return (
        value.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("\r", "")
        .replace("\n", "\\n")
    )


def _load_dotenv(path: str) -> None:
    """Minimal KEY=VALUE loader (no export syntax, # comments)."""
    with open(path, encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            k, v = line.split("=", 1)
            k, v = k.strip(), v.strip().strip('"').strip("'")
            if k and k not in os.environ:
                os.environ[k] = v


def _crumb(session, base: str) -> tuple[str, str]:
    r = session.get(f"{base}/crumbIssuer/api/json", timeout=30)
    r.raise_for_status()
    data = r.json()
    return str(data["crumbRequestField"]), str(data["crumb"])


def _run_script(session, base: str, crumb_field: str, crumb: str, script: str) -> str:
    headers = {crumb_field: crumb}
    # Jenkins accepts application/x-www-form-urlencoded with key "script"
    r = session.post(
        f"{base}/scriptText",
        data={"script": script},
        headers=headers,
        timeout=120,
    )
    if r.status_code == 404:
        r = session.post(
            f"{base}/script",
            data={"script": script},
            headers=headers,
            timeout=120,
        )
    r.raise_for_status()
    return r.text


def main() -> int:
    p = argparse.ArgumentParser(description="Bootstrap GitHub credential + Pipeline job on Jenkins")
    p.add_argument("--env-file", help="Path to secrets.env (KEY=VALUE lines)")
    args = p.parse_args()

    if args.env_file:
        _load_dotenv(args.env_file)

    requests = _require_requests()

    base = os.environ.get("JENKINS_URL", "http://localhost:8080").rstrip("/")
    user = os.environ.get("JENKINS_USER", "admin")
    password = os.environ.get("JENKINS_PASSWORD") or os.environ.get("JENKINS_API_TOKEN")
    gh_user = os.environ.get("GITHUB_USERNAME", "vishal1142")
    gh_pat = os.environ.get("GITHUB_PAT", "")
    repo = os.environ.get("GITHUB_REPO_URL", "https://github.com/vishal1142/java.git")
    job_name = os.environ.get("JENKINS_JOB_NAME", "java-ci")
    jfile = os.environ.get("JENKINSFILE_PATH", "Jenkinsfile")

    if not password:
        print("Set JENKINS_PASSWORD or JENKINS_API_TOKEN", file=sys.stderr)
        return 1
    if not gh_pat:
        print("Set GITHUB_PAT (GitHub -> Settings -> Developer settings -> PAT)", file=sys.stderr)
        return 1

    session = requests.Session()
    session.auth = (user, password)

    crumb_field, crumb = _crumb(session, base)

    u = _groovy_double_escape(gh_user)
    pw = _groovy_double_escape(gh_pat)
    rid = "github-pat"
    ru = _groovy_double_escape(repo)
    jn = _groovy_double_escape(job_name)
    jf = _groovy_double_escape(jfile)

    script = textwrap.dedent(
        f"""
        import java.util.Collections
        import jenkins.model.Jenkins
        import com.cloudbees.plugins.credentials.CredentialsScope
        import com.cloudbees.plugins.credentials.SystemCredentialsProvider
        import com.cloudbees.plugins.credentials.domains.Domain
        import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
        import hudson.plugins.git.BranchSpec
        import hudson.plugins.git.GitSCM
        import org.jenkinsci.plugins.workflow.job.WorkflowJob
        import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition

        def j = Jenkins.getInstance()
        def credStore = j.getExtensionList(SystemCredentialsProvider.class)[0].getStore()

        def credId = "{rid}"
        def existing = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            UsernamePasswordCredentialsImpl.class, j, null, null
        ).find {{ it.id == credId }}
        if (existing != null) {{
          credStore.removeCredentials(Domain.global(), existing)
        }}
        def c = new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL,
            credId,
            "GitHub PAT (bootstrap)",
            "{u}",
            "{pw}"
        )
        credStore.addCredentials(Domain.global(), c)

        def jobName = "{jn}"
        def old = j.getItem(jobName)
        if (old != null) {{
          old.delete()
        }}
        def job = j.createProject(WorkflowJob.class, jobName)
        // Git plugin modern constructor (5 args) — see GitSCM javadoc on your Jenkins version.
        def scm = new GitSCM(
            GitSCM.createRepoList("{ru}", credId),
            Collections.singletonList(new BranchSpec("*/main")),
            null,
            null,
            Collections.emptyList()
        )
        job.setDefinition(new CpsScmFlowDefinition(scm, "{jf}"))
        job.save()

        return "OK: credential ${{credId}} + job ${{jobName}}"
        """
    ).strip()

    out = _run_script(session, base, crumb_field, crumb, script)
    print(out)
    if "ERROR" in out or "Exception" in out:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
