package typecheck;

import java.util.HashMap;
import java.util.HashSet;

/*
    variables, types, functions, contracts
*/

public class SymTab {
    SymTab parent;
    HashMap<String, Sym> table;
    public SymTab() {
        parent = null;
        table = new HashMap<>();
        //Utils.addBuiltInSyms(table);
    }
    public SymTab(SymTab parent) {
        this.parent = parent;
        table = new HashMap<>();
    }

    public Sym lookup(String id) {
        System.out.println("sym lookup: " + id + " @" + this);
        if (table.get(id) != null) {
            return table.get(id);
        }
        return parent.lookup(id);
    }
    public void add(String id, Sym sym) {
        System.out.println("sym add: " + id + " @" + this);
        table.put(id, sym);
    }

    public SymTab getParent() {
        return parent;
    }

    public HashSet<String> getTypeSet() {
        HashSet<String> rtn = new HashSet<>();
        for (Sym sym : table.values()) {
            rtn.add(sym.getSLCName());
        }
        return rtn;
    }
}
