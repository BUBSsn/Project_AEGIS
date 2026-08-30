import json
from typing import List, Dict, Any


# Complete rule catalogue for every CWE emitted by AEGIS scanners.
# SARIF spec §3.49: rules listed here are used by viewers (VS Code, GitHub
# Security, Reviewdog) to display rule metadata alongside each finding.
_RULE_CATALOGUE: List[Dict[str, Any]] = [
    {
        "id": "CWE-22",
        "name": "PathTraversal",
        "shortDescription": {
            "text": "Improper Limitation of a Pathname to a Restricted Directory."
        },
    },
    {
        "id": "CWE-78",
        "name": "OSCommandInjection",
        "shortDescription": {
            "text": "Improper Neutralization of Special Elements used in an OS Command."
        },
    },
    {
        "id": "CWE-79",
        "name": "CrossSiteScripting",
        "shortDescription": {
            "text": "Improper Neutralization of Input During Web Page Generation (XSS)."
        },
    },
    {
        "id": "CWE-89",
        "name": "SQLInjection",
        "shortDescription": {
            "text": "Improper Neutralization of Special Elements used in an SQL Command."
        },
    },
    {
        "id": "CWE-90",
        "name": "LDAPInjection",
        "shortDescription": {
            "text": "Improper Neutralization of Special Elements used in an LDAP Query."
        },
    },
    {
        "id": "CWE-94",
        "name": "ServerSideTemplateInjection",
        "shortDescription": {
            "text": "Improper Control of Generation of Code (Server-Side Template Injection)."
        },
    },
    {
        "id": "CWE-95",
        "name": "DynamicCodeExecution",
        "shortDescription": {
            "text": "Improper Neutralization of Directives in Dynamically Evaluated Code."
        },
    },
    {
        "id": "CWE-117",
        "name": "LogInjection",
        "shortDescription": {
            "text": "Improper Output Neutralization for Logs."
        },
    },
    {
        "id": "CWE-295",
        "name": "ImproperCertValidation",
        "shortDescription": {
            "text": "Improper Certificate Validation."
        },
    },
    {
        "id": "CWE-312",
        "name": "SensitiveDataExposure",
        "shortDescription": {
            "text": "Cleartext Storage of Sensitive Information."
        },
    },
    {
        "id": "CWE-327",
        "name": "WeakCryptographicAlgorithm",
        "shortDescription": {
            "text": "Use of a Broken or Risky Cryptographic Algorithm."
        },
    },
    {
        "id": "CWE-347",
        "name": "ImproperJWTVerification",
        "shortDescription": {
            "text": "Improper Verification of Cryptographic Signature (JWT)."
        },
    },
    {
        "id": "CWE-352",
        "name": "CrossSiteRequestForgery",
        "shortDescription": {
            "text": "Cross-Site Request Forgery (CSRF)."
        },
    },
    {
        "id": "CWE-359",
        "name": "SensitiveInfoExposure",
        "shortDescription": {
            "text": "Exposure of Private Personal Information to an Unauthorized Actor."
        },
    },
    {
        "id": "CWE-502",
        "name": "UnsafeDeserialization",
        "shortDescription": {
            "text": "Deserialization of Untrusted Data."
        },
    },
    {
        "id": "CWE-601",
        "name": "OpenRedirect",
        "shortDescription": {
            "text": "URL Redirection to Untrusted Site (Open Redirect)."
        },
    },
    {
        "id": "CWE-611",
        "name": "XMLExternalEntity",
        "shortDescription": {
            "text": "Improper Restriction of XML External Entity Reference (XXE)."
        },
    },
    {
        "id": "CWE-798",
        "name": "HardcodedCredential",
        "shortDescription": {
            "text": "Use of Hard-coded Credentials."
        },
    },
]

# Index by id for O(1) deduplication when building the per-run rule list.
_CATALOGUE_BY_ID: Dict[str, Dict[str, Any]] = {r["id"]: r for r in _RULE_CATALOGUE}

# SARIF §3.27.10: level must be one of "none" | "note" | "warning" | "error".
# Map any scanner-emitted severity that deviates from this set.
_LEVEL_MAP: Dict[str, str] = {
    "critical": "error",
    "high":     "error",
    "medium":   "warning",
    "low":      "note",
    "info":     "note",
}


def _sarif_level(severity: str) -> str:
    """Normalise an arbitrary severity string to a valid SARIF level."""
    normalised = severity.lower()
    return _LEVEL_MAP.get(normalised, normalised)


def generate_sarif(findings: List[Dict[str, Any]], output_path: str) -> None:
    """Serialise *findings* to an OASIS SARIF v2.1.0 file at *output_path*.

    The ``rules`` array in ``tool.driver`` is populated with metadata for
    every CWE that appears in *findings*, sourced from ``_RULE_CATALOGUE``.
    Unknown rule IDs are emitted with a minimal stub so the SARIF remains
    valid even if a scanner produces a rule not yet in the catalogue.
    """
    # Collect the ordered, deduplicated set of rules referenced by findings.
    seen_ids: Dict[str, None] = {}  # ordered-set via insertion-ordered dict
    for item in findings:
        rid = item.get("rule_id", "")
        if rid and rid not in seen_ids:
            seen_ids[rid] = None

    rules = []
    for rid in seen_ids:
        if rid in _CATALOGUE_BY_ID:
            rules.append(_CATALOGUE_BY_ID[rid])
        else:
            # Unknown rule — emit a minimal stub so SARIF remains valid.
            rules.append({
                "id": rid,
                "name": rid.replace("-", ""),
                "shortDescription": {"text": f"Security finding: {rid}."},
            })

    sarif_doc = {
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "version": "2.1.0",
        "runs": [
            {
                "tool": {
                    "driver": {
                        "name": "IBM-Bob-AEGIS-Engine",
                        "semanticVersion": "2.0.0",
                        "informationUri": "https://github.com/IBM/Project_AEGIS",
                        "rules": rules,
                    }
                },
                "results": [
                    {
                        "ruleId": item["rule_id"],
                        "level": _sarif_level(item["severity"]),
                        "message": {"text": item["message"]},
                        "locations": [
                            {
                                "physicalLocation": {
                                    "artifactLocation": {
                                        "uri": item["file"].replace("\\", "/"),
                                        "uriBaseId": "%SRCROOT%",
                                    },
                                    "region": {
                                        "startLine": item["line"]
                                    },
                                }
                            }
                        ],
                    }
                    for item in findings
                ],
            }
        ],
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(sarif_doc, f, indent=2)
