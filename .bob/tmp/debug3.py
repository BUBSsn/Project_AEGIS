import sys
sys.path.insert(0, '.')
from core.java_scanner import _SQL_SINK_RE
import re

line = '                stmt.executeQuery("SELECT * FROM products WHERE name = \'" + q + "\'");'
print("SQL sink match:", bool(_SQL_SINK_RE.search(line)))
tainted = {'q'}
has_concat = bool(re.search(r'["\'][\s\+]+[A-Za-z_]|[A-Za-z_][A-Za-z0-9_]*\s*\+\s*["\']|\+\s*[A-Za-z_]', line))
has_tainted = any(tv in line for tv in tainted)
print("has_concat:", has_concat)
print("has_tainted:", has_tainted)
print("tainted vars:", tainted)
# The issue: 'q' in line is True because '"q"' contains the char q
print("'q' in line:", 'q' in line)

# For JS open redirect:
from core.js_scanner import _USER_SOURCE_RE
line2 = "        window.location.href = '/dashboard';"
m = _USER_SOURCE_RE.search(line2)
print("\nJS user source match:", m)
print("Match text:", m.group(0) if m else None)
# 'location.href' is matching as a source -- it's on the assignment LHS
