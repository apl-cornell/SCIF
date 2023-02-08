package typecheck;

import ast.*;

import java.util.ArrayList;
import java.util.List;

public class ContractSym extends TypeSym {

    // public HashSet<String> iptContracts;
    /*public HashMap<String, Type> typeMap;
    public HashMap<String, VarInfo> varMap;
    public HashMap<String, FuncInfo> funcMap;*/
    public SymTab symTab;
    // private Label label;
    // public ArrayList<TrustConstraint> trustCons;
    private List<Assumption> assumptions;
    private final Contract astNode;
    //private VarSym thisSym;

    public ContractSym(String name,
            SymTab symTab,
            // HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap,
            List<Assumption> assumptions,
            // Label label,
            Contract contract) {
        super(name, contract.getScopeContext());
        this.symTab = symTab;
        this.assumptions = assumptions;
        // this.label = label;
        astNode = contract;
    }

    public ContractSym(String contractName, Contract contract) {
        super(contractName, contract.getScopeContext());
        astNode = contract;
        symTab = new SymTab();
        assumptions = new ArrayList<>();
    }

    public TypeSym toType(String typeName) {
        Sym sym = symTab.lookup(typeName);
        if (sym == null) {
            return null;
        }
        if (sym instanceof TypeSym) {
            return (TypeSym) sym;
        }
        return null;
    }

    public TypeSym toStructType(String typeName, ArrayList<AnnAssign> members) {
        Sym sym = symTab.lookup(typeName);
        if (sym != null) {
            if (sym instanceof TypeSym) {
                return (TypeSym) sym;
            } else {
                return null;
            }
        }
        ArrayList<VarSym> memberList = new ArrayList<>();
        for (AnnAssign member : members) {
            VarSym tmp = member.toVarInfo(this);
            memberList.add(tmp);
        }
        return new StructTypeSym(typeName, memberList, astNode.getScopeContext());
    }

    public TypeSym toTypeSym(ast.Type astType, ScopeContext defContext) {
        if (astType == null) {
            return new BuiltinTypeSym("void");
        }
        // System.err.println("[in]toTypeSym: " + astType.x);

        Sym s = symTab.lookup(astType.name());
        TypeSym typeSym = null;
        if (s instanceof TypeSym) {
            typeSym = (TypeSym) s;
        } else {
            if (astType instanceof DepMap) {
                DepMap depMap = (DepMap) astType;
                typeSym = new DepMapTypeSym(toTypeSym(depMap.keyType, defContext), depMap.keyName(), toTypeSym(depMap.valueType, defContext), defContext, new ScopeContext(depMap, defContext));
            } else if (astType instanceof Map) {
                Map map = (Map) astType;
                typeSym = new MapTypeSym(toTypeSym(map.keyType, defContext), toTypeSym(map.valueType, defContext), defContext);
            }

        }
        // System.err.println("[out]toTypeSym: " + typeSym.name);
        return typeSym;
    }


    public VarSym toVarSym(String localName, ast.LabeledType astType, boolean isStatic, boolean isFinal, boolean isBuiltIn,
            CodeLocation loc, ScopeContext defContext) {
        System.err.println("toVarSym: " + localName + " " + astType.type().name());
        TypeSym typeSym = toTypeSym(astType.type(), defContext);
        System.err.println("toVarSym: " + localName + " " + typeSym);
        Label ifl;
        ifl = toLabel(astType.label());
        return new VarSym(localName, typeSym, ifl, loc, defContext, isStatic, isFinal, isBuiltIn);
    }

    public Label toLabel(IfLabel ifl) {
        if (ifl instanceof PrimitiveIfLabel) {
            VarSym label = (VarSym) lookupSym(((PrimitiveIfLabel) ifl).value().id);
            if (label == null) return null;
            return new PrimitiveLabel(label, ifl.getLocation());
        } else if (ifl instanceof ComplexIfLabel) {
            return new ComplexLabel(toLabel(((ComplexIfLabel) ifl).getLeft()),
                    ((ComplexIfLabel) ifl).getOp(),
                    toLabel(((ComplexIfLabel) ifl).getRight()),
                    ifl.getLocation());
        } else {
            throw new RuntimeException();
        }
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

    public void addContract(String typename, ContractSym typeSym) {
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
        if (sym instanceof FuncSym) {
            return (FuncSym) sym;
        } else {
            return null;
        }
    }


    public String getLabelNameContract() {
        return Utils.getLabelNameContract(getContractNode().getScopeContext());
    }

    public String getLabelContract() {
        return Utils.getLabelNameContract(getContractNode().getScopeContext());
        // return ifl.toSHErrLocFmt(name());
    }

    public Contract getContractNode() {
        return astNode;
    }

    public ExceptionTypeSym toExceptionType(String exceptionName, Arguments arguments, ScopeContext defContext) {

        ExceptionTypeSym sym = getExceptionSym(exceptionName);
        if (sym != null) {
            return sym;
        }
        ArrayList<VarSym> memberList = arguments.parseArgs(this);
        return new ExceptionTypeSym(exceptionName, memberList, defContext);
    }

    public ExceptionTypeSym getExceptionSym(String exceptionName) {
        Sym sym = symTab.lookup(exceptionName);
        if (sym == null || (!(sym instanceof ExceptionTypeSym))) {
            return null;
        }
        return (ExceptionTypeSym) sym;
    }

    /**
     * Get all principals and addresses
     * @return
     */
//    public Set<Sym> getPrincipalSet() {
//        Set<Sym> rtn = new HashSet<>();
//        for (Entry<String, VarSym> entry : symTab.getVars().entrySet()) {
//            //if (sym instanceof VarSym) {
//            VarSym sym = entry.getValue();
//            if (sym.typeSym.name().equals(Utils.PRINCIPAL_TYPE)
//                    || (sym.isFinal
//                    && (sym.typeSym instanceof ContractSym
//                        || sym.typeSym.name().equals(Utils.ADDRESS_TYPE)))) {
//                rtn.add(sym);
//            }
//            //}
//        }
//        return rtn;
//    }

    public Iterable<Assumption> assumptions() {
        return assumptions;
    }

    public void updateAssumptions(List<Assumption> assumptions) {
        this.assumptions = assumptions;
    }

    public VarSym thisSym() {
        VarSym thisSym = (VarSym) lookupSym(Utils.LABEL_THIS);
        assert thisSym != null;
        return thisSym;
    }
}
