package ast;

import compile.SolCode;
import typecheck.Assumption;
import typecheck.ContractSym;
import typecheck.NTCEnv;
import typecheck.PrimitiveLabel;
import typecheck.ScopeContext;
import typecheck.sherrlocUtils.Relation;

import java.util.ArrayList;
import java.util.HashSet;

public class TrustConstraint extends TopLayerNode {

    private PrimitiveIfLabel lhs, rhs;
    private Relation optor;

    public TrustConstraint(PrimitiveIfLabel lhs, Relation optor, PrimitiveIfLabel rhs) {
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

//    public void findPrincipal(HashSet<String> principalSet) {
//        lhs.findPrincipal(principalSet);
//        rhs.findPrincipal(principalSet);
//    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        lhs.ntcGenCons(env, parent);
        rhs.ntcGenCons(env, parent);
        return parent;
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

    public Assumption toAssumption(ContractSym contractSym) {
        return new Assumption((PrimitiveLabel) contractSym.toLabel(lhs),
                optor,
                (PrimitiveLabel) contractSym.toLabel(rhs),
                location);
    }
}
