package typecheck;

import ast.TrustConstraint;
import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class VisitEnv {
    public String ctxt;
    public Context prevContext;
    public HashMap<String, FuncInfo> funcMap;
    public ArrayList<Constraint> cons;
    public ArrayList<Constraint> trustCons;
    public LookupMaps varNameMap;
    public Hypothesis hypothesis;
    public HashSet<String> principalSet;
    public ContractInfo contractInfo;
    public HashMap<String, ContractInfo> contractMap;


    public VisitEnv(String ctxt, Context prevContext,
                    HashMap<String, FuncInfo> funcMap,
                    ArrayList<Constraint> cons,
                    ArrayList<Constraint> trustCons,
                    LookupMaps varNameMap, Hypothesis hypothesis,
                    HashSet<String> principalSet,
                    ContractInfo contractInfo, HashMap<String, ContractInfo> contractMap) {
        this.ctxt = ctxt;
        this.prevContext = prevContext;
        this.funcMap = funcMap;
        this.cons = cons;
        this.trustCons = trustCons;
        this.varNameMap = varNameMap;
        this.hypothesis = hypothesis;
        this.principalSet = principalSet;
        this.contractInfo = contractInfo;
        this.contractMap = contractMap;
    }

    public VisitEnv() {
        ctxt = "";
        prevContext = new Context();
        funcMap = new HashMap<>();
        cons = new ArrayList<>();
        trustCons = new ArrayList<>();
        varNameMap = new LookupMaps();
        hypothesis = new Hypothesis();
        principalSet = new HashSet<>();
        contractInfo = null;
        contractMap = new HashMap<>();
    }
}
