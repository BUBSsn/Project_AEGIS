"""
Tests for scan_java_ast() and scan_sql_ast() in core/ast_scanner.py.

Java tests write .java source to a temp file and assert that javalang-based
AST rules fire or stay silent as expected.

SQL tests write Oracle PL/SQL snippets to a temp .sql file and assert that
the sqlparse-based EXECUTE IMMEDIATE rule fires or stays silent.
"""

import sys
import textwrap
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from core.ast_scanner import scan_java_ast, scan_sql_ast


def _write(tmp_path, name: str, content: str) -> str:
    p = tmp_path / name
    p.write_text(textwrap.dedent(content), encoding="utf-8")
    return str(p)


def _rule_ids(findings) -> list:
    return [f["rule_id"] for f in findings]


# ===========================================================================
# scan_java_ast — CWE-798: Hard-coded Credentials (entropy-based)
# ===========================================================================

class TestJavaAstCWE798:

    def test_high_entropy_field_flagged(self, tmp_path):
        java = (
            'public class Config {\n'
            '    private static final String API_KEY = "xK9#mP2$qR7@nL4!vZ8";\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "Config.java", java))
        assert "CWE-798" in _rule_ids(findings)

    def test_credential_named_field_lower_entropy_flagged(self, tmp_path):
        # Variable name contains "password" — flagged even without extreme entropy
        java = (
            'public class Auth {\n'
            '    String password = "openSesame1";\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "Auth.java", java))
        assert "CWE-798" in _rule_ids(findings)

    def test_secret_token_local_variable_flagged(self, tmp_path):
        java = (
            'public class Token {\n'
            '    void setup() {\n'
            '        String token = "eyJhbGciOiJIUzI1NiJ9";\n'
            '    }\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "Token.java", java))
        assert "CWE-798" in _rule_ids(findings)

    def test_short_low_entropy_string_not_flagged(self, tmp_path):
        # "hello" is short and low-entropy and has no credential keyword in name
        java = (
            'public class Greet {\n'
            '    String greeting = "hello";\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "Greet.java", java))
        creds = [f for f in findings if f["rule_id"] == "CWE-798"]
        assert len(creds) == 0

    def test_null_assignment_not_flagged(self, tmp_path):
        java = (
            'public class Cfg {\n'
            '    private String password = null;\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "Cfg.java", java))
        creds = [f for f in findings if f["rule_id"] == "CWE-798"]
        assert len(creds) == 0

    def test_finding_has_correct_schema(self, tmp_path):
        java = (
            'public class K {\n'
            '    private static final String SECRET = "s3cr3tP@ssw0rd!";\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "K.java", java))
        assert findings, "Expected at least one finding"
        f = findings[0]
        for key in ("rule_id", "rule_name", "severity", "message", "line", "file"):
            assert key in f, f"Missing key: {key}"
        assert f["rule_id"] == "CWE-798"
        assert f["line"] == 2

    def test_finding_line_number_local_var(self, tmp_path):
        java = (
            'public class Secrets {\n'
            '    void init() {\n'
            '        String apikey = "AKIAIOSFODNN7EXAMPLE";\n'
            '    }\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "Secrets.java", java))
        creds = [f for f in findings if f["rule_id"] == "CWE-798"]
        assert len(creds) == 1
        assert creds[0]["line"] == 3


# ===========================================================================
# scan_java_ast — CWE-502: Unsafe Deserialization (ObjectInputStream)
# ===========================================================================

class TestJavaAstCWE502:

    def test_object_input_stream_instantiation_flagged(self, tmp_path):
        java = (
            'import java.io.*;\n'
            'public class Deserializer {\n'
            '    Object load(InputStream is) throws Exception {\n'
            '        ObjectInputStream ois = new ObjectInputStream(is);\n'
            '        return ois.readObject();\n'
            '    }\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "Deserializer.java", java))
        assert "CWE-502" in _rule_ids(findings)

    def test_finding_schema_and_line(self, tmp_path):
        java = (
            'public class D {\n'
            '    void run(InputStream s) throws Exception {\n'
            '        ObjectInputStream o = new ObjectInputStream(s);\n'
            '    }\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "D.java", java))
        serial = [f for f in findings if f["rule_id"] == "CWE-502"]
        assert len(serial) == 1
        f = serial[0]
        for key in ("rule_id", "rule_name", "severity", "message", "line", "file"):
            assert key in f
        assert f["severity"] == "error"
        assert f["line"] == 3

    def test_other_stream_not_flagged(self, tmp_path):
        java = (
            'public class SafeIO {\n'
            '    void run(InputStream s) throws Exception {\n'
            '        BufferedInputStream bis = new BufferedInputStream(s);\n'
            '    }\n'
            '}\n'
        )
        findings = scan_java_ast(_write(tmp_path, "SafeIO.java", java))
        serial = [f for f in findings if f["rule_id"] == "CWE-502"]
        assert len(serial) == 0

    def test_syntax_error_returns_empty(self, tmp_path):
        java = "this is not valid java %%% !!!"
        findings = scan_java_ast(_write(tmp_path, "Bad.java", java))
        assert findings == []


# ===========================================================================
# scan_sql_ast — CWE-89: EXECUTE IMMEDIATE injection
# ===========================================================================

class TestSqlAstCWE89:

    def test_concat_operator_flagged(self, tmp_path):
        sql = (
            "BEGIN\n"
            "  EXECUTE IMMEDIATE 'SELECT * FROM t WHERE x = ' || v_input;\n"
            "END;\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "vuln.sql", sql))
        assert "CWE-89" in _rule_ids(findings)

    def test_concat_multipart_flagged(self, tmp_path):
        sql = (
            "EXECUTE IMMEDIATE 'DELETE FROM t WHERE id = ' || p_id || ' AND active = 1';\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "vuln2.sql", sql))
        assert "CWE-89" in _rule_ids(findings)

    def test_bare_variable_no_using_flagged(self, tmp_path):
        # v_sql is a pre-built dynamic string with no USING clause — suspicious
        sql = (
            "BEGIN\n"
            "  EXECUTE IMMEDIATE v_sql;\n"
            "END;\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "bare_var.sql", sql))
        assert "CWE-89" in _rule_ids(findings)

    def test_bind_variable_not_flagged(self, tmp_path):
        sql = (
            "EXECUTE IMMEDIATE 'SELECT * FROM t WHERE x = :1' USING v_input;\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "safe_bind.sql", sql))
        sqli = [f for f in findings if f["rule_id"] == "CWE-89"]
        assert len(sqli) == 0

    def test_static_string_not_flagged(self, tmp_path):
        sql = (
            "EXECUTE IMMEDIATE 'DROP TABLE temp_tbl';\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "safe_static.sql", sql))
        sqli = [f for f in findings if f["rule_id"] == "CWE-89"]
        assert len(sqli) == 0

    def test_using_clause_with_bare_var_not_flagged(self, tmp_path):
        # EXECUTE IMMEDIATE v_sql USING v_param — USING makes it safe
        sql = (
            "EXECUTE IMMEDIATE v_sql USING v_param;\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "safe_using.sql", sql))
        sqli = [f for f in findings if f["rule_id"] == "CWE-89"]
        assert len(sqli) == 0

    def test_mixed_file_two_stmts_one_vuln(self, tmp_path):
        sql = (
            "-- safe\n"
            "EXECUTE IMMEDIATE 'SELECT sysdate FROM dual';\n"
            "\n"
            "-- vulnerable\n"
            "EXECUTE IMMEDIATE 'SELECT * FROM t WHERE id = ' || p_id;\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "mixed.sql", sql))
        sqli = [f for f in findings if f["rule_id"] == "CWE-89"]
        assert len(sqli) == 1

    def test_finding_has_correct_schema(self, tmp_path):
        sql = "EXECUTE IMMEDIATE 'SELECT * FROM t WHERE id = ' || v_id;\n"
        findings = scan_sql_ast(_write(tmp_path, "schema.sql", sql))
        assert findings
        f = findings[0]
        for key in ("rule_id", "rule_name", "severity", "message", "line", "file"):
            assert key in f, f"Missing key: {key}"
        assert f["rule_id"] == "CWE-89"
        assert f["severity"] == "error"
        assert f["line"] >= 1

    def test_empty_file_returns_no_findings(self, tmp_path):
        findings = scan_sql_ast(_write(tmp_path, "empty.sql", ""))
        assert findings == []

    def test_no_execute_immediate_returns_no_findings(self, tmp_path):
        sql = (
            "SELECT * FROM employees WHERE dept_id = 10;\n"
            "INSERT INTO log_tbl (msg) VALUES ('started');\n"
        )
        findings = scan_sql_ast(_write(tmp_path, "plain.sql", sql))
        sqli = [f for f in findings if f["rule_id"] == "CWE-89"]
        assert len(sqli) == 0
