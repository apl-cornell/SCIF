package typecheck;

import ast.IfLabel;

public class TypeInfo {
    public Type type;
    public IfLabel ifl;
    public boolean isConst;

    public TypeInfo() {
        this.type = new BuiltinType("UNKNOWN");
        this.ifl = null;
        this.isConst = false;
    }

    public TypeInfo(Type type, IfLabel ifl, boolean isConst) {
        this.type = type;
        this.ifl = ifl;
        this.isConst = isConst;
    }

    public TypeInfo(TypeInfo typeInfo) {
        type = typeInfo.type;
        ifl = typeInfo.ifl;
        isConst = typeInfo.isConst;
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
