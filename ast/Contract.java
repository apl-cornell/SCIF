package ast;

import compile.SolCode;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Contract extends Node {
    public String contractName;
    public String superContractName = "";
    public TrustSetting trustSetting;
    // public ArrayList<TrustConstraint> trustCons;
    public IfLabel ifl;
    ArrayList<Statement> body;
    public Contract(String contractName, TrustSetting trustSetting, ArrayList<Statement> body, IfLabel ifl) {
        this.contractName = contractName;
        this.trustSetting = trustSetting;
        this.body = body;
        this.ifl = ifl;
    }
    public Contract(String contractName, String superContractName, TrustSetting trustSetting, ArrayList<Statement> body, IfLabel ifl) {
        this.contractName = contractName;
        this.superContractName = superContractName;
        this.trustSetting = trustSetting;
        this.body = body;
        this.ifl = ifl;
    }

    public boolean NTCinherit(InheritGraph graph) {
        // add an edge from superclass to this contract
        if (!superContractName.equals(""))
            graph.addEdge(superContractName, contractName);
        return true;
    }

    @Override
    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.setCurSymTab(new SymTab(env.curSymTab));
        ContractSym contractSym = new ContractSym(contractName, env.curSymTab, trustSetting, ifl);
        env.addGlobalSym(contractName, contractSym);

        for (Statement stmt : body) {
            if (!stmt.NTCGlobalInfo(env, now)) return false;
        }
        env.curSymTab = env.curSymTab.getParent();
        return true;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        for (Statement stmt : body) {
            stmt.NTCgenCons(env, now);
        }
        return now;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        contractSym.name = contractName;
        contractSym.trustSetting = trustSetting;
        contractSym.ifl = ifl;
        contractSym.addContract(contractName, contractSym);
        for (Statement stmt : body) {
            stmt.globalInfoVisit(contractSym);
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        //env.prevContext = new Context()
        findPrincipal(env.principalSet);
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (TrustConstraint trustConstraint : trustSetting.trust_list) {
            trustConstraint.findPrincipal(principalSet);
        }
        ifl.findPrincipal(principalSet);
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
    }

    public void SolCodeGen(SolCode code) {
        code.setDynamicOption(trustSetting);
        code.enterContractDef(contractName);
        for (Statement stmt : body) {
            stmt.SolCodeGen(code);
        }
        code.leaveContractDef();
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(trustSetting.trust_list);
        rtn.addAll(body);
        return rtn;
    }

    public boolean codePasteContract(HashMap<String, Contract> contractMap) {
        if (superContractName.equals(""))
            return true;

        // check no functions with the same name
        // add other functions from superContract
        // trust_list is also inherited
        Contract superContract = contractMap.get(superContractName);
        if (superContract == null) {
            // TODO: superContract not found
            return  false;
        }

        // inherit from superContract

        // trust_list
        ArrayList<TrustConstraint> newTrustCons = new ArrayList<>();
        newTrustCons.addAll(superContract.trustSetting.trust_list);
        newTrustCons.addAll(trustSetting.trust_list);
        trustSetting.trust_list = newTrustCons;

        // Statement
        HashMap<String, AnnAssign> varNames = new HashMap<>();
        HashMap<String, FunctionSig> funcNames = new HashMap<>();
        for (Statement statement : body) {
            if (statement instanceof FunctionSig) {
                FunctionSig f = (FunctionSig) statement;
                if (varNames.containsKey(f.name) || funcNames.containsKey(f.name)) {
                    //TODO: duplicate names
                    return false;
                }
                funcNames.put(f.name, f);
            } else {
                AnnAssign a = (AnnAssign) statement;
                Name x = (Name) a.target;
                if (varNames.containsKey(x.id) || funcNames.containsKey(x.id)) {
                    //TODO: duplicate names
                    return false;
                }
                varNames.put(x.id, a);
            }
        }

        ArrayList<Statement> newBody = new ArrayList<>();
        for (Statement statement : superContract.body) {
            // check if duplicate names
            // check if they match exactly
            boolean overriden = false;
            if (statement instanceof FunctionSig) {
                FunctionSig f = (FunctionSig) statement;
                if (funcNames.containsKey(f.name)) {
                    if (!funcNames.get(f.name).typeMatch(f)) {
                        // TODO: func overriden type not match
                        return false;
                    }
                    overriden = true;
                } else if (varNames.containsKey(f.name)) {
                    // TODO: var overriden by func
                    return false;
                }
            } else {
                AnnAssign a = (AnnAssign) statement;
                Name x = (Name) a.target;
                if (varNames.containsKey(x.id)) {
                    // TODO: var being overriden
                    return false;
                } else if (funcNames.containsKey(x.id)) {
                    // TODO: func overriden by var
                    return false;
                }
            }

            if (!overriden) {
                newBody.add(statement);
            }
        }
        newBody.addAll(body);
        body = newBody;

        return true;
    }
}
