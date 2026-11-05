package ast;

import compile.CompileEnv;
import compile.ast.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;

import java.util.ArrayList;

import typecheck.exceptions.SemanticException;

public class Delete extends Statement {

    Expression target;

    public Delete(Expression target) {
        this.target = target;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        String ifNamePc = beginContext.pc;

        ExpOutcome to = null;
        String ifNameTgt = "";
        if (target instanceof Name) {
            //Assuming target is Name
            ifNameTgt = env.getVar(((Name) target).id).labelNameSLC();
        } else if (target instanceof Subscript || target instanceof Attribute) {
            to = target.genIFConstraints(env, false);
            ifNameTgt = to.valueLabelName;
            env.inContext = Utils.genNewContextAndConstraints(env, false, to.psi.getNormalPath().c, beginContext.lambda, target.nextPcSHL(), location);
        } else {
            throw new AssertionError("delete target was not rejected by type checking: " + target);
        }

        env.checkFlow(ifNamePc, ifNameTgt, target.location,
                "Integrity of control flow must be trusted to allow this assignment");

        typecheck.Utils.contextFlow(env, env.inContext, endContext, target.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.checkFlow(endContext.lambda, beginContext.lambda, location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }

        if (to != null) {
            to.psi.set(Utils.getNormalPathException(), endContext);
            return to.psi;
        } else {
            return new PathOutcome(new PsiUnit(endContext));
        }
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // making sure that the variable to be deleted is global
        boolean deletable = true;
        if (target instanceof Name) {
            ScopeContext rtn = target.genTypeConstraints(env, parent);
            VarSym varSym = target.getVarInfo(env);
            if (varSym.isGlobal()) {
                return rtn;
            } else {
                deletable = false;
            }
        } else if (target instanceof Attribute) {
            ScopeContext rtn = target.genTypeConstraints(env, parent);
            if (((Attribute) target).isGlobalStruct(env)) {
                return rtn;
            } else {
                deletable = false;
            }
        } else if (!(target instanceof Subscript)) {
            throw new SemanticException(
                    "only state variables, struct fields, and map or array elements can be deleted",
                    target.location);
        }
        if (!deletable) {
            throw new SemanticException("only state variables can be deleted", target.location);
        }
        return null;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        compile.ast.Expression targetExp;
        targetExp = target.solidityCodeGen(result, code);
        result.add(new compile.ast.Delete(targetExp));
        return result;
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(target);
        return rtn;
    }
    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
//        for (Expression target: targets) {
            result.putAll(target.readMap(code));
//        }
        return result;
    }
    @Override
    public java.util.Map<String, compile.ast.Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
//        for (Expression target: targets) {
            result.putAll(target.writeMap(code));
//        }
        return result;
    }
}
