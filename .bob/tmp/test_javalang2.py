import javalang

src = (
    "public class Demo {\n"
    "    private static final String PASSWORD = \"s3cr3t_k3y!\";\n"
    "    void bad() throws Exception {\n"
    "        ObjectInputStream ois = new ObjectInputStream(null);\n"
    "    }\n"
    "}\n"
)

tree = javalang.parse.parse(src)
for path, node in tree:
    pos = node.position
    t = type(node).__name__
    if pos and t in ("VariableDeclarator", "ClassCreator", "FieldDeclaration"):
        print(f"  {t:25s} line={pos.line} col={pos.column}")
