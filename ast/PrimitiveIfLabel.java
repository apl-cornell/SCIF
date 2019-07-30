package ast;

import utils.CodeLocation;
import utils.Utils;

import java.util.HashSet;

public class PrimitiveIfLabel extends IfLabel {
    Variable value; // could be a name
    public PrimitiveIfLabel(Variable value) {
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

    public void findPrincipal(HashSet<String> principalSet) {
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (!name.equals(Utils.TOP) && !name.equals(Utils.BOTTOM)) {
                principalSet.add(name);
            }
        }
    }

    public void replace(String k, String v) {
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (name.equals(k))
                ((Name) value).id = v;
        }
    }
}
