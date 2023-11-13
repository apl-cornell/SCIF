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
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

public class Throw extends Statement {

    Call exception;

    public Throw(Call exception) {
        this.exception = exception;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);

        String exceptionName;
        ExceptionTypeSym exceptionSym;
        if (!(exception.value instanceof Name)) {
            if (exception.value instanceof Attribute) {
                // a.b(c), a must be a contract
                Attribute att = (Attribute) exception.value;
                // (att.value).(att.attr)
                String contractTypeName = ((Name) att.value).id;
                exceptionName = att.attr.id;
                ContractSym s = env.getContract(contractTypeName);
                logger.debug("contract " + contractTypeName + ": " + s.getName());
                if (!(env.getExtSym(contractTypeName, exceptionName) == null)) {
                    System.err.println("a.b not found");
                    return null;
                }
                exceptionSym = s.getExceptionSym(exceptionName);
                if (exceptionSym == null) {
                    System.err.println("exception in a.b() not found");
                    return null;
                }
            } else {
                return null;
            }
        } else {
            // a(b)
            exceptionName = ((Name) exception.value).id;
            Sym s = env.getCurSym(exceptionName);
            if (s == null) {
                System.err.println("exception type not found");
                return null;
            }
            if (!(s instanceof ExceptionTypeSym)) {
                // err: type mismatch
                System.err.println("exception type mismatch");
                return null;
            }
            exceptionSym = ((ExceptionTypeSym) s);
        }
        // check if the parameter number matches
        if (exceptionSym.parameters().size() != exception.args.size()) {
            System.err.println("Throwing an exception " + exceptionSym.getName() + " with unmatched parameter number at " + "location: "
                    + location.toString());
            throw new RuntimeException();
        }

        // typecheck arguments
        for (int i = 0; i < exception.args.size(); ++i) {
            Expression arg = exception.args.get(i);
            TypeSym paraInfo = exceptionSym.parameters().get(i).typeSym;
            assert !now.getSHErrLocName().startsWith("null");
            ScopeContext argContext = arg.ntcGenCons(env, now);
            String typeNameSLC = paraInfo.toSHErrLocFmt();
            Constraint argCon = argContext.genCons(typeNameSLC, Relation.GEQ, env, arg.location);
            env.addCons(argCon);
        }
        // String rtnTypeName = exceptionSym.returnType.name;
        // env.addCons(now.genCons(env.getSymName(rtnTypeName), Relation.EQ, env, location));

        if (!parent.isCheckedException(exceptionSym, false)) {
            System.err.println("Unchecked exception " + exceptionSym.getName() + " at " + "location: "
                    + location.errString());
            throw new RuntimeException();
        }
        return now;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        String exceptionName = null;
        if (!(exception.value instanceof Name)) {
            if (exception.value instanceof Attribute) {
                // a.b(c), a must be a contract
                Attribute att = (Attribute) exception.value;
                // (att.value).(att.attr)
                String contractTypeName = ((Name) att.value).id;
                exceptionName = att.attr.id;
            }
        } else {
            // a(b)
            exceptionName = ((Name) exception.value).id;
        }
        ExceptionTypeSym exceptionTypeSym = code.findExceptionTypeSym(exceptionName);
        List<compile.ast.Expression> args = new ArrayList<>();
        // typecheck arguments
        for (int i = 0; i < exception.args.size(); ++i) {
            Expression arg = exception.args.get(i);
            args.add(arg.solidityCodeGen(result, code));
        }
        if (code.currentScope() == ScopeType.METHOD) {
            result.add(new Return(
                    List.of(
                            new Literal(String.valueOf(code.getExceptionId(exceptionTypeSym))),
                            CompileEnv.encodeException(exceptionTypeSym, args)
                    )));
        } else if (code.currentScope() == ScopeType.TRY) {
            result.add(new Return(
                    List.of(
                            new Literal(String.valueOf(code.getExceptionId(exceptionTypeSym))),
                            code.encodeVarsAndException(exceptionTypeSym, args)
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
        return rtn;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        PathOutcome psi = new PathOutcome(new PsiUnit(beginContext));
        ExpOutcome ao = null;

        for (int i = 0; i < exception.args.size(); ++i) {
            Expression arg = exception.args.get(i);
            ao = arg.genConsVisit(env, false);
            psi.joinExe(ao.psi);
            env.inContext = new Context(
                    Utils.joinLabels(ao.psi.getNormalPath().c.pc, beginContext.pc),
                    beginContext.lambda);
        }

        String expName = ((Name) exception.value).id;
        ExceptionTypeSym expSym = env.getExp(expName);

        PsiUnit expUnit = new PsiUnit(
                new Context(env.inContext.pc,
                        psi.getNormalPath().c.lambda),
                false);
        PathOutcome psi2 = new PathOutcome();
        psi2.set(expSym, expUnit);

        psi.remove(Utils.getNormalPathException());
        psi.join(psi2);

        return psi;
    }

    @Override
    protected java.util.Map<String,? extends Type> readMap(CompileEnv code) {
        return exception.readMap(code);
    }
}
