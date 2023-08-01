package typecheck;

import ast.*;
import java.util.List;
import java.util.Set;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;

public class NTCEnv {

    private java.util.Map<String, InterfaceSym> contractSymMap; // full filename -> ContractSym
    private SymTab curSymTab;
    private List<Constraint> cons;
    private Hypothesis globalHypothesis;
    private InterfaceSym curContractSym;
    private String currentSourceFileFullName;
    private java.util.Map<String, SourceFile> programMap; // full filename -> SourceFile

    public NTCEnv(ContractSym contractSym) {
        contractSymMap = new HashMap<>();
        cons = new ArrayList<>();
        curSymTab = new SymTab();
        globalHypothesis = new Hypothesis();
        curContractSym = contractSym;
        programMap = new HashMap<>();
    }

    public void addContractSym(String fullFileName, InterfaceSym contractSym) {
        contractSymMap.put(fullFileName, contractSym);
    }

    public void setNewCurSymTab() {
        this.curSymTab = new SymTab();
    }
    public void enterSourceFile(String fullFileName) {
        currentSourceFileFullName = fullFileName;
    }
    public String currentSourceFileFullName() {
        return currentSourceFileFullName;
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
                // map(keyType keyName(), valueType{valueLabel()})
                ScopeContext depMapScope = new ScopeContext(depMap, defContext);
                TypeSym keyTypeSym = toTypeSym(depMap.keyType, depMapScope);

                // create a variable keyType keyName() in the new scope
                enterNewScope();
                VarSym keyNameVar = newVarSym(depMap.keyName(), depMap.labeledKeyType(),
                        false, true, false,
                        depMap.keyType.getLocation(), depMapScope);
                addSym(depMap.keyName(), keyNameVar);

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
                assert false: astType.name();
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

    /**
     * Look up a symbol in another contract
     * @param contractName
     * @param funcName
     * @return
     */
    public Sym getExtSym(String contractName, String funcName) {
        ContractSym extST = getContract(contractName);
        assert extST != null;
        return extST.lookupSym(funcName);
    }

    public void addSym(String name, Sym sym) {
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

    public void setCurSymTab(String currentSourceFileFullName) {
        assert contractSymMap.containsKey(currentSourceFileFullName);
        curSymTab = contractSymMap.get(currentSourceFileFullName).symTab;
    }

    public void importContract(String iptContract) {
        InterfaceSym sym = contractSymMap.get(iptContract);
        assert sym != null : "not containing imported contract/interface: " + iptContract;
        System.err.println("importing contract/interface " + sym.getName());
        addSym(sym.getName(), sym);
    }
}
