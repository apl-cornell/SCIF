package typecheck;

import ast.AnnAssign;
import ast.Arguments;
import ast.Array;
import ast.ComplexIfLabel;
import ast.Contract;
import ast.DepMap;
import ast.ExtType;
import ast.IfLabel;
import ast.Interface;
import ast.LabeledType;
import ast.Map;
import ast.Node;
import ast.PrimitiveIfLabel;
import ast.StateVariableDeclaration;
import ast.Type;
import typecheck.exceptions.SemanticException;
import compile.ast.PrimitiveType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class InterfaceSym extends TypeSym {

    // public HashSet<String> iptContracts;
    /*public HashMap<String, Type> typeMap;
    public HashMap<String, VarInfo> varMap;
    public HashMap<String, FuncInfo> funcMap;*/
    public SymTab symTab;
    // private Label label;
    // public ArrayList<TrustConstraint> trustCons;
    protected List<Assumption> assumptions;
    protected Node astNode;
    //private VarSym thisSym;
    private VarSym anySym;

    public InterfaceSym(String name,
            SymTab symTab,
            // HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap,
            List<Assumption> assumptions,
            // Label label,
            Interface itrface,
            VarSym any) {
        super(name, itrface.getScopeContext());
        this.symTab = symTab;
        this.assumptions = assumptions;
        // this.label = label;
        astNode = itrface;
        anySym = any;
    }
    public InterfaceSym(String name,
            SymTab symTab,
            // HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap,
            List<Assumption> assumptions,
            // Label label,
            ScopeContext context,
            VarSym any) {
        super(name, context);
        this.symTab = symTab;
        this.assumptions = assumptions;
        // this.label = label;
        astNode = null;
        anySym = any;
    }

    public InterfaceSym(String contractName, Interface itrface) {
        super(contractName, itrface.getScopeContext());
        astNode = itrface;
        symTab = new SymTab();
        assumptions = new ArrayList<>();
    }
    public InterfaceSym(String contractName, ScopeContext scopeContext) {
        super(contractName, scopeContext);
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
    public TypeSym toTypeSym(Type astType, ScopeContext defContext) throws SemanticException {
        if (astType == null) {
            return new BuiltinTypeSym("void");
        }

        if (astType instanceof ExtType extType) {
            Sym s = symTab.lookup(extType.contractName());
            assert s instanceof InterfaceSym;
            s = ((InterfaceSym) s).getTypeSym(extType.name());
            assert s instanceof StructTypeSym;
            return (TypeSym) s;
        }

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
                addVar(depMap.keyName(), keyNameVar, astType.location());

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
            } else if (astType instanceof Array array) {
                typeSym = new ArrayTypeSym(array.size, toTypeSym(array.valueType, defContext), defContext);
            }

        }
        assert typeSym != null: astType.name();
        return typeSym;
    }

    public VarSym newVarSym(String localName, LabeledType astType, boolean isStatic, boolean isFinal, boolean isBuiltIn,
            CodeLocation loc, ScopeContext defContext) throws SemanticException {
        TypeSym typeSym = toTypeSym(astType.type(), defContext);
        assert typeSym != null;
        Label ifl;
        ifl = newLabel(astType.label());
        return new VarSym(localName, typeSym, ifl, loc, defContext, isStatic, isFinal, isBuiltIn);
    }

    public Label newLabel(IfLabel ifl) {
        if (ifl instanceof PrimitiveIfLabel) {
            VarSym label = (VarSym) lookupSym(((PrimitiveIfLabel) ifl).value().id);
            // assert label != null: ((PrimitiveIfLabel) ifl).value().id;
            return label != null ? new PrimitiveLabel(label, ifl.getLocation()) : null;
        } else if (ifl instanceof ComplexIfLabel) {
            return new ComplexLabel(newLabel(((ComplexIfLabel) ifl).getLeft()),
                    ((ComplexIfLabel) ifl).getOp(),
                    newLabel(((ComplexIfLabel) ifl).getRight()),
                    ifl.getLocation());
        } else {
            throw new RuntimeException("Unable to resolve the label: " + ifl);
        }
    }

    public void addVar(String varname, VarSym varSym, CodeLocation loc) throws SemanticException {
        try {
            symTab.add(varname, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Variable already defined: " + varname,
                    loc);
        }
    }

    public void addFunc(String funcname, FuncSym funcSym, CodeLocation loc) throws SemanticException {
        try {
            symTab.add(funcname, funcSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Function already defined: " + funcname,
                    loc);
        }
    }

    public void addType(String typename, TypeSym typeSym, CodeLocation loc) throws SemanticException {
        try {
            symTab.add(typename, typeSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Type already defined: " + typename,
                    loc);
        }
    }

    public void addInterface(String typename, InterfaceSym typeSym, CodeLocation loc) throws SemanticException {
        try {
            symTab.add(typename, typeSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Interface already defined: " + typename,
                    loc);
        }
    }

    public boolean isLValue() {
        return false;
    }

    @Override
    public compile.ast.Type getType() {
        return new PrimitiveType(this.getName());
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
        return Utils.getLabelNameContract(defContext());
    }

    public String getLabelContract() {
        return Utils.getLabelNameContract(defContext());
        // return ifl.toSHErrLocFmt(name());
    }

    /*public Interface getInterfaceNode() {
        return (Interface) astNode;
    }*/

    public ExceptionTypeSym toExceptionType(String namespace, String exceptionName, Arguments arguments, ScopeContext defContext) throws SemanticException {
        // TODO: record namespace

        ExceptionTypeSym sym = getExceptionSym(exceptionName);
        if (sym != null) {
            return sym;
        }

        symTab = new SymTab(symTab);
        List<VarSym> memberList = arguments.parseArgs(this);
        symTab = symTab.getParent();
        return new ExceptionTypeSym(exceptionName, memberList, defContext);
    }

    public ExceptionTypeSym getExceptionSym(String exceptionName) {
        Sym sym = symTab.lookup(exceptionName);
        if (sym == null || (!(sym instanceof ExceptionTypeSym))) {
            return null;
        }
        return (ExceptionTypeSym) sym;
    }

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
    public void addContract(String typename, InterfaceSym typeSym) throws SymTab.AlreadyDefined {
        symTab.add(typename, typeSym);
    }

    public TypeSym toStructType(String typeName, List<StateVariableDeclaration> members) throws SemanticException {
        Sym sym = symTab.lookup(typeName);
        if (sym != null) {
            if (sym instanceof TypeSym) {
                return (TypeSym) sym;
            } else {
                return null;
            }
        }
        List<VarSym> memberList = new ArrayList<>();
        for (StateVariableDeclaration member : members) {
            VarSym tmp = member.toVarInfo(this);
            memberList.add(tmp);
        }
        return new StructTypeSym(typeName, memberList, astNode.getScopeContext());
    }

    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        for (Entry<String, FuncSym> sym: symTab.getAllFuncs().entrySet()) {
            sb.append("|");
            sb.append(sym.getKey());
        }
        return sb.toString();
    }

    public VarSym any() {
        VarSym anySym = (VarSym) lookupSym(Utils.LABEL_BOTTOM);
        assert anySym != null;
        return anySym;
    }
}
