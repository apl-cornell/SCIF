package typecheck;

import ast.Node;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;


// each variable has a defContext
public class VarInfo {
    //public String fullName;
    public String name;
    public boolean isConst;
    public TypeInfo typeInfo;

    public CodeLocation location; // where it is defined
    public ScopeContext defContext;

    public VarInfo() {
        this.name = "ANONYMOUS";
        this.typeInfo = new TypeInfo();
        this.location = new CodeLocation();
        defContext = null;
        this.isConst = false;
    }

    public VarInfo(String localName, TypeInfo type, CodeLocation location, ScopeContext context, boolean isConst) {
        this.name = localName;
        this.typeInfo = type;
        this.location = location;
        this.defContext = context;
        this.isConst = isConst;
    }

    /*public VarInfo(String localName, TypeInfo type, CodeLocation location, boolean isConst) {
        this.name = localName;
        this.typeInfo = type;
        this.location = location;
        this.defContext = null;
        this.isConst = isConst;
    }*/

    public VarInfo(VarInfo varInfo) {
        this.name = varInfo.name;
        this.typeInfo = new TypeInfo(varInfo.typeInfo);
        this.location = varInfo.location;
        this.defContext = varInfo.defContext;
        this.isConst = varInfo.isConst;
    }

    public String getLabel() {
        return typeInfo.getLabel();
    }

    public String toSherrlocFmt() {
        return  defContext.getSHErrLocName() + "." + name;
        // return fullName;// + ".." + "lblVar";
    }
    public String labelToSherrlocFmt() {return  toSherrlocFmt() + ".." + "lbl"; }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return genson.serialize(this);
    }
}
