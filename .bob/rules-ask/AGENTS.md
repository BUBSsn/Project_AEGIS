# AGENTS.md — Ask Mode

This file provides guidance to agents answering questions about this repository.

## Non-Obvious Documentation Context

- **`project.md`** is the canonical design document and contains full implementation specs, including the complete code for all core modules. It is the source of truth for architecture intent.
- **`.bob/rules`** (no extension) is an unstructured plaintext file — not JSON or YAML — containing project behavioral constraints enforced by the Actor-Critic loop. It is not auto-parsed.
- **`src/sample_service.py` contains intentional vulnerabilities** — it is a testbed, not production code. Its bugs (CWE-89, CWE-798, CWE-95) are the scanner's primary targets.
- **`reports/` files are gitignored runtime artifacts** (`security-findings.sarif`, `validation_report.json`). Their absence is normal on a fresh clone.
- The MCP server (`core/mcp_server.py`) wraps `core/ast_scanner.py` and `core/critic_engine.py` as subprocess calls, not direct Python imports for the scan/critic tools — this is intentional so they run in isolated process contexts.
- **`mcp.json`** at the project root defines the AEGIS MCP server registration for IBM Bob. The server name is `"aegis-engine"`.
