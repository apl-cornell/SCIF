package ast;

import compile.CompileEnv;
import compile.CompileEnv.ScopeType;
import compile.ast.Literal;
import compile.ast.Return;
import compile.ast.Revert;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.*;

public class Throw extends Statement {

    private final Name exception;
    private final List<Expression> args;

    /** A statement throwing the exception named {@code exception} with arguments {@code args}. */
    public Throw(Name exception, List<Expression> args) {
        this.exception = exception;
        this.args = args;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        if (!(env.getCurSym(exception.id) instanceof ExceptionTypeSym exceptionSym)) {
            throw new SemanticException("not an exception: " + exception.id, exception.location);
        }
        CallArguments.genTypeConstraints(args,
                exceptionSym.parameters().stream().map(p -> p.typeSym).toList(),
                "exception " + exceptionSym.getName(), env, now, location);
        if (!parent.isCheckedException(exceptionSym, false)) {
            throw new SemanticException("exception " + exceptionSym.getName()
                    + " must be caught or declared to be thrown", location);
        }
        return now;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        String exceptionName = exception.id;
        ExceptionTypeSym exceptionTypeSym = code.findExceptionTypeSym(exceptionName);
        List<compile.ast.Expression> solArgs = new ArrayList<>();
        for (Expression arg : args) {
            solArgs.add(arg.solidityCodeGen(result, code));
        }
        if (code.currentScope() == ScopeType.METHOD) {
            result.add(new Return(
                    List.of(
                            new Literal(String.valueOf(code.getExceptionId(exceptionTypeSym))),
                            CompileEnv.encodeException(exceptionTypeSym, solArgs)
                    )));
        } else if (code.currentScope() == ScopeType.TRY) {
            result.add(new Return(
                    List.of(
                            new Literal(String.valueOf(code.getExceptionId(exceptionTypeSym))),
                            code.encodeVarsAndException(exceptionTypeSym, solArgs)
                    )));
        } else if (code.currentScope() == ScopeType.ATOMIC) {
            result.add(new Revert(new Literal(exceptionName)));
        }
        return result;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(exception);
        rtn.addAll(args);
        return rtn;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        PathOutcome psi = new PathOutcome(new PsiUnit(env.inContext));
        ExceptionTypeSym expSym = env.getExp(exception.id);
        CallArguments.Evaluated evaluated = CallArguments.evaluate(args, env, psi);

        // Each argument is checked like an argument of a call.
        Map<String, String> mapping = new HashMap<>();
        // An argument holding an unknown principal is checked as if it held the most
        // trusted one.
        CallArguments.mapPrincipals(expSym.parameters(), evaluated,
                env.curContractSym().top().toSHErrLocFmt(), mapping);
        CallArguments.checkFlowInto(args, evaluated, env.inContext.pc,
                expSym.parameters().stream().map(p -> p.ifl).toList(), mapping,
                " of the exception", env);

        PsiUnit expUnit = new PsiUnit(
                new Context(env.inContext.pc, psi.getNormalPath().c.lambda), false);
        PathOutcome psi2 = new PathOutcome();
        psi2.set(expSym, expUnit);

        psi.remove(Utils.getNormalPathException());
        psi.join(psi2);
        return psi;
    }

    @Override
    protected Map<String,? extends Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Expression arg : args) {
            result.putAll(arg.readMap(code));
        }
        return result;
    }
}
