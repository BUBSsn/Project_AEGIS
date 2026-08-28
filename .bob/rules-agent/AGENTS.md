# AGENTS.md — Agent (Coding) Mode

This file provides guidance to agents when writing or modifying code in this repository.

## Non-Obvious Coding Rules

- **Never commit patches during the Actor phase.** `rollback_target()` runs `git checkout -- <file>` against HEAD. Committing before the Critic runs permanently destroys the rollback baseline.
- **Taint tracking is function-scoped.** `SecurityASTVisitor.tainted_vars` resets at every `visit_FunctionDef`. Don't expect cross-function taint to be caught by the scanner — add explicit tests for those cases.
- **`run_ast_scan()` silently skips `test_` files.** Any file whose name starts with `test_` is excluded from AST scanning. Security checks in test helpers won't be reported.
- **All subprocess calls in `core/mcp_server.py` use `cwd=PROJECT_ROOT`.** Relative path arguments (e.g. `"src"`, `"reports/..."`) are always resolved from project root, regardless of where the caller invokes the MCP tool.
- **`sys.path.insert(0, project_root)` must appear before any local imports** in every new module or test file — both are executed as `__main__` scripts AND imported as modules.
- **Findings dict must have exactly these keys:** `rule_id`, `rule_name`, `severity`, `message`, `line`, `file`. The SARIF exporter in `core/sarif_exporter.py` reads these by key name with no fallback.
- **Actor-Critic retry hard limit is 3.** After three consecutive Critic failures, halt and output a manual intervention notice — do not silently continue or reset the counter.
- **CWE-798 entropy threshold:** strings assigned to secret-like variable names are flagged if `len >= 12` OR `entropy > 3.2`. Short low-entropy replacements (e.g. `"changeme"`) will not be flagged even if hardcoded — use `os.getenv()` regardless.
