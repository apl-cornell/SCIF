package typecheck;

import java.util.HashMap;

public class SymTab {
    SymTab parent;
    HashMap<String, Sym> table;
    public SymTab() {
        parent = null;
        table = new HashMap<>();
    }
    public SymTab(SymTab parent) {
        this.parent = parent;
        table = new HashMap<>();
    }

    public Sym lookup(String id) {
        if (table.get(id) != null) return table.get(id);
        return parent.lookup(id);
    }
    public void add(String id, Sym sym) {
        table.put(id, sym);
    }

    public SymTab getParent() {
        return parent;
    }
}
