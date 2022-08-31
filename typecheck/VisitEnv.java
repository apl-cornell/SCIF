package typecheck;

import ast.ExceptionType;
import ast.Program;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class VisitEnv {
    public Context inContext;
    // public PathOutcome outContext;
    // public HashMap<String, FuncInfo> funcMap;
    public List<Constraint> cons;
    public List<Constraint> trustCons;
    // public LookupMaps varNameMap;
    public SymTab globalSymTab;
    public SymTab curSymTab;
    public Hypothesis hypothesis;
    public HashSet<String> principalSet; // TODO: better imp
    public ContractSym curContractSym;
    // public HashMap<String, ContractInfo> contractMap;
    //public HashMap<String, SigCons> sigConsMap;
    public HashMap<String, String> sigReq = new HashMap<>();
    public HashMap<String, Program> programMap;
    //public HashMap<ExceptionTypeSym, PsiUnit> psi;


    public VisitEnv(Context inContext,
                    // PathOutcome outContext,
                    // HashMap<String, FuncInfo> funcMap,
                    List<Constraint> cons,
                    List<Constraint> trustCons,
                    // LookupMaps varNameMap,
                    SymTab globalSymTab,
                    SymTab curSymTab,
                    Hypothesis hypothesis,
                    HashSet<String> principalSet,
                    ContractSym curContractSym,
                    HashMap<String, Program> programMap
                    // HashMap<ExceptionTypeSym, PsiUnit> psi
                    //HashMap<String, SigCons> sigConsMap
                    /*HashMap<String, ContractInfo> contractMap*/) {
        // this.ctxt = ctxt;
        this.inContext = inContext;
        // this.outContext = outContext;
        // this.funcMap = funcMap;
        this.cons = cons;
        this.trustCons = trustCons;
        // this.varNameMap = varNameMap;
        this.globalSymTab = globalSymTab;
        this.curSymTab = curSymTab;
        this.hypothesis = hypothesis;
        this.principalSet = principalSet;
        this.curContractSym = curContractSym;
        this.programMap = programMap;
        // this.psi = psi;
        // this.contractMap = contractMap;
        // this.sigConsMap = sigConsMap;
    }

    /*public VisitEnv() {
        // ctxt = null;
        inContext = new Context();
        outContext = new PsiUnit();
        // funcMap = new HashMap<>();
        cons = new ArrayList<>();
        trustCons = new ArrayList<>();
        globalSymTab = new SymTab();
        curSymTab = globalSymTab;
        // varNameMap = new LookupMaps();
        hypothesis = new Hypothesis();
        principalSet = new HashSet<>();
        curContractSym = null;
        programMap = new HashMap<>();
        psi = new HashMap<>();
        // contractMap = new HashMap<>();
        //sigConsMap = new HashMap<>();
    }*/

    public void addVar(String id, VarSym varSym) {
        curContractSym.addVar(id, varSym);
    }

    public VarSym getVar(String id) {
        Sym sym = curSymTab.lookup(id);
        if (sym instanceof VarSym)
            return (VarSym) sym;
        else
            return null;
    }

    public ContractSym getContract(String id) {
        Sym sym = curSymTab.lookup(id);
        if (sym instanceof ContractSym)
            return (ContractSym) sym;
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

    public void incScopeLayer() {
        curSymTab = new SymTab(curSymTab);
    }

    public void decScopeLayer() {
        curSymTab = curSymTab.getParent();
    }

    public boolean containsVar(String id) {
        return getVar(id) != null;
    }

    /*public void addSigCons(String contractName, ArrayList<Constraint> trustCons, ArrayList<Constraint> cons) {
        SigCons sigCons = new SigCons(contractName, trustCons, cons);
        sigConsMap.put(contractName, sigCons);
    }*/

    /*public SigCons getSigCons(String contractName) {
        return sigConsMap.get(contractName);
    }*/

    public void addSigReq(String namespace, String name) {
        sigReq.put(namespace, name);
    }

    public Sym getCurSym(String name) {
        return curSymTab.lookup(name);
    }

    public Sym getExtSym(String contractName, String funcName) {
        Sym extST = globalSymTab.lookup(contractName);
        if (extST == null) return null;
        return ((ContractSym) extST).lookupSym(funcName);
    }

    public ExceptionTypeSym toExceptionTypeSym(ExceptionType t) {
        if (t.isLocal(curContractSym.name)) {
            return (ExceptionTypeSym) getCurSym(t.getName());
        } else {
            return (ExceptionTypeSym) getExtSym(t.getContractName(), t.getName());
        }
    }

    public ExceptionTypeSym getExp(String name) {
        Sym sym = curSymTab.lookup(name);
        if (sym instanceof ExceptionTypeSym)
            return (ExceptionTypeSym) sym;
        else
            return null;
    }
}
