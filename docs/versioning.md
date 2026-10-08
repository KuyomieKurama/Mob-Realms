# Mob Realms versioning

Mob Realms uses `MAJOR.MINOR.PATCH-dev` while it is a development candidate. For every delivered change, classify its largest user-visible impact and bump that component once for the release:

| Name | Bump | Use for |
|---|---|---|
| Mini (patch) | `0.7.0-dev` → `0.7.1-dev` | Compatible bug fixes, performance work, documentation and small polish. |
| Minor | `0.6.4-dev` → `0.7.0-dev` | Compatible new gameplay, commands, interfaces or capabilities. Reset patch to zero. |
| Major | `0.7.0-dev` → `1.0.0-dev` | Incompatible behavior, save format or API changes that require users to migrate or reset data. Reset minor and patch. |

When several changes ship together, use the highest category. A backward-readable save-format update alone does not require a major bump. Keep `-dev` until a stable release is explicitly approved. State the chosen category and reason in the changelog and delivery note.

Update `gradle.properties`, `scripts/server_manager.py`, the source-version allowlist in `scripts/upgrade_server.py`, current-version documentation, CI artifact paths and `CHANGELOG.md` together. Build and test the resulting JAR, then upgrade the stopped server with a full backup. Clients and servers must install the same JAR.
