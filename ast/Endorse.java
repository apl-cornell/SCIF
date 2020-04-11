package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;

public class Endorse extends Expression {
    Expression value;
    IfLabel from, to;

    public Endorse(Expression value, IfLabel from, IfLabel to) {
        this.value = value;
        this.from = from;
        this.to = to;
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        return value.NTCgenCons(env, parent);
    }
    @Override
    public Context genConsVisit(VisitEnv env) {
        Context tmp = value.genConsVisit(env);
        String ifNameValue = tmp.valueLabelName;
        String ifNameRtn = env.ctxt + "." + "endorse" + location.toString();

        String fromLabel = from.toSherrlocFmt();
        String toLabel = to.toSherrlocFmt();

        env.cons.add(new Constraint(new Inequality(ifNameValue, fromLabel), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(ifNameRtn, toLabel), env.hypothesis, location));

        return new Context(ifNameRtn, tmp.lockName);
    }

    @Override
    public String toSolCode() {
        return value.toSolCode();
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        rtn.add(from);
        rtn.add(to);
        return rtn;
    }
}
