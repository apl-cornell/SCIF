package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Try extends NonFirstLayerStatement {
    ArrayList<Statement> body;
    ArrayList<ExceptHandler> handlers;
    // ArrayList<Statement> orelse;
    // ArrayList<Statement> finalbody;
    public Try(ArrayList<Statement> body, ArrayList<ExceptHandler> handlers) {//, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        this.body = body;
        this.handlers = handlers;
        // this.orelse = orelse;
        // this.finalbody = finalbody;
    }


    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);
        ScopeContext rtn = null;

        // ScopeContext tryclause = new ScopeContext(this, now);
        ScopeContext tmp;

        for (ExceptHandler h : handlers) {
            now.addException(env.toExceptionTypeSym(h.type), false);
        }

        for (Statement s : body) {
            tmp = s.NTCgenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();

        for (ExceptHandler h : handlers) {
            tmp = h.NTCgenCons(env, parent);
        }
        return now;
    }
}
