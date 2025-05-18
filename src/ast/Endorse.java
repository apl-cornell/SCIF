package ast;

import compile.CompileEnv;
import compile.ast.Statement;

import java.util.List;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
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

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        return value.genTypeConstraints(env, parent);
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position) {
        //TODO: change to conditioned form
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        ExpOutcome vo = value.genIFConstraints(env, tail_position);
        String ifNameValue = vo.valueLabelName;
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "endorse" + location.toString();

        String fromLabel = fromNameSLC();
                //from.toSherrlocFmt(scopeContext);
        String toLabel = toNameSLC();
                //to.toSherrlocFmt(scopeContext);

        env.cons.add(
                new Constraint(new Inequality(ifNameValue, fromLabel), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "The expression must be trusted to be endorsed"));

        env.cons.add(new Constraint(new Inequality(toLabel, ifNameRtn), env.hypothesis(), location,
                env.curContractSym().getName(),
                "The integrity level of this expression would be endorsed"));

        //env.outContext = endContext;

        env.addTrustConstraint(
                new Constraint(new Inequality(beginContext.pc, endContext.pc), env.hypothesis(),
                        location, env.curContractSym().getName(),
                        "The control flow of this expression would be endorsed"));

        return new ExpOutcome(ifNameRtn, vo.psi);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        code.countEndorse();
        return value.solidityCodeGen(result, code);
    }

    private String fromNameSLC() {
        return scopeContext + ".endorse" + "." + "from" + from.location;
    }
    private String toNameSLC() {
        return scopeContext + ".endorse" + "." + "to" + from.location;
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

    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        return value.readMap(code);
    }
}
