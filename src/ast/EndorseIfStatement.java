package ast;

import compile.CompileEnv;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.CodeLocation;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

public class EndorseIfStatement extends Statement {

    List<Name> expressionList;
    IfLabel from, to;
    If ifStatement;

    public EndorseIfStatement(List<Name> expressionList, IfLabel from, IfLabel to,
            If ifStatement) {
        this.expressionList = expressionList;
        this.from = from;
        this.to = to;
        this.ifStatement = ifStatement;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        for (Name name: expressionList) {
            name.ntcGenCons(env, parent);
        }
        from.ntcGenCons(env, parent);
        to.ntcGenCons(env, parent);
        return ifStatement.ntcGenCons(env, parent);
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        return ifStatement.solidityCodeGen(code);
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        // create new same-name variables inside the if-branch
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        // String originalCtxt = env.ctxt;

        // curContext.lambda = env.prevContext.lambda;
        String rtnValueLabel;

        ExpOutcome toOutCome = ifStatement.test.genConsVisit(env,
                ifStatement.body.size() == 0 && ifStatement.orelse.size() == 0 && tail_position);
        //Context condContext = to.psi
        rtnValueLabel = toOutCome.valueLabelName;
        String IfNameTest = toOutCome.valueLabelName;
        String IfNamePcBefore = Utils.getLabelNamePc(scopeContext.getParent().getSHErrLocName());
        // env.ctxt += ".If" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        boolean createdHypo = false;
        //TestableVarInfo testedVar = null;
        //String beforeTestedLabel = "";
        boolean tested = false;
        if (ifStatement.test instanceof FlowsToExp fte) {
            Name left = (Name) fte.lhs, right = (Name) fte.rhs;
            if (env.containsVar(left.id) && env.containsVar(right.id)) {

                logger.debug("if both exists");
                VarSym l = env.getVar(left.id), r = env.getVar(right.id);
                logger.debug(l.toString());
                logger.debug(r.toString());
                createdHypo = true;
                Inequality hypo = new Inequality(l.toSHErrLocFmt(), Relation.LEQ,
                        r.toSHErrLocFmt());

                env.hypothesis().add(hypo);
                logger.debug("testing label");
            }
        }
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis(),
                location, env.curContractSym().getName(),
                "Control flow integrity before this if condition doesn't flow to the one after this condition"));
        /*env.cons.add(
                new Constraint(new Inequality(IfNameTest, IfNamePcAfter), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of this condition doesn't flow to the control flow in this if branch"));

         */

        /*if (body.size() > 0 || orelse.size() > 0) {
            env.cons.add(new Constraint(new Inequality(prevLockLabel, Relation.GEQ, curContext.lambda), env.hypothesis, test.location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }*/
        env.incScopeLayer();

        // test if all vars to be endorsed flows to from-label
        for (Name name: expressionList) {
            String id = name.id;
            VarSym sym = env.getVar(id);
            env.cons.add(new Constraint(new Inequality(sym.labelNameSLC(), from.toSHErrLocFmt()), env.hypothesis(), name.location, env.curContractSym().getName(),
            "Variables to be endorsed must flows to from-label"));
        }

        // create new identical variables in the same scope with to-label
        for (Name name: expressionList) {
            String id = name.id;
            VarSym sym = env.getVar(id);
            VarSym newSym = new VarSym(sym, scopeContext);
            newSym.ifl = env.toLabel(to);
            env.cons.add(new Constraint(
                    new Inequality(newSym.labelNameSLC(), Relation.EQ, newSym.labelValueSLC()),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    "Variable " + newSym.getName() + " may be endorsed incorrectly"));
            env.addVar(newSym.getName(), newSym);
        }

        // Context leftContext = new Context(curContext), rightContext = new Context(curContext);
        CodeLocation loc = null;
        PathOutcome ifo = toOutCome.psi;
        env.inContext = new Context(IfNamePcAfter, beginContext.lambda);
        int index = 0;
        for (Statement stmt : ifStatement.body) {
            ++index;
            ifo = stmt.genConsVisit(env, index == ifStatement.body.size() && tail_position);
            // env.prevContext = tmp;
            // prev2 = leftContext;
            //env.context = context;
            PsiUnit normalUnit = ifo.getNormalPath();
            if (normalUnit == null) {
                break;
            }
            env.inContext = normalUnit.c;
            loc = stmt.location;
            // leftContext = new Context(tmp);
        }
        env.decScopeLayer();

        if (createdHypo) {
            env.hypothesis().pop();
        }

        logger.debug("finished if branch");
        //System.err.println("finished if branch");
        // env.prevContext.lambda = curContext.lambda;
        env.incScopeLayer();
        index = 0;
        PathOutcome elseo = toOutCome.psi;
        env.inContext = new Context(IfNamePcAfter, beginContext.lambda);
        for (Statement stmt : ifStatement.orelse) {
            ++index;
            elseo = stmt.genConsVisit(env, index == ifStatement.orelse.size() && tail_position);
            PsiUnit normalUnit = elseo.getNormalPath();
            if (normalUnit == null) {
                break;
            }
            env.inContext = elseo.getNormalPath().c;
            // env.prevContext.lambda = rightContext.lambda;
        }
        env.decScopeLayer();

        ifo.join(elseo);
        // env.cons.add(new Constraint(new Inequality(IfNameLock, Relation.REQ, Utils.joinLabels(leftContext.lambda, rightContext.lambda)), env.hypothesis, location));

        /*env.cons.add(new Constraint(new Inequality(IfNameLock, Relation.GEQ, leftContext.lambda), env.hypothesis, location, env.curContractSym.name,
                "Lock of then branch contributes to lock of this if statement"));
        env.cons.add(new Constraint(new Inequality(IfNameLock, Relation.GEQ, rightContext.lambda), env.hypothesis, location, env.curContractSym.name,
                "Lock of else branch contributes to lock of this if statement"));*/

        logger.debug("finished orelse branch");

        // env.ctxt = originalCtxt;
        return ifo;
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(ifStatement.body);
        rtn.addAll(ifStatement.orelse);
        return rtn;
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Name name: expressionList) {
            name.passScopeContext(parent);
        }
        from.passScopeContext(parent);
        to.passScopeContext(parent);
        ifStatement.scopeContext = scopeContext;
        ifStatement.test.passScopeContext(parent);
        for (Node child: children()) {
            child.passScopeContext(scopeContext);
        }
    }

    @Override
    public boolean exceptionHandlingFree() {
        return ifStatement.exceptionHandlingFree();
    }

    @Override
    protected java.util.Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        return ifStatement.readMap(code);
    }

    @Override
    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        return ifStatement.writeMap(code);
    }
}
