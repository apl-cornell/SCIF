package typecheck;

import ast.ComplexIfLabel;
import ast.IfLabel;
import ast.PrimitiveIfLabel;
import ast.SourceFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

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

    /**
     * The current version of each mutable principal variable that has been versioned in
     * the current method, keyed by the variable's SHErrLoc name. A variable absent from
     * the map is at its initial version, which is named by the variable itself.
     */
    private Map<String, String> versions = new HashMap<>();
    private final Set<String> versionNames = new java.util.LinkedHashSet<>();
    private int versionCount = 0;

    private final Set<String> rigidAtoms = new java.util.LinkedHashSet<>();
    private final List<Inequality> assumptions = new ArrayList<>();

    public void addRigidAtom(String name) {
        rigidAtoms.add(name);
    }

    public Set<String> rigidAtoms() {
        return rigidAtoms;
    }

    /**
     * Assumes that {@code inequality} holds when checking the methods of the current contract.
     */
    public void assume(Inequality inequality) {
        assumptions.add(inequality);
    }

    /** The inequalities assumed so far. */
    public List<Inequality> assumptions() {
        return assumptions;
    }

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

    /** The built-in type {@code t}. */
    public TypeSym builtinType(BuiltInT t) {
        return (TypeSym) curSymTab.lookup(Utils.BuiltinType2ID(t));
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

    public void addTrustConstraint(Constraint constraint) {
        trustCons.add(constraint);
    }

    /**
     * Adds the constraint {@code from <= to} under the current hypothesis.
     * If it cannot be satisfied, the violation is reported at {@code location}
     * with {@code explanation}.
     */
    public void checkFlow(String from, String to, CodeLocation location,
                          String explanation) {
        cons.add(new Constraint(new Inequality(from, Relation.LEQ, to),
                hypothesis, location, explanation));
    }

    /**
     * Adds the constraint {@code a == b} under the current hypothesis.
     * If it cannot be satisfied, the violation is reported at {@code location}
     * with {@code explanation}.
     */
    public void checkEqual(String a, String b, CodeLocation location,
                           String explanation) {
        cons.add(new Constraint(new Inequality(a, Relation.EQ, b), hypothesis,
                location, explanation));
    }

    /** Like {@link #checkFlow}, but adds a trust constraint. */
    public void checkTrustedBy(String from, String to, CodeLocation location,
                               String explanation) {
        trustCons.add(new Constraint(new Inequality(from, Relation.LEQ, to),
                hypothesis, location, explanation));
    }

    /** Like {@link #checkEqual}, but adds a trust constraint. */
    public void checkTrustEqual(String a, String b, CodeLocation location,
                                String explanation) {
        trustCons.add(new Constraint(new Inequality(a, Relation.EQ, b),
                hypothesis, location, explanation));
    }

    public void addPrincipal(VarSym varSym) {
        principalSet.add(varSym);
    }

    /**
     * The SHErrLoc name of the principal that {@code v} holds at the current program point.
     * For a variable that is not a mutable principal, this is the variable's own name.
     */
    public String principalName(VarSym v) {
        String base = v.toSHErrLocFmt();
        return versions.getOrDefault(base, base);
    }

    /**
     * Starts a new version of the mutable principal variable {@code v}, about which nothing
     * is known, and returns its SHErrLoc name.
     */
    public String newVersion(VarSym v) {
        assert v.isMutablePrincipal() : v.getName();
        return newVersion(v.toSHErrLocFmt());
    }

    private String newVersion(String base) {
        String version = base + "..v" + (++versionCount);
        versions.put(base, version);
        versionNames.add(version);
        return version;
    }

    /**
     * Assumes that the principals named {@code a} and {@code b} are the same, as when one
     * variable is assigned the principal held by another.
     */
    public void assumeSamePrincipal(String a, String b) {
        assume(new Inequality(a, Relation.EQ, b));
    }

    /**
     * Registers {@code name} as a principal about which nothing is known, such as a
     * newly created contract, and returns it.
     */
    public String freshPrincipal(String name) {
        versionNames.add(name);
        return name;
    }

    /** The SHErrLoc names of all versions created so far. Each names a principal. */
    public Set<String> versionNames() {
        return versionNames;
    }

    /** The current versions, for restoring with {@link #restoreVersions}. */
    public Map<String, String> saveVersions() {
        return new HashMap<>(versions);
    }

    /** Makes {@code saved} the current versions. */
    public void restoreVersions(Map<String, String> saved) {
        versions = new HashMap<>(saved);
    }

    /**
     * Makes the current versions those at a point where the control-flow paths ending in
     * {@code branches} merge. A variable whose version differs among the branches gets a
     * new version (a phi-version), about which nothing is known.
     */
    public void mergeVersions(List<Map<String, String>> branches) {
        Set<String> keys = new java.util.HashSet<>();
        for (Map<String, String> b : branches) keys.addAll(b.keySet());
        Map<String, String> merged = new HashMap<>();
        for (String base : keys) {
            Set<String> seen = new java.util.HashSet<>();
            for (Map<String, String> b : branches) seen.add(b.getOrDefault(base, base));
            if (seen.size() == 1) {
                String only = seen.iterator().next();
                if (!only.equals(base)) merged.put(base, only);
            } else {
                merged.put(base, base + "..v" + (++versionCount));
                versionNames.add(merged.get(base));
            }
        }
        versions = merged;
    }

    /**
     * Gives a new version to each mutable principal variable visible here whose name is in
     * {@code names}, and to each mutable principal field of the current contract.
     */
    public void havoc(Set<String> names) {
        for (String name : names) {
            if (curSymTab.lookup(name) instanceof VarSym v && v.isMutablePrincipal()) {
                newVersion(v);
            }
        }
        havocFields();
    }

    /**
     * Gives a new version to each mutable principal field of the current contract, as
     * needed after any call, which may assign to fields.
     */
    public void havocFields() {
        for (VarSym v : curContractSym.symTab.getVars().values()) {
            if (v.isMutablePrincipal()) {
                newVersion(v);
            }
        }
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

    /**
     * Resolves {@code ifl} at the current program point: a mutable principal variable in it
     * names the version it holds here.
     */
    public Label toLabel(IfLabel ifl) {
        if (ifl instanceof PrimitiveIfLabel) {
            VarSym label = getVar(((PrimitiveIfLabel) ifl).value().id);
            return new PrimitiveLabel(label,
                    label.isMutablePrincipal() ? principalName(label) : null, ifl.getLocation());
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
        versions = new HashMap<>();
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