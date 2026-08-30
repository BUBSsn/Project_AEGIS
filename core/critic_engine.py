import subprocess
import json
import os
import sys
from pathlib import Path
from typing import Dict, Any

# Ensure project root is importable when run as __main__ or via MCP subprocess.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))


def rollback_target(file_path: str = "src/sample_service.py") -> bool:
    """
    Restores the target file to its last committed Git state.

    Note: This reverts to HEAD, not a pre-Actor snapshot. The Actor phase
    must NOT commit changes before the Critic runs — patches are staged as
    working-tree modifications only, ensuring rollback always returns to the
    clean baseline.
    """
    result = subprocess.run(
        ["git", "checkout", "--", file_path],
        capture_output=True,
        text=True
    )
    return result.returncode == 0


def run_test_verification(test_directory: str = "tests") -> Dict[str, Any]:
    """
    Executes the pytest harness and captures combined stdout/stderr diagnostics.
    Writes a full validation report to reports/validation_report.json.
    """
    cmd = [sys.executable, "-m", "pytest", test_directory, "-v"]
    process = subprocess.run(cmd, capture_output=True, text=True)

    passed = process.returncode == 0
    report = {
        "status": "PASSED" if passed else "FAILED",
        "return_code": process.returncode,
        "combined_output": f"STDOUT:\n{process.stdout}\nSTDERR:\n{process.stderr}"
    }

    os.makedirs("reports", exist_ok=True)
    with open("reports/validation_report.json", "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    return report


if __name__ == "__main__":
    _test_dir = sys.argv[1] if len(sys.argv) > 1 else "tests"
    result = run_test_verification(_test_dir)
    print(f"Critic Verification Verdict: {result['status']}")
    if result["status"] != "PASSED":
        print(result["combined_output"])
        sys.exit(1)
