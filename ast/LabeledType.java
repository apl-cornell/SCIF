package ast;

import java.util.ArrayList;
import java.util.HashSet;

public class LabeledType extends Type {
    public IfLabel ifl;
    public LabeledType(String x, IfLabel ifl) {
        super(x);
        this.ifl = ifl;
    }

    public String toSherrloc(String k, String v) {
        return "";
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        ifl.findPrincipal(principalSet);
    }


    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
        ifl.findPrincipal(principalSet, getRidOf);
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
}
