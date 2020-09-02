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
            System.out.println("SUCC");

            return table.get(id);
        }
        return parent == null ? null : parent.lookup(id);
    }
    public void add(String id, Sym sym) {
        System.out.println("sym add: " + id + " @" + this);
        System.out.println(sym.name);
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

    public HashMap<String, VarSym> getVars() {
        HashMap<String, VarSym> rtn = new HashMap<>();
        for (Sym sym : table.values()) {
            if (sym instanceof VarSym)
                rtn.put(sym.name, (VarSym) sym);
        }
        return rtn;
    }

    public HashMap<String, FuncSym> getFuncs() {
        HashMap<String, FuncSym> rtn = new HashMap<>();
        for (Sym sym : table.values()) {
            if (sym instanceof FuncSym)
                rtn.put(sym.name, (FuncSym) sym);
        }
        return rtn;
    }

    public void setParent(SymTab parent) {
        this.parent = parent;
    }
}
