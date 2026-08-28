# Skill: actor_critic_harden

## Trigger
Execute to patch vulnerabilities and generate automated unit test suites.

## Actions
1. Read `@reports/security-findings.sarif` and `@src/sample_service.py`.

2. **Actor Step — Test Generation & Patching:**
   - Write comprehensive pytest unit tests in `tests/test_sample_service.py` verifying
     both valid inputs and malicious injection payloads (e.g. `1' OR '1'='1`).
   - Refactor `@src/sample_service.py` using inline diffs:
     - Replace tainted f-string queries with parameterized tuples: `cursor.execute("... WHERE username = ?", (username,))`.
     - Replace hardcoded secrets with: `os.getenv("JWT_SECRET_KEY", "fallback_dev_key")`.
     - Replace `eval()` with `ast.literal_eval()` or a safe arithmetic parser.
   - Do NOT commit changes. Leave patches as working-tree modifications only.

3. **Critic Step — Verification & Bounded Rollback:**
   - Call the MCP tool `run_critic_test_verification`.
   - If `CRITIC_VERIFICATION_PASSED`: proceed to step 4.
   - If `CRITIC_VERIFICATION_FAILED`: call `rollback_file`, inspect the combined
     diagnostic output, and re-enter the Actor Step.
   - After 3 consecutive failures: halt and output a manual intervention notice.

4. Run `/review --pr-desc` to produce the final pull request release notes.
