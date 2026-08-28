# AGENTS.md — Plan Mode

This file provides guidance to agents planning or designing changes in this repository.

## Non-Obvious Architectural Constraints

- **Rollback requires patches to remain uncommitted.** The entire Critic rollback mechanism depends on working-tree-only changes. Any design that introduces intermediate commits during an Actor-Critic loop will break the rollback guarantee.
- **Taint analysis is intentionally function-scoped** — cross-function taint propagation is out of scope by design. Do not design features that expect it.
- **SARIF output is the only structured interface between scanner and consumers.** The `findings` list is an in-memory transport; only `reports/security-findings.sarif` is persisted and read by downstream tools or skills.
- **The MCP server runs scanner/critic as subprocesses** (not imports) to ensure process isolation. If you add new core tools, they should follow the same subprocess pattern in `mcp_server.py`.
- **The Actor-Critic loop has a hard retry ceiling of 3** (enforced by `.bob/rules`). Any new retry or backoff logic must respect this ceiling; exceeding it requires a manual intervention halt, not silent recovery.
- **No database outside SQLite in `src/`.** The `users.db` path is configurable but the only storage layer. Tests always inject a `tmp_path` DB and must not touch the real `users.db`.
- **Entropy threshold for secret detection is dual-condition** (`len >= 12 OR entropy > 3.2`). Replacement values that satisfy neither condition bypass detection — plan remediations to use `os.getenv()` unconditionally, not just to lower entropy.
