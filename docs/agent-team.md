# Agent team for Mob Realms

`scripts/agent_team.py` coordinates four roles with three interchangeable CLI adapters. By default Claude plans, Hermes investigates, Codex implements, and Claude plus Hermes review. The planning and investigation runs start in parallel. Every agent gets its own Git worktree, and the implementation stays on an `agent-team/<run-id>` branch for inspection. The orchestrator never merges or deploys it.

All three tools read the shared `AGENTS.md`: Codex reads it directly, Claude reads it through `CLAUDE.md`, and Hermes reads `AGENTS.md` because this repository has no Hermes-specific override. Role prompts live in `.agent-team/roles/`. A run writes prompts, responses, test output, the patch, and `status.json` under `.agent-team/runs/<run-id>/`; that directory is ignored by Git.

## Shared project skills

The versioned skills in `.agent-team/skills/` are usable by all three agents through `AGENTS.md`:

- [`mob-realms-buildings`](../.agent-team/skills/mob-realms-buildings/SKILL.md) guides blueprint design, physical completion checks and construction-stall diagnosis.
- [`mob-realms-civilization`](../.agent-team/skills/mob-realms-civilization/SKILL.md) guides settlement growth, resident behavior, offline work and save compatibility.

On this VM, Codex also has four third-party Minecraft skills installed locally from [`Jahrome907/minecraft-agent-skills`](https://github.com/Jahrome907/minecraft-agent-skills/tree/dd57c5a97741cdc0eb5bb3a8f876581a4f09eeb0/.agents/skills): `minecraft-modding`, `minecraft-testing`, `minecraft-server-admin` and `minecraft-ci-release`. Their pinned source revision is `dd57c5a97741cdc0eb5bb3a8f876581a4f09eeb0`. These external files are not copied into this repository; Claude and Hermes may consult that source when applicable. Repository instructions and the project's Minecraft 26.3/Java 25 pins take precedence over general examples.

## Requirements

Install and authenticate the `claude`, `hermes`, and `codex` CLIs separately, then put them on `PATH`. The script does not handle credentials. Use `python3 scripts/agent_team.py doctor` to see what is available. The repository must be clean before a run, so every worktree starts from the same commit. Java changes are checked with `./gradlew test build --no-daemon`; set `JAVA_HOME` to a Java 25 JDK if needed.

For this Linux VM, the official installers are `curl -fsSL https://claude.ai/install.sh | bash -s stable` and `curl -fsSL https://hermes-agent.nousresearch.com/install.sh | bash`. Review the linked installation pages before running either script. Then run `claude auth login` to complete its account sign-in and `hermes setup --portal` or configure another Hermes provider. Claude Code requires a qualifying account or supported API provider; Hermes also needs a configured model provider. The orchestrator cannot perform those account steps for you.

The adapters use the documented non-interactive forms: `claude -p`, `hermes chat --query-file`, and `codex exec`. Claude's investigative roles use its `dontAsk` permission mode; Codex writes only in its implementation worktree. Hermes and Claude still run in isolated worktrees because local user settings may grant extra tools.

## Run

```sh
python3 scripts/agent_team.py doctor
python3 scripts/agent_team.py run --task "Fix residents stalled while building a house"
python3 scripts/agent_team.py status
```

Use `--task-file path/to/task.md` for a longer task. `--dry-run` shows assignments without launching agents. `--planner`, `--investigator`, and `--implementer` can each be `claude`, `hermes`, or `codex`. `--test-command` changes the validation command; `--no-test` skips it for documentation-only tasks. A failed CLI or test leaves reports and worktrees intact for inspection. Reviewers receive the implementation patch and test output. Worktree paths are in `status.json`; review the implementation there before merging or deploying it.

CLI references: [Claude Code installation](https://code.claude.com/docs/en/setup), [Claude Code project instructions](https://code.claude.com/docs/en/memory), [Claude Code non-interactive mode](https://code.claude.com/docs/en/headless), [Hermes installation](https://hermes-agent.nousresearch.com/docs/getting-started/installation), [Hermes project context](https://hermes-agent.nousresearch.com/docs/user-guide/features/context-files), [Hermes CLI](https://hermes-agent.nousresearch.com/docs/user-guide/cli), [Codex AGENTS.md behavior](https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide).
