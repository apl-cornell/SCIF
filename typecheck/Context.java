package typecheck;

import sherrlocUtils.Constraint;
import sherrlocUtils.Hypothesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Context {
    public String ctxt;
    public HashMap<String, FuncInfo> funcMap;
    public ArrayList<Constraint> cons;
    public LookupMaps varNameMap;
    public Hypothesis hypothesis;
    public HashSet<String> principalSet;
    public ContractInfo contractInfo;
    public HashMap<String, ContractInfo> contractMap;
}
