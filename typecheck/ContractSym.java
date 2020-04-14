package typecheck;

import ast.*;

import java.util.ArrayList;

public class ContractSym extends Sym {
    // public HashSet<String> iptContracts;
    /*public HashMap<String, Type> typeMap;
    public HashMap<String, VarInfo> varMap;
    public HashMap<String, FuncInfo> funcMap;*/
    public SymTab symTab;
    public ArrayList<TrustConstraint> trustCons;

    public ContractSym(String name,
                       SymTab symTab,
                       // HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap,
                       ArrayList<TrustConstraint> trustCons) {
        this.name = name;
        /*this.iptContracts = iptContracts;
        this.typeMap = typeMap;
        this.varMap = varMap;
        this.funcMap = funcMap;*/
        this.symTab = symTab;
        this.trustCons = trustCons;
    }

    public ContractSym() {
        name = "UNKNOWN";
        /*iptContracts = new HashSet<>();
        typeMap = new HashMap<>();
        varMap = new HashMap<>();
        funcMap = new HashMap<>();*/
        symTab = new SymTab();
        trustCons = new ArrayList<>();
    }

    public TypeSym toType(String typeName) {
        Sym sym = symTab.lookup(typeName);
        if (sym == null) return null;
        if (sym instanceof TypeSym)
            return (TypeSym) sym;
        else if (sym instanceof ContractSym)
            return new ContractTypeSym(typeName, (ContractSym) sym);
        return null;
    }

    public TypeSym toStructType(String typeName, ArrayList<AnnAssign> members) {
        Sym sym = symTab.lookup(typeName);
        if (sym != null)
            if (sym instanceof TypeSym)
                return (TypeSym) sym;
            else
                return null;
        ArrayList<VarSym> memberList = new ArrayList<>();
        for (AnnAssign member : members) {
            VarSym tmp = member.toVarInfo(this);
            memberList.add(tmp);
        }
        return new StructTypeSym(typeName, memberList);
    }

    public TypeSym toTypeSym(ast.Type astType) {
        if (astType == null) {
            return new BuiltinTypeSym("void");
        }

        TypeSym typeSym = null;
        if (!(astType instanceof LabeledType)) {
            String typeName = astType.x;
            TypeSym type = toType(typeName);
            typeSym = type;
        } else {
            LabeledType lt = (LabeledType) astType;
            if (lt instanceof DepMap) {
                DepMap depMap = (DepMap) lt;
                typeSym = new DepMapTypeSym(toTypeSym(depMap.keyType), toTypeSym(depMap.valueType));
            } else if (lt instanceof Map) {
                Map map = (Map) lt;
                typeSym = new MapTypeSym(toTypeSym(map.keyType), toTypeSym(map.valueType));
            } else {
                typeSym = toType(lt.x);
            }
        }
        return typeSym;
    }


    public VarSym toVarSym(String localName, ast.Type astType, boolean isConst, CodeLocation loc, ScopeContext scopeContext) {
        TypeSym typeSym = toTypeSym(astType);
        IfLabel ifl = null;
        if (astType instanceof LabeledType)
            ifl = ((LabeledType) astType).ifl;
        return new VarSym(localName, typeSym, ifl, loc, scopeContext, isConst);
    }

    public void addVar(String varname, VarSym varSym) {
        symTab.add(varname, varSym);
    }
    public void addFunc(String funcname, FuncSym funcSym) {
        symTab.add(funcname, funcSym);
    }
    public void addType(String typename, TypeSym typeSym) {
        symTab.add(typename, typeSym);
    }
    public boolean isLValue() {
        return false;
    }

    public Sym lookupSym(String funcName) {
        return symTab.lookup(funcName);
    }
    public boolean containVar(String varName) {
        Sym sym = symTab.lookup(varName);
        return (sym != null && sym instanceof VarSym);
    }

    public FuncSym getFunc(String id) {
        Sym sym = symTab.lookup(id);
        if (sym instanceof FuncSym)
            return (FuncSym) sym;
        else
            return null;
    }
}
