"""
JavaScript / TypeScript Security Scanner — regex-based taint analysis.

Detected CWEs
-------------
  CWE-79   DOM-based XSS
             – innerHTML / outerHTML / document.write / insertAdjacentHTML assigned
               from location.*, URLSearchParams, user input, or tainted variables
  CWE-89   SQL Injection (Node.js)
             – query()/execute() / db.run() with string concatenation
  CWE-95   Code Injection
             – eval() / new Function() / setTimeout/setInterval with a string arg
  CWE-78   OS Command Injection (Node.js)
             – child_process exec/execSync/spawn with a dynamic string
  CWE-601  Open Redirect
             – window.location = user_input / res.redirect(req.query.*)
  CWE-798  Hard-coded Credentials
             – const/let/var password/secret/token/apiKey = "literal" (len >= 8)
  CWE-327  Weak Cryptography
             – MD5 / SHA1 / DES used via crypto.createHash / CryptoJS
  CWE-352  CSRF (client-side)
             – AJAX POST/PUT/DELETE without explicit CSRF header/token

Scanning strategy
-----------------
Line-oriented taint tracking.  Sources: location.search/hash/pathname,
URLSearchParams.get(), document.cookie, req.query, req.body, req.params.
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Any, Set

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

# ---------------------------------------------------------------------------
# Source patterns
# ---------------------------------------------------------------------------
# Source: reading FROM location/cookie/request — exclude assignment targets.
# location.search/hash/pathname are read-only properties; location.href can be
# both read and written.  Only treat href as a source when it appears on the
# right-hand side of an expression (i.e. not immediately before \s*=).
_USER_SOURCE_RE = re.compile(
    r'location\s*\.\s*(?:search|hash|pathname)(?!\s*=)'
    r'|location\s*\.\s*href(?!\s*=)'      # href as source (not assignment target)
    r'|URLSearchParams\s*\('
    r'|\.searchParams\s*\.\s*get\s*\('
    r'|document\s*\.\s*cookie'
    r'|req\s*\.\s*(?:query|body|params)\b'
    r'|request\s*\.\s*(?:query|body|params)\b'
    r'|event\s*\.\s*data\b'
    r'|getParameter\s*\(',
    re.IGNORECASE,
)

_ASSIGN_TAINT_RE = re.compile(
    r'(?:var|let|const)\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*='
    r'.*(?:'
    r'location\s*\.\s*(?:search|hash|pathname|href)'
    r'|URLSearchParams'
    r'|document\.cookie'
    r'|req\s*\.\s*(?:query|body|params)'
    r'|request\s*\.\s*(?:query|body|params)'
    r'|getParameter'
    r')',
    re.IGNORECASE,
)

# Also handles reassignment without declaration
_REASSIGN_TAINT_RE = re.compile(
    r'^[^/]*?\b([A-Za-z_$][A-Za-z0-9_$]*)\s*='
    r'(?!=)'
    r'.*(?:'
    r'location\s*\.\s*(?:search|hash|pathname|href)'
    r'|URLSearchParams'
    r'|document\.cookie'
    r'|req\s*\.\s*(?:query|body|params)'
    r'|\.get\s*\(["\']'
    r')',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# DOM XSS sinks
# ---------------------------------------------------------------------------
_DOM_XSS_SINK_RE = re.compile(
    r'\.innerHTML\s*='
    r'|\.outerHTML\s*='
    r'|document\s*\.\s*write\s*\('
    r'|document\s*\.\s*writeln\s*\('
    r'|\.insertAdjacentHTML\s*\('
    r'|\$\s*\([^)]+\)\s*\.\s*html\s*\('  # jQuery .html()
    r'|\$\s*\([^)]+\)\s*\.\s*append\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# SQL sinks (Node.js mysql/pg/sqlite)
# ---------------------------------------------------------------------------
_SQL_SINK_RE = re.compile(
    r'\.query\s*\('
    r'|\.execute\s*\('
    r'|db\.run\s*\('
    r'|db\.all\s*\('
    r'|db\.get\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Code injection sinks
# ---------------------------------------------------------------------------
_EVAL_SINK_RE = re.compile(
    r'\beval\s*\('
    r'|new\s+Function\s*\('
    r'|setTimeout\s*\(\s*(?:["\']|[A-Za-z_$])'
    r'|setInterval\s*\(\s*(?:["\']|[A-Za-z_$])',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Node OS command sinks
# ---------------------------------------------------------------------------
_CMD_SINK_RE = re.compile(
    r'child_process\s*\.\s*(?:exec|execSync|execFile|spawn|spawnSync)\s*\('
    r'|require\s*\(\s*["\']child_process["\']\s*\)',
    re.IGNORECASE,
)
_EXEC_CALL_RE = re.compile(
    r'\b(?:exec|execSync|execFile|spawn|spawnSync)\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Open redirect sinks
# ---------------------------------------------------------------------------
_REDIRECT_SINK_RE = re.compile(
    r'window\s*\.\s*location\s*(?:\.\s*href)?\s*='
    r'|location\s*\.\s*replace\s*\('
    r'|res\s*\.\s*redirect\s*\('
    r'|response\s*\.\s*redirect\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Hard-coded credentials
# ---------------------------------------------------------------------------
_HARDCODED_CRED_RE = re.compile(
    r'(?:var|let|const)\s+([A-Za-z_$][A-Za-z0-9_$]*'
    r'(?:password|passwd|secret|token|api_key|apikey|auth_key)[A-Za-z0-9_$]*'
    r'|(?:password|passwd|secret|token|api_key|apikey|auth_key)[A-Za-z0-9_$]*)\s*=\s*["\']([^"\']{8,})["\']',
    re.IGNORECASE,
)

# Also: { apiKey: "value", password: "value" }
_HARDCODED_PROP_RE = re.compile(
    r'["\']?(?:password|passwd|secret|api_key|apiKey|auth_key|token)["\']?\s*:\s*["\']([^"\']{8,})["\']',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Weak crypto
# ---------------------------------------------------------------------------
_WEAK_CRYPTO_RE = re.compile(
    r'\.createHash\s*\(\s*["\'](?:md5|sha1|sha-1)["\']'
    r'|CryptoJS\s*\.\s*(?:MD5|SHA1)\s*\('
    r'|new\s+Md5\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# CSRF — AJAX calls without token header
# ---------------------------------------------------------------------------
_AJAX_STATE_CHANGE_RE = re.compile(
    r'(?:type|method)\s*:\s*["\'](?:POST|PUT|DELETE|PATCH)["\']'
    r'|\$\s*\.\s*(?:post|put|delete)\s*\(',
    re.IGNORECASE,
)
_CSRF_HEADER_RE = re.compile(
    r'X-CSRF-Token|X-Requested-With|csrf|_token|csrfToken',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Skip minified or auto-generated files
# ---------------------------------------------------------------------------
_SKIP_FILENAME_RE = re.compile(
    r'\.min\.js$|\.bundle\.js$|\.map$|bootstrap\.|jquery[^/]*\.js$',
    re.IGNORECASE,
)


def _is_comment(line: str) -> bool:
    stripped = line.strip()
    return stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*')


def scan_js_file(filepath: str) -> List[Dict[str, Any]]:
    """Scan a single .js or .ts file and return a list of findings."""
    if _SKIP_FILENAME_RE.search(os.path.basename(filepath)):
        return []

    findings: List[Dict[str, Any]] = []

    try:
        with open(filepath, "r", encoding="utf-8", errors="replace") as fh:
            lines = fh.readlines()
    except OSError:
        return findings

    tainted_vars: Set[str] = set()
    ajax_post_lines: List[int] = []
    csrf_token_seen = False

    for lineno_0, raw_line in enumerate(lines):
        lineno = lineno_0 + 1
        line = raw_line.rstrip('\n')

        if _is_comment(line):
            continue

        # ----------------------------------------------------------------
        # Taint propagation
        # ----------------------------------------------------------------
        for m in _ASSIGN_TAINT_RE.finditer(line):
            tainted_vars.add(m.group(1))
        for m in _REASSIGN_TAINT_RE.finditer(line):
            tainted_vars.add(m.group(1))

        # ----------------------------------------------------------------
        # CWE-79: DOM XSS
        # ----------------------------------------------------------------
        if _DOM_XSS_SINK_RE.search(line):
            has_src = bool(_USER_SOURCE_RE.search(line))
            has_tainted = any(tv in line for tv in tainted_vars)
            has_concat = bool(re.search(r'["\'][^"\']*\+\s*[A-Za-z_$]|\+\s*[A-Za-z_$][A-Za-z0-9_$]*\s*[+;,)]', line))
            if has_src or has_tainted or has_concat:
                findings.append({
                    "rule_id": "CWE-79",
                    "rule_name": "DOMBasedXSS",
                    "severity": "error",
                    "message": (
                        "User-controlled value assigned to a DOM sink (innerHTML/outerHTML"
                        "/document.write/jQuery.html). Sanitize with DOMPurify or textContent."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-89: SQL Injection (Node.js)
        # ----------------------------------------------------------------
        if _SQL_SINK_RE.search(line):
            has_concat = bool(re.search(r'["\'][^"\']*\+\s*[A-Za-z_$]|\`[^`]*\$\{', line))
            has_tainted = any(tv in line for tv in tainted_vars)
            if has_concat or has_tainted:
                findings.append({
                    "rule_id": "CWE-89",
                    "rule_name": "SQLInjection",
                    "severity": "error",
                    "message": (
                        "SQL query built with string concatenation or template literal "
                        "from user input. Use parameterized queries."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-95: Code Injection (eval)
        # ----------------------------------------------------------------
        if _EVAL_SINK_RE.search(line):
            has_tainted = any(tv in line for tv in tainted_vars)
            has_src = bool(_USER_SOURCE_RE.search(line))
            # Detect concatenation OUTSIDE string literals: `eval(expr + something)`
            # or template literals: eval(`...${var}...`)
            # Avoid matching + signs that are inside a quoted string passed to eval.
            # Strip the innermost string to test: if after removing quoted literals
            # the `+` still appears before/after a variable name, it's real concat.
            stripped = re.sub(r'"[^"]*"', '""', re.sub(r"'[^']*'", "''", line))
            has_concat = bool(re.search(r'\+\s*[A-Za-z_$]|[A-Za-z_$][A-Za-z0-9_$]*\s*\+', stripped)) or \
                         bool(re.search(r'`[^`]*\$\{', line))
            if has_tainted or has_src or has_concat:
                findings.append({
                    "rule_id": "CWE-95",
                    "rule_name": "CodeInjection",
                    "severity": "error",
                    "message": (
                        "eval() / new Function() called with dynamic or user-controlled "
                        "input. This allows arbitrary code execution."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-78: OS Command Injection (Node.js)
        # ----------------------------------------------------------------
        if _EXEC_CALL_RE.search(line):
            has_tainted = any(tv in line for tv in tainted_vars)
            has_src = bool(_USER_SOURCE_RE.search(line))
            has_concat = bool(re.search(r'["\'][^"\']*\+\s*[A-Za-z_$]|\`[^`]*\$\{', line))
            if has_tainted or has_src or has_concat:
                findings.append({
                    "rule_id": "CWE-78",
                    "rule_name": "OSCommandInjection",
                    "severity": "error",
                    "message": (
                        "OS command executed with dynamic/user-controlled argument. "
                        "Use execFile with an argument array instead of exec(string)."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-601: Open Redirect
        # ----------------------------------------------------------------
        if _REDIRECT_SINK_RE.search(line):
            has_tainted = any(tv in line for tv in tainted_vars)
            has_src = bool(_USER_SOURCE_RE.search(line))
            if has_tainted or has_src:
                findings.append({
                    "rule_id": "CWE-601",
                    "rule_name": "OpenRedirect",
                    "severity": "warning",
                    "message": (
                        "Redirect target derived from user-controlled input. "
                        "Validate the URL against an allowlist before redirecting."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-798: Hard-coded Credentials
        # ----------------------------------------------------------------
        m = _HARDCODED_CRED_RE.search(line)
        if m:
            findings.append({
                "rule_id": "CWE-798",
                "rule_name": "HardcodedCredential",
                "severity": "warning",
                "message": (
                    f"Hard-coded credential in variable '{m.group(1)}'. "
                    "Use environment variables or a secrets manager."
                ),
                "line": lineno,
                "file": filepath,
            })
        elif _HARDCODED_PROP_RE.search(line) and not re.search(r'placeholder|label|hint|example', line, re.IGNORECASE):
            findings.append({
                "rule_id": "CWE-798",
                "rule_name": "HardcodedCredential",
                "severity": "warning",
                "message": (
                    "Hard-coded credential value in object property. "
                    "Use environment variables or a secrets manager."
                ),
                "line": lineno,
                "file": filepath,
            })

        # ----------------------------------------------------------------
        # CWE-327: Weak Cryptographic Algorithm
        # ----------------------------------------------------------------
        if _WEAK_CRYPTO_RE.search(line):
            findings.append({
                "rule_id": "CWE-327",
                "rule_name": "WeakCryptographicAlgorithm",
                "severity": "warning",
                "message": (
                    "MD5 or SHA-1 hash used for security-sensitive purpose. "
                    "Use SHA-256 or stronger."
                ),
                "line": lineno,
                "file": filepath,
            })

        # ----------------------------------------------------------------
        # CWE-352: CSRF — collect AJAX state-change calls
        # ----------------------------------------------------------------
        if _AJAX_STATE_CHANGE_RE.search(line):
            ajax_post_lines.append(lineno)
        if _CSRF_HEADER_RE.search(line):
            csrf_token_seen = True

    # ----------------------------------------------------------------
    # CWE-352: emit if POST/PUT/DELETE AJAX found with no CSRF token
    # ----------------------------------------------------------------
    if ajax_post_lines and not csrf_token_seen:
        findings.append({
            "rule_id": "CWE-352",
            "rule_name": "CrossSiteRequestForgery",
            "severity": "warning",
            "message": (
                "AJAX state-changing request (POST/PUT/DELETE) without a visible "
                "CSRF token header. Add X-CSRF-Token or X-Requested-With to requests."
            ),
            "line": ajax_post_lines[0],
            "file": filepath,
        })

    return findings


def run_js_scan(target_directory: str) -> List[Dict[str, Any]]:
    """Walk target_directory recursively and scan every .js/.ts file."""
    findings: List[Dict[str, Any]] = []
    target_directory = os.path.abspath(target_directory)

    for root, dirs, files in os.walk(target_directory):
        dirs[:] = [
            d for d in dirs
            if d not in ('.git', 'node_modules', 'vendor', '.gradle', 'build', 'dist')
        ]
        for filename in files:
            if filename.lower().endswith(('.js', '.ts')) and \
               not filename.lower().endswith(('.min.js', '.min.ts')):
                full_path = os.path.join(root, filename)
                findings.extend(scan_js_file(full_path))

    return findings


if __name__ == "__main__":
    from core.sarif_exporter import generate_sarif

    src_dir = sys.argv[1] if len(sys.argv) > 1 else "src"
    out_sarif = sys.argv[2] if len(sys.argv) > 2 else "reports/security-findings.sarif"

    os.makedirs(os.path.dirname(out_sarif), exist_ok=True)
    results = run_js_scan(src_dir)
    generate_sarif(results, out_sarif)
    print(f"JS/TS Scan Completed: {len(results)} issue(s) found -> {out_sarif}")
