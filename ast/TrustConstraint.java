package ast;

import sherrlocUtils.Relation;

import java.util.ArrayList;
import java.util.HashSet;

public class TrustConstraint extends Node {
    public IfLabel lhs, rhs;
    public Relation optor;
    public TrustConstraint(IfLabel lhs, Relation optor, IfLabel rhs) {
        this.lhs = lhs;
        this.optor = optor;
        this.rhs = rhs;
    }
    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        lhs.findPrincipal(principalSet);
        rhs.findPrincipal(principalSet);
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(lhs);
        rtn.add(rhs);
        return rtn;
    }
}
