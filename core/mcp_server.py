import re
import subprocess
import os
import sys
import stat
import shutil
from pathlib import Path

import git

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
    # Forward test_dir so callers can scope the run to a specific sub-suite.
    cmd = [sys.executable, str(engine), test_dir]
    proc = subprocess.run(cmd, capture_output=True, text=True, cwd=str(PROJECT_ROOT))

    if proc.returncode == 0:
        return "CRITIC_VERIFICATION_PASSED: All tests passed with zero regressions."
    return f"CRITIC_VERIFICATION_FAILED:\n{proc.stdout}\n{proc.stderr}"


def _force_rmtree(path: Path) -> None:
    """Remove a directory tree, clearing read-only flags on Windows before deletion."""
    def _on_error(func, fpath, exc_info):
        # Clear the read-only bit and retry
        os.chmod(fpath, stat.S_IWRITE)
        func(fpath)

    shutil.rmtree(path, onerror=_on_error)


def _normalize_github_url(url: str) -> str:
    """Strip /tree/<ref>/... or /blob/<ref>/... suffixes from a GitHub URL so
    that the result is a valid git-cloneable repository root URL."""
    return re.sub(r"/(tree|blob)/[^/]+(/.*)?$", "", url.rstrip("/"))


@mcp.tool()
def clone_github_repo(repo_url: str, target_dir: str = "src") -> str:
    """Clones a remote GitHub repository into the local target directory, clearing it first if it exists."""
    normalized = _normalize_github_url(repo_url)
    abs_target = PROJECT_ROOT / target_dir
    if abs_target.exists():
        _force_rmtree(abs_target)
    abs_target.mkdir(parents=True, exist_ok=True)

    try:
        git.Repo.clone_from(normalized, str(abs_target))
        msg = f"Clone successful: '{normalized}' cloned into '{target_dir}'."
        if normalized != repo_url:
            msg += f" (URL normalized from '{repo_url}')"
        return msg
    except git.exc.GitCommandError as e:
        return f"Clone failed: {e}"


@mcp.tool()
def rollback_file(file_path: str = "src/sample_service.py") -> str:
    """Discards uncommitted working-tree changes to a file on Critic failure."""
    if rollback_target(file_path):
        return f"Rollback successful: Restored '{file_path}' to clean git baseline."
    return f"Rollback failed for '{file_path}'. Ensure the file is tracked by git."


if __name__ == "__main__":
    mcp.run()
