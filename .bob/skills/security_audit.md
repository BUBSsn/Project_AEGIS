# Skill: security_audit

## Trigger
Execute whenever the user requests a code audit, vulnerability scan, or SARIF report.

## Actions
1. Call the MCP tool `scan_ast_vulnerabilities` with `target_dir="src"`.
2. Load `@reports/security-findings.sarif` into context.
3. Summarize all discovered CWE vulnerabilities, file locations, and affected line numbers.
