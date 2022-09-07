package ast;

import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VisitEnv;

import java.util.ArrayList;
import java.util.HashSet;

public class PrimitiveIfLabel extends IfLabel {

    Name value;

    public PrimitiveIfLabel(Name value) {
        this.value = value;
    }

    @Override
    public String toSherrlocFmt() {
        String rnt = "";
        if (value != null) {
            String name = value.id;
            if (name.equals(Utils.LABEL_BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.LABEL_TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else {
                rnt = name;
            }
        }
        return rnt;
    }

    public String toSherrlocFmt(String namespace) {
        String rnt = "";
        if (value != null) {
            String name = value.id;
            if (name.equals(Utils.LABEL_BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.LABEL_TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else {
                if (namespace != "") {
                    namespace += "..";
                }
                rnt = namespace + name;
            }
        }
        return rnt;
    }

    public String toSherrlocFmt(String k, String v) {
        String rnt = "";
        if (value != null) {
            String name = value.id;
            if (name.equals(Utils.LABEL_BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.LABEL_TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else if (name.equals(k)) {
                rnt = v;
            } else {
                rnt = name;
            }
        }
        return rnt;
    }

    public String toSherrlocFmtApply(HashSet<String> strSet, int no) {
        String rnt = "";
        if (value != null) {
            String name = value.id;
            if (name.equals(Utils.LABEL_BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.LABEL_TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else if (strSet.contains(name)) {
                rnt = name + ".apply" + no;
            } else {
                rnt = name;
            }
        }
        return rnt;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        if (value != null) {
            String name = value.id;
            if (!name.equals(Utils.LABEL_TOP) && !name.equals(Utils.LABEL_BOTTOM)) {
                principalSet.add(name);
            }
        }
    }

    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
        if (value != null) {
            String name = value.id;
            if (!name.equals(Utils.LABEL_TOP) && !name.equals(Utils.LABEL_BOTTOM) && !name.equals(
                    getRidOf)) {
                principalSet.add(name);
            }
        }
    }

    @Override
    public boolean typeMatch(IfLabel begin_pc) {
        if (!(begin_pc instanceof PrimitiveIfLabel)) {
            return false;
        }
        return value.typeMatch(((PrimitiveIfLabel) begin_pc).value);
    }

    public void replace(String k, String v) {
        if (value != null) {
            String name = value.id;
            if (name.equals(k)) {
                value.id = v;
            }
        }
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        return rtn;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof PrimitiveIfLabel &&
                typeMatch((IfLabel) expression);
    }

    public String toString() {
        return value.id;
    }
}
