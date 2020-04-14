package typecheck;

import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;


// each variable has a defContext
public class VarSym extends Sym {
    //public String fullName;
    public boolean isConst;
    public TypeSym typeSym;
    public IfLabel ifl;

    public CodeLocation location; // where it is defined
    public ScopeContext defContext;

    public VarSym() {
        this.name = "ANONYMOUS";
        this.typeSym = null;
        this.location = new CodeLocation();
        defContext = null;
        this.isConst = false;
        this.ifl = null;
    }

    public VarSym(String localName, TypeSym type, IfLabel ifl, CodeLocation location, ScopeContext context, boolean isConst) {
        this.name = localName;
        this.typeSym = type;
        this.ifl = ifl;
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

    public VarSym(VarSym varSym) {
        this.name = varSym.name;
        this.typeSym = varSym.typeSym;
        this.ifl = varSym.ifl;
        this.location = varSym.location;
        this.defContext = varSym.defContext;
        this.isConst = varSym.isConst;
    }

    public String getLabel() {
        if (ifl != null) {
            return ifl.toSherrlocFmt();
        } else {
            return null;
        }
    }
    public boolean isLValue() {
        return true;
    }

    public void replace(String k, String v) {
        ifl.replace(k, v);
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
