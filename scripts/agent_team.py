#!/usr/bin/env python3
"""Run a provider-neutral coding team in isolated Git worktrees."""

from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import secrets
import shlex
import shutil
import subprocess
import sys


ROOT = Path(__file__).resolve().parent.parent
ROLES = ROOT / ".agent-team" / "roles"
RUNS = ROOT / ".agent-team" / "runs"
PROVIDERS = ("claude", "hermes", "codex")


def command(argv: list[str], cwd: Path, *, input_text: str | None = None,
            timeout: int = 1800, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(argv, cwd=cwd, input=input_text, text=True,
                          capture_output=True, timeout=timeout, env=env, check=False)


def git(*args: str, cwd: Path = ROOT) -> str:
    result = command(["git", *args], cwd, timeout=120)
    if result.returncode:
        raise RuntimeError(f"git {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def available(providers: set[str]) -> dict[str, str | None]:
    return {provider: shutil.which(provider) for provider in sorted(providers)}


def prompt(role: str, task: str, context: str = "") -> str:
    instructions = (ROLES / f"{role}.md").read_text(encoding="utf-8")
    return f"{instructions}\n\n# User task\n\n{task}\n\n{context}\n"


def invoke(provider: str, role: str, worktree: Path, run_dir: Path,
           text: str, timeout: int, *, writable: bool = False,
           label: str | None = None) -> dict[str, object]:
    prefix = label or f"{role}-{provider}"
    prompt_path = run_dir / f"{prefix}.prompt.md"
    prompt_path.write_text(text, encoding="utf-8")
    if provider == "claude":
        mode = "acceptEdits" if writable else "dontAsk"
        argv = ["claude", "-p", "Follow the task and context on stdin.",
                "--permission-mode", mode, "--permission-prompts", "none"]
        stdin = text
    elif provider == "hermes":
        argv = ["hermes", "chat", "--query-file", str(prompt_path)]
        stdin = None
    else:
        argv = ["codex", "exec", "--sandbox", "workspace-write" if writable else "read-only",
                "--output-last-message", str(run_dir / f"{prefix}.final.md"), "-"]
        stdin = text
    try:
        result = command(argv, worktree, input_text=stdin, timeout=timeout)
        stdout, stderr, code = result.stdout, result.stderr, result.returncode
    except subprocess.TimeoutExpired as exc:
        stdout = exc.stdout.decode(errors="replace") if isinstance(exc.stdout, bytes) else (exc.stdout or "")
        stderr = exc.stderr.decode(errors="replace") if isinstance(exc.stderr, bytes) else (exc.stderr or "")
        stderr += f"\nTimed out after {timeout} seconds."
        code = 124
    (run_dir / f"{prefix}.stdout.log").write_text(stdout, encoding="utf-8")
    (run_dir / f"{prefix}.stderr.log").write_text(stderr, encoding="utf-8")
    final_path = run_dir / f"{prefix}.final.md"
    report = final_path.read_text(encoding="utf-8") if final_path.exists() else stdout
    (run_dir / f"{prefix}.report.md").write_text(report, encoding="utf-8")
    return {"provider": provider, "role": role, "exit_code": code,
            "report": str(run_dir / f"{prefix}.report.md"), "worktree": str(worktree)}


def write_status(run_dir: Path, status: dict[str, object]) -> None:
    (run_dir / "status.json").write_text(json.dumps(status, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def java_env() -> dict[str, str]:
    env = os.environ.copy()
    if "JAVA_HOME" not in env:
        for architecture in ("x64", "aarch64"):
            candidate = Path.home() / ".local" / "share" / "mobrealms" / f"jdk-25-{architecture}"
            if (candidate / "bin" / "java").is_file():
                env["JAVA_HOME"] = str(candidate)
                break
    return env


def run_team(args: argparse.Namespace) -> int:
    task = args.task or Path(args.task_file).read_text(encoding="utf-8").strip()
    if not task.strip():
        raise ValueError("Task must not be empty")
    assignments = {"planner": args.planner, "investigator": args.investigator,
                   "implementer": args.implementer}
    needed = set(assignments.values())
    if args.dry_run:
        print(json.dumps({"assignments": assignments, "reviewers": sorted({args.planner, args.investigator}),
                          "available": available(needed), "task": task}, indent=2, ensure_ascii=False))
        return 0
    missing = [name for name, path in available(needed).items() if path is None]
    if missing:
        raise RuntimeError("Missing CLI(s): " + ", ".join(missing) + ". Run doctor after installing and authenticating them.")
    if git("status", "--porcelain", "--untracked-files=all"):
        raise RuntimeError("Working tree is not clean. Commit or set aside existing changes before starting an isolated team run.")
    run_id = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S") + "-" + secrets.token_hex(3)
    run_dir = RUNS / run_id
    run_dir.mkdir(parents=True)
    worktree_root = ROOT.parent / f"{ROOT.name}-agent-team" / run_id
    worktrees = {role: worktree_root / role for role in assignments}
    status: dict[str, object] = {"id": run_id, "state": "starting", "task": task,
                                 "assignments": assignments, "worktrees": {k: str(v) for k, v in worktrees.items()},
                                 "steps": {}}
    write_status(run_dir, status)
    try:
        for role in ("planner", "investigator"):
            git("worktree", "add", "--detach", str(worktrees[role]), "HEAD")
        git("worktree", "add", "-b", f"agent-team/{run_id}", str(worktrees["implementer"]), "HEAD")
        status["state"] = "researching"
        write_status(run_dir, status)
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = {role: pool.submit(invoke, assignments[role], role, worktrees[role], run_dir,
                                         prompt(role, task), args.timeout) for role in ("planner", "investigator")}
            reports = {role: future.result() for role, future in futures.items()}
        status["steps"] = reports
        write_status(run_dir, status)
        if any(report["exit_code"] != 0 for report in reports.values()):
            raise RuntimeError("Research stage failed; inspect its stderr logs")
        context = "# Team handoff\n\n" + "\n\n".join(
            f"## {role} ({assignments[role]})\n\n" + Path(str(reports[role]["report"])).read_text(encoding="utf-8")
            for role in ("planner", "investigator"))
        status["state"] = "implementing"
        write_status(run_dir, status)
        implementation = invoke(assignments["implementer"], "implementer", worktrees["implementer"],
                                run_dir, prompt("implementer", task, context), args.timeout, writable=True)
        status["steps"]["implementer"] = implementation
        write_status(run_dir, status)
        if implementation["exit_code"] != 0:
            raise RuntimeError("Implementation stage failed; inspect its stderr log")
        git("add", "-N", ".", cwd=worktrees["implementer"])
        patch = git("diff", "--binary", cwd=worktrees["implementer"])
        (run_dir / "implementation.patch").write_text(patch, encoding="utf-8")
        if not patch:
            raise RuntimeError("Implementer produced no changes")
        test_report = "Skipped (--no-test)."
        test_code = 0
        if not args.no_test:
            test_argv = shlex.split(args.test_command)
            if not test_argv:
                raise ValueError("Test command must not be empty")
            try:
                result = command(test_argv, worktrees["implementer"], timeout=args.timeout, env=java_env())
                test_code = result.returncode
                test_report = result.stdout + result.stderr
            except subprocess.TimeoutExpired:
                test_code = 124
                test_report = f"Timed out after {args.timeout} seconds."
        (run_dir / "validation.log").write_text(test_report, encoding="utf-8")
        status["validation_exit_code"] = test_code
        status["state"] = "reviewing"
        write_status(run_dir, status)
        review_context = ("# Implementation report\n\n" + Path(str(implementation["report"])).read_text(encoding="utf-8")
                          + "\n\n# Validation\n\n" + test_report[-20000:]
                          + "\n\n# Patch\n\n" + patch)
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = {role: pool.submit(invoke, assignments[role], "reviewer", worktrees[role], run_dir,
                                         prompt("reviewer", task, review_context), args.timeout,
                                         label=f"review-{role}-{assignments[role]}")
                       for role in ("planner", "investigator")}
            reviews = {role: future.result() for role, future in futures.items()}
        status["steps"]["reviews"] = reviews
        status["state"] = "reviewed" if test_code == 0 and all(r["exit_code"] == 0 for r in reviews.values()) else "needs_attention"
        write_status(run_dir, status)
        print(f"Run {run_id}: {status['state']}\nImplementation: {worktrees['implementer']}\nReports: {run_dir}")
        return 0 if status["state"] == "reviewed" else 1
    except Exception as exc:
        status["state"] = "failed"
        status["error"] = str(exc)
        write_status(run_dir, status)
        print(f"Run {run_id} failed: {exc}\nReports: {run_dir}", file=sys.stderr)
        return 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="action", required=True)
    sub.add_parser("doctor", help="Show installed provider CLIs and worktree state")
    sub.add_parser("status", help="List recent team runs")
    run = sub.add_parser("run", help="Run the team in isolated worktrees")
    task_input = run.add_mutually_exclusive_group(required=True)
    task_input.add_argument("--task")
    task_input.add_argument("--task-file")
    for role, default in (("planner", "claude"), ("investigator", "hermes"), ("implementer", "codex")):
        run.add_argument(f"--{role}", choices=PROVIDERS, default=default)
    run.add_argument("--timeout", type=int, default=1800, help="Seconds per CLI call or validation")
    run.add_argument("--test-command", default="./gradlew test build --no-daemon")
    run.add_argument("--no-test", action="store_true")
    run.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if args.action == "doctor":
        for name, path in available(set(PROVIDERS)).items():
            print(f"{name}: {path or 'not installed'}")
        print("repository:", ROOT)
        print("working tree:", "clean" if not git("status", "--porcelain", "--untracked-files=all") else "has changes")
        return 0
    if args.action == "status":
        for path in sorted(RUNS.glob("*/status.json"), reverse=True)[:20]:
            data = json.loads(path.read_text(encoding="utf-8"))
            print(f"{data['id']}: {data['state']} — {data['task'][:80]}")
        return 0
    if args.timeout < 1:
        parser.error("--timeout must be positive")
    try:
        return run_team(args)
    except (RuntimeError, ValueError, OSError) as exc:
        print(f"agent-team: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
