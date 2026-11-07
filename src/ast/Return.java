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
        if (value == null && !funcSym.returnType.isVoid()) {
            throw new SemanticException("missing return value: the method returns "
                    + funcSym.returnType.getName(), location);
        }
        if (value != null && funcSym.returnType.isVoid()) {
            throw new SemanticException("a method that returns void cannot return a value",
                    value.location);
        }
        if (value != null) {
            ScopeContext rtn = value.genTypeConstraints(env, now);
            env.addCons(now.genTypeConstraints(rtn, Relation.EQ, env, location, "A return statement has the type of its value"));
        }

        env.addCons(
                now.genTypeConstraints(funcSym.returnTypeSLC(), Relation.EQ, env, location, "The returned value must have the return type of the method"));
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
                    "Control flow must be trusted at the label of the return value to return");
            psi.setReturnPath(env.inContext);
            return psi;
        }

        ExpOutcome vo = value.genIFConstraints(env, true);
        Context expContext = vo.psi.getNormalPath().c;
        String ifNameValue = vo.valueLabelName;
        env.checkFlow(ifNameValue, ifNameRtnValue, location,
                "The returned value must be trusted at the label of the return value");
        env.checkFlow(ifNamePc, ifNameRtnValue, location,
                "Control flow must be trusted at the label of the return value to return");

        if (funcSym.returnType instanceof typecheck.ClosureTypeSym targetCs) {
            ClosureCreation.checkFlowInto(value, targetCs, new java.util.HashMap<>(), env, location,
                "The returned closure's labels must be compatible with the declared return type");
        }

        env.checkFlow(Utils.joinLabels(endContext.lambda, beginContext.lambda), ifRtnLockName,
                location, "The method must maintain its reentrancy lock label when it returns");
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
