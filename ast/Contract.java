package ast;

import compile.SolCode;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Contract extends Node {
    public String contractName;
    public String superContractName = "";
    public ArrayList<TrustConstraint> trustCons;
    ArrayList<Statement> body;
    public Contract(String contractName, ArrayList<TrustConstraint> trustCons, ArrayList<Statement> body) {
        this.contractName = contractName;
        this.trustCons = trustCons;
        this.body = body;
    }
    public Contract(String contractName, String superContractName, ArrayList<TrustConstraint> trustCons, ArrayList<Statement> body) {
        this.contractName = contractName;
        this.superContractName = superContractName;
        this.trustCons = trustCons;
        this.body = body;
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
        ContractSym contractSym = new ContractSym(contractName, env.curSymTab, trustCons);
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
        contractSym.trustCons = trustCons;
        contractSym.addContract(contractName, contractSym);
        for (Statement stmt : body) {
            stmt.globalInfoVisit(contractSym);
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        //env.prevContext = new Context()
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (TrustConstraint trustConstraint : trustCons) {
            trustConstraint.findPrincipal(principalSet);
        }
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
    }

    public void SolCodeGen(SolCode code) {
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
        rtn.addAll(trustCons);
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
        newTrustCons.addAll(superContract.trustCons);
        newTrustCons.addAll(trustCons);
        trustCons = newTrustCons;

        // Statement
        ArrayList<Statement> newBody = new ArrayList<>();
        newBody.addAll(superContract.body);
        newBody.addAll(body);
        body = newBody;

        return true;
    }
}
