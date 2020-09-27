package ast;

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
        begin_pc.findPrincipal(principalSet);
        to_pc.findPrincipal(principalSet);
        gamma_label.findPrincipal(principalSet);
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
        return begin_pc.typeMatch(funcLabels.begin_pc)
                && to_pc.typeMatch(funcLabels.to_pc)
                && gamma_label.typeMatch(funcLabels.gamma_label);
    }
}
