package ast;

import compile.SolCode;
import java.util.List;
import typecheck.*;

import java.util.ArrayList;

public class ExceptHandler extends Statement {

    ExceptionType type;
    String name;
    List<Statement> body;

    public ExceptHandler(ExceptionType type, String name, List<Statement> body) {
        this.type = type;
        this.name = name;
        this.body = body;
    }

    public void setBody(List<Statement> body) {
        this.body = body;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.setCurSymTab(new SymTab(env.curSymTab()));

        VarSym var = env.toVarSym(name, type, true, true, location, now);
        if (var == null) {
            System.err.println("Exception type " + type.getName() + " not found");
            throw new RuntimeException();
        }
        env.addSym(name, var);

        for (Statement s : body) {
            ScopeContext tmp = s.ntcGenCons(env, now);
        }
        env.setCurSymTab(env.curSymTab().getParent());
        return now;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    public String getHandlerPcLabelName() {
        return scopeContext.getSHErrLocName() + "." + "handlerPcLabelName" + location.toString();
    }

    public String getHandlerLockLabelName() {
        return scopeContext.getSHErrLocName() + "." + "handlerLockLabelName" + location.toString();
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}
