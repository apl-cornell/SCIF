package ast;

import compile.CompileEnv;
import typecheck.*;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Relation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Emit extends Statement {

    Call event;

    public Emit(Call event) {
        this.event = event;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        // check event must of form a(b)
        String eventName;
        EventTypeSym eventTypeSym;
        if (event.value instanceof Name) {
            // a(b)
            eventName = ((Name) event.value).id;
            Sym s = env.getCurSym(eventName);
            if (s == null) {
                System.err.println("event type not found");
                return null;
            }
            if (!(s instanceof EventTypeSym)) {
                System.err.println("event type mismatch");
                return null;
            }
            eventTypeSym = (EventTypeSym) s;
        } else {
            System.err.println("event illegal");
            return null;
        }

        if (eventTypeSym.parameters().size() != event.args.size()) {
            throw new SemanticException("event " + eventTypeSym.getName() + " takes "
                    + eventTypeSym.parameters().size() + " arguments, but "
                    + event.args.size() + " were given", location);
        }

        // typecheck arguments
        // TODO steph double check with correctness
        for (int i = 0; i < event.args.size(); i++) {
            Expression arg = event.args.get(i);
            TypeSym paraInfo = eventTypeSym.parameters().get(i).typeSym;
            assert !now.getSHErrLocName().startsWith("null");
            ScopeContext argContext = arg.genTypeConstraints(env, now);
            String typeNameSLC = paraInfo.toSHErrLocFmt();
            Constraint argCon = argContext.genTypeConstraints(typeNameSLC, Relation.GEQ, env, arg.location);
            env.addCons(argCon);
        }


        return now;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
//        compile.ast.Call call = (compile.ast.Call) event.solidityCodeGen(result, code);
//        result.add(new compile.ast.Emit(call));

        assert event.value instanceof Name : "internal error: event is malformed"; // internal
        String eventName = ((Name) event.value).id;
        EventTypeSym eventTypeSym = code.findEventTypeSym(eventName);
        List<compile.ast.Expression> args = new ArrayList<>();
        for (int i = 0; i < event.args.size(); i++) {
            Expression arg = event.args.get(i);
            args.add(arg.solidityCodeGen(result, code));
        }
        result.add(new compile.ast.Emit(eventName, args));
        return result;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        EventTypeSym eventTypeSym = (EventTypeSym) env.getCurSym(((Name) event.value).id);

        // Evaluate the arguments from left to right.
        PathOutcome psi = new PathOutcome(new PsiUnit(beginContext));
        List<String> argValueLabelNames = new ArrayList<>();
        List<String> argPrincipalNames = new ArrayList<>();
        for (Expression arg : event.args) {
            ExpOutcome ao = arg.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            argValueLabelNames.add(ao.valueLabelName);
            argPrincipalNames.add(arg.principalName(env));
            env.inContext = Utils.genNewContextAndConstraints(env, false,
                    ao.psi.getNormalPath().c, beginContext.lambda, arg.nextPcSHL(), arg.location);
        }

        // A parameter label naming another parameter names the principal passed for it.
        Map<String, String> dependentLabelMapping = new HashMap<>();
        for (int i = 0; i < event.args.size(); i++) {
            VarSym param = eventTypeSym.parameters().get(i);
            if (param.isPrincipalTyped() && argPrincipalNames.get(i) != null) {
                dependentLabelMapping.put(param.toSHErrLocFmt(), argPrincipalNames.get(i));
            }
        }

        // Each argument, and the control flow that emits it, must be trusted at the
        // parameter's label, as for the arguments of a call.
        String ifNamePc = env.inContext.pc;
        for (int i = 0; i < event.args.size(); i++) {
            Expression arg = event.args.get(i);
            String paramLabel = eventTypeSym.parameters().get(i).ifl.toSHErrLocFmt(dependentLabelMapping);
            env.checkFlow(argValueLabelNames.get(i), paramLabel, arg.location,
                    "Input to the " + Utils.ordNumString(i + 1)
                            + " argument of the event must be trusted enough");
            env.checkFlow(ifNamePc, paramLabel, arg.location,
                    "Current control flow must be trusted to emit the "
                            + Utils.ordNumString(i + 1) + " argument of the event");
        }

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
        return rtn;
    }
}
