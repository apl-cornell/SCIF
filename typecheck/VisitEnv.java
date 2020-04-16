package typecheck;

import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashSet;

public class VisitEnv {
    // public ScopeContext ctxt;
    public Context prevContext;
    // public HashMap<String, FuncInfo> funcMap;
    public ArrayList<Constraint> cons;
    public ArrayList<Constraint> trustCons;
    // public LookupMaps varNameMap;
    public SymTab globalSymTab;
    public SymTab curSymTab;
    public Hypothesis hypothesis;
    public HashSet<String> principalSet; // TODO: better imp
    public ContractSym curContractSym;
    // public HashMap<String, ContractInfo> contractMap;


    public VisitEnv(ScopeContext ctxt, Context prevContext,
                    // HashMap<String, FuncInfo> funcMap,
                    ArrayList<Constraint> cons,
                    ArrayList<Constraint> trustCons,
                    // LookupMaps varNameMap,
                    SymTab globalSymTab,
                    SymTab curSymTab,
                    Hypothesis hypothesis,
                    HashSet<String> principalSet,
                    ContractSym curContractSym
                    /*HashMap<String, ContractInfo> contractMap*/) {
        // this.ctxt = ctxt;
        this.prevContext = prevContext;
        // this.funcMap = funcMap;
        this.cons = cons;
        this.trustCons = trustCons;
        // this.varNameMap = varNameMap;
        this.globalSymTab = globalSymTab;
        this.curSymTab = curSymTab;
        this.hypothesis = hypothesis;
        this.principalSet = principalSet;
        this.curContractSym = curContractSym;
        // this.contractMap = contractMap;
    }

    public VisitEnv() {
        // ctxt = null;
        prevContext = new Context();
        // funcMap = new HashMap<>();
        cons = new ArrayList<>();
        trustCons = new ArrayList<>();
        globalSymTab = new SymTab();
        curSymTab = globalSymTab;
        // varNameMap = new LookupMaps();
        hypothesis = new Hypothesis();
        principalSet = new HashSet<>();
        curContractSym = null;
        // contractMap = new HashMap<>();
    }

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
}
