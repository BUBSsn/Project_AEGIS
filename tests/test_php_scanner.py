"""
Tests for core/php_scanner.py

Each test writes a minimal PHP snippet to a temp file, runs scan_php_file()
on it, and asserts that the expected CWE is (or is not) reported.
"""

import sys
import textwrap
from pathlib import Path

# Ensure project root is on sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from core.php_scanner import scan_php_file, run_php_scan


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _write(tmp_path, name: str, content: str) -> str:
    """Write PHP content to a temp file and return the file path."""
    p = tmp_path / name
    p.write_text(textwrap.dedent(content), encoding="utf-8")
    return str(p)


def _rule_ids(findings) -> list:
    return [f["rule_id"] for f in findings]


# ---------------------------------------------------------------------------
# CWE-89: SQL Injection
# ---------------------------------------------------------------------------

def test_sqli_superglobal_in_query(tmp_path):
    php = """
    <?php
    $query = "SELECT * FROM users WHERE id = " . $_GET['id'];
    $result = $db->query($query);
    """
    findings = scan_php_file(_write(tmp_path, "sqli.php", php))
    assert "CWE-89" in _rule_ids(findings)


def test_sqli_tainted_var_in_query(tmp_path):
    php = """
    <?php
    $name = $_POST['name'];
    $result = $db->query("SELECT * FROM products WHERE name = '$name'");
    """
    findings = scan_php_file(_write(tmp_path, "sqli2.php", php))
    assert "CWE-89" in _rule_ids(findings)


def test_sqli_parameterized_not_flagged(tmp_path):
    # Parameterised query — should NOT produce a CWE-89 finding
    php = """
    <?php
    $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
    $stmt->execute([$_GET['id']]);
    """
    findings = scan_php_file(_write(tmp_path, "sqli_safe.php", php))
    # prepare() alone on a static string is fine
    sqli = [f for f in findings if f["rule_id"] == "CWE-89"]
    assert len(sqli) == 0


# ---------------------------------------------------------------------------
# CWE-79: Cross-Site Scripting
# ---------------------------------------------------------------------------

def test_xss_echo_get(tmp_path):
    php = """
    <?php
    echo $_GET['search'];
    """
    findings = scan_php_file(_write(tmp_path, "xss.php", php))
    assert "CWE-79" in _rule_ids(findings)


def test_xss_echo_tainted_var(tmp_path):
    php = """
    <?php
    $q = $_POST['q'];
    echo $q;
    """
    findings = scan_php_file(_write(tmp_path, "xss2.php", php))
    assert "CWE-79" in _rule_ids(findings)


def test_xss_htmlspecialchars_not_flagged(tmp_path):
    php = """
    <?php
    $q = $_GET['q'];
    echo htmlspecialchars($q);
    """
    findings = scan_php_file(_write(tmp_path, "xss_safe.php", php))
    xss = [f for f in findings if f["rule_id"] == "CWE-79"]
    assert len(xss) == 0


# ---------------------------------------------------------------------------
# CWE-78: OS Command Injection
# ---------------------------------------------------------------------------

def test_cmd_injection_exec_tainted(tmp_path):
    php = """
    <?php
    $page = $_GET['page'];
    exec('cat ' . $page, $output);
    """
    findings = scan_php_file(_write(tmp_path, "cmd.php", php))
    assert "CWE-78" in _rule_ids(findings)


def test_cmd_injection_system_superglobal(tmp_path):
    php = """
    <?php
    system("ping " . $_GET['host']);
    """
    findings = scan_php_file(_write(tmp_path, "cmd2.php", php))
    assert "CWE-78" in _rule_ids(findings)


def test_cmd_injection_static_arg_not_flagged(tmp_path):
    php = """
    <?php
    exec('ls /tmp', $output);
    """
    findings = scan_php_file(_write(tmp_path, "cmd_safe.php", php))
    cmd = [f for f in findings if f["rule_id"] == "CWE-78"]
    assert len(cmd) == 0


# ---------------------------------------------------------------------------
# CWE-22: Path Traversal
# ---------------------------------------------------------------------------

def test_path_traversal_file_get_contents(tmp_path):
    php = """
    <?php
    $file = $_GET['file'];
    echo file_get_contents('/var/www/pages/' . $file);
    """
    findings = scan_php_file(_write(tmp_path, "pt.php", php))
    assert "CWE-22" in _rule_ids(findings)


def test_path_traversal_include_tainted(tmp_path):
    php = """
    <?php
    $page = $_GET['page'];
    include('pages/' . $page . '.php');
    """
    findings = scan_php_file(_write(tmp_path, "pt2.php", php))
    assert "CWE-22" in _rule_ids(findings)


def test_path_traversal_static_include_not_flagged(tmp_path):
    php = """
    <?php
    include(__DIR__ . '/config.php');
    """
    findings = scan_php_file(_write(tmp_path, "pt_safe.php", php))
    pt = [f for f in findings if f["rule_id"] == "CWE-22"]
    assert len(pt) == 0


# ---------------------------------------------------------------------------
# CWE-611: XML External Entity
# ---------------------------------------------------------------------------

def test_xxe_simplexml_load_string(tmp_path):
    php = """
    <?php
    $xml = simplexml_load_string($userInput);
    """
    findings = scan_php_file(_write(tmp_path, "xxe.php", php))
    assert "CWE-611" in _rule_ids(findings)


def test_xxe_safe_when_entity_loader_disabled(tmp_path):
    php = """
    <?php
    libxml_disable_entity_loader(true);
    $xml = simplexml_load_string($userInput);
    """
    findings = scan_php_file(_write(tmp_path, "xxe_safe.php", php))
    xxe = [f for f in findings if f["rule_id"] == "CWE-611"]
    assert len(xxe) == 0


# ---------------------------------------------------------------------------
# CWE-798: Hard-coded Credentials
# ---------------------------------------------------------------------------

def test_hardcoded_password(tmp_path):
    php = """
    <?php
    $password = "supersecret123";
    """
    findings = scan_php_file(_write(tmp_path, "cred.php", php))
    assert "CWE-798" in _rule_ids(findings)


def test_hardcoded_api_key(tmp_path):
    php = """
    <?php
    $api_key = "AKIAIOSFODNN7EXAMPLE";
    """
    findings = scan_php_file(_write(tmp_path, "cred2.php", php))
    assert "CWE-798" in _rule_ids(findings)


def test_short_value_not_flagged(tmp_path):
    # 7 chars — below the length threshold
    php = """
    <?php
    $password = "abc123";
    """
    findings = scan_php_file(_write(tmp_path, "cred_safe.php", php))
    cred = [f for f in findings if f["rule_id"] == "CWE-798"]
    assert len(cred) == 0


# ---------------------------------------------------------------------------
# CWE-352: Cross-Site Request Forgery
# ---------------------------------------------------------------------------

def test_csrf_post_without_token(tmp_path):
    php = """
    <?php
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $name = $_POST['name'];
        // Do something without CSRF check
        saveData($name);
    }
    """
    findings = scan_php_file(_write(tmp_path, "csrf.php", php))
    assert "CWE-352" in _rule_ids(findings)


def test_csrf_post_with_token_not_flagged(tmp_path):
    php = """
    <?php
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $this->checkCsrfToken('myform');
        $name = $_POST['name'];
        saveData($name);
    }
    """
    findings = scan_php_file(_write(tmp_path, "csrf_safe.php", php))
    csrf = [f for f in findings if f["rule_id"] == "CWE-352"]
    assert len(csrf) == 0


# ---------------------------------------------------------------------------
# run_php_scan: integration test against the Hackazon src directory
# ---------------------------------------------------------------------------

def test_run_php_scan_finds_issues_in_hackazon():
    """
    Smoke-test: scanning the Hackazon 'src' tree must surface at least one
    finding for each of the primary CWEs we care about.
    """
    import os
    import glob
    src_dir = os.path.join(os.path.dirname(__file__), '..', 'src')
    php_files = glob.glob(os.path.join(src_dir, '**', '*.php'), recursive=True)
    if not os.path.isdir(src_dir) or not php_files:
        import pytest
        pytest.skip("src/ Hackazon PHP tree not present")

    findings = run_php_scan(src_dir)
    assert len(findings) > 0, "Expected at least one PHP finding"

    found_rule_ids = {f["rule_id"] for f in findings}
    # These CWEs are all present in Hackazon
    for expected_cwe in ("CWE-89", "CWE-79", "CWE-78", "CWE-22"):
        assert expected_cwe in found_rule_ids, (
            f"{expected_cwe} not detected in Hackazon src/"
        )
