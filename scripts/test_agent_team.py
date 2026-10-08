"""Local integration test for the CLI coordinator; no model accounts are needed."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


SOURCE = Path(__file__).resolve().parent.parent


class AgentTeamTests(unittest.TestCase):
    def test_three_clis_handoff_in_isolated_worktrees(self):
        with tempfile.TemporaryDirectory() as temporary:
            base = Path(temporary)
            repo = base / "repo"
            (repo / "scripts").mkdir(parents=True)
            (repo / ".agent-team" / "roles").mkdir(parents=True)
            shutil.copy2(SOURCE / "scripts" / "agent_team.py", repo / "scripts" / "agent_team.py")
            for role in ("planner", "investigator", "implementer", "reviewer"):
                shutil.copy2(SOURCE / ".agent-team" / "roles" / f"{role}.md",
                             repo / ".agent-team" / "roles" / f"{role}.md")
            (repo / ".gitignore").write_text("/.agent-team/runs/\n", encoding="utf-8")
            (repo / "AGENTS.md").write_text("Shared instructions.\n", encoding="utf-8")
            for argv in (["git", "init", "-q"], ["git", "config", "user.email", "test@example.invalid"],
                         ["git", "config", "user.name", "Test"], ["git", "add", "."],
                         ["git", "commit", "-qm", "fixture"]):
                subprocess.run(argv, cwd=repo, check=True)
            bin_dir = base / "bin"
            bin_dir.mkdir()
            fake = """#!/usr/bin/env python3
import pathlib, sys
name = pathlib.Path(sys.argv[0]).name
if name == 'codex':
    text = sys.stdin.read()
    if 'workspace-write' in sys.argv:
        pathlib.Path('done.txt').write_text('implemented\\n')
    output = pathlib.Path(sys.argv[sys.argv.index('--output-last-message') + 1])
    output.write_text(name + ' report')
elif name == 'hermes':
    text = pathlib.Path(sys.argv[sys.argv.index('--query-file') + 1]).read_text()
else:
    text = sys.stdin.read()
assert 'literal $(touch bad)' in text
print(name + ' report')
"""
            for name in ("claude", "hermes", "codex"):
                path = bin_dir / name
                path.write_text(fake, encoding="utf-8")
                path.chmod(0o755)
            env = os.environ.copy()
            env["PATH"] = str(bin_dir) + os.pathsep + env["PATH"]
            result = subprocess.run([sys.executable, "scripts/agent_team.py", "run", "--task",
                                     "literal $(touch bad)", "--no-test", "--timeout", "30"],
                                    cwd=repo, env=env, text=True, capture_output=True, timeout=90)
            self.assertEqual(result.returncode, 0, result.stderr)
            run_dir = next((repo / ".agent-team" / "runs").iterdir())
            status = json.loads((run_dir / "status.json").read_text(encoding="utf-8"))
            self.assertEqual(status["state"], "reviewed")
            self.assertTrue((Path(status["worktrees"]["implementer"]) / "done.txt").is_file())
            self.assertIn("done.txt", (run_dir / "implementation.patch").read_text(encoding="utf-8"))
            self.assertFalse((repo / "done.txt").exists())
            self.assertFalse((repo / "bad").exists())
            self.assertTrue((run_dir / "review-planner-claude.report.md").is_file())
            self.assertTrue((run_dir / "review-investigator-hermes.report.md").is_file())


if __name__ == "__main__":
    unittest.main()
