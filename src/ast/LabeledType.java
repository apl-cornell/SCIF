package ast;

import java.util.ArrayList;
import java.util.HashSet;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

public class LabeledType extends Type {

    public IfLabel ifl;

    public LabeledType(String x, IfLabel ifl) {
        super(x);
        this.ifl = ifl;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(ifl);
        return rtn;
    }

    public boolean typeMatch(Type annotation) {
        return annotation instanceof LabeledType &&
                super.typeMatch(annotation) &&
                ifl.typeMatch(((LabeledType) annotation).ifl);
    }

    public void setToDefault(IfLabel lbl) {
        if (this.ifl == null) {
            this.ifl = lbl;
        }
    }
    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ifl.ntcGenCons(env, parent);
        return now;
    }
}
