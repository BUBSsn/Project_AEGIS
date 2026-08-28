# AEGIS Agent Architecture & Workspace Index

## Overview
This document serves as the persistent workspace index for IBM Bob 2.0 and agent clients.

## Project Structure
- `core/`: Core security analysis, SARIF export, and Actor-Critic test verification engines.
  - `ast_scanner.py`: AST parser with taint analysis (CWE-89), Shannon entropy (CWE-798), dynamic code execution (CWE-95).
  - `sarif_exporter.py`: Formats findings into OASIS SARIF v2.1.0 specification.
  - `critic_engine.py`: Test runner and git rollback utility for verification.
  - `mcp_server.py`: FastMCP server exposing tools to IBM Bob and AI agents.
- `src/`: Target source code and microservices.
  - `sample_service.py`: Target service testbed containing security flaws.
- `tests/`: Automated unit and security test suites.
- `reports/`: SARIF and validation report outputs.
- `.bob/`: Behavioral rules and skill definitions for IBM Bob 2.0.
  - `rules`: Rules and constraints for Actor-Critic loop.
  - `skills/security_audit.md`: Playbook for AST vulnerability scan.
  - `skills/actor_critic_harden.md`: Playbook for Actor-Critic test generation & patching.
