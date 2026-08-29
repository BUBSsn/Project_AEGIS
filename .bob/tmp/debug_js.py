import sys
sys.path.insert(0, '.')
from core.js_scanner import scan_js_file, _REDIRECT_SINK_RE, _USER_SOURCE_RE
import tempfile, os, re

# Test open redirect
with tempfile.NamedTemporaryFile(suffix='.js', mode='w', delete=False, encoding='utf-8') as f:
    f.write("window.location.href = '/dashboard';\n")
    name = f.name
results = scan_js_file(name)
print("Redirect findings:", results)
os.unlink(name)

# Check regexes
line = "window.location.href = '/dashboard';"
print("redirect sink:", bool(_REDIRECT_SINK_RE.search(line)))
print("user source:", bool(_USER_SOURCE_RE.search(line)))
# The _USER_SOURCE_RE includes location.href -- check:
m = _USER_SOURCE_RE.search(line)
print("user source match:", m)
