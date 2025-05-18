package typecheck;

import ast.ComplexIfLabel;
import ast.IfLabel;
import ast.Interface;
import ast.PrimitiveIfLabel;
import ast.SourceFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;

import java.util.HashMap;
import java.util.Set;
import java.util.List;

/**
 * The environment for information-flow type-checking.
 * Mutable.
 */
public class VisitEnv {
    public Context inContext;
    private Map<String, List<Constraint>> conMap, trustConMap;
    public List<Constraint> cons;
    private List<Constraint> trustCons;
    public SymTab globalSymTab;
    public SymTab curSymTab;
    private Hypothesis hypothesis;
    private final Set<VarSym> principalSet;
    private InterfaceSym curContractSym;
    private FuncSym curFuncSym;
    public Map<String, String> sigReq = new HashMap<>();
    public Map<String, List<SourceFile>> programMap;

    public VisitEnv(Context inContext,
                    List<Constraint> cons,
                    List<Constraint> trustCons,
                    SymTab globalSymTab,
                    SymTab curSymTab,
                    Hypothesis hypothesis,
                    Set<VarSym> principalSet,
                    InterfaceSym curContractSym,
                    HashMap<String, List<SourceFile>> programMap) {
        this.inContext = inContext;
        this.cons = cons;
        this.trustCons = trustCons;
        this.globalSymTab = globalSymTab;
        this.curSymTab = curSymTab;
        this.hypothesis = hypothesis;
        this.principalSet = principalSet;
        this.curContractSym = curContractSym;
        this.programMap = programMap;
    }

    public Hypothesis hypothesis() {
        return hypothesis;
    }

    public void addVar(String id, VarSym varSym) throws SymTab.AlreadyDefined {
        curSymTab.add(id, varSym);
    }

    /**
     * Should never fail
     * @param id
     * @return
     */
    public VarSym getVar(String id)  {
        Sym sym = curSymTab.lookup(id);
        if (sym instanceof VarSym)
            return (VarSym) sym;
        else {
            throw new RuntimeException("VisitEnv getVar failure:" + id + " " + sym);
        }
    }

    public InterfaceSym getContract(String id) {
        Sym sym = curSymTab.lookup(id);
        if (sym instanceof InterfaceSym)
            return (InterfaceSym) sym;
        else
            return null;
    }

    public FuncSym getFunc(String id) {
        Sym sym = curSymTab.lookup(id);
        if (sym instanceof FuncSym)
            return (FuncSym) sym;
        else
            return null;
    }

    public boolean containsFunc(String id) {
        return getFunc(id) != null;
    }

    public boolean containsContract(String funcName) {
        return getContract(funcName) != null;
    }

    //    public static int counter = 0;
    public void incScopeLayer() {
//        counter++;
        curSymTab = new SymTab(curSymTab);
        hypothesis.enterScope();
    }

    public void decScopeLayer() {
//        counter--;
//        assert counter >= 0;
        curSymTab = curSymTab.getParent();
        hypothesis.exitScope();
    }

    public boolean containsVar(String id) {
        return getVar(id) != null;
    }

    public void addSigReq(String namespace, String name) {
        sigReq.put(namespace, name);
    }

    public Sym getCurSym(String name) {
        return curSymTab.lookup(name);
    }

    public Sym getExtSym(String contractName, String funcName) {
        Sym extST = globalSymTab.lookup(contractName);
        if (extST == null) return null;
        return ((InterfaceSym) extST).lookupSym(funcName);
    }

    public ExceptionTypeSym toExceptionTypeSym(ast.Type t) {
        return (ExceptionTypeSym) getCurSym(t.name());
    }

    public ExceptionTypeSym getExp(String name) {
        Sym sym = curSymTab.lookup(name);
        if (sym instanceof ExceptionTypeSym)
            return (ExceptionTypeSym) sym;
        else
            return null;
    }

    public ScopeContext getScopeContext() {
        return curContractSym.defContext();
    }

    public Set<VarSym> principalSet() {
        return principalSet;
    }

    public List<Constraint> trustCons() {
        return Collections.unmodifiableList(trustCons);
    }

    public void addTrustConstraint(Constraint constraint) {
        trustCons.add(constraint);
    }

    public void addPrincipal(VarSym varSym) {
        principalSet.add(varSym);
    }

    public VarSym thisSym() {
        return curContractSym.thisSym();
    }

    public VarSym sender() {
        return curFuncSym.sender();
    }

    public InterfaceSym curContractSym() {
        return curContractSym;
    }

    public void setCurContract(InterfaceSym contractSym) {
        curContractSym = contractSym;
        curSymTab = contractSym.symTab;
    }

    public void setCurFuncSym(FuncSym funcSym) {
        curFuncSym = funcSym;
    }

    public Label toLabel(IfLabel ifl) {
        if (ifl instanceof PrimitiveIfLabel) {
            VarSym label = (VarSym) getVar(((PrimitiveIfLabel) ifl).value().id);
            if (label == null) return null;
            return new PrimitiveLabel(label, ifl.getLocation());
        } else if (ifl instanceof ComplexIfLabel) {
            return new ComplexLabel(toLabel(((ComplexIfLabel) ifl).getLeft()),
                    ((ComplexIfLabel) ifl).getOp(),
                    toLabel(((ComplexIfLabel) ifl).getRight()),
                    ifl.getLocation());
        } else {
            throw new RuntimeException("Unable to resolve the label: " + ifl);
        }
    }

    public void enterNewContract() {
        conMap = new HashMap<>();
        trustConMap = new HashMap<>();
        cons = new ArrayList<>();
        trustCons = new ArrayList<>();
        conMap.put("contract", cons);
        trustConMap.put("contract", trustCons);
    }

    public void enterNewMethod(String name) {
        cons = new ArrayList<>();
        trustCons = new ArrayList<>();
        conMap.put(name, cons);
        trustConMap.put(name, trustCons);
    }

    public void exitMethod() {
        cons = conMap.get(Utils.CONTRACT_KEYWORD);
        trustCons = trustConMap.get(Utils.CONTRACT_KEYWORD);
    }

    public List<Constraint> getCons(String key) {
        return conMap.get(key);
    }

    public List<Constraint> getTrustCons(String key) {
        return trustConMap.get(key);
    }

    public List<String> getMethodNameSet() {
        List<String> tmp = conMap.keySet().stream().sorted().collect(Collectors.toList());
        List<String> result = new ArrayList<>();
        Collections.reverse(tmp);
        tmp = moveFirst("addLiquidity", tmp);
        boolean existConstructor = false;
        for (String name: tmp) {
            if (name.equals("constructor")) {
                existConstructor = true;
            } else {
                result.add(name);
            }
        }
        if (existConstructor) {
            result.add("constructor");
        }
        return result;
    }

    private List<String> moveFirst(String name0, List<String> tmp) {
        List<String> result = new ArrayList<>();
        boolean existname0 = false;
        for (String name: tmp) {
            if (name.equals(name0)) {
                existname0 = true;
            } else {
                result.add(name);
            }
        }
        if (existname0) {
            result.add(0, name0);
        }
        return result;
    }

}