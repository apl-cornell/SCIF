package ast;

import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.SymTab;
import typecheck.VarSym;

import java.util.ArrayList;

public class ExceptHandler extends NonFirstLayerStatement {
    ExceptionType type;
    String name;
    ArrayList<Statement> body;
    public ExceptHandler(ExceptionType type, String name, ArrayList<Statement> body) {
        this.type = type;
        this.name = name;
        this.body = body;
    }
    public void setBody(ArrayList<Statement> body) {
        this.body = body;
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.curSymTab = new SymTab(env.curSymTab);

        env.addSym(name, new VarSym(env.toVarSym(name, type, true, true, location, now)));

        for (Statement s : body) {
            ScopeContext tmp = s.NTCgenCons(env, now);
            now.mergeExceptions(tmp);
        }
        env.curSymTab = env.curSymTab.getParent();
        return now;
    }
}
