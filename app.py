import json
import math
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

    /* ── Skeleton shimmer (applied via inline style= on individual elements) ── */
    @keyframes skeleton-shimmer {
        0%   { background-position: -400px 0; }
        100% { background-position:  400px 0; }
    }
    </style>
    """,
    unsafe_allow_html=True,
)

st.title("🛡️ AEGIS: Automated Test & Security Hub")
st.markdown("---")

# ── Metric cards ──────────────────────────────────────────────────────────────
# Skeleton helpers — defined early so they're available for the metric cards
# and the findings section below.
_SK = (
    'background:linear-gradient(90deg,'
    'rgba(130,130,130,0.10) 25%,rgba(130,130,130,0.22) 50%,rgba(130,130,130,0.10) 75%);'
    'background-size:400px 100%;'
    'animation:skeleton-shimmer 1.4s ease-in-out infinite;'
    'border-radius:5px;display:block;'
)

def _sk_bar(height: int = 16, width: str = "100%", extra: str = "") -> None:
    """Render one shimmer bar as its own st.markdown block (correct iframe sizing)."""
    st.markdown(
        f'<div style="{_SK}height:{height}px;width:{width};{extra}"></div>',
        unsafe_allow_html=True,
    )

def _sk_metric_col(col) -> None:
    """Skeleton placeholder shaped like a metric card."""
    col.markdown(
        f'<div style="border:1px solid rgba(130,130,130,0.15);border-radius:8px;'
        f'padding:20px 24px;text-align:center;">'
        f'<div style="{_SK}height:36px;width:50%;margin:0 auto 10px;"></div>'
        f'<div style="{_SK}height:14px;width:65%;margin:0 auto;"></div>'
        f'</div>',
        unsafe_allow_html=True,
    )

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
sarif_ready = False
if SARIF_PATH.exists():
    try:
        sarif = json.loads(SARIF_PATH.read_text())
        results = sarif.get("runs", [{}])[0].get("results", [])
        vuln_count = len(results)
        sarif_ready = True
    except (json.JSONDecodeError, IndexError):
        pass

if sarif_ready:
    _metric_card(col1, "Vulnerabilities Found", str(vuln_count))
    _metric_card(col2, "Security Gaps Detected", str(max(0, vuln_count - 1)))
    _metric_card(col3, "Est. Time Saved (hrs)", f"{vuln_count * 0.25:.1f}")
else:
    _sk_metric_col(col1)
    _sk_metric_col(col2)
    _sk_metric_col(col3)

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
        with st.status(f"Scanning `{repo_url}`…", expanded=True) as status:
            status.write(f"Cloning `{repo_url}`…")
            clone_result = clone_github_repo(repo_url, target_dir="src")

            if clone_result.startswith("Clone failed"):
                status.update(label="Clone failed", state="error", expanded=True)
                st.error(clone_result)
            else:
                status.write(clone_result)
                status.write("Running AST security scan…")
                scan_result = scan_ast_vulnerabilities(
                    target_dir="src",
                    output_sarif="reports/security-findings.sarif",
                )
                if scan_result.startswith("Scan Error"):
                    status.update(label="Scan failed", state="error", expanded=True)
                    st.error(scan_result)
                else:
                    status.update(label="Scan complete", state="complete", expanded=False)
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
    "Answer the developer's question about the code vulnerability below. "
    "Keep the answer concise (2-3 short paragraphs), avoid unnecessary jargon "
    "or define it briefly if used, and if proposing a fix, put the corrected "
    "code in a Markdown code block. "
    "Use only the file names, line numbers, and details provided below — "
    "do not invent any."
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

_OFFICIAL_DOCS: dict = {
    "CWE-89":  "https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html",
    "CWE-78":  "https://cheatsheetseries.owasp.org/cheatsheets/OS_Command_Injection_Defense_Cheat_Sheet.html",
    "CWE-798": "https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html",
    "CWE-295": "https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Security_Cheat_Sheet.html",
    "CWE-327": "https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html",
    "CWE-347": "https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html",
    "CWE-601": "https://cheatsheetseries.owasp.org/cheatsheets/Unvalidated_Redirects_and_Forwards_Cheat_Sheet.html",
    "CWE-90":  "https://cheatsheetseries.owasp.org/cheatsheets/LDAP_Injection_Prevention_Cheat_Sheet.html",
    "CWE-117": "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html",
    "CWE-359": "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html",
    "CWE-312": "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html",
    "CWE-502": "https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html",
    "CWE-79":  "https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html",
    "CWE-352": "https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html",
    "CWE-611": "https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html",
    "CWE-95":  "https://cheatsheetseries.owasp.org/cheatsheets/Injection_Prevention_Cheat_Sheet.html",
    # CWE-94 (SSTI) and CWE-22 (Path Traversal) have no dedicated OWASP cheat sheet —
    # deliberately omitted so they fall through to the MITRE CWE-definition fallback.
}


def _blame_authors(abs_path: str) -> dict:
    """Return {line_number: author_name} for a git-tracked file, or {} if unavailable."""
    try:
        result = subprocess.run(
            ["git", "blame", "--porcelain", abs_path],
            capture_output=True, text=True, encoding="utf-8", errors="replace",
            cwd=str(Path(abs_path).resolve().parent),
        )
        if result.returncode != 0:
            return {}
        authors: dict = {}
        current_line: Optional[int] = None
        current_author: Optional[str] = None
        for line in result.stdout.splitlines():
            if line and line[0].isalnum() and len(line.split()[0]) == 40:  # commit hash
                parts = line.split()
                current_line = int(parts[2])
            elif line.startswith("author "):
                current_author = line[len("author "):]
            elif line.startswith("\t") and current_line is not None:
                authors[current_line] = current_author or "Unknown"
        return authors
    except (OSError, ValueError):
        return {}


def _proposed_solution(rule_id: str) -> str:
    """Return the static Markdown-formatted proposed fix for the given rule ID."""
    solution_text = _PROPOSED_SOLUTIONS.get(
        rule_id,
        f"Review the flagged code and consult the [{rule_id} CWE entry]"
        f"(https://cwe.mitre.org/data/definitions/{rule_id.replace('CWE-', '')}.html)"
        f" for remediation guidance.",
    )
    doc_url = _OFFICIAL_DOCS.get(
        rule_id,
        f"https://cwe.mitre.org/data/definitions/{rule_id.replace('CWE-', '')}.html",
    )
    return solution_text + f"\n\n📖 [Official documentation]({doc_url})"


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
if not SARIF_PATH.exists():
    # No scan yet — render skeleton placeholders using native Streamlit widgets
    # so each element is individually sized and the page scrolls normally.
    st.subheader("Security Findings")

    # Table header row
    th = st.columns([1, 2, 2, 5, 1, 2])
    for col in th:
        with col:
            _sk_bar(12)
    st.markdown("")

    # Table body rows
    for _ in range(8):
        tr = st.columns([1, 2, 2, 5, 1, 2])
        for i, col in enumerate(tr):
            with col:
                _sk_bar(14)

    st.markdown("---")

    # Expander card skeletons
    for w in ["60%", "75%", "50%", "68%", "55%", "72%", "45%", "63%"]:
        with st.container():
            _sk_bar(18, w)
            st.markdown("")

elif SARIF_PATH.exists():
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
            # ── 1. Filter panel ───────────────────────────────────────────────
            all_severities = sorted({r["severity"].capitalize() for r in rows})
            all_rule_ids   = sorted({r["rule_id"] for r in rows})

            fc1, fc2, fc3 = st.columns(3)
            with fc1:
                sel_severities = st.multiselect(
                    "Severity", options=all_severities, default=all_severities,
                    key="filter_severity",
                )
            with fc2:
                sel_rule_ids = st.multiselect(
                    "Rule ID (CWE)", options=all_rule_ids, default=all_rule_ids,
                    key="filter_rule_id",
                )
            with fc3:
                file_search = st.text_input(
                    "File search", placeholder="substring match…", key="filter_file",
                )

            filtered_rows = [
                r for r in rows
                if r["severity"].capitalize() in sel_severities
                and r["rule_id"] in sel_rule_ids
                and file_search.lower() in r["rel_path"].lower()
            ]

            # Sort: errors/critical first, then warnings, then notes/unknown
            _SEVERITY_RANK = {"error": 0, "critical": 0, "warning": 1, "note": 2}
            filtered_rows = sorted(
                filtered_rows,
                key=lambda r: _SEVERITY_RANK.get(r["severity"].lower(), 3),
            )

            # ── 2. Summary dataframe (quick overview, no horizontal scroll) ──
            PAGE_SIZE = 20
            total_pages = max(1, math.ceil(len(filtered_rows) / PAGE_SIZE))
            st.session_state.setdefault("findings_page", 1)
            st.session_state["findings_page"] = min(
                st.session_state["findings_page"], total_pages
            )
            current_page = st.session_state["findings_page"]
            start = (current_page - 1) * PAGE_SIZE
            page_rows = filtered_rows[start : start + PAGE_SIZE]

            # Batched git-blame: resolve all unique files in filtered_rows (for the
            # full table) plus the current page (for the cards). Already-cached files
            # are skipped so page navigation never re-runs blame.
            st.session_state.setdefault("blame_cache", {})
            for f in {row["abs_path"] for row in filtered_rows}:
                if f not in st.session_state["blame_cache"]:
                    st.session_state["blame_cache"][f] = _blame_authors(f)

            summary_rows = [
                {
                    "Sev":      _severity_emoji(row["severity"]),
                    "Rule ID":  row["rule_id"],
                    "Severity": row["severity"].capitalize(),
                    "File":     row["rel_path"],
                    "Line":     row["line"],
                    "Author":   st.session_state["blame_cache"].get(row["abs_path"], {}).get(row["line"], "Unknown"),
                }
                for row in filtered_rows
            ]
            st.dataframe(
                summary_rows,
                use_container_width=True,
                column_config={
                    "Sev":      st.column_config.TextColumn("",       width=28),
                    "Rule ID":  st.column_config.TextColumn("Rule",   width=120),
                    "Severity": st.column_config.TextColumn("Sev.",   width=90),
                    "File":     st.column_config.TextColumn("File",   width=310),
                    "Line":     st.column_config.NumberColumn("Line", width=60),
                    "Author":   st.column_config.TextColumn("Author", width=110),
                },
                hide_index=True,
            )

            if len(filtered_rows) != len(rows):
                st.markdown(
                    f"**Showing {min(start + PAGE_SIZE, len(filtered_rows))} of "
                    f"{len(filtered_rows)} filtered findings "
                    f"({len(rows)} total) — expand a card for detail and source context.**"
                )
            else:
                st.markdown(
                    f"**Showing {min(start + PAGE_SIZE, len(rows))} of "
                    f"{len(rows)} finding{'s' if len(rows) != 1 else ''} — "
                    f"expand a card for detail and source context.**"
                )
            st.markdown("")

            # ── 3. Per-finding expander cards (paginated) ─────────────────────
            if not filtered_rows:
                st.info("No findings match the current filters.")


            for row in page_rows:
                emoji   = _severity_emoji(row["severity"])
                label   = f"{emoji} {row['rel_path']}  (Line {row['line']})"
                ext     = Path(row["rel_path"]).suffix.lower()
                lang    = _EXT_TO_LANG.get(ext, "text")

                with st.expander(label, expanded=False):
                    # Metadata block
                    c1, c2 = st.columns([1, 3])
                    with c1:
                        author = st.session_state["blame_cache"].get(row["abs_path"], {}).get(row["line"], "Unknown")
                        st.markdown(f"**Rule ID**\n\n`{row['rule_id']}`")
                        st.markdown(f"**Severity**\n\n{emoji} {row['severity'].capitalize()}")
                        st.markdown(f"**Line**\n\n`{row['line']}`")
                        st.markdown(f"**Author**\n\n`{author}`")
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

                    # ── Proposed solution ─────────────────────────────────────
                    st.markdown("**Proposed solution**")
                    st.markdown(_proposed_solution(row["rule_id"]))  # instant dict lookup

                    custom_key = (row["rel_path"], row["line"], row["rule_id"])
                    existing_custom = st.session_state.get("custom_solutions", {}).get(custom_key)

                    if existing_custom:
                        st.markdown("**Customized fix:**")
                        st.markdown(existing_custom)

                    if _BOB_EXE and _BOB_API_KEY and not existing_custom:
                        if st.button(
                            "🔧 Generate customized solution",
                            key=f"gen_custom_{row['rel_path']}_{row['line']}",
                        ):
                            fix_question = (
                                f"Provide a concise, language-matched ({lang}) fix for the "
                                f"{row['rule_id']} vulnerability below. Output a short "
                                f"explanation followed by the corrected code in a Markdown "
                                f"code block. Do not use generic Python examples if the "
                                f"file is {lang}."
                            )
                            fix_context = (
                                f"VULNERABILITY DETAILS:\n"
                                f"- Rule ID: {row['rule_id']}\n"
                                f"- File: {row['rel_path']}\n"
                                f"- Line: {row['line']}\n"
                                f"- Language: {lang}\n"
                                f"- Source snippet:\n```{lang}\n{snippet or '(not available)'}\n```\n"
                            )
                            with st.spinner("Generating customized solution…"):
                                try:
                                    gen_result = subprocess.run(
                                        [
                                            _BOB_EXE, "run",
                                            "--mode", "ask",
                                            "--disable-mcp",
                                            "--disable-subagents",
                                            "--format", "json",
                                            fix_question,   # positional arg = the question
                                        ],
                                        input=fix_context,  # stdin = grounding context only
                                        capture_output=True,
                                        text=True,
                                        encoding="utf-8",
                                        errors="replace",
                                        timeout=60,
                                        env={**os.environ, "BOBSHELL_API_KEY": _BOB_API_KEY},
                                        cwd=str(Path(__file__).resolve().parent),
                                    )
                                    raw_gen = gen_result.stdout.strip()
                                    if raw_gen:
                                        try:
                                            generated = json.loads(raw_gen).get("last_message") or raw_gen
                                        except (json.JSONDecodeError, AttributeError):
                                            generated = raw_gen
                                    else:
                                        generated = gen_result.stderr.strip() or _proposed_solution(row["rule_id"])
                                except Exception:
                                    # Bob unavailable or timed out — fall back to static entry
                                    generated = _proposed_solution(row["rule_id"])

                            st.session_state.setdefault("custom_solutions", {})[custom_key] = generated
                            st.rerun()

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

                        # CLI arg = the user's question; stdin = grounding context only.
                        # Keeping them separate prevents Bob from treating context as the question.
                        def _build_context(question: str) -> str:
                            """Build stdin context: skill instructions + history + finding details."""
                            history = st.session_state[chat_key]
                            prior_turns = history[-6:]  # up to 3 pairs (user+assistant)
                            history_block = ""
                            if prior_turns:
                                turn_lines = []
                                for m in prior_turns:
                                    role_label = "User" if m["role"] == "user" else "Bob"
                                    # Truncate each turn to 300 chars — no re-sent snippets
                                    turn_lines.append(f"{role_label}: {m['content'][:300]}")
                                history_block = (
                                    "PRIOR CONVERSATION (last 3 turns, truncated):\n"
                                    + "\n".join(turn_lines)
                                    + "\n\n"
                                )
                            custom_fix = st.session_state.get("custom_solutions", {}).get(
                                (row["rel_path"], row["line"], row["rule_id"])
                            )
                            custom_block = (
                                f"\nPREVIOUSLY GENERATED FIX:\n{custom_fix}\n"
                                if custom_fix else ""
                            )
                            return (
                                f"{_SKILL_PREAMBLE}\n\n"
                                f"{history_block}"
                                f"VULNERABILITY DETAILS:\n"
                                f"- Rule ID: {row['rule_id']}\n"
                                f"- File: {row['rel_path']}\n"
                                f"- Line: {row['line']}\n"
                                f"- Description: {row['message']}\n"
                                f"- Source snippet:\n```\n{snippet or '(not available)'}\n```\n"
                                f"{custom_block}"
                            )

                        user_input = st.chat_input(
                            "Ask a question about this finding…",
                            key=f"chat_input_{row['rel_path']}_{row['line']}",
                        )

                        if user_input:
                            grounding_context = _build_context(user_input)

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
                                        user_input,         # positional arg = the question
                                    ],
                                    input=grounding_context,  # stdin = context only
                                    capture_output=True,
                                    text=True,
                                    encoding="utf-8",
                                    errors="replace",
                                    timeout=60,
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

                        st.caption(
                            "⚠️ Bob is AI and can make mistakes. "
                            "Always review generated fixes and explanations before applying them."
                        )

            # ── Bottom pagination bar ─────────────────────────────────────────
            if filtered_rows:
                _, bc1, bc2, bc3, _ = st.columns([2, 1, 2, 1, 2])
                with bc1:
                    if st.button("◀ Prev", disabled=(current_page <= 1), key="page_prev"):
                        st.session_state["findings_page"] -= 1
                        st.rerun()
                with bc2:
                    st.markdown(
                        f"<div style='text-align:center;padding-top:6px'>"
                        f"Page {current_page} of {total_pages}</div>",
                        unsafe_allow_html=True,
                    )
                with bc3:
                    if st.button("Next ▶", disabled=(current_page >= total_pages), key="page_next"):
                        st.session_state["findings_page"] += 1
                        st.rerun()

    except (json.JSONDecodeError, KeyError, IndexError) as exc:
        st.error(f"Could not parse SARIF report: {exc}")
else:
    st.info("No scan report found yet. Run a scan above.")
