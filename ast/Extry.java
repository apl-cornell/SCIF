package ast;

import java.util.List;
import typecheck.*;

import java.util.ArrayList;

public class Extry extends Try {

    public Extry(List<Statement> body,
            List<ExceptHandler> handlers) {//, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        super(body, handlers);
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
            now.addException(env.toExceptionTypeSym(h.type), true);
        }

        for (Statement s : body) {
            tmp = s.ntcGenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();

        for (ExceptHandler h : handlers) {
            tmp = h.ntcGenCons(env, parent);
        }
        return now;
    }
}
