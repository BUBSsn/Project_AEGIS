# Skill: security_audit

## Trigger
Execute whenever the user requests a code audit, vulnerability scan, or SARIF report.

## Actions
1. If a GitHub URL is present in the prompt, call the MCP tool `clone_github_repo` with `repo_url=<URL>` and `target_dir="src"` before proceeding.
2. Call the MCP tool `scan_ast_vulnerabilities` with `target_dir="src"` and `output_sarif="reports/security-findings.sarif"`.
3. Load `@reports/security-findings.sarif` into context.
4. Summarize all discovered CWE vulnerabilities, file locations, and affected line numbers.
