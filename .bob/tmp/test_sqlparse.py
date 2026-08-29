import sqlparse
from sqlparse import tokens as T

# Test the three key patterns we need to distinguish:
# 1. EXECUTE IMMEDIATE with || concat (vulnerable)
# 2. EXECUTE IMMEDIATE with :1 bind variable (safe)  
# 3. EXECUTE IMMEDIATE bare variable (potentially unsafe, flag it)
# 4. Multi-statement file

content = """\
-- Test 1: vulnerable concat
EXECUTE IMMEDIATE 'SELECT * FROM users WHERE id = ' || v_id;

-- Test 2: safe bind variable
EXECUTE IMMEDIATE 'SELECT * FROM users WHERE id = :1' USING v_id;

-- Test 3: bare variable (no bind)
EXECUTE IMMEDIATE v_dynamic_sql;

-- Test 4: safe static string only
EXECUTE IMMEDIATE 'DROP TABLE temp_tbl';
"""

parsed = sqlparse.parse(content)
for stmt in parsed:
    flat = list(stmt.flatten())
    # Find EXECUTE IMMEDIATE
    for i, tok in enumerate(flat):
        if tok.ttype is T.Keyword and tok.normalized == "EXECUTE":
            # peek ahead for IMMEDIATE
            j = i + 1
            while j < len(flat) and flat[j].ttype in (T.Text.Whitespace, T.Newline):
                j += 1
            if j < len(flat) and flat[j].ttype is T.Keyword and flat[j].normalized == "IMMEDIATE":
                # collect remaining tokens of this statement
                rest = flat[j+1:]
                has_concat = any(t.ttype is T.Operator and t.value == "||" for t in rest)
                has_bind = any(t.ttype is T.Name.Placeholder for t in rest)
                has_bare_var = any(t.ttype is T.Name for t in rest)
                # line number: find first token with meaningful ttype for this stmt
                line = stmt.tokens[0].value.count('\n') if stmt.tokens else 0
                print(f"EXEC IMM: concat={has_concat}, bind={has_bind}, bare_var={has_bare_var}")
