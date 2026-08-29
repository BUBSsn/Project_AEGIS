import json
from collections import Counter

with open('reports/security-findings.sarif') as f:
    sarif = json.load(f)

results = sarif['runs'][0]['results']
by_rule = Counter(r['ruleId'] for r in results)

print(f'Total findings: {len(results)}')
print()
for rule, count in sorted(by_rule.items()):
    print(f'  {rule}: {count}')

print()
print('Sample findings (one per CWE):')
shown = set()
for r in results:
    rid = r['ruleId']
    if rid not in shown:
        shown.add(rid)
        loc = r['locations'][0]['physicalLocation']
        uri = loc['artifactLocation']['uri']
        line = loc['region']['startLine']
        msg = r['message']['text']
        print(f'  [{rid}] {uri}:{line}')
        print(f'         {msg}')
