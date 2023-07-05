package typecheck;

import ast.*;
import java.util.List;
import java.util.Set;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;

public class NTCEnv {

    private SymTab globalSymTab;
    private SymTab curSymTab;
    // public HashMap<String, SymTab> externalSymTab;
    // external contracts will be added to the global SymTab
    private ArrayList<Constraint> cons;
    private Hypothesis globalHypothesis;
    private InterfaceSym curContractSym;
    private java.util.Map<String, SourceFile> programMap;

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

    private void setCurSymTab(SymTab curSymTab) {
        this.curSymTab = curSymTab;
    }

    public void initCurSymTab() {
        // this.globalSymTab = globalSymTab;
        this.curSymTab = globalSymTab;
    }
    public void enterNewScope() {
        curSymTab = new SymTab(curSymTab);
    }
    public void exitNewScope() {
        curSymTab = curSymTab.getParent();
    }

    public void setCurContractSym(InterfaceSym curContractSym) {
        this.curContractSym = curContractSym;
    }

    public VarSym newVarSym(String varName, LabeledType labeledType, boolean isConst, boolean isFinal, boolean isBuiltIn,
            CodeLocation location, ScopeContext context) {
        TypeSym typeSym = toTypeSym(labeledType.type(), context);
        if (typeSym == null) {
            throw new RuntimeException("Type not found: " + labeledType.type().name());
        }
        return new VarSym(varName, typeSym, null, location, context, isConst, isFinal, isBuiltIn);
    }

    public TypeSym toTypeSym(ast.Type astType, ScopeContext defContext) {
        TypeSym typeSym = null;
        if (astType == null) {
            return new BuiltinTypeSym(Utils.BuiltinType2ID(BuiltInT.VOID));
        }

        Sym s = getCurSym(astType.name());
        if (s instanceof TypeSym)//Utils.isPrimitiveType(astType.x))
        {
            typeSym = (TypeSym) s;// new BuiltinTypeSym(astType.x);
        } else {
            if (astType instanceof DepMap depMap) {
                typeSym = new DepMapTypeSym(toTypeSym(depMap.keyType, defContext), depMap.keyName(), toTypeSym(depMap.valueType, defContext),
                        newLabel(depMap.valueLabel()),
                        defContext, new ScopeContext(depMap, defContext));
            } else if (astType instanceof Map map) {
                typeSym = new MapTypeSym(toTypeSym(map.keyType, defContext), toTypeSym(map.valueType, defContext), defContext);
            } else if (astType instanceof Array array) {
                typeSym = new ArrayTypeSym(array.size, toTypeSym(array.valueType, defContext), defContext);
            } else {
                // return null;
                // typeSym = new BuiltinTypeSym(lt.x);
            }
        }
        return typeSym;
    }

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

    public void addGlobalSym(String contractName, Sym contractSym) {
        globalSymTab.add(contractName, contractSym);
    }

    public Sym newExceptionType(String exceptionName, Arguments arguments, ScopeContext parent) {
        Sym sym = curSymTab.lookup(exceptionName);
        if (sym != null) {
            if (sym instanceof TypeSym) {
                return (TypeSym) sym;
            } else {
                return null;
            }
        }
        ArrayList<VarSym> memberList = arguments.parseArgs(this, parent);
        return new ExceptionTypeSym(exceptionName, memberList, parent);
    }

    public ExceptionTypeSym getExceptionTypeSym(Type t) {
        return (ExceptionTypeSym) getCurSym(t.name());
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

    public java.util.Map<String, SourceFile> programMap() {
        return programMap;
    }

    public Hypothesis globalHypothesis() {
        return globalHypothesis;
    }

    public InterfaceSym curContractSym() {
        return curContractSym;
    }

    public SymTab curSymTab() {
        return curSymTab;
    }

    public Label newLabel(IfLabel ifl) {
        if (ifl instanceof PrimitiveIfLabel) {
            VarSym label = (VarSym) curSymTab.lookup(((PrimitiveIfLabel) ifl).value().id);
            if (label == null) return null;
            return new PrimitiveLabel(label, ifl.getLocation());
        } else if (ifl instanceof ComplexIfLabel) {
            return new ComplexLabel(newLabel(((ComplexIfLabel) ifl).getLeft()),
                    ((ComplexIfLabel) ifl).getOp(),
                    newLabel(((ComplexIfLabel) ifl).getRight()),
                    ifl.getLocation());
        } else {
            throw new RuntimeException();
        }
    }

    public boolean containsInterface(String name) {
        return getInterface(name) != null;
    }

    private InterfaceSym getInterface(String name) {
        Sym sym = globalSymTab.lookup(name);
        if (sym != null && sym instanceof InterfaceSym) {
            return (InterfaceSym) sym;
        }
        return null;
    }
}
