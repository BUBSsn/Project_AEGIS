> 🛡️ **Project AEGIS** — Autonomous Security Hardening & Test Generation Engine  
> Built for the IBM Bob 2.0 Hackathon · Powered by IBM Bob MCP Tools · OASIS SARIF v2.1.0
>

## What is AEGIS?

AEGIS is an autonomous developer-workflow engine built on **IBM Bob 2.0**. Point it at any GitHub repository and it:

1. **Scans** every source file with an AST-based vulnerability detector — catching SQL injection (CWE-89), hardcoded secrets (CWE-798), and dangerous code execution (CWE-95) across Python, JavaScript, Java, PHP, and SQL.
2. **Reports** findings in an industry-standard OASIS SARIF v2.1.0 report with file paths, line numbers, and severity ratings.
3. **Explains** every vulnerability through an embedded IBM Bob chat — attack vectors, system impact, and remediation — grounded in the actual code.
4. **Fixes** bugs with Bob-generated, context-aware patches via the Actor-Critic hardening loop.

All of this is exposed through a Streamlit dashboard. No IDE. No security expertise required. Just a GitHub URL.

## IBM Bob Integration
- **Custom Skills** — `.bob/skills/security_audit.md` and `actor_critic_harden.md` define the full scan-to-patch workflow
- **MCP Server** — `core/mcp_server.py` exposes `scan_ast_vulnerabilities`, `run_critic_test_verification`, and `rollback_file` as Bob tools
- **Behavioral Rules** — `.bob/rules` enforces zero-tolerance constraints on SQL, secrets, and eval usage
