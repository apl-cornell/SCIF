package ast;

import compile.CompileEnv;
import compile.ast.Type;
import java.util.HashMap;
import java.util.List;

import typecheck.exceptions.SemanticException;
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

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        FuncSym funcSym = Utils.getCurrentFuncInfo(env, now);
        if (value != null) {
            ScopeContext rtn = value.genTypeConstraints(env, now);
            env.addCons(now.genTypeConstraints(rtn, Relation.EQ, env, location));
        }

        env.addCons(
                now.genTypeConstraints(funcSym.returnTypeSLC(), Relation.EQ, env, location));
        return now;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
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
            env.checkFlow(ifNamePc, ifNameRtnValue, location,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
            psi.setReturnPath(env.inContext);
            return psi;
        }

        ExpOutcome vo = value.genIFConstraints(env, true);
        Context expContext = vo.psi.getNormalPath().c;
        String ifNameValue = vo.valueLabelName;
        env.checkFlow(ifNameValue, ifNameRtnValue, location,
                "Value must be trusted to be returned");
        env.checkFlow(ifNamePc, ifNameRtnValue, location,
                "Control flow must be trusted to return");

        if (funcSym.returnType instanceof typecheck.ClosureTypeSym targetCs) {
            ClosureCreation.checkFlowInto(value, targetCs, new java.util.HashMap<>(), env, location,
                "Returned closure value must subtype the declared return closure type");
        }

        env.checkFlow(Utils.joinLabels(endContext.lambda, beginContext.lambda), ifRtnLockName,
                location, "Reentrancy locks must be respected to return");
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
