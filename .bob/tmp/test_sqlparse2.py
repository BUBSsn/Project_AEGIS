import sqlparse
from sqlparse import tokens as T

content = (
    "-- comment\n"
    "EXECUTE IMMEDIATE 'SELECT * FROM t WHERE x = ' || v_x;\n"
    "\n"
    "EXECUTE IMMEDIATE v_sql;\n"
)

# sqlparse doesn't track line numbers in tokens directly.
# We reconstruct by counting newlines in the source up to each statement's offset.
source_lines = content.split('\n')

# Approach: split by statement, track cumulative line offset
parsed = sqlparse.parse(content)
line_offset = 1
for stmt in parsed:
    stmt_str = str(stmt)
    # Count leading newlines + the offset accumulated before this stmt
    stmt_lines = stmt_str.split('\n')
    # Find the first non-whitespace/comment line in the stmt
    stmt_start_line = line_offset
    for i, l in enumerate(stmt_lines):
        if l.strip() and not l.strip().startswith('--'):
            stmt_start_line = line_offset + i
            break
    print(f"stmt_start_line={stmt_start_line}: {stmt_str[:40]!r}")
    line_offset += stmt_str.count('\n')
