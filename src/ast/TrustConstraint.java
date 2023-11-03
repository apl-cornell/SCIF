package ast;

import compile.CompileEnv;
import compile.ast.SolNode;
import typecheck.Assumption;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.PrimitiveLabel;
import typecheck.ScopeContext;
import typecheck.sherrlocUtils.Relation;

import java.util.ArrayList;

public class TrustConstraint extends TopLayerNode {

    private PrimitiveIfLabel lhs, rhs;
    private Relation optor;

    public TrustConstraint(PrimitiveIfLabel lhs, Relation optor, PrimitiveIfLabel rhs) {
        this.lhs = lhs;
        this.optor = optor;
        this.rhs = rhs;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        
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
//
//    @Override
//    public SolNode solidityCodeGen(CompileEnv code) {
//
//    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(lhs);
        rtn.add(rhs);
        return rtn;
    }

    public Assumption toAssumption(InterfaceSym contractSym) {
        return new Assumption((PrimitiveLabel) contractSym.newLabel(lhs),
                optor,
                (PrimitiveLabel) contractSym.newLabel(rhs),
                location);
    }
}
