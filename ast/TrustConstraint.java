package ast;

import compile.SolCode;
import typecheck.ContractSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.sherrlocUtils.Relation;

import java.util.ArrayList;
import java.util.HashSet;

public class TrustConstraint extends TopLayerNode {

    public TrustAtom lhs, rhs;
    public Relation optor;

    public TrustConstraint(TrustAtom lhs, Relation optor, TrustAtom rhs) {
        this.lhs = lhs;
        this.optor = optor;
        this.rhs = rhs;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        lhs.findPrincipal(principalSet);
        rhs.findPrincipal(principalSet);
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(lhs);
        rtn.add(rhs);
        return rtn;
    }
}
