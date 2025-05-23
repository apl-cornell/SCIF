package ast;

import java.util.ArrayList;
import java.util.List;
import typecheck.CodeLocation;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

public class LabeledType extends Node {

    private Type type;
    private IfLabel ifl;

    public LabeledType(Type x, IfLabel ifl) {
        this.type = x;
        this.ifl = ifl;
    }

    public LabeledType(Type x) {
        this.type = x;
    }
    public LabeledType(String x, IfLabel ifl, CodeLocation location) {
        this.type = new Type(x);
        this.type.setLoc(location);
        this.ifl = ifl;
        this.location = location;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(type);
        rtn.add(ifl);
        return rtn;
    }

    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        assert false;
        return null;
    }

    public boolean typeMatch(LabeledType annotation) {
        return annotation instanceof LabeledType &&
                type.typeMatch(((LabeledType) annotation).type) &&
                ifl.typeMatch(((LabeledType) annotation).ifl);
    }

    public void setToDefault(IfLabel lbl) {
        if (this.ifl == null) {
            this.ifl = lbl;
        }
        type.setToDefault(this.ifl);
    }
    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext rtn = type.genTypeConstraints(env, parent);
        if (ifl != null) ifl.genTypeConstraints(env, parent);
        return rtn;
    }

    public Type type() {
        return type;
    }

    public IfLabel label() {
        return ifl;
    }

    public String toSolCode() {
        return type.toSolCode();
    }
}
