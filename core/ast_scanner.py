import ast
import os
import sys
import math
from pathlib import Path
from typing import List, Dict, Any, Set

# Ensure project root is on sys.path for both script and module invocation
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from core.sarif_exporter import generate_sarif
from core.php_scanner import run_php_scan
from core.java_scanner import run_java_scan
from core.js_scanner import run_js_scan

try:
    import javalang
    _JAVALANG_AVAILABLE = True
except ImportError:  # pragma: no cover
    _JAVALANG_AVAILABLE = False

try:
    import sqlparse
    from sqlparse import tokens as _ST
    _SQLPARSE_AVAILABLE = True
except ImportError:  # pragma: no cover
    _SQLPARSE_AVAILABLE = False


def calculate_shannon_entropy(data: str) -> float:
    """Calculates Shannon Entropy to detect high-entropy random strings/keys."""
    if not data:
        return 0.0
    entropy = 0.0
    for x in set(data):
        p_x = float(data.count(x)) / len(data)
        if p_x > 0:
            entropy += -p_x * math.log2(p_x)
    return entropy


# Weak cipher algorithm names flagged by CWE-327
_WEAK_CIPHER_NAMES = {
    "TripleDES", "Blowfish", "ARC4", "ARC2",
    "DES", "DES3", "RC2", "RC4",
}

# Logger method names that sink untrusted data (CWE-117)
_LOG_METHODS = {"debug", "info", "warning", "error", "critical", "exception", "log"}

# OS-level command execution sinks (CWE-78)
_OS_EXEC_FUNCS = {"system", "popen"}

# subprocess sinks (CWE-78)
_SUBPROCESS_FUNCS = {"call", "run", "Popen", "check_output", "check_call"}

# SSL contexts that skip verification by default (CWE-295)
_UNVERIFIED_SSL_FUNCS = {"_create_unverified_context", "_create_stdlib_context", "_create_default_https_context"}


class SecurityASTVisitor(ast.NodeVisitor):
    def __init__(self, filepath: str):
        self.filepath = filepath
        self.findings: List[Dict[str, Any]] = []
        # Tainted vars are scoped per-function to prevent cross-method false positives
        self.tainted_vars: Set[str] = set()
        # Track imported names: alias -> module  e.g. {"os": "os", "ssl": "ssl"}
        self._imports: Dict[str, str] = {}
        # Track which weak cipher names are imported from crypto libs
        self._weak_cipher_imports: Set[str] = set()

    def visit_FunctionDef(self, node: ast.FunctionDef):
        """Reset taint scope at each function boundary."""
        saved = self.tainted_vars.copy()
        self.tainted_vars = set()
        self.generic_visit(node)
        self.tainted_vars = saved

    # Also handle async functions
    visit_AsyncFunctionDef = visit_FunctionDef

    # ------------------------------------------------------------------
    # Import tracking
    # ------------------------------------------------------------------
    def visit_Import(self, node: ast.Import):
        for alias in node.names:
            name = alias.asname if alias.asname else alias.name
            self._imports[name] = alias.name
        self.generic_visit(node)

    def visit_ImportFrom(self, node: ast.ImportFrom):
        module = node.module or ""
        for alias in node.names:
            imported_name = alias.name
            local_name = alias.asname if alias.asname else alias.name
            # Track crypto weak cipher imports
            if imported_name in _WEAK_CIPHER_NAMES:
                self._weak_cipher_imports.add(local_name)
            self._imports[local_name] = f"{module}.{imported_name}"
        self.generic_visit(node)

    # ------------------------------------------------------------------
    # Assignment: taint tracking + secret detection
    # ------------------------------------------------------------------
    def visit_Assign(self, node: ast.Assign):
        # 1. Taint tracking: mark variables assigned from dynamic string expressions (SQLi)
        is_dynamic_string = isinstance(node.value, (ast.JoinedStr, ast.BinOp))
        for target in node.targets:
            if isinstance(target, ast.Name):
                if is_dynamic_string:
                    self.tainted_vars.add(target.id)
                elif target.id in self.tainted_vars:
                    self.tainted_vars.discard(target.id)

        # 2. Advanced Secret Detection (CWE-798)
        secret_keywords = (
            "secret", "token", "password", "api_key",
            "jwt", "bearer", "auth_header", "db_pass"
        )
        ignore_suffixes = (
            "_label", "_hint", "_placeholder", "_title", "_text", "_name"
        )

        for target in node.targets:
            if isinstance(target, ast.Name):
                var_name = target.id.lower()
                is_candidate = any(kw in var_name for kw in secret_keywords)
                is_ignored = any(var_name.endswith(sfx) for sfx in ignore_suffixes)

                if is_candidate and not is_ignored:
                    if isinstance(node.value, ast.Constant) and isinstance(node.value.value, str):
                        val = node.value.value
                        entropy = calculate_shannon_entropy(val)
                        # Flag if sufficiently long OR high entropy (API keys, hashes, tokens)
                        if len(val) >= 12 or entropy > 3.2:
                            self.findings.append({
                                "rule_id": "CWE-798",
                                "rule_name": "HardcodedCredential",
                                "severity": "warning",
                                "message": (
                                    f"Hardcoded secret assigned to '{target.id}' "
                                    f"(entropy: {entropy:.2f}). Use os.getenv()."
                                ),
                                "line": node.lineno,
                                "file": self.filepath
                            })

        # 3. CWE-295: ssl check_hostname = False
        if isinstance(node.value, ast.Constant) and node.value.value is False:
            for target in node.targets:
                if isinstance(target, ast.Attribute) and target.attr == "check_hostname":
                    self.findings.append({
                        "rule_id": "CWE-295",
                        "rule_name": "ImproperCertValidation",
                        "severity": "error",
                        "message": (
                            "SSL/TLS hostname verification disabled via "
                            "'check_hostname = False'. This allows MITM attacks."
                        ),
                        "line": node.lineno,
                        "file": self.filepath
                    })

        self.generic_visit(node)

    # ------------------------------------------------------------------
    # Call expressions
    # ------------------------------------------------------------------
    def visit_Call(self, node: ast.Call):
        func = node.func

        # ── CWE-89: SQL Injection ──────────────────────────────────────
        if isinstance(func, ast.Attribute) and func.attr in ("execute", "raw_query"):
            if node.args:
                first_arg = node.args[0]
                is_direct_sqli = isinstance(first_arg, (ast.JoinedStr, ast.BinOp))
                is_tainted_var = (
                    isinstance(first_arg, ast.Name) and first_arg.id in self.tainted_vars
                )
                if is_direct_sqli or is_tainted_var:
                    self.findings.append({
                        "rule_id": "CWE-89",
                        "rule_name": "SQLInjection",
                        "severity": "error",
                        "message": (
                            f"Insecure SQL query construction detected in "
                            f"'{func.attr}()'. Use parameterized queries."
                        ),
                        "line": node.lineno,
                        "file": self.filepath
                    })

        # ── CWE-95: Dynamic Code Execution ────────────────────────────
        if isinstance(func, ast.Name) and func.id in ("eval", "exec"):
            self.findings.append({
                "rule_id": "CWE-95",
                "rule_name": "DynamicCodeExecution",
                "severity": "error",
                "message": f"Dangerous execution function '{func.id}()' used.",
                "line": node.lineno,
                "file": self.filepath
            })

        # ── CWE-78: OS Command Injection ──────────────────────────────
        # os.system(dynamic) / os.popen(dynamic)
        if (
            isinstance(func, ast.Attribute)
            and func.attr in _OS_EXEC_FUNCS
            and isinstance(func.value, ast.Name)
            and self._imports.get(func.value.id, "") in ("os", "os.path")
        ):
            if node.args and self._is_dynamic(node.args[0]):
                self.findings.append({
                    "rule_id": "CWE-78",
                    "rule_name": "OSCommandInjection",
                    "severity": "error",
                    "message": (
                        f"OS command injection risk: '{func.attr}()' called with "
                        "a dynamic string. Use subprocess with a list and shell=False."
                    ),
                    "line": node.lineno,
                    "file": self.filepath
                })

        # subprocess.run/call/Popen/check_output(dynamic_str, shell=True)
        if (
            isinstance(func, ast.Attribute)
            and func.attr in _SUBPROCESS_FUNCS
            and isinstance(func.value, ast.Name)
            and self._imports.get(func.value.id, "") == "subprocess"
        ):
            shell_true = any(
                isinstance(kw, ast.keyword)
                and kw.arg == "shell"
                and isinstance(kw.value, ast.Constant)
                and kw.value.value is True
                for kw in node.keywords
            )
            if node.args and self._is_dynamic(node.args[0]) and shell_true:
                self.findings.append({
                    "rule_id": "CWE-78",
                    "rule_name": "OSCommandInjection",
                    "severity": "error",
                    "message": (
                        f"OS command injection risk: 'subprocess.{func.attr}()' "
                        "called with a dynamic string and shell=True."
                    ),
                    "line": node.lineno,
                    "file": self.filepath
                })

        # ── CWE-90: LDAP Injection ────────────────────────────────────
        if isinstance(func, ast.Attribute) and func.attr in ("search_s", "search", "search_ext"):
            # Third positional arg is the filter; flag if dynamic
            if len(node.args) >= 3 and self._is_dynamic(node.args[2]):
                self.findings.append({
                    "rule_id": "CWE-90",
                    "rule_name": "LDAPInjection",
                    "severity": "error",
                    "message": (
                        f"LDAP injection risk: '{func.attr}()' called with a "
                        "dynamically constructed filter. Sanitize user input first."
                    ),
                    "line": node.lineno,
                    "file": self.filepath
                })

        # ── CWE-117: Log Injection ────────────────────────────────────
        if isinstance(func, ast.Attribute) and func.attr in _LOG_METHODS:
            # Flag if any argument is a tainted variable or dynamic string
            for arg in node.args:
                if self._is_dynamic(arg) or (isinstance(arg, ast.Name) and arg.id in self.tainted_vars):
                    self.findings.append({
                        "rule_id": "CWE-117",
                        "rule_name": "LogInjection",
                        "severity": "warning",
                        "message": (
                            f"Log injection risk: '{func.attr}()' called with "
                            "unsanitized user input. Sanitize before logging."
                        ),
                        "line": node.lineno,
                        "file": self.filepath
                    })
                    break

        # ── CWE-94: Server-Side Template Injection ────────────────────
        if isinstance(func, ast.Attribute) and func.attr == "render_template_string":
            if node.args and self._is_dynamic(node.args[0]):
                self.findings.append({
                    "rule_id": "CWE-94",
                    "rule_name": "ServerSideTemplateInjection",
                    "severity": "error",
                    "message": (
                        "SSTI risk: 'render_template_string()' called with a "
                        "dynamically constructed template containing user input."
                    ),
                    "line": node.lineno,
                    "file": self.filepath
                })

        # ── CWE-601: Open Redirect ────────────────────────────────────
        if isinstance(func, ast.Name) and func.id == "redirect":
            if node.args and self._is_request_input(node.args[0]):
                self.findings.append({
                    "rule_id": "CWE-601",
                    "rule_name": "OpenRedirect",
                    "severity": "warning",
                    "message": (
                        "Open redirect risk: 'redirect()' called with unvalidated "
                        "user-supplied URL from request arguments."
                    ),
                    "line": node.lineno,
                    "file": self.filepath
                })

        # ── CWE-359: Sensitive Information Exposure via print ─────────
        if isinstance(func, ast.Name) and func.id == "print":
            sensitive_kw = ("password", "passwd", "secret", "token", "api_key", "credential")
            for arg in node.args:
                if isinstance(arg, ast.Name) and any(kw in arg.id.lower() for kw in sensitive_kw):
                    self.findings.append({
                        "rule_id": "CWE-359",
                        "rule_name": "SensitiveInfoExposure",
                        "severity": "warning",
                        "message": (
                            f"Sensitive variable '{arg.id}' passed to print(). "
                            "Avoid logging or printing credentials."
                        ),
                        "line": node.lineno,
                        "file": self.filepath
                    })
                    break
                # Also catch string concat like "Received password: " + password
                if isinstance(arg, ast.BinOp):
                    names = [n.id for n in ast.walk(arg) if isinstance(n, ast.Name)]
                    if any(any(kw in n.lower() for kw in sensitive_kw) for n in names):
                        self.findings.append({
                            "rule_id": "CWE-359",
                            "rule_name": "SensitiveInfoExposure",
                            "severity": "warning",
                            "message": (
                                "Sensitive value concatenated into print(). "
                                "Avoid logging or printing credentials."
                            ),
                            "line": node.lineno,
                            "file": self.filepath
                        })
                        break

        # ── CWE-327: Weak Cryptographic Algorithm ────────────────────
        # Pattern A: WeakCipher.new(...) when name was imported from a crypto lib
        if isinstance(func, ast.Attribute) and func.attr == "new":
            if isinstance(func.value, ast.Name) and func.value.id in self._weak_cipher_imports:
                self.findings.append({
                    "rule_id": "CWE-327",
                    "rule_name": "WeakCryptographicAlgorithm",
                    "severity": "warning",
                    "message": (
                        f"Weak cipher '{func.value.id}' used. "
                        "Replace with AES-256-GCM or ChaCha20-Poly1305."
                    ),
                    "line": node.lineno,
                    "file": self.filepath
                })

        # Pattern B: Cipher(algorithms.WeakName(...), ...) from cryptography.hazmat
        if isinstance(func, ast.Name) and func.id == "Cipher":
            if node.args:
                first = node.args[0]
                # algorithms.TripleDES(...) etc.
                if (
                    isinstance(first, ast.Call)
                    and isinstance(first.func, ast.Attribute)
                    and first.func.attr in _WEAK_CIPHER_NAMES
                ):
                    self.findings.append({
                        "rule_id": "CWE-327",
                        "rule_name": "WeakCryptographicAlgorithm",
                        "severity": "warning",
                        "message": (
                            f"Weak cipher 'algorithms.{first.func.attr}' used. "
                            "Replace with AES-256-GCM or ChaCha20-Poly1305."
                        ),
                        "line": node.lineno,
                        "file": self.filepath
                    })

        # ── CWE-347: Improper JWT Verification ───────────────────────
        # jwt.decode(..., verify=False) or options={"verify_signature": False}
        if isinstance(func, ast.Attribute) and func.attr == "decode":
            verify_false = any(
                isinstance(kw, ast.keyword)
                and kw.arg == "verify"
                and isinstance(kw.value, ast.Constant)
                and kw.value.value is False
                for kw in node.keywords
            )
            sig_false = any(
                isinstance(kw, ast.keyword)
                and kw.arg == "options"
                and isinstance(kw.value, ast.Dict)
                and any(
                    isinstance(k, ast.Constant) and k.value == "verify_signature"
                    and isinstance(v, ast.Constant) and v.value is False
                    for k, v in zip(kw.value.keys, kw.value.values)
                )
                for kw in node.keywords
            )
            if verify_false or sig_false:
                self.findings.append({
                    "rule_id": "CWE-347",
                    "rule_name": "ImproperJWTVerification",
                    "severity": "error",
                    "message": (
                        "JWT signature verification disabled in 'decode()'. "
                        "Tokens can be forged. Remove verify=False / verify_signature=False."
                    ),
                    "line": node.lineno,
                    "file": self.filepath
                })

        # jwt.process_jwt(token) — python-jwt library does not verify by default
        if isinstance(func, ast.Attribute) and func.attr == "process_jwt":
            self.findings.append({
                "rule_id": "CWE-347",
                "rule_name": "ImproperJWTVerification",
                "severity": "warning",
                "message": (
                    "'process_jwt()' does not verify the JWT signature. "
                    "Use 'verify_jwt()' instead."
                ),
                "line": node.lineno,
                "file": self.filepath
            })

        # ── CWE-295: Improper Cert Validation (ssl functions) ─────────
        if (
            isinstance(func, ast.Attribute)
            and func.attr in _UNVERIFIED_SSL_FUNCS
            and isinstance(func.value, ast.Name)
            and self._imports.get(func.value.id, "") == "ssl"
        ):
            self.findings.append({
                "rule_id": "CWE-295",
                "rule_name": "ImproperCertValidation",
                "severity": "error",
                "message": (
                    f"ssl.{func.attr}() creates an unverified SSL context. "
                    "Use ssl.create_default_context() instead."
                ),
                "line": node.lineno,
                "file": self.filepath
            })

        self.generic_visit(node)

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------
    def _is_dynamic(self, node: ast.expr) -> bool:
        """Return True if the node is an f-string, string concatenation, or tainted var."""
        if isinstance(node, ast.JoinedStr):
            return True
        if isinstance(node, ast.BinOp) and isinstance(node.op, (ast.Add, ast.Mod)):
            return True
        if isinstance(node, ast.Name) and node.id in self.tainted_vars:
            return True
        return False

    def _is_request_input(self, node: ast.expr) -> bool:
        """Return True if the node looks like request.args[...] / request.args.get(...)."""
        # request.args["url"]
        if isinstance(node, ast.Subscript):
            val = node.value
            if (
                isinstance(val, ast.Attribute)
                and isinstance(val.value, ast.Name)
                and val.value.id == "request"
            ):
                return True
        # request.args.get("url")
        if isinstance(node, ast.Call):
            fn = node.func
            if (
                isinstance(fn, ast.Attribute)
                and fn.attr == "get"
                and isinstance(fn.value, ast.Attribute)
                and isinstance(fn.value.value, ast.Name)
                and fn.value.value.id == "request"
            ):
                return True
        return False


# ---------------------------------------------------------------------------
# Java AST scanner — javalang-based, AST-precise rules only.
#
# Complements core/java_scanner.py (regex taint analysis) with two rules that
# benefit from structural AST traversal and are NOT already covered there:
#
#   CWE-798  Hard-coded Credentials — detects high-entropy String literals
#            assigned to *any* variable, regardless of variable name, using
#            Shannon entropy on the unquoted value (threshold: len >= 12 OR
#            entropy > 3.5).  The regex scanner only flags variables whose
#            *name* contains a credential keyword; this catches the rest.
#
#   CWE-502  Unsafe Deserialization — flags every instantiation of
#            ObjectInputStream via the AST ClassCreator node, which is more
#            reliable than regex matching across multi-line declarations.
# ---------------------------------------------------------------------------

# Entropy threshold for Java String literals (slightly higher than Python's
# 3.2 to account for normal identifier-like strings in Java codebases).
_JAVA_ENTROPY_THRESHOLD = 3.5
_JAVA_MIN_LEN = 12

# Variable name keywords that indicate a credential even at lower entropy
_JAVA_CRED_KW = frozenset({
    "password", "passwd", "secret", "token", "api_key", "apikey",
    "auth_key", "db_pass", "private_key", "signing_key",
})


def scan_java_ast(filepath: str) -> List[Dict[str, Any]]:
    """
    Parse *filepath* with javalang and return AST-based findings.

    Rules
    -----
    CWE-798 : String field/local-variable whose literal value has high Shannon
              entropy (len >= 12 OR entropy > 3.5), OR whose *name* contains a
              credential keyword and has a non-trivial value (len >= 8).
    CWE-502 : Any instantiation of ObjectInputStream.
    """
    if not _JAVALANG_AVAILABLE:  # pragma: no cover
        sys.stderr.write("scan_java_ast: javalang not installed, skipping.\n")
        return []

    findings: List[Dict[str, Any]] = []

    try:
        with open(filepath, "r", encoding="utf-8", errors="replace") as fh:
            source = fh.read()
    except OSError:
        return findings

    try:
        tree = javalang.parse.parse(source)
    except javalang.parser.JavaSyntaxError:
        sys.stderr.write(f"javalang syntax error in {filepath}\n")
        return findings

    # Walk every node in the AST with its ancestor path (needed for line nums)
    for path, node in tree:
        node_type = type(node).__name__

        # ------------------------------------------------------------------
        # CWE-798: Hard-coded String credentials
        # Targets: FieldDeclaration  (class-level fields)
        #          LocalVariableDeclaration  (method-local variables)
        # ------------------------------------------------------------------
        if node_type in ("FieldDeclaration", "LocalVariableDeclaration"):
            line = node.position.line if node.position else 0
            type_name = getattr(node.type, "name", "")
            if type_name != "String":
                continue

            for decl in node.declarators:
                if not isinstance(decl.initializer, javalang.tree.Literal):
                    continue
                raw = decl.initializer.value or ""
                # Strip surrounding quotes from the Java string literal
                value = raw.strip('"')
                if not value or value.lower() in ("null", "true", "false"):
                    continue

                entropy = calculate_shannon_entropy(value)
                var_name_lower = decl.name.lower()
                is_cred_name = any(kw in var_name_lower for kw in _JAVA_CRED_KW)

                # Flag if: high entropy/length, OR credential-named with non-trivial value
                if (len(value) >= _JAVA_MIN_LEN and entropy > _JAVA_ENTROPY_THRESHOLD) or \
                        (is_cred_name and len(value) >= 8):
                    findings.append({
                        "rule_id": "CWE-798",
                        "rule_name": "HardcodedCredential",
                        "severity": "warning",
                        "message": (
                            f"High-entropy String literal assigned to '{decl.name}' "
                            f"(length={len(value)}, entropy={entropy:.2f}). "
                            "Use environment variables or a secrets manager."
                        ),
                        "line": line,
                        "file": filepath,
                    })

        # ------------------------------------------------------------------
        # CWE-502: ObjectInputStream instantiation
        # Targets: ClassCreator nodes where the constructed type is
        #          ObjectInputStream.  Line is resolved from the nearest
        #          ancestor node that carries a position.
        # ------------------------------------------------------------------
        elif node_type == "ClassCreator":
            type_name = node.type.name if (node.type) else ""
            if type_name == "ObjectInputStream":
                # Walk ancestors (path is a list of parent nodes) for a line
                line = 0
                for ancestor in reversed(list(path)):
                    pos = getattr(ancestor, "position", None)
                    if pos:
                        line = pos.line
                        break

                findings.append({
                    "rule_id": "CWE-502",
                    "rule_name": "UnsafeDeserialization",
                    "severity": "error",
                    "message": (
                        "Instantiation of ObjectInputStream detected. "
                        "Deserializing untrusted data can lead to remote code "
                        "execution. Validate input or use a safe serialization format."
                    ),
                    "line": line,
                    "file": filepath,
                })

    return findings


# ---------------------------------------------------------------------------
# Oracle SQL scanner — sqlparse-based.
#
# Rule: CWE-89  EXECUTE IMMEDIATE with string concatenation (||) or a bare
#               variable argument that is not protected by a bind placeholder
#               (:1, :name).  Parameterised EXECUTE IMMEDIATE with USING is
#               safe; a static string-only argument is also safe.
# ---------------------------------------------------------------------------

def scan_sql_ast(filepath: str) -> List[Dict[str, Any]]:
    """
    Parse *filepath* with sqlparse and return SQL injection findings.

    CWE-89 : EXECUTE IMMEDIATE whose argument contains the Oracle string
             concatenation operator (||) — a direct injection vector.
             Also flags EXECUTE IMMEDIATE called with a bare identifier
             (pre-built dynamic SQL variable) that contains no bind
             placeholder, which is a likely injection point depending on
             how the variable was constructed.

    Safe patterns (not flagged):
    * EXECUTE IMMEDIATE 'static string' — no user data involved.
    * EXECUTE IMMEDIATE '... :1 ...' / '... :name ...' — bind variables used.
    * EXECUTE IMMEDIATE v_sql USING v_param — USING clause supplies binds.
    """
    if not _SQLPARSE_AVAILABLE:  # pragma: no cover
        sys.stderr.write("scan_sql_ast: sqlparse not installed, skipping.\n")
        return []

    findings: List[Dict[str, Any]] = []

    try:
        with open(filepath, "r", encoding="utf-8", errors="replace") as fh:
            source = fh.read()
    except OSError:
        return findings

    statements = sqlparse.parse(source)
    line_offset = 1  # 1-based running line counter across all statements

    for stmt in statements:
        stmt_str = str(stmt)
        stmt_lines = stmt_str.split("\n")

        # Determine the 1-based line of the first non-blank, non-comment token
        stmt_start_line = line_offset
        for i, l in enumerate(stmt_lines):
            stripped = l.strip()
            if stripped and not stripped.startswith("--"):
                stmt_start_line = line_offset + i
                break

        flat = list(stmt.flatten())

        # Locate EXECUTE IMMEDIATE token pairs
        for i, tok in enumerate(flat):
            if not (tok.ttype is _ST.Keyword and tok.normalized.upper() == "EXECUTE"):
                continue

            # Advance past whitespace to find IMMEDIATE
            j = i + 1
            while j < len(flat) and flat[j].ttype in (
                _ST.Text.Whitespace, _ST.Text.Whitespace.Newline,
                _ST.Newline, _ST.Comment.Single, _ST.Comment.Multiline,
            ):
                j += 1
            if j >= len(flat):
                continue
            if not (flat[j].ttype is _ST.Keyword and
                    flat[j].normalized.upper() == "IMMEDIATE"):
                continue

            # Collect all tokens after IMMEDIATE up to end of statement
            rest = flat[j + 1:]

            has_concat = any(
                t.ttype is _ST.Operator and t.value == "||"
                for t in rest
            )
            has_bind = any(
                t.ttype is _ST.Name.Placeholder
                for t in rest
            )
            # A bare Name token (identifier / variable) in the argument
            has_bare_var = any(
                t.ttype is _ST.Name
                for t in rest
            )
            # USING clause supplies bind variables safely
            has_using = any(
                t.ttype is _ST.Keyword and t.normalized.upper() == "USING"
                for t in rest
            )

            # Flag if: concat used,  OR  bare variable with no bind and no USING
            is_vulnerable = has_concat or (has_bare_var and not has_bind and not has_using)
            if not is_vulnerable:
                continue

            if has_concat:
                detail = (
                    "EXECUTE IMMEDIATE argument uses the || concatenation operator. "
                    "Use bind variables (:1) with a USING clause instead."
                )
            else:
                detail = (
                    "EXECUTE IMMEDIATE receives a variable argument with no bind "
                    "placeholder and no USING clause. Ensure the variable is not "
                    "constructed from user input, or rewrite with bind variables."
                )

            findings.append({
                "rule_id": "CWE-89",
                "rule_name": "SQLInjection",
                "severity": "error",
                "message": detail,
                "line": stmt_start_line,
                "file": filepath,
            })
            break  # one finding per statement is enough

        line_offset += stmt_str.count("\n")

    return findings


def run_ast_scan(target_directory: str) -> List[Dict[str, Any]]:
    findings = []

    # Normalise so we can detect a self-nested clone (e.g. src/src/ when
    # the AEGIS project itself is scanned with target_directory="src").
    abs_target = os.path.abspath(target_directory)
    target_basename = os.path.basename(abs_target)

    # Extension-routed file walk
    for root, dirs, files in os.walk(target_directory):
        # Prune a subdirectory whose name matches the scan root's own basename
        # (prevents re-scanning AEGIS internals left in src/src/ after a clone).
        dirs[:] = [d for d in dirs if d != target_basename or
                   os.path.abspath(os.path.join(root, d)) == abs_target]
        for file in files:
            full_path = os.path.join(root, file)

            # .py  → Python AST (SecurityASTVisitor)
            if file.endswith(".py") and not file.startswith("test_"):
                with open(full_path, "r", encoding="utf-8") as f:
                    try:
                        tree = ast.parse(f.read(), filename=full_path)
                        visitor = SecurityASTVisitor(full_path)
                        visitor.visit(tree)
                        findings.extend(visitor.findings)
                    except SyntaxError as err:
                        sys.stderr.write(f"Syntax error parsing {full_path}: {err}\n")

            # .java → javalang AST (CWE-798 entropy + CWE-502; no overlap with
            #         run_java_scan which handles regex-taint rules separately)
            elif file.endswith(".java"):
                findings.extend(scan_java_ast(full_path))

            # .sql  → sqlparse AST (CWE-89 EXECUTE IMMEDIATE)
            elif file.endswith(".sql"):
                findings.extend(scan_sql_ast(full_path))

    # PHP regex-based scan
    findings.extend(run_php_scan(target_directory))

    # Java regex-based taint scan (CWE-89/78/295/312/22/611/798-name/502-regex)
    findings.extend(run_java_scan(target_directory))

    # JavaScript / TypeScript regex-based scan
    findings.extend(run_js_scan(target_directory))

    return findings


if __name__ == "__main__":
    src_dir = sys.argv[1] if len(sys.argv) > 1 else "src"
    out_sarif = sys.argv[2] if len(sys.argv) > 2 else "reports/security-findings.sarif"

    os.makedirs(os.path.dirname(out_sarif), exist_ok=True)
    scan_results = run_ast_scan(src_dir)
    generate_sarif(scan_results, out_sarif)
    print(f"AST Scan Completed: {len(scan_results)} issue(s) discovered -> {out_sarif}")
