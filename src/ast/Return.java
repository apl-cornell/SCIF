package ast;

import compile.CompileEnv;
import compile.ast.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Return extends Statement {

    Expression value;

    public Return(Expression value) {
        this.value = value;
    }

    public Return() {
        this.value = null;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        FuncSym funcSym = Utils.getCurrentFuncInfo(env, now);
        if (value != null) {
            ScopeContext rtn = value.ntcGenCons(env, now);
            env.addCons(now.genCons(rtn, Relation.EQ, env, location));
        }

        env.addCons(
                now.genCons(funcSym.returnTypeSLC(), Relation.EQ, env, location));
        return now;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        // String prevLock = env.prevContext.lambda;
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        //env.context = curContext;
        // int occur = env.ctxt.indexOf(".");
        String funcName = scopeContext.getFuncName();
        FuncSym funcSym = env.getFunc(funcName);
        // String ifNameRtnLock = funcSym.getLabelNameRtnLock();
        String ifNameRtnValue = funcSym.returnSLC();
        String ifRtnLockName = funcSym.getLabelNameCallGamma();
        PathOutcome psi = new PathOutcome();
        if (value == null) {
            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis(),
                    location, env.curContractSym().getName(),
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            psi.setReturnPath(env.inContext);
            return psi;
        }

        ExpOutcome vo = value.genConsVisit(env, true);
        Context expContext = vo.psi.getNormalPath().c;
        String ifNameValue = vo.valueLabelName;
        env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtnValue), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Value must be trusted to be returned"));
        env.cons.add(
                new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Control flow must be trusted to return"));

        env.cons.add(new Constraint(
                new Inequality(Utils.joinLabels(endContext.lambda, beginContext.lambda),
                        ifRtnLockName), env.hypothesis(), location, env.curContractSym().getName(),
                "Reentrancy locks must be respected to return"));
        PsiUnit pathn = vo.psi.getNormalPath();
        vo.psi.removeNormalPath();
        vo.psi.setReturnPath(pathn.c);

        return vo.psi;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        // if inside any try/atomic,
        // otherwise,
        List<compile.ast.Statement> result = new ArrayList<>();
        result.addAll(code.compileReturn(value != null ? value.solidityCodeGen(result, code) : null));
        assert result.size() > 0;
        return result;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        if (value != null) {
            rtn.add(value);
        }
        return rtn;
    }

    @Override
    protected java.util.Map<String,? extends Type> readMap(CompileEnv code) {
        return value != null ? value.readMap(code) : new HashMap<>();
    }
}
