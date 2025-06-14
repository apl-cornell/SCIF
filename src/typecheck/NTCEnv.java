package typecheck;

import ast.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import typecheck.exceptions.SemanticException;
import typecheck.exceptions.TypeCheckFailure;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * XXX What is this class for?
 */
public class NTCEnv {

    private java.util.Map<String, InterfaceSym> contractSymMap; // fullFileName ":" contractName -> ContractSym
    private SymTab curSymTab;
    private List<Constraint> cons;
    private java.util.Map<String, java.util.Map<String, List<Constraint>>> conMap;
    private List<Constraint> contractCons;
    private Hypothesis globalHypothesis;
    private InterfaceSym curContractSym;
    private String currentSourceFileFullName;
    private String currentContractName;
    private java.util.Map<String, List<SourceFile>> programMap; // full filename -> SourceFile

    // info about constructor
    boolean inConstructor, calledSuper;
    private boolean inAtomic = false;

    public NTCEnv(ContractSym contractSym) {
        contractSymMap = new HashMap<>();
        contractCons = cons = new ArrayList<>();
        conMap = new HashMap<>();
        curSymTab = new SymTab();
        globalHypothesis = new Hypothesis();
        curContractSym = contractSym;
        programMap = new HashMap<>();
    }

    public void addContractSym(String fullFileName, String contractName, InterfaceSym contractSym) {
        contractSymMap.put(fullFileName + ":" + contractName, contractSym);
    }

    public void setNewCurSymTab() {
        this.curSymTab = new SymTab();
    }
    public void enterSourceFile(String fullFileName, String contractName) {
        currentSourceFileFullName = fullFileName;
        currentContractName = contractName;
    }
    public String currentSourceFileFullName() {
        return currentSourceFileFullName;
    }
    public String currentContractName() {
        return currentContractName;
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
                            CodeLocation location, ScopeContext context) throws SemanticException {
        return newVarSym(varName, labeledType, isConst, isFinal, isBuiltIn, location, context, false);
    }
    public VarSym newVarSym(String varName, LabeledType labeledType, boolean isConst, boolean isFinal, boolean isBuiltIn,
                            CodeLocation location, ScopeContext context, boolean isGlobal) throws SemanticException{
        TypeSym typeSym = toTypeSym(labeledType.type(), context);
        assert typeSym != null;
        return new VarSym(varName, typeSym, null, location, context, isConst, isFinal, isBuiltIn, isGlobal);
    }

    public TypeSym toTypeSym(ast.Type astType, ScopeContext defContext) throws SemanticException {
        TypeSym typeSym = null;
        if (astType == null) {
            return new BuiltinTypeSym(Utils.BuiltinType2ID(BuiltInT.VOID));
        }

        if (astType instanceof ExtType extType) {
            Sym s = getExtSym(extType.contractName(), extType.name());
            assert s instanceof StructTypeSym;
            return (TypeSym) s;
        }

        Sym s = getCurSym(astType.name());
        if (s instanceof TypeSym)//Utils.isPrimitiveType(astType.x))
        {
            typeSym = (TypeSym) s;// new BuiltinTypeSym(astType.x);
        } else {
            if (astType instanceof DepMap depMap) {
                // map(keyType keyName(), valueType{valueLabel()})
                ScopeContext depMapScope = new ScopeContext(depMap, defContext);
                TypeSym keyTypeSym = toTypeSym(depMap.keyType, depMapScope);

                // create a variable keyType keyName() in the new scope
                enterNewScope();
                VarSym keyNameVar = newVarSym(depMap.keyName(), depMap.labeledKeyType(),
                        false, true, false,
                        depMap.keyType.getLocation(), depMapScope);
                try {
                    addSym(depMap.keyName(), keyNameVar);
                } catch (SymTab.AlreadyDefined e) {
                    throw new SemanticException("Dependent type key name already defined: " +
                            depMap.keyName(), astType.location());
                }

                Label valueLabel = newLabel(depMap.valueLabel());

                TypeSym valueTypeSym = toTypeSym(depMap.valueType, depMapScope);
                assert valueLabel != null;
                typeSym = new DepMapTypeSym(keyTypeSym, depMap.keyName(),
                        valueTypeSym,
                        valueLabel,
                        defContext, depMapScope);

                exitNewScope();
            } else if (astType instanceof Map map) {
                typeSym = new MapTypeSym(toTypeSym(map.keyType, defContext), toTypeSym(map.valueType, defContext), defContext);
            } else if (astType instanceof Array array) {
                typeSym = new ArrayTypeSym(array.size, toTypeSym(array.valueType, defContext), defContext);
            } else {
                // System.out.println(astType.getClass().getName());
                throw new TypeCheckFailure("Not a type: " + astType.name(),
                        astType.location());
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

    /**
     * Look up a symbol in another contract
     * @param contractName
     * @param funcName
     * @return
     */
    public Sym getExtSym(String contractName, String funcName) {
        InterfaceSym extST = getInterface(contractName);
        assert extST != null: "external interface not found: " + contractName;
        return extST.lookupSym(funcName);
    }

    public void addSym(String name, Sym sym) throws SymTab.AlreadyDefined {
        curSymTab.add(name, sym);
    }

    public Set<Sym> getTypeSet() {
        Set<Sym> rtn = curSymTab.getTypeSet();
        for (BuiltInT builtInT : BuiltInT.values()) {
            rtn.add(getSym(builtInT));
        }
        return rtn;
    }

    public ArrayList<Constraint> getTypeRelationCons() {
        return new ArrayList<>();
    }

    public ContractSym getContract(String name) {
        Sym sym = curSymTab.lookup(name);
        if (sym != null && sym instanceof ContractSym) {
            return (ContractSym) sym;
        }
        return null;
    }

    public boolean containsContract(String iptContract) {
        return getContract(iptContract) != null;
    }

    public ExceptionTypeSym newExceptionType(String namespace, String exceptionName, Arguments arguments, ScopeContext parent)
            throws SemanticException {
        // TODO: record namespace
        Sym sym = curSymTab.lookup(exceptionName);
        if (sym != null) {
            if (sym instanceof ExceptionTypeSym) {
                return (ExceptionTypeSym) sym;
            } else {
                return null;
            }
        }

        enterNewScope();
        ArrayList<VarSym> memberList = arguments.parseArgs(this, parent);
        exitNewScope();
        return new ExceptionTypeSym(exceptionName, memberList, parent);
    }

    public EventTypeSym newEventType(String eventName, Arguments arguments, ScopeContext parent) throws SemanticException {
        Sym sym = curSymTab.lookup(eventName);
        if (sym != null) {
            if (sym instanceof EventTypeSym) {
                return (EventTypeSym) sym;
                // TODO steph haven't figured out the code logic
            } else {
                return null;
            }
        }

        enterNewScope();
        ArrayList<VarSym> memberList = arguments.parseArgs(this, parent);
        exitNewScope();
        return new EventTypeSym(eventName, memberList, parent);
    }

    public ExceptionTypeSym getExceptionTypeSym(Type t) {
        return (ExceptionTypeSym) getCurSym(t.name());
    }

    public void addSourceFile(String contractName, SourceFile root) {
        // programMap.put(contractName, root);
        programMap.computeIfAbsent(contractName, k -> new ArrayList<>()).add(root);
    }

    public List<Constraint> cons() {
        return cons;
    }

    public java.util.Map<String, List<SourceFile>> programMap() {
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
            // if (label == null) return null;
            assert label != null :  ((PrimitiveIfLabel) ifl).value().id;
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
        Sym sym = getCurSym(name);
        if (sym != null && sym instanceof InterfaceSym) {
            return (InterfaceSym) sym;
        }
        return null;
    }

    public void setCurSymTab(String currentSourceFileFullName, String currentContractName) {
        assert contractSymMap.containsKey(currentSourceFileFullName + ":" +  currentContractName);
        curSymTab = contractSymMap.get(currentSourceFileFullName + ":" +  currentContractName).symTab;
    }

    public void importContract(String iptContract, String contractName,
                               CodeLocation location) throws SemanticException {
        InterfaceSym sym = contractSymMap.get(iptContract + ":" + contractName);
        if (sym == null) {
            throw new SemanticException("not containing imported contract/interface: " + iptContract,
                    location);
        }
        try {
            // System.err.println("importing contract/interface " + sym.getName());
            addSym(sym.getName(), sym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("already imported: " + iptContract,
                    location);
        }
    }

    public java.util.Map<String, ExceptionTypeSym> getExceptionTypeSymMap() {
        return curSymTab.getExceptionMap();
    }

    public java.util.Map<String, EventTypeSym> getEventTypeSymMap() {
        return curSymTab.getEventMap();
    }


    public void enterConstructor() {
//        System.err.println("entering the constructor");
        inConstructor = true;
        calledSuper = false;
    }

    public void leaveConstructor() {
//        System.err.println("leaving the constructor");
        inConstructor = false;
    }

    public boolean superCalled() {
        return calledSuper;
    }

    public void callSuper() {
        calledSuper = true;
    }

    public boolean inConstructor() {
        return inConstructor;
    }

    public StructTypeSym toStructType(String structName, List<StateVariableDeclaration> members) throws SemanticException {
        Sym sym = getCurSym(structName);
        if (sym != null) {
            assert sym instanceof StructTypeSym: "Existing name: " + structName;
            return (StructTypeSym) sym;
        }

        List<VarSym> memberList = new ArrayList<>();
        for (StateVariableDeclaration member : members) {
            VarSym tmp = member.toVarInfo(curContractSym);
            memberList.add(tmp);
        }
        return new StructTypeSym(structName, memberList, curContractSym.astNode.getScopeContext());
    }

    public void addType(String structName, StructTypeSym toStructType) {
        try {
            addSym(structName, toStructType);
        } catch (SymTab.AlreadyDefined e) {
            throw new RuntimeException(e);
        }
    }

    public void enterAtomic() {
        inAtomic = true;
    }

    public void exitAtomic() {
        inAtomic = false;
    }

    public boolean inAtomic() {
        return inAtomic;
    }

    public void enterMethod(String name) {
        cons = new ArrayList<>();
        conMap.get(currentSourceFileFullName).put(name, cons);
    }

    public void enterFile(String sourceFilePath) {
        conMap.put(sourceFilePath, new HashMap<>());
    }

    public void exitMethod() {
        cons = contractCons;
    }

    public Set<String> getFilenames() {
        return conMap.keySet();
    }

    public Set<String> getMethodnames(String filename) {
        return conMap.get(filename).keySet();
    }

    public List<Constraint> contractCons() {
        return contractCons;
    }

    public Collection<? extends Constraint> methodCons(String sourceFilePath, String methodname) {
        return conMap.get(sourceFilePath).get(methodname);
    }
}