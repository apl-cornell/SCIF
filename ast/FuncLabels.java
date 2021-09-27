package ast;

import typecheck.Utils;

import java.util.ArrayList;
import java.util.HashSet;

public class FuncLabels extends Node {
    public IfLabel begin_pc, to_pc, gamma_label;

    public FuncLabels() {
        begin_pc = null;
        to_pc = null;
        gamma_label = null;
    }

    public FuncLabels(IfLabel begin_pc, IfLabel to_pc, IfLabel gamma_label) {
        this.begin_pc = begin_pc;
        this.to_pc = to_pc;
        this.gamma_label = gamma_label;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        if (begin_pc != null) begin_pc.findPrincipal(principalSet);
        if (to_pc != null) to_pc.findPrincipal(principalSet);
        if (gamma_label != null) gamma_label.findPrincipal(principalSet);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(begin_pc);
        rtn.add(to_pc);
        rtn.add(gamma_label);
        return rtn;
    }

    public boolean typeMatch(FuncLabels funcLabels) {
        boolean l1 = begin_pc == null, l2 = to_pc == null, l3 = gamma_label == null;
        boolean r1 = funcLabels.begin_pc == null, r2 = funcLabels.to_pc == null, r3 = funcLabels.gamma_label == null;
        return ((l1 && r1) || (!(l1 || r1) && begin_pc.typeMatch(funcLabels.begin_pc)))
                && ((l2 && r2) || (!(l2 || r2) && to_pc.typeMatch(funcLabels.to_pc)))
                && ((l3 && r3) || (!(l3 || r3) && gamma_label.typeMatch(funcLabels.gamma_label)));
    }

    public void setToDefault(boolean isConstructor, ArrayList<String> decoratorList) {
        IfLabel thisLbl = new PrimitiveIfLabel(new Name(Utils.LABEL_THIS));
        if (isConstructor) {
            begin_pc = to_pc = gamma_label = thisLbl;
        } else {
            if (gamma_label != null) return;
            else {
                if (to_pc != null) {
                    gamma_label = to_pc;
                } else if (begin_pc != null) {
                    gamma_label = to_pc = begin_pc;
                } else {
                    boolean isPublic = false;
                    for (String decorator : decoratorList)
                        if (decorator.equals(Utils.PUBLIC_DECORATOR)) {
                            isPublic = true;
                            break;
                        }
                    if (isPublic) {
                        IfLabel botLbl = new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM));
                        begin_pc = to_pc = gamma_label = botLbl;
                    } else {
                        begin_pc = to_pc = gamma_label = thisLbl;
                    }
                }
            }
        }
    }
}
