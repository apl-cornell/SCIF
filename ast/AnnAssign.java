package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class AnnAssign extends Statement {
    public Expression target;
    public Type annotation;
    public Expression value;
    public boolean isConst;
    public boolean simple; //if the target is a pure name or an expression

    public AnnAssign(Expression target, Type annotation, Expression value, boolean simple, boolean isConst) {
        this.target = target;
        this.annotation = annotation;
        this.value = value;
        this.simple = simple;
        this.isConst = isConst;
    }

    public void setTarget(Expression target) {
        this.target = target;
    }
    public void setSimple(boolean simple) {
        this.simple = simple;
    }
    public VarInfo toVarInfo(ContractInfo contractInfo) {
        return contractInfo.toVarInfo(((Name)target).id, annotation, isConst, location);
    }

    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        if (!(target instanceof Name)) return false;
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = new ScopeContext(target, now);

        String name = ((Name) target).id;
        env.globalSymTab.add(name, new VarSym(name, env.toVarInfo(name, annotation, isConst, location, tgt)));
        return true;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext type = annotation.NTCgenCons(env, now);
        ScopeContext tgt = target.NTCgenCons(env, now);

        if (!parent.isContractLevel()) {
            String name = ((Name) target).id;
            env.addSym(name, new VarSym(name, env.toVarInfo(name, annotation, isConst, location, tgt)));
        }

        env.cons.add(type.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.NTCgenCons(env, now);
            env.cons.add(tgt.genCons(v, Relation.LEQ, env, location));
        }
        return null;
    }

    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        if (simple) {
            String id = ((Name) target).id;
            CodeLocation loc = location;
            contractInfo.varMap.put(id, contractInfo.toVarInfo(id, annotation, isConst, loc));
        } else {
            //TODO
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        logger.debug("entering AnnAssign: \n");
        logger.debug(this.toString() + "\n");
        if (!simple) {
            //TODO
        }
        String ifNameTgt;
        VarInfo varInfo;
        if (!env.ctxt.isEmpty()) {
            ifNameTgt = (env.ctxt.equals("") ? "" : env.ctxt + ".") + ((Name) target).id;
            String id = ((Name) target).id;
            CodeLocation loc = location;
            varInfo = env.contractInfo.toVarInfo(id, annotation, isConst, loc);
            env.varNameMap.add(((Name) target).id, ifNameTgt, varInfo);
            if (annotation instanceof LabeledType) {
                String ifLabel = ((LabeledType) annotation).ifl.toSherrlocFmt();
                env.cons.add(new Constraint(new Inequality(ifLabel, ifNameTgt + "..lbl"), env.hypothesis, location));
                env.cons.add(new Constraint(new Inequality(ifNameTgt + "..lbl", ifLabel), env.hypothesis, location));
            }
        } else {
            ifNameTgt = ((Name) target).id;
            varInfo = env.varNameMap.getInfo(ifNameTgt);
        }
        logger.debug(varInfo.typeInfo.toString());
        if (varInfo.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {
            env.principalSet.add(varInfo.toSherrlocFmt());
        }

        if (annotation instanceof LabeledType) {
            if (annotation instanceof DepMap) {
                ((DepMap) annotation).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) annotation).ifl.findPrincipal(env.principalSet);
            }
        }
        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        String ifNameTgtLbl = ifNameTgt + "..lbl";
        Context prevContext = env.prevContext;

        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameTgtLbl), env.hypothesis, location));
        if (value != null) {
            Context tmp = value.genConsVisit(env);
            String ifNameValue = tmp.valueLabelName;
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameTgtLbl), env.hypothesis, location));
            if (prevContext != null && prevContext.lockName != null) {
                env.cons.add(new Constraint(new Inequality(tmp.lockName, CompareOperator.Eq, prevContext.lockName), env.hypothesis, location));
            }
            env.prevContext.lockName = tmp.lockName;
        }

        return new Context(ifNameTgtLbl, env.prevContext.lockName);
    }

    public void findPrincipal(HashSet<String> principalSet) {
        if (annotation instanceof LabeledType) {
            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
        }
    }

    public void SolCodeGen(SolCode code) {
        if (value != null)
            code.addVarDef(annotation.toSolCode(), target.toSolCode(), isConst, value.toSolCode());
        else
            code.addVarDef(annotation.toSolCode(), target.toSolCode(), isConst);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(target);
        rtn.add(annotation);
        rtn.add(value);
        return rtn;
    }
}
