"""
Tests for core/js_scanner.py

Each test writes a minimal JavaScript snippet to a temp file, runs
scan_js_file(), and asserts the expected CWE is (or is not) reported.
"""

import sys
import textwrap
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from core.js_scanner import scan_js_file, run_js_scan


def _write(tmp_path, name: str, content: str) -> str:
    p = tmp_path / name
    p.write_text(textwrap.dedent(content), encoding="utf-8")
    return str(p)


def _rule_ids(findings) -> list:
    return [f["rule_id"] for f in findings]


# ---------------------------------------------------------------------------
# CWE-79: DOM-based XSS
# ---------------------------------------------------------------------------

def test_dom_xss_inner_html_location(tmp_path):
    js = """
    var q = location.search;
    document.getElementById('result').innerHTML = q;
    """
    findings = scan_js_file(_write(tmp_path, "xss.js", js))
    assert "CWE-79" in _rule_ids(findings)


def test_dom_xss_inner_html_tainted_var(tmp_path):
    js = """
    var userInput = location.hash;
    el.innerHTML = userInput;
    """
    findings = scan_js_file(_write(tmp_path, "xss2.js", js))
    assert "CWE-79" in _rule_ids(findings)


def test_dom_xss_textcontent_not_flagged(tmp_path):
    js = """
    var q = location.search;
    document.getElementById('result').textContent = q;
    """
    findings = scan_js_file(_write(tmp_path, "xss_safe.js", js))
    xss = [f for f in findings if f["rule_id"] == "CWE-79"]
    assert len(xss) == 0


# ---------------------------------------------------------------------------
# CWE-95: Code Injection (eval)
# ---------------------------------------------------------------------------

def test_eval_with_concat(tmp_path):
    js = """
    var expr = location.search;
    eval("result = " + expr);
    """
    findings = scan_js_file(_write(tmp_path, "eval.js", js))
    assert "CWE-95" in _rule_ids(findings)


def test_eval_static_string_not_flagged(tmp_path):
    js = """
    eval("var x = 1 + 2;");
    """
    findings = scan_js_file(_write(tmp_path, "eval_safe.js", js))
    code_inj = [f for f in findings if f["rule_id"] == "CWE-95"]
    assert len(code_inj) == 0


# ---------------------------------------------------------------------------
# CWE-601: Open Redirect
# ---------------------------------------------------------------------------

def test_open_redirect_window_location(tmp_path):
    js = """
    var redirect = location.search;
    window.location.href = redirect;
    """
    findings = scan_js_file(_write(tmp_path, "redirect.js", js))
    assert "CWE-601" in _rule_ids(findings)


def test_open_redirect_static_not_flagged(tmp_path):
    js = """
    window.location.href = '/dashboard';
    """
    findings = scan_js_file(_write(tmp_path, "redirect_safe.js", js))
    redir = [f for f in findings if f["rule_id"] == "CWE-601"]
    assert len(redir) == 0


# ---------------------------------------------------------------------------
# CWE-798: Hard-coded Credentials
# ---------------------------------------------------------------------------

def test_hardcoded_api_key(tmp_path):
    js = """
    const apiKey = "AKIAIOSFODNN7EXAMPLE";
    """
    findings = scan_js_file(_write(tmp_path, "creds.js", js))
    assert "CWE-798" in _rule_ids(findings)


def test_hardcoded_password_prop(tmp_path):
    js = """
    var config = { password: "supersecret123", host: "localhost" };
    """
    findings = scan_js_file(_write(tmp_path, "creds2.js", js))
    assert "CWE-798" in _rule_ids(findings)


def test_short_credential_not_flagged(tmp_path):
    js = """
    var password = "abc";
    """
    findings = scan_js_file(_write(tmp_path, "creds_safe.js", js))
    creds = [f for f in findings if f["rule_id"] == "CWE-798"]
    assert len(creds) == 0


# ---------------------------------------------------------------------------
# CWE-327: Weak Cryptography
# ---------------------------------------------------------------------------

def test_md5_hash_flagged(tmp_path):
    js = """
    const hash = crypto.createHash('md5').update(data).digest('hex');
    """
    findings = scan_js_file(_write(tmp_path, "crypto.js", js))
    assert "CWE-327" in _rule_ids(findings)


def test_sha1_flagged(tmp_path):
    js = """
    const h = crypto.createHash('sha1').update(data).digest('hex');
    """
    findings = scan_js_file(_write(tmp_path, "crypto2.js", js))
    assert "CWE-327" in _rule_ids(findings)


def test_sha256_not_flagged(tmp_path):
    js = """
    const hash = crypto.createHash('sha256').update(data).digest('hex');
    """
    findings = scan_js_file(_write(tmp_path, "crypto_safe.js", js))
    weak = [f for f in findings if f["rule_id"] == "CWE-327"]
    assert len(weak) == 0


# ---------------------------------------------------------------------------
# CWE-352: CSRF — AJAX POST without token
# ---------------------------------------------------------------------------

def test_csrf_ajax_post_no_token(tmp_path):
    js = """
    $.ajax({
        url: '/api/update',
        type: 'POST',
        data: { name: 'value' }
    });
    """
    findings = scan_js_file(_write(tmp_path, "ajax.js", js))
    assert "CWE-352" in _rule_ids(findings)


def test_csrf_ajax_post_with_csrf_header_not_flagged(tmp_path):
    js = """
    $.ajax({
        url: '/api/update',
        type: 'POST',
        headers: { 'X-CSRF-Token': token },
        data: { name: 'value' }
    });
    """
    findings = scan_js_file(_write(tmp_path, "ajax_safe.js", js))
    csrf = [f for f in findings if f["rule_id"] == "CWE-352"]
    assert len(csrf) == 0


# ---------------------------------------------------------------------------
# Minified files are skipped
# ---------------------------------------------------------------------------

def test_minified_file_skipped(tmp_path):
    js = """
    eval(location.search);document.write(location.hash);
    """
    findings = scan_js_file(_write(tmp_path, "bundle.min.js", js))
    assert len(findings) == 0


# ---------------------------------------------------------------------------
# Integration: run_js_scan on the Hackazon src
# ---------------------------------------------------------------------------

def test_run_js_scan_finds_issues_in_hackazon():
    import os
    import glob
    src_dir = os.path.join(os.path.dirname(__file__), '..', 'src')
    js_files = glob.glob(os.path.join(src_dir, '**', '*.js'), recursive=True)
    if not os.path.isdir(src_dir) or not js_files:
        import pytest
        pytest.skip("src/ Hackazon JS tree not present")

    findings = run_js_scan(src_dir)
    assert len(findings) > 0, "Expected at least one JS finding"
