package ast;

import compile.SolCode;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class Contract extends Node {
    public String contractName;
    public ArrayList<TrustConstraint> trustCons;
    ArrayList<Statement> body;
    public Contract(String contractName, ArrayList<TrustConstraint> trustCons, ArrayList<Statement> body) {
        this.contractName = contractName;
        this.trustCons = trustCons;
        this.body = body;
    }

    @Override
    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        for (Statement stmt : body) {
            if (!stmt.NTCGlobalInfo(env, now)) return false;
        }
        ContractSym contractSym = new ContractSym(contractName, env.globalSymTab, trustCons);
        env.addSym(contractName, contractSym);
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
        for (Node node : children())
            node.passScopeContext(scopeContext);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(trustCons);
        rtn.addAll(body);
        return rtn;
    }
}
