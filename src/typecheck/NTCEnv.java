package typecheck;

import ast.*;
import java.util.List;
import java.util.Set;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class NTCEnv {

    private SymTab globalSymTab;
    private SymTab curSymTab;
    // public HashMap<String, SymTab> externalSymTab;
    // external contracts will be added to the global SymTab
    private ArrayList<Constraint> cons;
    private Hypothesis globalHypothesis;
    private ContractSym curContractSym;
    private HashMap<String, SourceFile> programMap;

    public NTCEnv(ContractSym contractSym) {
        globalSymTab = new SymTab();
        cons = new ArrayList<>();
        curSymTab = globalSymTab;
        globalHypothesis = new Hypothesis();
        curContractSym = contractSym;
        programMap = new HashMap<>();
    }

    public void setGlobalSymTab(SymTab curSymTab) {
        globalSymTab = curSymTab;
    }

    public void setCurSymTab(SymTab curSymTab) {
        this.curSymTab = curSymTab;
    }

    public void setCurContractSym(ContractSym curContractSym) {
        this.curContractSym = curContractSym;
    }

    public VarSym toVarSym(String varName, ast.Type astType, boolean isConst, boolean isFinal,
            CodeLocation location, ScopeContext context) {
        TypeSym typeSym = toTypeSym(astType, context);
        if (typeSym == null) {
            throw new RuntimeException("Type not found: " + astType.getName());
        }
        return new VarSym(varName, typeSym, null, location, context, isConst, isFinal);
    }

    public TypeSym toTypeSym(ast.Type astType, ScopeContext defContext) {
        TypeSym typeSym = null;
        if (astType == null) {
            return new BuiltinTypeSym(Utils.BuiltinType2ID(BuiltInT.VOID));
        }
        // System.err.println("[in]toTypeSym: " + astType.x);

        if (astType instanceof ExceptionType exceptionType) {
            return toExceptionTypeSym(exceptionType);
        }

        Sym s = getCurSym(astType.getName());
        if (s instanceof TypeSym)//Utils.isPrimitiveType(astType.x))
        {
            typeSym = (TypeSym) s;// new BuiltinTypeSym(astType.x);
        } else {
            LabeledType lt = (LabeledType) astType;
            if (lt instanceof DepMap) {
                DepMap depMap = (DepMap) lt;
                typeSym = new DepMapTypeSym(toTypeSym(depMap.keyType, defContext), toTypeSym(depMap.valueType, defContext), defContext);
            } else if (lt instanceof Map) {
                Map map = (Map) lt;
                typeSym = new MapTypeSym(toTypeSym(map.keyType, defContext), toTypeSym(map.valueType, defContext), defContext);
            } else {
                // return null;
                // typeSym = new BuiltinTypeSym(lt.x);
            }
        }
        // System.err.println("[out]toTypeSym: " + typeSym.name);
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

//    public String getSymName(String id) {
//        return curSymTab.lookup(id).getSLCName();
//    }

    public Sym getSym(BuiltInT type) {
        Sym symbol = curSymTab.lookup(Utils.BuiltinType2ID(type));
        return symbol;
    }

    public String getSymName(BuiltInT type) {
        Sym symbol = curSymTab.lookup(Utils.BuiltinType2ID(type));
        return symbol.getName();
    }

    public Sym getCurSym(String name) {
        return curSymTab.lookup(name);
    }

    public void addCons(Constraint genCons) {
        cons.add(genCons);
    }

    public boolean contractExists(String contractName) {
        Sym rtn = globalSymTab.lookup(contractName);
        if (rtn == null) {
            return false;
        }
        return rtn instanceof ContractSym;
    }

    public Sym getExtSym(String contractName, String funcName) {
        Sym extST = globalSymTab.lookup(contractName);
        if (extST == null) {
            return null;
        }
        return ((ContractSym) extST).lookupSym(funcName);
    }

    public void addSym(String name, Sym sym) {
        curSymTab.add(name, sym);
    }

    public Set<Sym> getTypeSet() {
        Set<Sym> rtn = globalSymTab.getTypeSet();
        for (BuiltInT builtInT : BuiltInT.values()) {
            rtn.add(getSym(builtInT));
        }
        return rtn;
    }

    public ArrayList<Constraint> getTypeRelationCons() {
        return new ArrayList<>();
    }

    public ContractSym getContract(String name) {
        Sym sym = globalSymTab.lookup(name);
        if (sym != null && sym instanceof ContractSym) {
            return (ContractSym) sym;
        }
        return null;
    }

    public boolean containsContract(String iptContract) {
        return getContract(iptContract) != null;
    }

    public void addGlobalSym(String contractName, ContractSym contractSym) {
        globalSymTab.add(contractName, contractSym);
    }

    public Sym toExceptionType(String exceptionName, Arguments arguments, ScopeContext parent) {
        Sym sym = curSymTab.lookup(exceptionName);
        if (sym != null) {
            if (sym instanceof TypeSym) {
                return (TypeSym) sym;
            } else {
                return null;
            }
        }
        ArrayList<VarSym> memberList = arguments.parseArgs(this, parent);
        return new ExceptionTypeSym(exceptionName, null, memberList, parent);
    }

    public ExceptionTypeSym toExceptionTypeSym(ExceptionType t) {
        if (t.isLocal(curContractSym.getName())) {
            System.out.println("isLocal");
            return (ExceptionTypeSym) getCurSym(t.getName());
        } else {
            return (ExceptionTypeSym) getExtSym(t.getContractName(), t.getName());
        }
    }

    public void addSourceFile(String contractName, SourceFile root) {
        programMap.put(contractName, root);
    }

    public SymTab globalSymTab() {
        return globalSymTab;
    }

    public List<Constraint> cons() {
        return cons;
    }

    public HashMap<String, SourceFile> programMap() {
        return programMap;
    }

    public Hypothesis globalHypothesis() {
        return globalHypothesis;
    }

    public ContractSym curContractSym() {
        return curContractSym;
    }

    public SymTab curSymTab() {
        return curSymTab;
    }

    public Label toLabel(IfLabel ifl) {
        if (ifl instanceof PrimitiveIfLabel) {
            VarSym label = (VarSym) curSymTab.lookup(((PrimitiveIfLabel) ifl).value().id);
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
}
