"""
PHP Security Scanner — regex-based taint analysis for PHP source files.

Detects the following CWEs:
  CWE-89   SQL Injection (user input directly concatenated into a query string)
  CWE-79   Cross-Site Scripting (unescaped echo of user-controlled variables)
  CWE-78   OS Command Injection (exec/system/shell_exec with non-literal arg)
  CWE-22   Path Traversal (user input flows into file-access functions)
  CWE-611  XML External Entity (simplexml/DOMDocument without disabling entity loading)
  CWE-798  Hard-coded Credentials (password/secret literals in PHP assignments)
  CWE-352  Cross-Site Request Forgery (POST handler with no CSRF-token check in sight)

Scanning strategy
-----------------
The scanner is line-oriented.  For each PHP file it:
  1. Collects "tainted" variable names on lines where $_GET / $_POST /
     $_REQUEST / $_COOKIE / $_SERVER / $_FILES are assigned.
  2. Applies per-line pattern rules that look for dangerous sinks that
     receive either a superglobal directly or a previously tainted variable.
  3. Emits a finding dict compatible with the SARIF exporter.

This is a heuristic approach — it will produce false positives on code that
sanitises input through methods the scanner does not recognise, and false
negatives on multi-line string constructions.  For a deliberately-vulnerable
training application like Hackazon the recall is the priority.
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Any, Set, Tuple

# Ensure project root on sys.path for both script and module invocation
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))


# ---------------------------------------------------------------------------
# Superglobal pattern — matches $_GET['x'], $_POST["x"], $_REQUEST['x'], etc.
# ---------------------------------------------------------------------------
_SUPERGLOBAL_RE = re.compile(
    r'\$_(GET|POST|REQUEST|COOKIE|SERVER|FILES)\s*\[',
    re.IGNORECASE,
)

# Variable assignment taint: $var = $_GET[...] or $var = something_tainted
_ASSIGN_TAINT_RE = re.compile(
    r'\$([A-Za-z_][A-Za-z0-9_]*)\s*='
    r'(?!=)'           # not ==
    r'.*\$_(GET|POST|REQUEST|COOKIE|SERVER|FILES)\s*\[',
    re.IGNORECASE,
)

# Secret-like variable names
_SECRET_VAR_RE = re.compile(
    r'\$([A-Za-z_][A-Za-z0-9_]*)',
    re.IGNORECASE,
)

_SECRET_KW_RE = re.compile(
    r'(?:password|passwd|secret|api_key|token|db_pass|auth_key|private_key)',
    re.IGNORECASE,
)

# SQL sink: functions/methods that execute raw SQL
_SQL_SINK_RE = re.compile(
    r'(?:'
    r'mysql_query\s*\('
    r'|mysqli_query\s*\(\s*\$[A-Za-z_][A-Za-z0-9_]*\s*,'
    r'|->query\s*\('
    r'|->exec\s*\('
    r'|->\s*prepare\s*\('
    r')',
    re.IGNORECASE,
)

# OS command sinks
_CMD_SINK_RE = re.compile(
    r'\b(?:exec|system|shell_exec|passthru|popen|proc_open)\s*\(',
    re.IGNORECASE,
)

# File-access sinks that can be exploited for path traversal / RFI
_FILE_SINK_RE = re.compile(
    r'\b(?:file_get_contents|file_put_contents|readfile|fopen|'
    r'include|include_once|require|require_once)\s*\(',
    re.IGNORECASE,
)

# XSS sinks — echo / print of a variable
_ECHO_RE = re.compile(
    r'\b(?:echo|print)\b',
    re.IGNORECASE,
)

# Safe XSS output wrappers used by Hackazon
_SAFE_OUTPUT_RE = re.compile(
    r'htmlspecialchars|htmlentities|escapeXSS|->escape\('
    r'|\$_\(|h\(',
    re.IGNORECASE,
)

# XML dangerous loading patterns
_XML_SINK_RE = re.compile(
    r'simplexml_load_string\s*\('
    r'|DOMDocument.*->loadXML\s*\('
    r'|xml_parse\s*\(',
    re.IGNORECASE,
)
_XML_SAFE_RE = re.compile(
    r'LIBXML_NOENT',
    re.IGNORECASE,
)
_ENTITY_DISABLE_RE = re.compile(
    r'libxml_disable_entity_loader\s*\(\s*true',
    re.IGNORECASE,
)

# Hard-coded credential patterns  (PHP assignments like $password = "literal")
_HARDCODED_CRED_RE = re.compile(
    r'\$(password|passwd|secret|api_key|token|db_pass|auth_key|private_key)'
    r'\s*=\s*["\']([^"\']{8,})["\']',
    re.IGNORECASE,
)

# CSRF: detect POST action handlers and check for token validation nearby
_CSRF_METHOD_CHECK_RE = re.compile(
    r'\$_(SERVER|REQUEST)\s*\[\s*["\']REQUEST_METHOD["\']\s*\]'
    r'|method\s*==\s*["\']POST["\']'
    r'|\$_POST\s*\[',
    re.IGNORECASE,
)
_CSRF_TOKEN_CHECK_RE = re.compile(
    r'checkCsrfToken|isTokenValid|csrf_token|_token\s*\(|verify_csrf',
    re.IGNORECASE,
)


def _is_comment_or_doc(line: str) -> bool:
    """Return True if the line is a comment or doc-block line."""
    stripped = line.strip()
    return stripped.startswith('//') or stripped.startswith('#') or stripped.startswith('*')


def _variable_name_in_line(line: str) -> str:
    """Return first PHP variable name found after a sink keyword, or ''."""
    m = re.search(r'\$([A-Za-z_][A-Za-z0-9_]*)', line)
    return m.group(0) if m else ''


def scan_php_file(filepath: str) -> List[Dict[str, Any]]:
    """
    Scan a single PHP file and return a list of finding dicts.

    Each dict has exactly the keys required by the SARIF exporter:
        rule_id, rule_name, severity, message, line, file
    """
    findings: List[Dict[str, Any]] = []

    try:
        with open(filepath, "r", encoding="utf-8", errors="replace") as fh:
            lines = fh.readlines()
    except OSError:
        return findings

    # Per-file state
    tainted_vars: Set[str] = set()
    entity_loader_disabled = False
    csrf_post_lines: List[int] = []   # line numbers where POST usage detected
    csrf_token_seen = False
    window = 30                        # CSRF: lines before/after to check for token

    for lineno_0, raw_line in enumerate(lines):
        lineno = lineno_0 + 1          # 1-based
        line = raw_line.rstrip('\n')

        if _is_comment_or_doc(line):
            continue

        # ----------------------------------------------------------------
        # Track: libxml_disable_entity_loader(true)
        # ----------------------------------------------------------------
        if _ENTITY_DISABLE_RE.search(line):
            entity_loader_disabled = True

        # ----------------------------------------------------------------
        # Taint propagation: $var = $_GET[...] / $_POST[...] / etc.
        # ----------------------------------------------------------------
        for m in _ASSIGN_TAINT_RE.finditer(line):
            tainted_vars.add('$' + m.group(1))

        # Also mark any variable directly receiving a superglobal value on this line
        if _SUPERGLOBAL_RE.search(line):
            for m in re.finditer(r'\$([A-Za-z_][A-Za-z0-9_]*)\s*=(?!=)', line):
                tainted_vars.add('$' + m.group(1))

        # ----------------------------------------------------------------
        # CWE-89: SQL Injection
        # ----------------------------------------------------------------
        if _SQL_SINK_RE.search(line):
            # Dangerous if the query argument contains a superglobal directly
            # or a string that interpolates a tainted variable
            has_superglobal = bool(_SUPERGLOBAL_RE.search(line))
            has_tainted = any(tv in line for tv in tainted_vars)
            # String concat with a variable: "SELECT ... " . $var or "SELECT $var"
            has_concat = bool(re.search(r'["\'][^"\']*\$[A-Za-z_]', line)) or \
                         bool(re.search(r'\.\s*\$[A-Za-z_]', line))
            if has_superglobal or has_tainted or has_concat:
                findings.append({
                    "rule_id": "CWE-89",
                    "rule_name": "SQLInjection",
                    "severity": "error",
                    "message": (
                        "SQL query constructed with unsanitized user input. "
                        "Use prepared statements with bound parameters."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-79: Cross-Site Scripting
        # ----------------------------------------------------------------
        if _ECHO_RE.search(line):
            has_superglobal = bool(_SUPERGLOBAL_RE.search(line))
            has_tainted = any(tv in line for tv in tainted_vars)
            if has_superglobal or has_tainted:
                # Skip if the entire echo expression is wrapped in a safe function
                if not _SAFE_OUTPUT_RE.search(line):
                    findings.append({
                        "rule_id": "CWE-79",
                        "rule_name": "CrossSiteScripting",
                        "severity": "error",
                        "message": (
                            "Unescaped user-controlled value passed to echo/print. "
                            "Wrap with htmlspecialchars() to prevent XSS."
                        ),
                        "line": lineno,
                        "file": filepath,
                    })

        # ----------------------------------------------------------------
        # CWE-78: OS Command Injection
        # ----------------------------------------------------------------
        if _CMD_SINK_RE.search(line):
            has_superglobal = bool(_SUPERGLOBAL_RE.search(line))
            has_tainted = any(tv in line for tv in tainted_vars)
            # Examine only the first argument (before first top-level comma) to
            # avoid flagging $output / capture-by-reference second arguments.
            m_sink = _CMD_SINK_RE.search(line)
            first_arg = line[m_sink.end():]
            first_arg = re.split(r",(?=(?:[^'\"]*['\"][^'\"]*['\"])*[^'\"]*$)", first_arg)[0]
            has_var_arg = bool(re.search(r'\$[A-Za-z_]', first_arg)) or \
                          bool(re.search(r'["\'][^"\']*\$[A-Za-z_]', first_arg)) or \
                          bool(re.search(r'\.\s*\$[A-Za-z_]', first_arg))
            if has_superglobal or has_tainted or has_var_arg:
                findings.append({
                    "rule_id": "CWE-78",
                    "rule_name": "OSCommandInjection",
                    "severity": "error",
                    "message": (
                        "OS command executed with a dynamic argument. "
                        "Use escapeshellarg() or avoid shell execution."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-22: Path Traversal / Remote File Include
        # ----------------------------------------------------------------
        if _FILE_SINK_RE.search(line):
            has_superglobal = bool(_SUPERGLOBAL_RE.search(line))
            has_tainted = any(tv in line for tv in tainted_vars)
            has_var_arg = bool(re.search(r'\(\s*\$', line)) or \
                          bool(re.search(r'["\'][^"\']*\$[A-Za-z_]', line)) or \
                          bool(re.search(r'\.\s*\$[A-Za-z_]', line))
            if has_superglobal or has_tainted or has_var_arg:
                # Skip common safe patterns: static includes, vendor, view includes
                if not re.search(r'\(\s*__DIR__|__FILE__|dirname|DIRECTORY_SEPARATOR', line):
                    findings.append({
                        "rule_id": "CWE-22",
                        "rule_name": "PathTraversal",
                        "severity": "error",
                        "message": (
                            "File path constructed from user-controlled input. "
                            "Validate and sanitize the path with realpath() and a whitelist."
                        ),
                        "line": lineno,
                        "file": filepath,
                    })

        # ----------------------------------------------------------------
        # CWE-611: XML External Entity
        # ----------------------------------------------------------------
        if _XML_SINK_RE.search(line) and not entity_loader_disabled:
            if not _XML_SAFE_RE.search(line):
                findings.append({
                    "rule_id": "CWE-611",
                    "rule_name": "XMLExternalEntity",
                    "severity": "error",
                    "message": (
                        "XML parsed without disabling external entity loading. "
                        "Call libxml_disable_entity_loader(true) before parsing."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-798: Hard-coded Credentials
        # ----------------------------------------------------------------
        m = _HARDCODED_CRED_RE.search(line)
        if m:
            var_name = m.group(1)
            findings.append({
                "rule_id": "CWE-798",
                "rule_name": "HardcodedCredential",
                "severity": "warning",
                "message": (
                    f"Hard-coded value assigned to credential variable "
                    f"'${var_name}'. Use environment variables or a secrets manager."
                ),
                "line": lineno,
                "file": filepath,
            })

        # ----------------------------------------------------------------
        # CWE-352: CSRF — collect POST-handling lines; evaluate at end
        # ----------------------------------------------------------------
        if _CSRF_METHOD_CHECK_RE.search(line):
            csrf_post_lines.append(lineno)
        if _CSRF_TOKEN_CHECK_RE.search(line):
            csrf_token_seen = True

    # ----------------------------------------------------------------
    # CWE-352: Emit CSRF finding if POST handling is present but no token
    # ----------------------------------------------------------------
    if csrf_post_lines and not csrf_token_seen:
        findings.append({
            "rule_id": "CWE-352",
            "rule_name": "CrossSiteRequestForgery",
            "severity": "warning",
            "message": (
                "POST request handler found without a CSRF token check. "
                "Add a synchronizer token to all state-changing requests."
            ),
            "line": csrf_post_lines[0],
            "file": filepath,
        })

    return findings


def run_php_scan(target_directory: str) -> List[Dict[str, Any]]:
    """
    Walk *target_directory* recursively and scan every .php file found,
    skipping the vendor/ tree to avoid noise from third-party libraries.

    Returns the combined list of findings.
    """
    findings: List[Dict[str, Any]] = []
    target_directory = os.path.abspath(target_directory)

    for root, dirs, files in os.walk(target_directory):
        # Skip vendor and test directories
        dirs[:] = [
            d for d in dirs
            if d not in ('vendor', '.git', 'node_modules', 'tests')
        ]
        for filename in files:
            if filename.lower().endswith('.php'):
                full_path = os.path.join(root, filename)
                findings.extend(scan_php_file(full_path))

    return findings


if __name__ == "__main__":
    import json
    from core.sarif_exporter import generate_sarif

    src_dir = sys.argv[1] if len(sys.argv) > 1 else "src"
    out_sarif = sys.argv[2] if len(sys.argv) > 2 else "reports/security-findings.sarif"

    os.makedirs(os.path.dirname(out_sarif), exist_ok=True)
    results = run_php_scan(src_dir)
    generate_sarif(results, out_sarif)
    print(f"PHP Scan Completed: {len(results)} issue(s) found -> {out_sarif}")
