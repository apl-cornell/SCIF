package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
import typecheck.*;

public class ExceptHandler extends Statement {

    private LabeledType labeledType;
    private String name;
    private List<Statement> body;

    public ExceptHandler(LabeledType type, String name, List<Statement> body) {
        this.labeledType = type;
        this.name = name;
        this.body = body;
    }

    public void setBody(List<Statement> body) {
        this.body = body;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();

        VarSym var = env.newVarSym(name, labeledType, true, true, true, location, now);
        if (var == null) {
            System.err.println("Exception type " + labeledType.type().name() + " not found");
            throw new RuntimeException();
        }
        env.addSym(name, var);

        for (Statement s : body) {
            ScopeContext tmp = s.ntcGenCons(env, now);
        }
        env.exitNewScope();
        return now;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(labeledType);
        rtn.addAll(body);
        return rtn;
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

    public Type type() {
        return labeledType.type();
    }

    public String name() {
        return name;
    }


}
