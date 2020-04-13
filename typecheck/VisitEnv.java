package typecheck;

import ast.TrustConstraint;
import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class VisitEnv {
    public ScopeContext ctxt;
    public Context prevContext;
    // public HashMap<String, FuncInfo> funcMap;
    public ArrayList<Constraint> cons;
    public ArrayList<Constraint> trustCons;
    // public LookupMaps varNameMap;
    public SymTab globalSymTab;
    public SymTab curSymTab;
    public Hypothesis hypothesis;
    public HashSet<String> principalSet; // TODO: better imp
    public ContractInfo curContractInfo;
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
                    ContractInfo curContractInfo
                    /*HashMap<String, ContractInfo> contractMap*/) {
        this.ctxt = ctxt;
        this.prevContext = prevContext;
        // this.funcMap = funcMap;
        this.cons = cons;
        this.trustCons = trustCons;
        // this.varNameMap = varNameMap;
        this.globalSymTab = globalSymTab;
        this.curSymTab = curSymTab;
        this.hypothesis = hypothesis;
        this.principalSet = principalSet;
        this.curContractInfo = curContractInfo;
        // this.contractMap = contractMap;
    }

    public VisitEnv() {
        ctxt = null;
        prevContext = new Context();
        // funcMap = new HashMap<>();
        cons = new ArrayList<>();
        trustCons = new ArrayList<>();
        globalSymTab = new SymTab();
        curSymTab = globalSymTab;
        // varNameMap = new LookupMaps();
        hypothesis = new Hypothesis();
        principalSet = new HashSet<>();
        curContractInfo = null;
        // contractMap = new HashMap<>();
    }
}
