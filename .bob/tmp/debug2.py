import sys
sys.path.insert(0, '.')

# Debug Java SQLi
from core.java_scanner import _SQL_SINK_RE, _ASSIGN_TAINT_RE, _USER_SOURCE_RE
import re

lines = [
    '        public class Search {',
    '            void search(HttpServletRequest request) {',
    '                String q = request.getParameter("q");',
    '                stmt.executeQuery("SELECT * FROM products WHERE name = \'" + q + "\'");',
    '            }',
    '        }',
]

tainted = set()
for i, line in enumerate(lines, 1):
    for m in _ASSIGN_TAINT_RE.finditer(line):
        tainted.add(m.group(1))
        print(f"  Taint added from line {i}: {m.group(1)!r}")
    if _USER_SOURCE_RE.search(line):
        for m in re.finditer(r'\b([A-Za-z_][A-Za-z0-9_]*)\s*=(?!=)', line):
            tainted.add(m.group(1))
            print(f"  Taint added (user src) from line {i}: {m.group(1)!r}")
    if _SQL_SINK_RE.search(line):
        has_concat = bool(re.search(r'["\'][\s\+]+[A-Za-z_]|[A-Za-z_][A-Za-z0-9_]*\s*\+\s*["\']|\+\s*[A-Za-z_]', line))
        has_tainted = any(tv in line for tv in tainted)
        print(f"  Line {i} SQL sink: concat={has_concat}, tainted={has_tainted}, vars={tainted}")

print()
# Debug JS open redirect
from core.js_scanner import _REDIRECT_SINK_RE, _USER_SOURCE_RE as JS_SRC, _REASSIGN_TAINT_RE
line = "        window.location.href = '/dashboard';"
print("Redirect sink:", bool(_REDIRECT_SINK_RE.search(line)))
print("User source:", bool(JS_SRC.search(line)))
for m in _REASSIGN_TAINT_RE.finditer(line):
    print("Reassign taint match:", m.group(0), "->", m.group(1))
