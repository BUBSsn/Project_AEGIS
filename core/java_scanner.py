"""
Java Security Scanner — regex-based taint analysis for .java source files.

Detected CWEs
-------------
  CWE-89   SQL Injection
             – String concat / format into execSQL / rawQuery / query
             – String concat into Statement.execute/executeQuery
  CWE-78   OS Command Injection
             – Runtime.exec() / ProcessBuilder with a dynamic/user string
  CWE-295  Improper Certificate Validation
             – TrustManager that never throws (empty checkServerTrusted)
             – HostnameVerifier that always returns true
             – SSLContext.getInstance("SSL") / allowAllHostnames
  CWE-312  Sensitive Information in Log
             – Log.d/i/w/e/v/wtf with password/token/secret/credential variables
  CWE-22   Path Traversal
             – new File() / FileInputStream / Paths.get() with user-controlled input
  CWE-611  XML External Entity
             – DocumentBuilderFactory / SAXParser without setFeature(DISALLOW_DOCTYPE)
  CWE-798  Hard-coded Credentials
             – String password/secret/token/api_key = "literal" (len >= 8)
  CWE-502  Unsafe Deserialization
             – ObjectInputStream.readObject() on data from network/user sources

Scanning strategy
-----------------
Line-oriented.  Taint is propagated from recognised source patterns
(getIntent().getStringExtra, getQueryParameter, getParameter, request.getParameter,
 cursor.getString, intent.getData, scanner.nextLine, etc.) to variable names.
Sinks are matched by pattern; a finding is emitted when a sink receives
a superglobal-like source or a tainted variable.
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Any, Set

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

# ---------------------------------------------------------------------------
# Source patterns — things that bring user / external data into a variable
# ---------------------------------------------------------------------------
_USER_SOURCE_RE = re.compile(
    r'getIntent\(\)\s*\.\s*get\w+Extra\b'
    r'|getQueryParameter\s*\('
    r'|getParameter\s*\('          # Servlet HttpServletRequest
    r'|request\.getParameter\s*\('
    r'|request\.getHeader\s*\('
    r'|request\.getBody\s*\('
    r'|scanner\.nextLine\s*\('
    r'|readLine\s*\('
    r'|cursor\.getString\s*\('
    r'|intent\.getData\s*\('
    r'|getStringExtra\s*\(',
    re.IGNORECASE,
)

# Assignment taint: Type varName = <user_source>
_ASSIGN_TAINT_RE = re.compile(
    r'\b([A-Za-z_][A-Za-z0-9_]*)\s*='
    r'(?!=)'
    r'.*(?:'
    r'getIntent\(\)\.get\w+Extra'
    r'|getQueryParameter'
    r'|getParameter'
    r'|request\.getParameter'
    r'|request\.getHeader'
    r'|scanner\.nextLine'
    r'|readLine\(\)'
    r'|cursor\.getString'
    r'|intent\.getData'
    r'|getStringExtra'
    r')',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# SQL sinks
# ---------------------------------------------------------------------------
_SQL_SINK_RE = re.compile(
    r'\b(?:execSQL|rawQuery|rawQueryWithFactory|compileStatement)\s*\('
    r'|\.(?:execute|executeQuery|executeUpdate)\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# OS command sinks
# ---------------------------------------------------------------------------
_CMD_SINK_RE = re.compile(
    r'Runtime\.getRuntime\(\)\s*\.\s*exec\s*\('
    r'|new\s+ProcessBuilder\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# SSL / certificate validation
# ---------------------------------------------------------------------------
_SSL_BYPASS_RE = re.compile(
    r'ALLOW_ALL_HOSTNAME_VERIFIER'
    r'|setHostnameVerifier\s*\(\s*(?:null|SSLSocketFactory\.ALLOW_ALL|ALLOW_ALL)'
    r'|SSLContext\.getInstance\s*\(\s*"SSL"\s*\)'
    r'|HttpsURLConnection\.setDefaultHostnameVerifier'
    r'|HttpsURLConnection\.setDefaultSSLSocketFactory',
    re.IGNORECASE,
)

# TrustManager / HostnameVerifier that always passes — empty or trivially-true bodies
_TRUST_EMPTY_RE = re.compile(
    r'checkServerTrusted\s*\([^)]*\)\s*\{[^}]*\}'            # empty checkServerTrusted
    r'|verify\s*\([^)]*\)\s*\{\s*return\s+true\s*;\s*\}'    # classic always-true verifier
    r'|->(?:\s*\{)?\s*return\s+true\s*;?\s*\}?'             # lambda: -> { return true; } / -> true
    r'|verify\s*\([^)]*\)\s*->\s*true\b',                    # method ref / one-liner lambda
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Sensitive log patterns
# ---------------------------------------------------------------------------
_LOG_SINK_RE = re.compile(
    r'\bLog\s*\.\s*(?:d|i|w|e|v|wtf)\s*\(',
    re.IGNORECASE,
)
_SENSITIVE_VAR_RE = re.compile(
    r'(?:password|passwd|secret|token|credential|api_key|auth)',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Path traversal sinks
# ---------------------------------------------------------------------------
_FILE_SINK_RE = re.compile(
    r'new\s+File\s*\('
    r'|new\s+FileInputStream\s*\('
    r'|new\s+FileOutputStream\s*\('
    r'|Paths\.get\s*\('
    r'|new\s+FileReader\s*\(',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# XXE sinks — XML parsing factory setup
# ---------------------------------------------------------------------------
_XXE_SINK_RE = re.compile(
    r'DocumentBuilderFactory\s*\.\s*newInstance\s*\(\s*\)'
    r'|SAXParserFactory\s*\.\s*newInstance\s*\(\s*\)'
    r'|XMLInputFactory\s*\.\s*newInstance\s*\(\s*\)'
    r'|TransformerFactory\s*\.\s*newInstance\s*\(\s*\)',
    re.IGNORECASE,
)
_XXE_SAFE_FEATURE_RE = re.compile(
    r'setFeature\s*\(\s*"http://apache.org/xml/features/disallow-doctype-decl"'
    r'|setFeature\s*\(\s*"http://xml.org/sax/features/external-general-entities".*false'
    r'|setExpandEntityReferences\s*\(\s*false\s*\)'
    r'|setFeature\s*\(\s*XMLConstants',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Hard-coded credentials
# ---------------------------------------------------------------------------
_HARDCODED_CRED_RE = re.compile(
    r'(?:String|final\s+String|private\s+(?:static\s+)?(?:final\s+)?String)\s+'
    r'([A-Za-z_][A-Za-z0-9_]*(?:password|passwd|secret|token|api_key|apikey|auth_key|db_pass)[A-Za-z0-9_]*'
    r'|(?:password|passwd|secret|token|api_key|apikey|auth_key|db_pass)[A-Za-z0-9_]*)\s*='
    r'\s*"([^"]{8,})"',
    re.IGNORECASE,
)

# Also catch: private static final String PASSWORD = "..."
_HARDCODED_CONST_RE = re.compile(
    r'(?:private|public|protected|)\s*(?:static\s+)?(?:final\s+)?String\s+'
    r'([A-Z_]*(?:PASSWORD|SECRET|TOKEN|API_KEY|AUTH_KEY|DB_PASS)[A-Z_0-9]*)\s*=\s*"([^"]{8,})"',
    re.IGNORECASE,
)

# ---------------------------------------------------------------------------
# Unsafe deserialization
# ---------------------------------------------------------------------------
_DESERIAL_SINK_RE = re.compile(
    r'new\s+ObjectInputStream\s*\('
    r'|\.readObject\s*\(\s*\)',
    re.IGNORECASE,
)


def _is_comment(line: str) -> bool:
    stripped = line.strip()
    return stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*')


def scan_java_file(filepath: str) -> List[Dict[str, Any]]:
    """Scan a single .java file and return a list of findings."""
    findings: List[Dict[str, Any]] = []

    try:
        with open(filepath, "r", encoding="utf-8", errors="replace") as fh:
            content = fh.read()
            lines = content.splitlines()
    except OSError:
        return findings

    tainted_vars: Set[str] = set()
    # Track whether a safe XXE feature has been set in this file
    xxe_factory_lines: List[int] = []
    xxe_safe = False

    for lineno_0, raw_line in enumerate(lines):
        lineno = lineno_0 + 1
        line = raw_line

        if _is_comment(line):
            continue

        # ----------------------------------------------------------------
        # Taint propagation
        # ----------------------------------------------------------------
        for m in _ASSIGN_TAINT_RE.finditer(line):
            tainted_vars.add(m.group(1))

        if _USER_SOURCE_RE.search(line):
            # Also mark any variable on the left of an assignment on this line
            for m in re.finditer(r'\b([A-Za-z_][A-Za-z0-9_]*)\s*=(?!=)', line):
                tainted_vars.add(m.group(1))

        # ----------------------------------------------------------------
        # CWE-89: SQL Injection
        # ----------------------------------------------------------------
        if _SQL_SINK_RE.search(line):
            # Match any + var after a string literal end quote (" or ')
            # or String.format injection
            has_concat = bool(re.search(r'["\'][\s\+]+[A-Za-z_]|[A-Za-z_][A-Za-z0-9_]*\s*\+\s*["\']|\+\s*[A-Za-z_]', line)) or \
                         bool(re.search(r'String\.format\s*\(', line))
            has_tainted = any(tv in line for tv in tainted_vars)
            if has_concat or has_tainted:
                findings.append({
                    "rule_id": "CWE-89",
                    "rule_name": "SQLInjection",
                    "severity": "error",
                    "message": (
                        "SQL query constructed with string concatenation or "
                        "tainted input. Use parameterized queries / PreparedStatement."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-78: OS Command Injection
        # ----------------------------------------------------------------
        if _CMD_SINK_RE.search(line):
            has_concat = bool(re.search(r'["\'][\s\+]+[A-Za-z_]|\+\s*[A-Za-z_]', line))
            has_tainted = any(tv in line for tv in tainted_vars)
            # For ProcessBuilder: flag if any argument position contains a bare variable
            # Strip string literals first to avoid matching "bash" as a variable
            stripped = re.sub(r'"[^"]*"', '""', re.sub(r"'[^']*'", "''", line))
            has_var_arg = bool(re.search(r',\s*[A-Za-z_][A-Za-z0-9_]*\s*[,)]', stripped))
            if has_concat or has_tainted or has_var_arg:
                findings.append({
                    "rule_id": "CWE-78",
                    "rule_name": "OSCommandInjection",
                    "severity": "error",
                    "message": (
                        "OS command executed with a potentially user-controlled argument. "
                        "Avoid Runtime.exec()/ProcessBuilder with dynamic input."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-295: Improper Certificate / Hostname Validation
        # ----------------------------------------------------------------
        if _SSL_BYPASS_RE.search(line):
            findings.append({
                "rule_id": "CWE-295",
                "rule_name": "ImproperCertValidation",
                "severity": "error",
                "message": (
                    "SSL/TLS certificate or hostname validation bypassed. "
                    "Do not use ALLOW_ALL_HOSTNAME_VERIFIER or SSLContext(\"SSL\")."
                ),
                "line": lineno,
                "file": filepath,
            })

        if _TRUST_EMPTY_RE.search(line):
            findings.append({
                "rule_id": "CWE-295",
                "rule_name": "ImproperCertValidation",
                "severity": "error",
                "message": (
                    "TrustManager or HostnameVerifier implementation never validates "
                    "certificates (empty body or always returns true)."
                ),
                "line": lineno,
                "file": filepath,
            })

        # ----------------------------------------------------------------
        # CWE-312: Sensitive Data Exposed in Log
        # ----------------------------------------------------------------
        if _LOG_SINK_RE.search(line) and _SENSITIVE_VAR_RE.search(line):
            findings.append({
                "rule_id": "CWE-312",
                "rule_name": "SensitiveDataExposure",
                "severity": "warning",
                "message": (
                    "Sensitive value (password/token/credential) passed to Log.*. "
                    "Remove credential logging before shipping to production."
                ),
                "line": lineno,
                "file": filepath,
            })

        # ----------------------------------------------------------------
        # CWE-22: Path Traversal
        # ----------------------------------------------------------------
        if _FILE_SINK_RE.search(line):
            has_tainted = any(tv in line for tv in tainted_vars)
            has_concat = bool(re.search(r'"\s*\+\s*[A-Za-z_]', line))
            has_user_src = bool(_USER_SOURCE_RE.search(line))
            if has_tainted or has_concat or has_user_src:
                findings.append({
                    "rule_id": "CWE-22",
                    "rule_name": "PathTraversal",
                    "severity": "error",
                    "message": (
                        "File path constructed from potentially tainted input. "
                        "Validate and canonicalize with File.getCanonicalPath()."
                    ),
                    "line": lineno,
                    "file": filepath,
                })

        # ----------------------------------------------------------------
        # CWE-611: XML External Entity — track factory instantiation
        # ----------------------------------------------------------------
        if _XXE_SINK_RE.search(line):
            xxe_factory_lines.append(lineno)
        if _XXE_SAFE_FEATURE_RE.search(line):
            xxe_safe = True

        # ----------------------------------------------------------------
        # CWE-798: Hard-coded Credentials
        # ----------------------------------------------------------------
        for pattern in (_HARDCODED_CRED_RE, _HARDCODED_CONST_RE):
            m = pattern.search(line)
            if m:
                var_name = m.group(1)
                findings.append({
                    "rule_id": "CWE-798",
                    "rule_name": "HardcodedCredential",
                    "severity": "warning",
                    "message": (
                        f"Hard-coded credential value assigned to '{var_name}'. "
                        "Use environment variables or a secrets manager."
                    ),
                    "line": lineno,
                    "file": filepath,
                })
                break  # one finding per line

        # ----------------------------------------------------------------
        # CWE-502: Unsafe Deserialization
        # ----------------------------------------------------------------
        if _DESERIAL_SINK_RE.search(line):
            findings.append({
                "rule_id": "CWE-502",
                "rule_name": "UnsafeDeserialization",
                "severity": "error",
                "message": (
                    "ObjectInputStream used — deserializing untrusted data can lead "
                    "to remote code execution. Validate or replace with a safe format."
                ),
                "line": lineno,
                "file": filepath,
            })

    # ----------------------------------------------------------------
    # CWE-611: emit findings for any XML factory not guarded by safe features
    # ----------------------------------------------------------------
    if xxe_factory_lines and not xxe_safe:
        for factory_line in xxe_factory_lines:
            findings.append({
                "rule_id": "CWE-611",
                "rule_name": "XMLExternalEntity",
                "severity": "error",
                "message": (
                    "XML parser factory instantiated without disabling external "
                    "entity processing. Set FEATURE_DISALLOW_DOCTYPE_DECL to true."
                ),
                "line": factory_line,
                "file": filepath,
            })

    return findings


def run_java_scan(target_directory: str) -> List[Dict[str, Any]]:
    """Walk target_directory recursively and scan every .java file, skipping vendor/test dirs."""
    findings: List[Dict[str, Any]] = []
    target_directory = os.path.abspath(target_directory)

    for root, dirs, files in os.walk(target_directory):
        dirs[:] = [
            d for d in dirs
            if d not in ('.git', 'node_modules', 'build', 'bin', '.gradle')
            and 'test' not in d.lower()
            and 'vendor' not in d.lower()
        ]
        for filename in files:
            if filename.lower().endswith('.java'):
                full_path = os.path.join(root, filename)
                findings.extend(scan_java_file(full_path))

    return findings


if __name__ == "__main__":
    from core.sarif_exporter import generate_sarif

    src_dir = sys.argv[1] if len(sys.argv) > 1 else "src"
    out_sarif = sys.argv[2] if len(sys.argv) > 2 else "reports/security-findings.sarif"

    os.makedirs(os.path.dirname(out_sarif), exist_ok=True)
    results = run_java_scan(src_dir)
    generate_sarif(results, out_sarif)
    print(f"Java Scan Completed: {len(results)} issue(s) found -> {out_sarif}")
