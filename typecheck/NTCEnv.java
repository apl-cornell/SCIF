package typecheck;

import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class NTCEnv {
    public SymTab globalSymTab;
    public SymTab curSymTab;
    public HashMap<String, SymTab> externalSymTab;
    public ArrayList<Constraint> cons;
    public Hypothesis globalHypothesis;
    public NTCEnv() {
        globalSymTab = new SymTab();
        externalSymTab = new HashMap<>();
        cons = new ArrayList<>();
        //globalSymTab = null; //TODO
        curSymTab = globalSymTab;
        globalHypothesis = new Hypothesis();
    }

    public void setGlobalSymTab(SymTab curSymTab) {
        globalSymTab = curSymTab;
    }
    public void setCurSymTab(SymTab curSymTab) { this.curSymTab = curSymTab; }

    public VarInfo toVarInfo(String varName, ast.Type astType, boolean isConst, CodeLocation location, NTCContext context) {
        TypeInfo typeInfo = toTypeInfo(astType, isConst);
        return new VarInfo(varName, varName, typeInfo, location, context);
    }

    public TypeInfo toTypeInfo(ast.Type astType, boolean isConst) {
        if (astType == null) return null;
        if (Utils.isPrimitiveType(astType.x))
            return new TypeInfo(new BuiltinType(astType.x), null, isConst);
        else {
            //TODO: non-primitive types
            return null;
        }
    }

    public String getSymName(String id) {
        return curSymTab.lookup(id).getSLCName();
    }

   public String getSymName(BuiltInT type) {
        Sym symbol = curSymTab.lookup(Utils.BuiltinType2ID(type));
        return symbol.getSLCName();
    }

    public Sym getCurSym(String name) {
        return curSymTab.lookup(name);
    }

    public void addCons(Constraint genCons) {
        cons.add(genCons);
    }

    public Sym getExtSym(String contractName, String funcName) {
        SymTab extST = externalSymTab.get(contractName);
        if (extST == null) return null;
        return extST.lookup(funcName);
    }

    public void addSym(String name, VarSym varSym) {
        curSymTab.add(name, varSym);
    }

    public HashSet<String> getTypeSet() {
        HashSet<String> rtn = globalSymTab.getTypeSet();
        for (BuiltInT builtInT : BuiltInT.values()) {
            rtn.add(getSymName(builtInT));
        }
        return rtn;
    }

    public ArrayList<Constraint> getTypeRelationCons() {
        return new ArrayList<>();
    }
}
