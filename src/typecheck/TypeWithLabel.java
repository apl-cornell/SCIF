package typecheck;

import ast.IfLabel;

public class TypeWithLabel {

    // TODO: this seems to be unused. AST node LabeledType and VarSym are the alternatives.

    public TypeSym type;
    IfLabel label;

    public TypeWithLabel(TypeSym type, IfLabel label) {
        this.type = type;
        this.label = label;
    }
}

