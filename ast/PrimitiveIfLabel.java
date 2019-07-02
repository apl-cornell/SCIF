package ast;

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
    public void findPrincipal(HashSet<String> principalSet) {
        if (value instanceof Name) {
            String name = ((Name) value).id;
            if (!name.equals(Utils.TOP) && !name.equals(Utils.BOTTOM)) {
                principalSet.add(name);
            }
        }
    }
}
