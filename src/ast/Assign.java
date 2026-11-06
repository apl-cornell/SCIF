package ast;

import compile.CompileEnv;
import compile.ast.Type;

import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Assign extends Statement {

    Expression target;
    Expression value;
    /** Whether this assignment is a constructor's initialization of a final field. */
    private boolean initializesFinalField = false;

    public Assign(Expression target, Expression value) {
        this.target = target;
        this.value = value;
    }

    /** The names of the variables assigned by assignments within {@code nodes}. */
    static java.util.Set<String> assignedNames(List<? extends Node> nodes) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Node node : nodes) {
            collectAssignedNames(node, names);
        }
        return names;
    }

    private static void collectAssignedNames(Node node, java.util.Set<String> names) {
        if (node instanceof Assign a && a.target instanceof Name n) {
            names.add(n.id);
        }
        for (Node child : node.children()) {
            if (child != null) {
                collectAssignedNames(child, names);
            }
        }
    }

    /** Marks this assignment as a constructor's initialization of a final field. */
    void markFinalFieldInit() {
        initializesFinalField = true;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        if (target instanceof Name n && env.getCurSym(n.id) instanceof VarSym v && v.isFinal
                && !(initializesFinalField && v.isGlobal())) {
            throw new SemanticException("cannot assign to final variable " + n.id, location);
        }
        if (target instanceof Attribute att && att.value.getVarInfo(env) != null
                && att.value.getVarInfo(env).typeSym instanceof typecheck.ClosureTypeSym
                && att.attr.id.equals(typecheck.Utils.CLOSURE_TARGET_MEMBER)) {
            throw new SemanticException("the target of a closure is read-only", location);
        }
        ScopeContext tgt = target.genTypeConstraints(env, now);
        ScopeContext v = value.genTypeConstraints(env, now);
        // con: tgt should be a supertype of v
//        // logger.debug(v);
//        // logger.debug(env);
//        // logger.debug(location);
        env.addCons(tgt.genTypeConstraints(v, Relation.LEQ, env, location));
        return now;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        // Context prevContext = env.prevContext;

        String ifNamePc = beginContext.pc; // Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        ExpOutcome to = null;
        String ifNameTgt = "";
        if (target instanceof Name) {
            //Assuming target is Name
            ifNameTgt = env.getVar(((Name) target).id).labelNameSLC();
        } else if (target instanceof Subscript || target instanceof Attribute) {
            // env.prevContext = valueContext;
            /*env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, valueContext.lambda), env.hypothesis, value.location, env.curContractSym.name,
                    "Lock should be maintained before execution of this operation"));*/
            to = target.genLocationConstraints(env);
            ifNameTgt = to.valueLabelName;
//            env.inContext = new Context(to.psi.getNormalPath().c.lambda, beginContext.lambda);
            env.inContext = Utils.genNewContextAndConstraints(env, false, to.psi.getNormalPath().c, beginContext.lambda, target.nextPcSHL(), location);
            // prevContext = tmp;
            // rtnLockName = tmp.lambda;
        } else {
            throw new SemanticException("Unexpected target in assignment", location);
        }
        ExpOutcome vo = value.genIFConstraints(env, false);
        String ifNameValue = vo.valueLabelName;
        // prevContext = valueContext;

        env.checkFlow(ifNameValue, ifNameTgt, value.location,
                "Integrity of the value being assigned must be trusted to allow this assignment");

        env.checkFlow(ifNamePc, ifNameTgt, value.location,
                "Integrity of control flow must be trusted to allow this assignment");

  
        if (target instanceof Name tgtName && env.getVar(tgtName.id).typeSym instanceof typecheck.ClosureTypeSym targetCs) {
            ClosureCreation.checkFlowInto(value, targetCs, new java.util.HashMap<>(), env,
                value.location, "Closure value flowing into the assignment target must subtype its slot");
        }

        // The target names the assigned principal from here on.
        if (target instanceof Name tgtName && env.getVar(tgtName.id) instanceof VarSym tgtSym
                && tgtSym.isPrincipalTyped()) {
            String source = value.principalName(env);
            String dest = tgtSym.isMutablePrincipal() ? env.newVersion(tgtSym) : tgtSym.toSHErrLocFmt();
            if (source != null) {
                env.assumeSamePrincipal(dest, source);
            }
        }

        typecheck.Utils.contextFlow(env, vo.psi.getNormalPath().c, endContext, value.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.checkFlow(endContext.lambda, beginContext.lambda, location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }

        if (to != null) {
            vo.psi.join(to.psi);
        }
        vo.psi.setNormalPath(endContext);
        assert vo.psi.getNormalPath().c != null;
        return vo.psi;
    }

    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        compile.ast.Expression targetExp = target.solidityCodeGen(result, code);
        compile.ast.Expression valueExp = value.solidityCodeGen(result, code);
        result.add(new compile.ast.Assign(targetExp, valueExp));
        return result;
    }

//    @Override
//    public String toSolCode() {
//        return CompileEnv.genAssign(target.toSolCode(), value.toSolCode());
//    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(target);
        rtn.add(value);
        return rtn;
    }

    @Override
    protected java.util.Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = target.readMap(code);
        result.putAll(value.readMap(code));
        return result;
    }

    @Override
    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        return target.writeMap(code);
    }
}
