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
    public VarSym toVarInfo(ContractSym contractSym) {
        IfLabel ifl = null;
        if (annotation instanceof LabeledType)
            ifl = ((LabeledType) annotation).ifl;
        return contractSym.toVarSym(((Name)target).id, annotation, isConst, location, scopeContext);
    }

    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        if (!(target instanceof Name)) return false;
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = new ScopeContext(target, now);

        String name = ((Name) target).id;
        env.globalSymTab.add(name, env.toVarSym(name, annotation, isConst, location, tgt));
        return true;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext type = annotation.NTCgenCons(env, now);
        ScopeContext tgt = target.NTCgenCons(env, now);

        if (!parent.isContractLevel()) {
            String name = ((Name) target).id;
            env.addSym(name, new VarSym(env.toVarSym(name, annotation, isConst, location, tgt)));
        }

        env.cons.add(type.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.NTCgenCons(env, now);
            env.cons.add(tgt.genCons(v, Relation.LEQ, env, location));
        }
        return null;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        if (simple) {
            String id = ((Name) target).id;
            CodeLocation loc = location;
            contractSym.addVar(id, contractSym.toVarSym(id, annotation, isConst, loc, scopeContext));
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
        String SLCNameVar, SLCNameVarLbl;
        VarSym varSym;
        String id = ((Name) target).id;
        if (!env.ctxt.isContractLevel()) {
            CodeLocation loc = location;
            varSym = env.curContractSym.toVarSym(id, annotation, isConst, loc, scopeContext);
            // ifNameTgt = varSym.toSherrlocFmt();
                    // (env.ctxt.equals("") ? "" : env.ctxt + ".") + ((Name) target).id;
            // varSym = env.contractInfo.toVarInfo(id, annotation, isConst, loc);
            env.addVar(id, varSym);
            // env.varNameMap.add(((Name) target).id, ifNameTgt, varSym);
            if (annotation instanceof LabeledType) {
                String ifLabel = ((LabeledType) annotation).ifl.toSherrlocFmt();
                env.cons.add(new Constraint(new Inequality(ifLabel, varSym.labelToSherrlocFmt()), env.hypothesis, location));
                env.cons.add(new Constraint(new Inequality(varSym.labelToSherrlocFmt(), ifLabel), env.hypothesis, location));
            }
        } else {
            // ifNameTgt = ((Name) target).id;
            varSym = env.getVar(id);
        }
        SLCNameVar = varSym.toSherrlocFmt();
        SLCNameVarLbl = varSym.labelToSherrlocFmt();
        logger.debug(varSym.typeSym.toString());
        if (varSym.typeSym.name.equals(Utils.ADDRESSTYPE)) {
            env.principalSet.add(varSym.toSherrlocFmt());
        }

        if (annotation instanceof LabeledType) {
            if (annotation instanceof DepMap) {
                ((DepMap) annotation).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) annotation).ifl.findPrincipal(env.principalSet);
            }
        }
        String ifNamePc = Utils.getLabelNamePc(env.ctxt.getSHErrLocName());
        // String ifNameTgtLbl = ifNameTgt + "..lbl";
        Context prevContext = env.prevContext;

        env.cons.add(new Constraint(new Inequality(ifNamePc, SLCNameVarLbl), env.hypothesis, location));
        if (value != null) {
            Context tmp = value.genConsVisit(env);
            String ifNameValue = tmp.valueLabelName;
            env.cons.add(new Constraint(new Inequality(ifNameValue, SLCNameVarLbl), env.hypothesis, location));
            if (prevContext != null && prevContext.lockName != null) {
                env.cons.add(new Constraint(new Inequality(tmp.lockName, CompareOperator.Eq, prevContext.lockName), env.hypothesis, location));
            }
            env.prevContext.lockName = tmp.lockName;
        }

        return new Context(SLCNameVarLbl, env.prevContext.lockName);
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
        if (value != null)
            rtn.add(value);
        return rtn;
    }
}
