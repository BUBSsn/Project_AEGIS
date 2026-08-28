import json
from typing import List, Dict, Any


def generate_sarif(findings: List[Dict[str, Any]], output_path: str) -> None:
    sarif_doc = {
        "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
        "version": "2.1.0",
        "runs": [
            {
                "tool": {
                    "driver": {
                        "name": "IBM-Bob-AEGIS-Engine",
                        "semanticVersion": "2.0.0",
                        "rules": [
                            {
                                "id": "CWE-89",
                                "name": "SQLInjection",
                                "shortDescription": {
                                    "text": "Improper Neutralization of Special Elements "
                                            "used in an SQL Command."
                                }
                            },
                            {
                                "id": "CWE-798",
                                "name": "HardcodedCredential",
                                "shortDescription": {
                                    "text": "Use of Hard-coded Credentials."
                                }
                            },
                            {
                                "id": "CWE-95",
                                "name": "DynamicCodeExecution",
                                "shortDescription": {
                                    "text": "Improper Neutralization of Directives in "
                                            "Dynamically Evaluated Code."
                                }
                            }
                        ]
                    }
                },
                "results": [
                    {
                        "ruleId": item["rule_id"],
                        "level": item["severity"],
                        "message": {"text": item["message"]},
                        "locations": [
                            {
                                "physicalLocation": {
                                    "artifactLocation": {
                                        "uri": item["file"].replace("\\", "/")
                                    },
                                    "region": {
                                        "startLine": item["line"]
                                    }
                                }
                            }
                        ]
                    }
                    for item in findings
                ]
            }
        ]
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(sarif_doc, f, indent=2)
