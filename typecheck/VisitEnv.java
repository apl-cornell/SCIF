package typecheck;

import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class VisitEnv {
    public String ctxt;
    public HashMap<String, FuncInfo> funcMap;
    public ArrayList<Constraint> cons;
    public LookupMaps varNameMap;
    public Hypothesis hypothesis;
    public HashSet<String> principalSet;

    public VisitEnv(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<Constraint> cons, LookupMaps varNameMap, Hypothesis hypothesis, HashSet<String> principalSet) {
        this.ctxt = ctxt;
        this.funcMap = funcMap;
        this.cons = cons;
        this.varNameMap = varNameMap;
        this.hypothesis = hypothesis;
        this.principalSet = principalSet;
    }

    public VisitEnv() {
        ctxt = "";
        funcMap = new HashMap<>();
        cons = new ArrayList<>();
        varNameMap = new LookupMaps();
        hypothesis = new Hypothesis();
        principalSet = new HashSet<>();
    }
}
