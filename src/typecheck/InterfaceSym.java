package typecheck;

import ast.AnnAssign;
import ast.Arguments;
import ast.ComplexIfLabel;
import ast.Contract;
import ast.DepMap;
import ast.IfLabel;
import ast.Interface;
import ast.LabeledType;
import ast.Map;
import ast.PrimitiveIfLabel;
import ast.Type;
import java.util.ArrayList;
import java.util.List;

public class InterfaceSym extends TypeSym {

    // public HashSet<String> iptContracts;
    /*public HashMap<String, Type> typeMap;
    public HashMap<String, VarInfo> varMap;
    public HashMap<String, FuncInfo> funcMap;*/
    public SymTab symTab;
    // private Label label;
    // public ArrayList<TrustConstraint> trustCons;
    private List<Assumption> assumptions;
    private final Interface astNode;
    //private VarSym thisSym;

    public InterfaceSym(String name,
            SymTab symTab,
            // HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap,
            List<Assumption> assumptions,
            // Label label,
            Interface itrface) {
        super(name, itrface.getScopeContext());
        this.symTab = symTab;
        this.assumptions = assumptions;
        // this.label = label;
        astNode = itrface;
    }

    public InterfaceSym(String contractName, Interface itrface) {
        super(contractName, itrface.getScopeContext());
        astNode = itrface;
        symTab = new SymTab();
        assumptions = new ArrayList<>();
    }

    public TypeSym getTypeSym(String typeName) {
        Sym sym = symTab.lookup(typeName);
        if (sym instanceof TypeSym) {
            return (TypeSym) sym;
        }
        return null;
    }

    /**
        Look up a type symbol.
        If there is no such a symbol, create one with the given defining scope.
     */
    public TypeSym toTypeSym(Type astType, ScopeContext defContext) {
        if (astType == null) {
            return new BuiltinTypeSym("void");
        }
        // System.err.println("[in]toTypeSym: " + astType.x);

        Sym s = symTab.lookup(astType.name());
        TypeSym typeSym = null;
        if (s instanceof TypeSym) {
            typeSym = (TypeSym) s;
        } else {
            if (astType instanceof DepMap depMap) {
                // map(keyType keyName(), valueType{valueLabel()})
                ScopeContext depMapScope = new ScopeContext(depMap, defContext);
                TypeSym keyTypeSym = toTypeSym(depMap.keyType, depMapScope);

                // create a variable keyType keyName() in the new scope
                symTab = new SymTab(symTab);
                VarSym keyNameVar = newVarSym(depMap.keyName(), depMap.labeledKeyType(),
                        false, true, false,
                        depMap.keyType.getLocation(), depMapScope);
                addVar(depMap.keyName(), keyNameVar);

                Label valueLabel = newLabel(depMap.valueLabel());

                TypeSym valueTypeSym = toTypeSym(depMap.valueType, depMapScope);
                assert valueLabel != null;
                typeSym = new DepMapTypeSym(keyTypeSym, depMap.keyName(),
                        valueTypeSym,
                        valueLabel,
                        defContext, depMapScope);

                symTab = symTab.parent;
            } else if (astType instanceof Map map) {
                typeSym = new MapTypeSym(toTypeSym(map.keyType, defContext), toTypeSym(map.valueType, defContext), defContext);
            }

        }
        assert typeSym != null;
        // System.err.println("[out]toTypeSym: " + typeSym.name);
        return typeSym;
    }

    public VarSym newVarSym(String localName, LabeledType astType, boolean isStatic, boolean isFinal, boolean isBuiltIn,
            CodeLocation loc, ScopeContext defContext) {
        System.err.println("toVarSym: " + localName + " " + astType.type().name());
        TypeSym typeSym = toTypeSym(astType.type(), defContext);
        System.err.println("toVarSym: " + localName + " " + typeSym);
        Label ifl;
        ifl = newLabel(astType.label());
        return new VarSym(localName, typeSym, ifl, loc, defContext, isStatic, isFinal, isBuiltIn);
    }

    public Label newLabel(IfLabel ifl) {
        if (ifl instanceof PrimitiveIfLabel) {
            VarSym label = (VarSym) lookupSym(((PrimitiveIfLabel) ifl).value().id);
            if (label == null) return null;
            return new PrimitiveLabel(label, ifl.getLocation());
        } else if (ifl instanceof ComplexIfLabel) {
            return new ComplexLabel(newLabel(((ComplexIfLabel) ifl).getLeft()),
                    ((ComplexIfLabel) ifl).getOp(),
                    newLabel(((ComplexIfLabel) ifl).getRight()),
                    ifl.getLocation());
        } else {
            throw new RuntimeException("Unable to resolve the label: " + ifl);
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

    public void addContract(String typename, InterfaceSym typeSym) {
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
        return Utils.getLabelNameContract(getInterfaceNode().getScopeContext());
    }

    public String getLabelContract() {
        return Utils.getLabelNameContract(getInterfaceNode().getScopeContext());
        // return ifl.toSHErrLocFmt(name());
    }

    public Interface getInterfaceNode() {
        return astNode;
    }

    public ExceptionTypeSym toExceptionType(String exceptionName, Arguments arguments, ScopeContext defContext) {

        ExceptionTypeSym sym = getExceptionSym(exceptionName);
        if (sym != null) {
            return sym;
        }
        List<VarSym> memberList = arguments.parseArgs(this);
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
