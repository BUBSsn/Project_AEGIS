import ast
import os
import sys
import math
from pathlib import Path
from typing import List, Dict, Any, Set

# Ensure project root is on sys.path for both script and module invocation
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from core.sarif_exporter import generate_sarif


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


class SecurityASTVisitor(ast.NodeVisitor):
    def __init__(self, filepath: str):
        self.filepath = filepath
        self.findings: List[Dict[str, Any]] = []
        # Tainted vars are scoped per-function to prevent cross-method false positives
        self.tainted_vars: Set[str] = set()

    def visit_FunctionDef(self, node: ast.FunctionDef):
        """Reset taint scope at each function boundary."""
        saved = self.tainted_vars.copy()
        self.tainted_vars = set()
        self.generic_visit(node)
        self.tainted_vars = saved

    # Also handle async functions
    visit_AsyncFunctionDef = visit_FunctionDef

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
        self.generic_visit(node)

    def visit_Call(self, node: ast.Call):
        # 1. SQL Injection (CWE-89): direct f-string/concat and tainted variable propagation
        if isinstance(node.func, ast.Attribute) and node.func.attr in ("execute", "raw_query"):
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
                            f"'{node.func.attr}()'. Use parameterized queries."
                        ),
                        "line": node.lineno,
                        "file": self.filepath
                    })

        # 2. Dynamic Code Execution (CWE-95)
        if isinstance(node.func, ast.Name) and node.func.id in ("eval", "exec"):
            self.findings.append({
                "rule_id": "CWE-95",
                "rule_name": "DynamicCodeExecution",
                "severity": "error",
                "message": f"Dangerous execution function '{node.func.id}()' used.",
                "line": node.lineno,
                "file": self.filepath
            })

        self.generic_visit(node)


def run_ast_scan(target_directory: str) -> List[Dict[str, Any]]:
    findings = []
    for root, _, files in os.walk(target_directory):
        for file in files:
            if file.endswith(".py") and not file.startswith("test_"):
                full_path = os.path.join(root, file)
                with open(full_path, "r", encoding="utf-8") as f:
                    try:
                        tree = ast.parse(f.read(), filename=full_path)
                        visitor = SecurityASTVisitor(full_path)
                        visitor.visit(tree)
                        findings.extend(visitor.findings)
                    except SyntaxError as err:
                        sys.stderr.write(f"Syntax error parsing {full_path}: {err}\n")
    return findings


if __name__ == "__main__":
    src_dir = sys.argv[1] if len(sys.argv) > 1 else "src"
    out_sarif = sys.argv[2] if len(sys.argv) > 2 else "reports/security-findings.sarif"

    os.makedirs(os.path.dirname(out_sarif), exist_ok=True)
    scan_results = run_ast_scan(src_dir)
    generate_sarif(scan_results, out_sarif)
    print(f"AST Scan Completed: {len(scan_results)} issue(s) discovered -> {out_sarif}")
