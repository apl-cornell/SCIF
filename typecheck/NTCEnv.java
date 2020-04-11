package typecheck;

import ast.DepMap;
import ast.LabeledType;
import ast.Map;
import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class NTCEnv {
    public SymTab globalSymTab;
    public SymTab curSymTab;
    // public HashMap<String, SymTab> externalSymTab;
    // external contracts will be added to the global SymTab
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

    public VarInfo toVarInfo(String varName, ast.Type astType, boolean isConst, CodeLocation location, ScopeContext context) {
        TypeInfo typeInfo = toTypeInfo(astType, isConst);
        return new VarInfo(varName, varName, typeInfo, location, context);
    }

    public TypeInfo toTypeInfo(ast.Type astType, boolean isConst) {
        TypeInfo typeInfo = null;

        if (astType == null) return new TypeInfo(new BuiltinType(Utils.BuiltinType2ID(BuiltInT.VOID)), null, isConst);
        if (Utils.isPrimitiveType(astType.x))
            typeInfo = new TypeInfo(new BuiltinType(astType.x), null, isConst);
        else {
            LabeledType lt = (LabeledType) astType;
            if (lt instanceof DepMap) {
                DepMap depMap = (DepMap) lt;
                typeInfo = new DepMapTypeInfo(new BuiltinType("DepMap"), depMap.ifl, isConst, toTypeInfo(depMap.keyType, isConst), toTypeInfo(depMap.valueType, isConst));
            } else if (lt instanceof Map) {
                Map map = (Map) lt;
                typeInfo = new MapTypeInfo(new BuiltinType("Map"), map.ifl, isConst, toTypeInfo(map.keyType, isConst), toTypeInfo(map.valueType, isConst));
            } else {
                typeInfo = new TypeInfo(new BuiltinType(lt.x), lt.ifl, isConst);
            }
        }
        return typeInfo;
    }

    /*private Type toType(LabeledType lt) {
        Type rtn = null;
        if (lt instanceof DepMap) {
            DepMapTypeInfo
        } else if (lt instanceof Map) {

        } else {
            TypeSym tp = (TypeSym) getCurSym(lt.x);
            rtn = tp.type;
        }
        return rtn;
    }*/

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

    public boolean contractExists(String contractName) {
        return externalSymTab.containsKey(contractName);
    }

    public Sym getExtSym(String contractName, String funcName) {
        SymTab extST = externalSymTab.get(contractName);
        if (extST == null) return null;
        return extST.lookup(funcName);
    }

    public void addSym(String name, Sym sym) {
        curSymTab.add(name, sym);
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
