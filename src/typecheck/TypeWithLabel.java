package typecheck;

import ast.IfLabel;

public class TypeWithLabel {
    public TypeSym type;
    IfLabel label;

    public TypeWithLabel(TypeSym type, IfLabel label) {
        this.type = type;
        this.label = label;
    }
}
