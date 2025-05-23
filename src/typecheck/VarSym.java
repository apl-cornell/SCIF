package typecheck;

import ast.LabeledType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;


// each variable has a defContext
public class VarSym extends Sym {
    //public String fullName;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isBuiltIn = false;
    public TypeSym typeSym;
    public Label ifl;

    public CodeLocation location; // where it is declared

    /*
    public VarSym() {
        super(Utils.ANONYMOUS_VARIABLE_NAME);
        this.typeSym = null;
        this.location = new CodeLocation();
        defContext = null;
        this.isStatic = false;
        this.isFinal = false;
        this.ifl = null;
    }*/

//    public VarSym(String localName, TypeSym type, Label ifl, CodeLocation location, ScopeContext context, boolean isConst, boolean isFinal) {
//        super(localName, context);
//        // this.name = localName;
//        this.typeSym = type;
//        this.ifl = ifl;
//        this.location = location;
//        this.isStatic = isConst;
//        this.isFinal = isFinal;
//    }
    public VarSym(String localName, TypeSym type, Label ifl,
            CodeLocation location, ScopeContext context,
            boolean isConst, boolean isFinal, boolean isBuiltIn) {
        super(localName, context);
        // this.name = localName;
        this.typeSym = type;
        this.ifl = ifl;
        this.location = location;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.isBuiltIn = isBuiltIn;
    }
    public VarSym(String localName, TypeSym type, Label ifl,
            CodeLocation location, ScopeContext context,
            boolean isConst, boolean isFinal, boolean isBuiltIn, boolean isGlobal) {
        super(localName, context, isGlobal);
        // this.name = localName;
        this.typeSym = type;
        this.ifl = ifl;
        this.location = location;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.isBuiltIn = isBuiltIn;
    }

    /*public VarInfo(String localName, TypeInfo type, CodeLocation location, boolean isConst) {
        this.name = localName;
        this.typeInfo = type;
        this.location = location;
        this.defContext = null;
        this.isConst = isConst;
    }*/

    public VarSym(VarSym varSym) {
        super(varSym.getName(), varSym.defContext(), varSym.isGlobal());
        // this.name = varSym.name;
        this.typeSym = varSym.typeSym;
        this.ifl = varSym.ifl;
        this.location = varSym.location;
        this.isStatic = varSym.isStatic;
        this.isFinal = varSym.isFinal;
        this.isBuiltIn = varSym.isBuiltIn;
    }

    public VarSym(VarSym varSym, ScopeContext context) {
        super(varSym.getName(), context, varSym.isGlobal());
        // this.name = varSym.name;
        this.typeSym = varSym.typeSym;
        this.ifl = varSym.ifl;
        this.location = varSym.location;
        this.isStatic = varSym.isStatic;
        this.isFinal = varSym.isFinal;
        this.isBuiltIn = varSym.isBuiltIn;
    }

    /**
     * Return the value of this symbol's label in SHErrLoc format
     * @return
     */
    // public String getLabelValueSLC() {
        //return ifl.toSHErrLocFmt();
    //}
//    public String getLabelValueSLC(String namespace) {
//        if (ifl != null) {
//            return ifl.toSHErrLocFmt(namespace);
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

    public String labelNameSLC() {return  toSHErrLocFmt() + ".." + "lbl"; }
    public String labelValueSLC() {return ifl.toSHErrLocFmt(); }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return getName() + "|" + isStatic + "|" + isFinal + "|" +
                typeSym + "|" +
                // (ifl != null ? ifl.toSHErrLocFmt(defContext) + "|" : "") +
                (location != null ? location.toString() : "");
    }

    public CodeLocation labelLocation() {
        return ifl.location();
    }

    public void setLabel(Label label) {
        ifl = label;
    }

    public boolean isPrincipalVar() {
        return isFinal && (typeSym instanceof ContractSym || typeSym instanceof InterfaceSym || typeSym.getName().equals(Utils.ADDRESS_TYPE));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof VarSym vo) {
            return vo.toSHErrLocFmt().equals(toSHErrLocFmt());
        } else {
            return false;
        }
    }
}
