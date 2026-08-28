# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Summary
Python 3.10+ security hardening engine. AST-based scanner → SARIF report → Actor-Critic patch/verify loop exposed via a FastMCP server.

## Commands

| Task | Command |
|------|---------|
| Run all tests | `python -m pytest tests/ -v` |
| Run a single test | `python -m pytest tests/test_sample_service.py::test_get_user_valid -v` |
| Run AST scan | `python core/ast_scanner.py src reports/security-findings.sarif` |
| Run Critic (tests + report) | `python core/critic_engine.py` |
| Start MCP server | `PYTHONPATH=. python core/mcp_server.py` |

## Critical Constraints (from `.bob/rules`)

- **No raw SQL string concat or f-strings in queries** — CWE-89. Use `cursor.execute("... WHERE x = ?", (val,))`.
- **No hardcoded secrets** — CWE-798. All secrets via `os.getenv()`. The scanner flags strings ≥12 chars OR entropy > 3.2 assigned to vars containing `secret`, `token`, `password`, `api_key`, `jwt`, `bearer`, `auth_header`, `db_pass`.
- **No `eval()`/`exec()`** — CWE-95. Use `ast.literal_eval()` or a safe parser.
- **Never commit Actor patches before the Critic runs** — `rollback_target()` uses `git checkout -- <file>` against HEAD. If you commit, rollback is destroyed.

## Actor-Critic Loop Rules

1. Actor generates tests + applies patches as working-tree-only diffs.
2. Critic calls `run_critic_test_verification()` → writes `reports/validation_report.json`.
3. On Critic failure: call `rollback_file()` via MCP, then re-enter Actor.
4. **Hard limit: 3 retry iterations.** On third failure, halt and emit a manual intervention notice.

## Key Architecture Details

- `core/ast_scanner.py` — taint scope is **per-function** (`tainted_vars` reset at each `visit_FunctionDef`). Cross-function taint does not propagate.
- `core/mcp_server.py` — all subprocess calls set `cwd=PROJECT_ROOT` (resolved at import time from `__file__`). Relative paths like `"src"` are resolved against project root, not the caller's CWD.
- `run_ast_scan()` **skips files prefixed `test_`** — scanner only targets non-test source files.
- Tests use a `tmp_path`-based SQLite fixture (`temp_service`); no real DB file is created or left behind.
- `sys.path.insert(0, project_root)` appears at the top of every module and test file — this is required because the MCP server and scanner are both run as `__main__` scripts and as importable modules.

## MCP Tools (exposed to Bob and agents)

| Tool | Purpose |
|------|---------|
| `scan_ast_vulnerabilities(target_dir, output_sarif)` | Runs AST scan, outputs SARIF |
| `run_critic_test_verification(test_dir)` | Runs pytest, writes `reports/validation_report.json` |
| `rollback_file(file_path)` | `git checkout -- <file>` on Critic failure |

## Outputs

- `reports/security-findings.sarif` — OASIS SARIF v2.1.0 scan results.
- `reports/validation_report.json` — Critic pytest report with combined stdout/stderr.

## Code Style

- All functions and method parameters use stdlib `typing` annotations (`List`, `Dict`, `Any`, `Optional`, `Set`).
- Findings are plain `List[Dict[str, Any]]` with keys: `rule_id`, `rule_name`, `severity`, `message`, `line`, `file`.
- Standard import order: stdlib → third-party → local (`from core.*`), with the `sys.path.insert` block immediately before local imports.
