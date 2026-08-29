import json
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import List, Optional

import streamlit as st

# Make sure core/ is importable when running as `streamlit run app.py`
sys.path.insert(0, str(Path(__file__).resolve().parent))
from core.mcp_server import clone_github_repo, scan_ast_vulnerabilities

SARIF_PATH = Path("reports/security-findings.sarif")

# ── Bob Shell availability (resolved once at startup) ─────────────────────────
_BOB_EXE     = shutil.which("bob")
_BOB_API_KEY = os.environ.get("BOBSHELL_API_KEY", "")

st.set_page_config(
    page_title="AEGIS: Automated Test & Security Hub",
    page_icon="🛡️",
    layout="wide",
)

# Theme-aware styling: respects light/dark mode via CSS media query
st.markdown(
    """
    <style>
    /* ── Metric Card Styling (Adapts to Light/Dark Mode Natively) ── */
    .metric-card {
        background-color: rgba(130, 130, 130, 0.1);
        border: 1px solid rgba(130, 130, 130, 0.2);
        border-radius: 8px;
        padding: 20px 24px;
        text-align: center;
    }
    .metric-value { 
        font-size: 2.4rem; 
        font-weight: 700; 
        /* Let Streamlit dictate the text color */
    }
    .metric-label { 
        font-size: 0.85rem; 
        margin-top: 4px; 
        opacity: 0.7; /* Fades the text slightly based on the current theme color */
    }
    </style>
    """,
    unsafe_allow_html=True,
)

st.title("🛡️ AEGIS: Automated Test & Security Hub")
st.markdown("---")

# ── Metric cards ──────────────────────────────────────────────────────────────
col1, col2, col3 = st.columns(3)

def _metric_card(col, label: str, value: str) -> None:
    col.markdown(
        f"""<div class="metric-card">
            <div class="metric-value">{value}</div>
            <div class="metric-label">{label}</div>
        </div>""",
        unsafe_allow_html=True,
    )

vuln_count = 0
if SARIF_PATH.exists():
    try:
        sarif = json.loads(SARIF_PATH.read_text())
        results = sarif.get("runs", [{}])[0].get("results", [])
        vuln_count = len(results)
    except (json.JSONDecodeError, IndexError):
        pass

_metric_card(col1, "Vulnerabilities Found", str(vuln_count))
_metric_card(col2, "Security Gaps Detected", str(max(0, vuln_count - 1)))
_metric_card(col3, "Est. Time Saved (hrs)", f"{vuln_count * 0.25:.1f}")

st.markdown("---")

# ── Supported languages ───────────────────────────────────────────────────────
st.markdown("**Detectable Languages**")
st.markdown(
    """
    <style>
    .lang-pill {
        display: inline-block;
        padding: 4px 12px;
        margin: 0 6px 6px 0;
        border-radius: 999px;
        border: 1px solid rgba(130,130,130,0.35);
        background-color: rgba(130,130,130,0.08);
        font-size: 0.82rem;
        font-weight: 500;
    }
    </style>
    <div>
        <span class="lang-pill">Python</span>
        <span class="lang-pill">SQL</span>
        <span class="lang-pill">PHP</span>
        <span class="lang-pill">Java</span>
        <span class="lang-pill">JavaScript / TypeScript</span>
    </div>
    """,
    unsafe_allow_html=True,
)

st.markdown("---")

# ── Scan form ─────────────────────────────────────────────────────────────────
with st.form("scan_form"):
    repo_url = st.text_input(
        "Target GitHub Repository URL",
        placeholder="https://github.com/owner/repo",
    )
    submitted = st.form_submit_button("🔍 Scan Codebase")

if submitted:
    if not repo_url.strip():
        st.error("Please enter a GitHub repository URL.")
    else:
        with st.spinner(f"Cloning `{repo_url}`…"):
            clone_result = clone_github_repo(repo_url, target_dir="src")

        if clone_result.startswith("Clone failed"):
            st.error(clone_result)
        else:
            st.info(clone_result)
            with st.spinner("Running AST security scan…"):
                scan_result = scan_ast_vulnerabilities(
                    target_dir="src",
                    output_sarif="reports/security-findings.sarif",
                )
            if scan_result.startswith("Scan Error"):
                st.error(scan_result)
            else:
                st.success("Scan complete.")
                st.rerun()

# ── Helpers ───────────────────────────────────────────────────────────────────

# Map file extension → language identifier for st.code()
_EXT_TO_LANG = {
    ".py": "python", ".java": "java", ".js": "javascript",
    ".ts": "typescript", ".php": "php", ".sql": "sql",
    ".rb": "ruby", ".cs": "csharp", ".cpp": "cpp",
    ".c": "c", ".go": "go", ".ts": "typescript",
}

def _rel_path(abs_uri: str) -> str:
    """Strip the local workspace prefix, returning a repo-relative path.

    The SARIF exporter writes forward-slash absolute URIs.  We want the
    portion starting from 'src/' (or whatever the scan root is named).
    Falls back to the basename if the expected prefix is not found.
    """
    # Normalise backslashes that may have survived the SARIF write
    clean = abs_uri.replace("\\", "/")
    # Walk up from 'src/' — covers nested paths like .../Project_AEGIS/src/...
    marker = "/src/"
    idx = clean.find(marker)
    if idx != -1:
        return "src" + clean[idx + len(marker) - 1:]
    # Fall back: just return the final two path components
    parts = [p for p in clean.split("/") if p]
    return "/".join(parts[-2:]) if len(parts) >= 2 else clean


def _severity_emoji(severity: str) -> str:
    """Return a colour-coded emoji for a SARIF severity level."""
    return {"error": "🔴", "critical": "🔴", "warning": "🟡", "note": "🔵"}.get(
        severity.lower(), "⚪"
    )


# ── Security-expert skill instructions (prepended to every Bob chat query) ────
_SKILL_PREAMBLE = (
    "You are a senior application security engineer answering a developer's "
    "question about a specific code vulnerability. "
    "Keep your answers concise, direct, and limited to 2-3 short paragraphs. "
    "Avoid using jargon where possible, or clearly define it if necessary. "
    "If proposing a code fix, always output the corrected code in a properly "
    "formatted Markdown block. "
    "Do not hallucinate file names or line numbers; refer strictly to the "
    "context provided in the prompt."
)

_PROPOSED_SOLUTIONS: dict = {
    "CWE-89":  (
        "**SQL Injection** — Replace string concatenation / f-string queries with "
        "parameterised queries:\n"
        "```python\n"
        "# ❌  cursor.execute(f\"SELECT * FROM t WHERE id = '{val}'\")\n"
        "# ✅  cursor.execute(\"SELECT * FROM t WHERE id = ?\", (val,))\n"
        "```"
    ),
    "CWE-78":  (
        "**OS Command Injection** — Avoid `shell=True` and never pass unsanitised "
        "input to subprocess calls. Pass arguments as a list instead:\n"
        "```python\n"
        "# ❌  subprocess.run(f\"ls {user_input}\", shell=True)\n"
        "# ✅  subprocess.run([\"ls\", user_input])\n"
        "```"
    ),
    "CWE-95":  (
        "**Dynamic Code Execution** — Remove `eval()` / `exec()`. Use "
        "`ast.literal_eval()` for safe value parsing, or redesign to avoid "
        "dynamic evaluation entirely."
    ),
    "CWE-798": (
        "**Hardcoded Credentials** — Move secrets to environment variables or a "
        "secrets manager:\n"
        "```python\n"
        "# ❌  API_KEY = \"AbCdEf123456...\"\n"
        "# ✅  API_KEY = os.getenv(\"API_KEY\")\n"
        "```"
    ),
    "CWE-295": (
        "**Improper Certificate Validation** — Never disable TLS verification. "
        "Remove `check_hostname = False` / `verify_mode = CERT_NONE` and ensure "
        "a valid CA bundle is used:\n"
        "```python\n"
        "# ❌  ctx.check_hostname = False; ctx.verify_mode = ssl.CERT_NONE\n"
        "# ✅  ctx = ssl.create_default_context()\n"
        "```"
    ),
    "CWE-327": (
        "**Weak Cryptographic Algorithm** — Replace MD5 / SHA-1 / DES / RC4 with "
        "a modern algorithm:\n"
        "```python\n"
        "# ❌  hashlib.md5(data).hexdigest()\n"
        "# ✅  hashlib.sha256(data).hexdigest()\n"
        "```"
    ),
    "CWE-347": (
        "**Improper JWT Verification** — Always verify the signature and enforce "
        "the expected algorithm. Never use `algorithms=[\"none\"]` or skip "
        "`verify_signature`:\n"
        "```python\n"
        "# ❌  jwt.decode(token, options={\"verify_signature\": False})\n"
        "# ✅  jwt.decode(token, SECRET, algorithms=[\"HS256\"])\n"
        "```"
    ),
    "CWE-94":  (
        "**Server-Side Template Injection** — Never render user-supplied strings "
        "as templates. Pass data as template variables instead:\n"
        "```python\n"
        "# ❌  env.from_string(user_input).render()\n"
        "# ✅  env.get_template(\"safe.html\").render(data=user_input)\n"
        "```"
    ),
    "CWE-601": (
        "**Open Redirect** — Validate redirect destinations against an allowlist "
        "of trusted URLs before issuing the redirect."
    ),
    "CWE-90":  (
        "**LDAP Injection** — Escape all user-supplied values with an LDAP-safe "
        "encoding function before incorporating them into filter strings."
    ),
    "CWE-117": (
        "**Log Injection** — Sanitise log messages by stripping or escaping "
        "newline characters (`\\n`, `\\r`) from untrusted input before logging."
    ),
    "CWE-359": (
        "**Sensitive Information Exposure** — Remove `print()` / logging calls "
        "that output PII, credentials, or internal state to stdout/logs."
    ),
    "CWE-502": (
        "**Unsafe Deserialization** — Avoid `pickle`, `ObjectInputStream`, or "
        "other formats that execute code on load. Use JSON or another "
        "data-only format with schema validation."
    ),
}


def _proposed_solution(rule_id: str) -> str:
    """Return a Markdown-formatted proposed fix for the given rule ID."""
    return _PROPOSED_SOLUTIONS.get(
        rule_id,
        f"Review the flagged code and consult the [{rule_id} CWE entry](https://cwe.mitre.org/data/definitions/{rule_id.replace('CWE-', '')}.html) for remediation guidance.",
    )


def _code_snippet(abs_path: str, line: int, context: int = 2) -> Optional[str]:
    """Read *abs_path* and return *context* lines either side of *line*.

    Returns None if the file cannot be opened (e.g. deleted after scan).
    """
    try:
        p = Path(abs_path)
        if not p.exists():
            return None
        all_lines = p.read_text(encoding="utf-8", errors="replace").splitlines()
        start = max(0, line - 1 - context)
        end = min(len(all_lines), line + context)
        return "\n".join(all_lines[start:end])
    except OSError:
        return None


# ── Results table ─────────────────────────────────────────────────────────────
if SARIF_PATH.exists():
    st.subheader("Security Findings")
    try:
        sarif = json.loads(SARIF_PATH.read_text())
        results = sarif.get("runs", [{}])[0].get("results", [])

        rows = []
        for r in results:
            rule_id  = r.get("ruleId", "—")
            # Severity is written directly onto each result as `level`
            severity = r.get("level", "warning")
            message  = r.get("message", {}).get("text", "—")
            locations = r.get("locations", [])
            if locations:
                phys      = locations[0].get("physicalLocation", {})
                abs_uri   = phys.get("artifactLocation", {}).get("uri", "")
                line_num  = phys.get("region", {}).get("startLine", 0)
            else:
                abs_uri, line_num = "", 0

            rows.append({
                "rule_id":  rule_id,
                "severity": severity,
                "message":  message,
                "abs_path": abs_uri.replace("/", "\\") if sys.platform == "win32"
                            else abs_uri,
                "rel_path": _rel_path(abs_uri),
                "line":     line_num,
            })

        if not rows:
            st.info("No vulnerabilities found in the last scan.")
        else:
            # ── 1. Summary dataframe (quick overview, no horizontal scroll) ──
            summary_rows = [
                {
                    "Sev": _severity_emoji(row["severity"]),
                    "Rule ID":  row["rule_id"],
                    "Severity": row["severity"].capitalize(),
                    "File":     row["rel_path"],
                    "Line":     row["line"],
                }
                for row in rows
            ]
            st.dataframe(
                summary_rows,
                use_container_width=True,
                column_config={
                    "Sev":      st.column_config.TextColumn("",      width=40),
                    "Rule ID":  st.column_config.TextColumn("Rule",  width=120),
                    "Severity": st.column_config.TextColumn("Sev.",  width=90),
                    "File":     st.column_config.TextColumn("File",  width=340),
                    "Line":     st.column_config.NumberColumn("Line", width=60),
                },
                hide_index=True,
            )

            st.markdown(f"**{len(rows)} finding{'s' if len(rows) != 1 else ''} — expand a card for detail and source context.**")
            st.markdown("")

            # ── 2. Per-finding expander cards ────────────────────────────────
            for row in rows:
                emoji   = _severity_emoji(row["severity"])
                label   = f"{emoji} {row['rel_path']}  (Line {row['line']})"
                ext     = Path(row["rel_path"]).suffix.lower()
                lang    = _EXT_TO_LANG.get(ext, "text")

                with st.expander(label, expanded=False):
                    # Metadata block
                    c1, c2 = st.columns([1, 3])
                    with c1:
                        st.markdown(f"**Rule ID**\n\n`{row['rule_id']}`")
                        st.markdown(f"**Severity**\n\n{emoji} {row['severity'].capitalize()}")
                        st.markdown(f"**Line**\n\n`{row['line']}`")
                    with c2:
                        st.markdown("**Description**")
                        st.markdown(row["message"])

                    # Source code snippet
                    snippet = _code_snippet(row["abs_path"], row["line"])
                    if snippet:
                        st.markdown("**Source context**")
                        st.code(snippet, language=lang, line_numbers=False)
                    else:
                        st.caption("_Source file not available for preview._")

                    # Proposed solution
                    st.markdown("**Proposed solution**")
                    st.markdown(_proposed_solution(row["rule_id"]))

                    # ── Chat with Bob ─────────────────────────────────────────
                    st.divider()
                    st.markdown("##### 💬 Ask Bob about this vulnerability")

                    if not _BOB_EXE:
                        st.caption(
                            "⚠️ Bob Shell is not installed or not on PATH. "
                            "Install it with: "
                            "`powershell -ep Bypass 'irm -Uri \"https://bob.ibm.com/download/bobshell.ps1\" | iex'`"
                        )
                    elif not _BOB_API_KEY:
                        st.caption(
                            "⚠️ `BOBSHELL_API_KEY` environment variable is not set. "
                            "Add it to your shell profile or restart Streamlit after running: "
                            "`$env:BOBSHELL_API_KEY = \"<your-key>\"`"
                        )
                    else:
                        chat_key = f"chat_history_{row['rel_path']}_{row['line']}"
                        if chat_key not in st.session_state:
                            st.session_state[chat_key] = []

                        # Render existing messages
                        for msg in st.session_state[chat_key]:
                            with st.chat_message(msg["role"]):
                                st.markdown(msg["content"])

                        # Hidden system context injected into every query.
                        # Question leads so Bob answers it directly; persona
                        # and vulnerability context follow as grounding.
                        def _build_prompt(question: str) -> str:
                            return (
                                f"{question}\n\n"
                                f"---\n"
                                f"CONTEXT (use this to ground your answer):\n"
                                f"{_SKILL_PREAMBLE}\n\n"
                                f"VULNERABILITY DETAILS:\n"
                                f"- Rule ID: {row['rule_id']}\n"
                                f"- File: {row['rel_path']}\n"
                                f"- Line: {row['line']}\n"
                                f"- Description: {row['message']}\n"
                                f"- Source snippet:\n```\n{snippet or '(not available)'}\n```"
                            )

                        user_input = st.chat_input(
                            "Ask a question about this finding…",
                            key=f"chat_input_{row['rel_path']}_{row['line']}",
                        )

                        if user_input:
                            combined_query = _build_prompt(user_input)

                            st.session_state[chat_key].append(
                                {"role": "user", "content": user_input}
                            )

                            with st.spinner("Bob is thinking…"):
                                result = subprocess.run(
                                    [
                                        _BOB_EXE, "run",
                                        "--mode", "ask",
                                        "--disable-mcp",
                                        "--disable-subagents",
                                        "--format", "json",
                                        combined_query,
                                    ],
                                    capture_output=True,
                                    text=True,
                                    env={**os.environ, "BOBSHELL_API_KEY": _BOB_API_KEY},
                                    cwd=str(Path(__file__).resolve().parent),
                                )

                            # Extract the assistant's text from the JSON output;
                            # fall back to raw stdout if parsing fails.
                            response = "_(No response returned.)_"
                            raw = result.stdout.strip()
                            if raw:
                                try:
                                    data = json.loads(raw)
                                    response = (
                                        data.get("last_message")
                                        or raw
                                    )
                                except (json.JSONDecodeError, AttributeError):
                                    response = raw
                            elif result.stderr.strip():
                                response = result.stderr.strip()

                            st.session_state[chat_key].append(
                                {"role": "assistant", "content": response}
                            )

                            st.rerun()

    except (json.JSONDecodeError, KeyError, IndexError) as exc:
        st.error(f"Could not parse SARIF report: {exc}")
else:
    st.info("No scan report found yet. Run a scan above.")
