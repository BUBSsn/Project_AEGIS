import subprocess
import os
import sys
from pathlib import Path

# Ensure project root is on sys.path for module imports
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from mcp.server.fastmcp import FastMCP
from core.critic_engine import rollback_target

# Absolute path to project root — all subprocess calls are resolved from here
PROJECT_ROOT = Path(__file__).resolve().parent.parent

mcp = FastMCP("AEGIS-Security-Engine")


@mcp.tool()
def scan_ast_vulnerabilities(
    target_dir: str = "src",
    output_sarif: str = "reports/security-findings.sarif"
) -> str:
    """Executes the AST security scanner on the target directory and outputs a SARIF report."""
    abs_sarif = PROJECT_ROOT / output_sarif
    abs_sarif.parent.mkdir(parents=True, exist_ok=True)

    scanner = PROJECT_ROOT / "core" / "ast_scanner.py"
    cmd = [sys.executable, str(scanner), target_dir, str(abs_sarif)]
    proc = subprocess.run(cmd, capture_output=True, text=True, cwd=str(PROJECT_ROOT))

    if proc.returncode == 0:
        return f"Scan Successful: Findings saved to {output_sarif}\n{proc.stdout}"
    return f"Scan Error:\n{proc.stderr}"


@mcp.tool()
def run_critic_test_verification(test_dir: str = "tests") -> str:
    """Runs the pytest test suite with full stdout and stderr diagnostics."""
    engine = PROJECT_ROOT / "core" / "critic_engine.py"
    cmd = [sys.executable, str(engine)]
    proc = subprocess.run(cmd, capture_output=True, text=True, cwd=str(PROJECT_ROOT))

    if proc.returncode == 0:
        return "CRITIC_VERIFICATION_PASSED: All tests passed with zero regressions."
    return f"CRITIC_VERIFICATION_FAILED:\n{proc.stdout}\n{proc.stderr}"


@mcp.tool()
def rollback_file(file_path: str = "src/sample_service.py") -> str:
    """Discards uncommitted working-tree changes to a file on Critic failure."""
    if rollback_target(file_path):
        return f"Rollback successful: Restored '{file_path}' to clean git baseline."
    return f"Rollback failed for '{file_path}'. Ensure the file is tracked by git."


if __name__ == "__main__":
    mcp.run()
