package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

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
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "endorse" + location.toString();

        String fromLabel = from.toSherrlocFmt();
        String toLabel = to.toSherrlocFmt();

        env.cons.add(new Constraint(new Inequality(ifNameValue, fromLabel), env.hypothesis, location, env.curContractSym.name,
                "The expression must be trusted to be endorsed"));

        env.cons.add(new Constraint(new Inequality(ifNameRtn, toLabel), env.hypothesis, location, env.curContractSym.name,
                "The integrity level of this expression would be endorsed"));

        return new Context(ifNameRtn, tmp.lockName);
    }

    @Override
    public String toSolCode() {
        return value.toSolCode();
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Endorse &&
                value.typeMatch(((Endorse) expression).value) &&
                from.typeMatch(((Endorse) expression).from) &&
                to.typeMatch(((Endorse) expression).to);
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
