package typecheck;

import ast.IfLabel;

public class TypeInfo {
    public Type type;
    public IfLabel ifl;

    public TypeInfo() {
        this.type = new BuiltinType("UNKNOWN");
        this.ifl = null;
    }

    public TypeInfo(Type type, IfLabel ifl) {
        this.type = type;
        this.ifl = ifl;
    }

    public TypeInfo(TypeInfo typeInfo) {
        type = typeInfo.type;
        ifl = typeInfo.ifl;
    }

    public String getLabel() {
        if (ifl != null) {
            return ifl.toSherrlocFmt();
        } else {
            return null;
        }
    }

    public void replace(String k, String v) {
        ifl.replace(k, v);
    }
}
