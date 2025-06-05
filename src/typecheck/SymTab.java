package typecheck;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
    variables, types, methods/exceptions, contracts
*/

public class SymTab {
    SymTab parent;
    Map<String, Sym> table;
    public SymTab() {
        parent = null;
        table = new HashMap<>();
        //Utils.addBuiltInASTNode(table);
    }
    public SymTab(SymTab parent) {
        this.parent = parent;
        table = new HashMap<>();
    }
    public static class AlreadyDefined extends Exception {
        public final String id;
        private AlreadyDefined(String id) {
            this.id = id;
        }
        @Override public String getMessage() {
            return "Identifier not found: " + id;
        }
    }

    public Sym lookup(String id) {
        if (table.get(id) != null) {
            return table.get(id);
        }
        return parent == null ? null : parent.lookup(id);
    }

    public void add(String id, Sym sym) throws AlreadyDefined {
        if (table.containsKey(id)) {
            throw new AlreadyDefined(id);
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

    public Map<String, ExceptionTypeSym> getExceptionMap() {
        Map<String, ExceptionTypeSym> result = parent == null ? new HashMap<>() : parent.getExceptionMap();
        for (Entry<String, Sym> entry: table.entrySet()) {
            if (entry.getValue() instanceof ExceptionTypeSym value) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    public Map<String, EventTypeSym> getEventMap() {
        Map<String, EventTypeSym> result = parent == null ? new HashMap<>() : parent.getEventMap();
        for (Entry<String, Sym> entry : table.entrySet()) {
            if (entry.getValue() instanceof EventTypeSym value) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }
}
