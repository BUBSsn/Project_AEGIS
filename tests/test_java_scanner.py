"""
Tests for core/java_scanner.py

Each test writes a minimal Java snippet to a temp file, runs scan_java_file(),
and asserts the expected CWE is (or is not) reported.
"""

import sys
import textwrap
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from core.java_scanner import scan_java_file, run_java_scan


def _write(tmp_path, name: str, content: str) -> str:
    p = tmp_path / name
    p.write_text(textwrap.dedent(content), encoding="utf-8")
    return str(p)


def _rule_ids(findings) -> list:
    return [f["rule_id"] for f in findings]


# ---------------------------------------------------------------------------
# CWE-89: SQL Injection
# ---------------------------------------------------------------------------

def test_sqli_string_concat_rawquery(tmp_path):
    java = """
    public class Dao {
        Cursor query(String input) {
            return db.rawQuery("SELECT * FROM users WHERE id = " + input, null);
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "Dao.java", java))
    assert "CWE-89" in _rule_ids(findings)


def test_sqli_tainted_execute(tmp_path):
    java = """
    public class Search {
        void search(HttpServletRequest request) {
            String q = request.getParameter("q");
            stmt.executeQuery("SELECT * FROM products WHERE name = '" + q + "'");
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "Search.java", java))
    assert "CWE-89" in _rule_ids(findings)


def test_sqli_prepared_statement_not_flagged(tmp_path):
    java = """
    public class SafeDao {
        void query(String id) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setString(1, id);
            ps.executeQuery();
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "SafeDao.java", java))
    sqli = [f for f in findings if f["rule_id"] == "CWE-89"]
    assert len(sqli) == 0


# ---------------------------------------------------------------------------
# CWE-78: OS Command Injection
# ---------------------------------------------------------------------------

def test_cmd_injection_runtime_exec(tmp_path):
    java = """
    public class Cmd {
        void run(String userInput) {
            Runtime.getRuntime().exec("ping " + userInput);
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "Cmd.java", java))
    assert "CWE-78" in _rule_ids(findings)


def test_cmd_injection_process_builder(tmp_path):
    java = """
    public class ProcRun {
        void run(String userInput) {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", userInput);
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "ProcRun.java", java))
    assert "CWE-78" in _rule_ids(findings)


# ---------------------------------------------------------------------------
# CWE-295: Improper Certificate Validation
# ---------------------------------------------------------------------------

def test_ssl_allow_all_hostname_verifier(tmp_path):
    java = """
    import javax.net.ssl.*;
    public class Net {
        void setup() {
            conn.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "Net.java", java))
    assert "CWE-295" in _rule_ids(findings)


def test_ssl_trust_manager_empty_body(tmp_path):
    java = """
    X509TrustManager tm = new X509TrustManager() {
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
    };
    """
    findings = scan_java_file(_write(tmp_path, "TrustMgr.java", java))
    assert "CWE-295" in _rule_ids(findings)


def test_ssl_hostname_verifier_always_true(tmp_path):
    java = """
    HostnameVerifier verifier = (hostname, session) -> { return true; };
    """
    findings = scan_java_file(_write(tmp_path, "HV.java", java))
    assert "CWE-295" in _rule_ids(findings)


# ---------------------------------------------------------------------------
# CWE-312: Sensitive Data in Log
# ---------------------------------------------------------------------------

def test_log_password(tmp_path):
    java = """
    public class Login {
        void auth(String password) {
            Log.d(TAG, "password = " + password);
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "Login.java", java))
    assert "CWE-312" in _rule_ids(findings)


def test_log_no_sensitive_data_not_flagged(tmp_path):
    java = """
    public class App {
        void start() {
            Log.d(TAG, "Application started");
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "App.java", java))
    sensitive = [f for f in findings if f["rule_id"] == "CWE-312"]
    assert len(sensitive) == 0


# ---------------------------------------------------------------------------
# CWE-798: Hard-coded Credentials
# ---------------------------------------------------------------------------

def test_hardcoded_password_string(tmp_path):
    java = """
    public class Config {
        private static final String DB_PASSWORD = "supersecret123";
    }
    """
    findings = scan_java_file(_write(tmp_path, "Config.java", java))
    assert "CWE-798" in _rule_ids(findings)


def test_hardcoded_credential_variable(tmp_path):
    java = """
    public class Auth {
        String authToken = "AKIAIOSFODNN7EXAMPLE";
    }
    """
    findings = scan_java_file(_write(tmp_path, "Auth.java", java))
    assert "CWE-798" in _rule_ids(findings)


# ---------------------------------------------------------------------------
# CWE-611: XML External Entity
# ---------------------------------------------------------------------------

def test_xxe_document_builder_factory(tmp_path):
    java = """
    import javax.xml.parsers.*;
    public class XmlParser {
        void parse(InputStream input) throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(input);
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "XmlParser.java", java))
    assert "CWE-611" in _rule_ids(findings)


def test_xxe_safe_when_feature_set(tmp_path):
    java = """
    import javax.xml.parsers.*;
    public class SafeXmlParser {
        void parse(InputStream input) throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(input);
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "SafeXmlParser.java", java))
    xxe = [f for f in findings if f["rule_id"] == "CWE-611"]
    assert len(xxe) == 0


# ---------------------------------------------------------------------------
# CWE-502: Unsafe Deserialization
# ---------------------------------------------------------------------------

def test_unsafe_deserialization(tmp_path):
    java = """
    public class Deserializer {
        Object deserialize(InputStream is) throws Exception {
            ObjectInputStream ois = new ObjectInputStream(is);
            return ois.readObject();
        }
    }
    """
    findings = scan_java_file(_write(tmp_path, "Deserializer.java", java))
    assert "CWE-502" in _rule_ids(findings)


# ---------------------------------------------------------------------------
# Integration: run_java_scan on the Hackazon android source
# ---------------------------------------------------------------------------

def test_run_java_scan_finds_issues_in_hackazon():
    import os
    import glob
    src_dir = os.path.join(os.path.dirname(__file__), '..', 'src')
    java_files = glob.glob(os.path.join(src_dir, '**', '*.java'), recursive=True)
    if not os.path.isdir(src_dir) or not java_files:
        import pytest
        pytest.skip("src/ Hackazon Java tree not present")

    findings = run_java_scan(src_dir)
    assert len(findings) > 0, "Expected at least one Java finding"

    found_rule_ids = {f["rule_id"] for f in findings}
    # Hackazon Android app logs passwords and has SSL issues
    assert "CWE-312" in found_rule_ids, "CWE-312 (credential logging) not found in Java src"
