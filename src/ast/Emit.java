package ast;

import compile.CompileEnv;
import typecheck.*;
import typecheck.exceptions.SemanticException;
import compile.ast.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Emit extends Statement {

    private final Name event;
    private final List<Expression> args;

    /** A statement emitting the event named {@code event} with arguments {@code args}. */
    public Emit(Name event, List<Expression> args) {
        this.event = event;
        this.args = args;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        if (!(env.getCurSym(event.id) instanceof EventTypeSym eventTypeSym)) {
            throw new SemanticException("not an event: " + event.id, event.location);
        }
        CallArguments.genTypeConstraints(args,
                eventTypeSym.parameters().stream().map(p -> p.typeSym).toList(),
                "event " + eventTypeSym.getName(), env, now, location);
        return now;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        List<compile.ast.Expression> solArgs = new ArrayList<>();
        for (Expression arg : args) {
            solArgs.add(arg.solidityCodeGen(result, code));
        }
        result.add(new compile.ast.Emit(event.id, solArgs));
        return result;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        EventTypeSym eventTypeSym = (EventTypeSym) env.getCurSym(event.id);

        PathOutcome psi = new PathOutcome(new PsiUnit(beginContext));
        CallArguments.Evaluated evaluated = CallArguments.evaluate(args, env, psi);

        // Each argument is checked like an argument of a call.
        Map<String, String> mapping = new HashMap<>();
        // An argument holding an unknown principal is checked as if it held the most
        // trusted one.
        CallArguments.mapPrincipals(eventTypeSym.parameters(), evaluated,
                env.curContractSym().top().toSHErrLocFmt(), mapping);
        CallArguments.checkFlowInto(args, evaluated, env.inContext.pc,
                eventTypeSym.parameters().stream().map(p -> p.ifl).toList(), mapping,
                " of the event", env);

        Utils.contextFlow(env, env.inContext, endContext, location);
        if (!tail_position) {
            env.checkFlow(endContext.lambda, beginContext.lambda, location,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }
        psi.setNormalPath(endContext);
        return psi;
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(event);
        rtn.addAll(args);
        return rtn;
    }

    @Override
    protected Map<String, ? extends Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Expression arg : args) {
            result.putAll(arg.readMap(code));
        }
        return result;
    }
}
