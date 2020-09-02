package typecheck;

import ast.DepMap;
import ast.LabeledType;
import ast.Map;
import ast.Type;
import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
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
        // externalSymTab = new HashMap<>();
        cons = new ArrayList<>();
        //globalSymTab = null; //TODO
        curSymTab = globalSymTab;
        globalHypothesis = new Hypothesis();
    }

    public void setGlobalSymTab(SymTab curSymTab) {
        globalSymTab = curSymTab;
    }
    public void setCurSymTab(SymTab curSymTab) { this.curSymTab = curSymTab; }

    public VarSym toVarSym(String varName, ast.Type astType, boolean isConst, CodeLocation location, ScopeContext context) {
        TypeSym typeSym = toTypeSym(astType);
        return new VarSym(varName, typeSym, null, location, context, isConst);
    }

    public TypeSym toTypeSym(ast.Type astType) {
        TypeSym typeSym = null;
        if (astType == null) return new BuiltinTypeSym(Utils.BuiltinType2ID(BuiltInT.VOID));
        System.err.println("[in]toTypeSym: " + astType.x);

        Sym s = getCurSym(astType.x);
        if (s instanceof TypeSym)//Utils.isPrimitiveType(astType.x))
            typeSym = (TypeSym) s;// new BuiltinTypeSym(astType.x);
        else {
            LabeledType lt = (LabeledType) astType;
            if (lt instanceof DepMap) {
                DepMap depMap = (DepMap) lt;
                typeSym = new DepMapTypeSym(toTypeSym(depMap.keyType), toTypeSym(depMap.valueType));
            } else if (lt instanceof Map) {
                Map map = (Map) lt;
                typeSym = new MapTypeSym(toTypeSym(map.keyType), toTypeSym(map.valueType));
            } else {
                // return null;
                // typeSym = new BuiltinTypeSym(lt.x);
            }
        }
        System.err.println("[out]toTypeSym: " + typeSym.name);
        return typeSym;
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
        Sym rtn = globalSymTab.lookup(contractName);
        if (rtn == null) return false;
        return rtn instanceof ContractSym;
    }

    public Sym getExtSym(String contractName, String funcName) {
        Sym extST = globalSymTab.lookup(contractName);
        if (extST == null) return null;
        return ((ContractSym) extST).lookupSym(funcName);
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

    public ContractSym getContract(String name) {
        Sym sym = globalSymTab.lookup(name);
        if (sym != null && sym instanceof ContractSym)
            return (ContractSym) sym;
        return null;
    }

    public boolean containsContract(String iptContract) {
        return getContract(iptContract) != null;
    }

    public void addGlobalSym(String contractName, ContractSym contractSym) {
        globalSymTab.add(contractName, contractSym);
    }
}
