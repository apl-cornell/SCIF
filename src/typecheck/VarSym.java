package typecheck;

import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;


// each variable has a defContext
public class VarSym extends Sym {
    //public String fullName;
    public boolean isStatic;
    public boolean isFinal;
    public TypeSym typeSym;
    public IfLabel ifl;

    public CodeLocation location; // where it is defined
    public ScopeContext defContext;

    public VarSym() {
        super(Utils.ANONYMOUS_VARIABLE_NAME);
        this.typeSym = null;
        this.location = new CodeLocation();
        defContext = null;
        this.isStatic = false;
        this.isFinal = false;
        this.ifl = null;
    }

    public VarSym(String localName, TypeSym type, IfLabel ifl, CodeLocation location, ScopeContext context, boolean isConst, boolean isFinal) {
        super(localName);
        // this.name = localName;
        this.typeSym = type;
        this.ifl = ifl;
        this.location = location;
        this.defContext = context;
        this.isStatic = isConst;
        this.isFinal = isFinal;
    }

    /*public VarInfo(String localName, TypeInfo type, CodeLocation location, boolean isConst) {
        this.name = localName;
        this.typeInfo = type;
        this.location = location;
        this.defContext = null;
        this.isConst = isConst;
    }*/

    public VarSym(VarSym varSym) {
        super(varSym.getName());
        // this.name = varSym.name;
        this.typeSym = varSym.typeSym;
        this.ifl = varSym.ifl;
        this.location = varSym.location;
        this.defContext = varSym.defContext;
        this.isStatic = varSym.isStatic;
        this.isFinal = varSym.isFinal;
    }

    public String getLabel() {
        if (ifl != null) {
            return ifl.toSherrlocFmt(defContext);
        } else {
            return null;
        }
    }
//    public String getLabel(String namespace) {
//        if (ifl != null) {
//            return ifl.toSherrlocFmt(namespace);
//        } else {
//            return null;
//        }
//    }
    public boolean isLValue() {
        return true;
    }

//    public void replace(String k, String v) {
//        ifl.replace(k, v);
//    }

    @Override
    public String toSherrlocFmt() {
        System.out.println(getName());
        return  defContext.getSHErrLocName() + "." + getName();
        // return fullName;// + ".." + "lblVar";
    }
    public String labelToSherrlocFmt() {return  toSherrlocFmt() + ".." + "lbl"; }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return getName() + "|" + isStatic + "|" + isFinal + "|" +
                typeSym + "|" +
                // (ifl != null ? ifl.toSherrlocFmt(defContext) + "|" : "") +
                (location != null ? location.toString() : "");
    }

}
