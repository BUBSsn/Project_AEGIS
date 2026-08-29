import javalang

src = (
    "public class Demo {\n"
    "    private static final String PASSWORD = \"s3cr3t_k3y!\";\n"
    "    private String normalVar = \"hello\";\n"
    "    void bad() throws Exception {\n"
    "        ObjectInputStream ois = new ObjectInputStream(null);\n"
    "        Object o = ois.readObject();\n"
    "    }\n"
    "}\n"
)

tree = javalang.parse.parse(src)
for path, node in tree:
    t = type(node).__name__
    if t == "VariableDeclarator":
        print("VariableDeclarator:", node.name, "->", node.initializer)
    elif t == "ClassCreator":
        print("ClassCreator:", node.type.name if node.type else "?")
    elif t == "MethodInvocation":
        print("MethodInvocation:", node.member)
    elif t == "Literal":
        print("Literal:", node.value)
