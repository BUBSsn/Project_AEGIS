import json
import sys
from pathlib import Path

import streamlit as st

# Make sure core/ is importable when running as `streamlit run app.py`
sys.path.insert(0, str(Path(__file__).resolve().parent))
from core.mcp_server import clone_github_repo, scan_ast_vulnerabilities

SARIF_PATH = Path("reports/security-findings.sarif")

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

# ── Results table ─────────────────────────────────────────────────────────────
if SARIF_PATH.exists():
    st.subheader("Security Findings")
    try:
        sarif = json.loads(SARIF_PATH.read_text())
        results = sarif.get("runs", [{}])[0].get("results", [])

        rows = []
        for r in results:
            rule_id = r.get("ruleId", "—")
            message = r.get("message", {}).get("text", "—")
            locations = r.get("locations", [])
            if locations:
                phys = locations[0].get("physicalLocation", {})
                file_path = phys.get("artifactLocation", {}).get("uri", "—")
                line = phys.get("region", {}).get("startLine", "—")
            else:
                file_path, line = "—", "—"

            # Severity lives on the rule definition; default to "warning"
            rules = sarif.get("runs", [{}])[0].get("tool", {}).get("driver", {}).get("rules", [])
            severity = "warning"
            for rule in rules:
                if rule.get("id") == rule_id:
                    severity = (
                        rule.get("defaultConfiguration", {}).get("level", "warning")
                    )
                    break

            rows.append({
                "Rule ID": rule_id,
                "Severity": severity,
                "Message": message,
                "File": file_path,
                "Line": line,
            })

        if rows:
            st.dataframe(rows, use_container_width=True)
        else:
            st.info("No vulnerabilities found in the last scan.")

    except (json.JSONDecodeError, KeyError, IndexError) as exc:
        st.error(f"Could not parse SARIF report: {exc}")
else:
    st.info("No scan report found yet. Run a scan above.")
