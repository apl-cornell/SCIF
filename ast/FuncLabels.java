package ast;

import java.util.HashSet;

public class FuncLabels extends Node {
    public IfLabel begin_pc, begin_lock, end_lock;

    public FuncLabels() {
        begin_pc = null;
        begin_lock = null;
        end_lock = null;
    }

    public FuncLabels(IfLabel begin_pc, IfLabel begin_lock, IfLabel end_lock) {
        this.begin_pc = begin_pc;
        this.begin_lock = begin_lock;
        this.end_lock = end_lock;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        begin_pc.findPrincipal(principalSet);
        begin_lock.findPrincipal(principalSet);
        end_lock.findPrincipal(principalSet);
    }

}
