package typecheck;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
    variables, types, methods/exceptions, contracts
*/

public class SymTab {
    SymTab parent;
    HashMap<String, Sym> table;
    public SymTab() {
        parent = null;
        table = new HashMap<>();
        //Utils.addBuiltInASTNode(table);
    }
    public SymTab(SymTab parent) {
        this.parent = parent;
        table = new HashMap<>();
    }

    public Sym lookup(String id) {
        // System.out.println("sym lookup: " + id + " @" + this);
        if (table.get(id) != null) {
            // System.out.println("SUCC");

            return table.get(id);
        }
        return parent == null ? null : parent.lookup(id);
    }
    public void add(String id, Sym sym) {
        if (table.containsKey(id)) {
            throw new RuntimeException("SymTab adding a symbol that existed: " + id);
        }
        table.put(id, sym);
    }

    public SymTab getParent() {
        return parent;
    }

    public Set<Sym> getTypeSet() {
        Set<Sym> rtn = new HashSet<>();
        for (Sym sym : table.values()) {
            if (sym instanceof TypeSym) {
                rtn.add(sym);
            }
        }
        return rtn;
    }

    public Map<String, VarSym> getVars() {
        Map<String, VarSym> rtn = new HashMap<>();
        for (Sym sym : table.values()) {
            if (sym instanceof VarSym)
                rtn.put(sym.getName(), (VarSym) sym);
        }
        return rtn;
    }

    public Map<String, FuncSym> getFuncs() {
        Map<String, FuncSym> rtn = new HashMap<>();
        for (Sym sym : table.values()) {
            if (sym instanceof FuncSym)
                rtn.put(sym.getName(), (FuncSym) sym);
        }
        return rtn;
    }

    public Map<String, FuncSym> getAllFuncs() {
        if (parent == null) {
            return getFuncs();
        } else {
            Map<String, FuncSym> rtn = parent.getAllFuncs();
            rtn.putAll(getFuncs());
            return rtn;
        }
    }

    public void setParent(SymTab parent) {
        this.parent = parent;
    }
}
