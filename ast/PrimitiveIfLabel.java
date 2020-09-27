package ast;

import typecheck.Utils;

import java.util.ArrayList;
import java.util.HashSet;

public class PrimitiveIfLabel extends IfLabel {
    Name value; // could be a name
    public PrimitiveIfLabel(Name value) {
        this.value = value;
    }

    @Override
    public String toSherrlocFmt() {
        String rnt = "";
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (name.equals(Utils.BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else {
                rnt = name;
            }
        }
        return rnt;
    }

    public String toSherrlocFmt(String k, String v) {
        String rnt = "";
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (name.equals(Utils.BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.TOP)) {
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
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (name.equals(Utils.BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else if (strSet.contains(name)) {
                rnt = name + ".apply" + no;
            } else {
                rnt = name;
            }
        }
        return rnt;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (!name.equals(Utils.TOP) && !name.equals(Utils.BOTTOM)) {
                principalSet.add(name);
            }
        }
    }

    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (!name.equals(Utils.TOP) && !name.equals(Utils.BOTTOM) && !name.equals(getRidOf)) {
                principalSet.add(name);
            }
        }
    }

    @Override
    public boolean typeMatch(IfLabel begin_pc) {
        if (!(begin_pc instanceof PrimitiveIfLabel))
            return false;
        return value.typeMatch(((PrimitiveIfLabel) begin_pc).value);
    }

    public void replace(String k, String v) {
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (name.equals(k))
                ((Name) value).id = v;
        }
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        return rtn;
    }
}
