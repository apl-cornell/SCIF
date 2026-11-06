package ast;

import compile.CompileEnv;
import compile.ast.Attr;
import compile.ast.ExternalCall;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.ArrayTypeSym;
import typecheck.BuiltInT;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;

/**
 * A built-in operation on an array, written {@code a.op(args)}: {@code push(x)}, {@code pop()}
 * or {@code length()}. The parser cannot tell it from a call, so the regular type check
 * resolves a {@link Call} to it.
 */
public class ArrayOperation extends Expression {

    private final Expression array;
    private final String op;
    private final List<Expression> args;

    /** The operation {@code op} on the array {@code array}, with arguments {@code args}. */
    ArrayOperation(Expression array, String op, List<Expression> args) {
        this.array = array;
        this.op = op;
        this.args = args;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        if (!(array instanceof Name)) {
            array.genTypeConstraints(env, now);
        }
        ArrayTypeSym arrayType = (ArrayTypeSym) array.getVarInfo(env).typeSym;
        String elementType = arrayType.valueType.toSHErrLocFmt();
        switch (op) {
            case "pop" -> {
                if (!args.isEmpty()) {
                    throw new SemanticException("pop expects 0 arguments", location);
                }
                env.addCons(now.genTypeConstraints(elementType, Relation.EQ, env, location));
            }
            case "push" -> {
                if (args.size() != 1) {
                    throw new SemanticException("push expects 1 argument", location);
                }
                Expression arg = args.get(0);
                ScopeContext argContext = arg.genTypeConstraints(env, now);
                env.addCons(argContext.genTypeConstraints(elementType, Relation.GEQ, env,
                        arg.location));
                TypeSym voidType = (TypeSym) env.getSym(BuiltInT.VOID);
                env.addCons(now.genTypeConstraints(voidType.toSHErrLocFmt(), Relation.EQ, env,
                        location));
            }
            case "length" -> {
                if (!args.isEmpty()) {
                    throw new SemanticException("length expects 0 arguments", location);
                }
                TypeSym uintType = (TypeSym) env.getSym(BuiltInT.UINT);
                env.addCons(now.genTypeConstraints(uintType.toSHErrLocFmt(), Relation.EQ, env,
                        location));
            }
            default -> throw new SemanticException("type error: unknown array operator",
                    location);
        }
        return now;
    }

    /**
     * {@code push} and {@code pop} modify the array, so the control flow, and the element
     * pushed, must be trusted at the array's location. {@code length} is trusted as much as
     * the array.
     */
    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));
        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
        List<String> argLabels = new ArrayList<>();
        for (Expression arg : args) {
            ExpOutcome ao = arg.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            argLabels.add(ao.valueLabelName);
            env.inContext = Utils.genNewContextAndConstraints(env, false,
                    ao.psi.getNormalPath().c, beginContext.lambda, arg.nextPcSHL(),
                    arg.location);
        }
        ExpOutcome vo = array.genIFConstraints(env, false);
        psi.joinExe(vo.psi);
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        String result = ifNamePc;
        if (op.equals("length")) {
            result = vo.valueLabelName;
        } else {
            String arrayLocation = array.genLocationConstraints(env).valueLabelName;
            env.checkFlow(ifNamePc, arrayLocation, location,
                    "Current control flow must be trusted to modify this array");
            if (op.equals("push")) {
                env.checkFlow(argLabels.get(0), arrayLocation, location,
                        "The element pushed must be trusted at the array's label");
            }
        }
        Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
        if (!tail_position) {
            env.checkFlow(psi.getNormalPath().c.lambda, beginContext.lambda, location,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }
        return new ExpOutcome(result, psi);
    }

    @Override
    protected TypeSym staticType(VisitEnv env) {
        return env.builtinType(op.equals("length") ? BuiltInT.UINT : BuiltInT.VOID);
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        List<compile.ast.Expression> argExps = new ArrayList<>();
        for (Expression arg : args) {
            argExps.add(arg.solidityCodeGen(result, code));
        }
        compile.ast.Expression target = array.solidityCodeGen(result, code);
        if (op.equals("length")) {
            return new Attr(target, op);
        }
        return new ExternalCall(target, op, argExps, null);
    }

    @Override
    public boolean typeMatch(Expression expression) {
        if (!(expression instanceof ArrayOperation o) || !op.equals(o.op)
                || !array.typeMatch(o.array) || args.size() != o.args.size()) {
            return false;
        }
        for (int i = 0; i < args.size(); ++i) {
            if (!args.get(i).typeMatch(o.args.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, compile.ast.Type> result = new HashMap<>();
        for (Expression arg : args) {
            result.putAll(arg.readMap(code));
        }
        return result;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(array);
        rtn.addAll(args);
        return rtn;
    }
}
