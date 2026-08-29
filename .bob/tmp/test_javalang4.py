import javalang

src = (
    "public class Demo {\n"
    "    private static final String SECRET_KEY = \"s3cr3t_k3y_val!\";\n"
    "    void bad() throws Exception {\n"
    "        String token = \"hardcoded_token_value\";\n"
    "        ObjectInputStream ois = new ObjectInputStream(null);\n"
    "    }\n"
    "}\n"
)

tree = javalang.parse.parse(src)
# Walk with path to inspect parent context
for path, node in tree:
    t = type(node).__name__
    if t == "FieldDeclaration":
        # field has a declarator with a Literal initializer
        line = node.position.line if node.position else 0
        type_name = node.type.name if node.type else ""
        for decl in node.declarators:
            if (isinstance(decl.initializer, javalang.tree.Literal) and
                    type_name == "String"):
                val = decl.initializer.value.strip('"')
                print(f"Field {decl.name!r} = {val!r} at line {line}, type={type_name}")
    elif t == "LocalVariableDeclaration":
        line = node.position.line if node.position else 0
        type_name = node.type.name if node.type else ""
        for decl in node.declarators:
            if (isinstance(decl.initializer, javalang.tree.Literal) and
                    type_name == "String"):
                val = decl.initializer.value.strip('"')
                print(f"LocalVar {decl.name!r} = {val!r} at line {line}, type={type_name}")
    elif t == "ClassCreator":
        if hasattr(node, "type") and node.type and node.type.name == "ObjectInputStream":
            # find closest ancestor with a position
            parent_line = None
            for ancestor in reversed(list(path)):
                if hasattr(ancestor, "position") and ancestor.position:
                    parent_line = ancestor.position.line
                    break
            print(f"ClassCreator ObjectInputStream, parent line={parent_line}")
