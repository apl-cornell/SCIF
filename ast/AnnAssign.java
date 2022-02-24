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
    public boolean isStatic;
    public boolean isFinal;
    public boolean simple; //if the target is a pure name or an expression

    public AnnAssign(Expression target, Type annotation, Expression value, boolean simple, boolean isConst, boolean isFinal) {
        this.target = target;
        this.annotation = annotation;
        this.value = value;
        this.simple = simple;
        this.isStatic = isConst;
        this.isFinal = isFinal;
    }

    public void setType(Type type) { this.annotation = type; }
    public void setToDefault(IfLabel lbl) { annotation.setToDefault(lbl); }
    public void setTarget(Expression target) {
        this.target = target;
    }
    public void setSimple(boolean simple) {
        this.simple = simple;
    }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal;}
    public void setStatic(boolean isStatic) {this.isStatic = isStatic;}

    public VarSym toVarInfo(ContractSym contractSym) {
        IfLabel ifl = null;
        if (annotation instanceof LabeledType)
            ifl = ((LabeledType) annotation).ifl;
        return contractSym.toVarSym(((Name)target).id, annotation, isStatic, isFinal, location, scopeContext);
    }

    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        if (!(target instanceof Name)) return false;
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = new ScopeContext(target, now);

        String name = ((Name) target).id;
        env.globalSymTab.add(name, env.toVarSym(name, annotation, isStatic, isFinal, location, tgt));
        return true;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        if (!parent.isContractLevel()) {
            String name = ((Name) target).id;
            env.addSym(name, new VarSym(env.toVarSym(name, annotation, isStatic, isFinal, location, now)));
        }
        ScopeContext type = annotation.NTCgenCons(env, now);
        ScopeContext tgt = target.NTCgenCons(env, now);

        logger.debug("1: \n" + env + "\n2: " + target.toSolCode() + "\n" + tgt);
        env.cons.add(type.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.NTCgenCons(env, now);
            env.cons.add(tgt.genCons(v, Relation.LEQ, env, location));
        }
        return now;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        if (simple) {
            String id = ((Name) target).id;
            CodeLocation loc = location;
            contractSym.addVar(id, contractSym.toVarSym(id, annotation, isStatic, isFinal, loc, scopeContext));
        } else {
            //TODO
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, Utils.getLabelNameLock(location), context.inLockName);

        logger.debug("entering AnnAssign: \n");
        // logger.debug(this.toString() + "\n");
        if (!simple) {
            //TODO
        }
        String SLCNameVar, SLCNameVarLbl;
        VarSym varSym;
        String id = ((Name) target).id;
        logger.debug(scopeContext.toString() + " | " + scopeContext.isContractLevel());
        if (!scopeContext.isContractLevel()) {
            CodeLocation loc = location;
            varSym = env.curContractSym.toVarSym(id, annotation, isStatic, isFinal, loc, scopeContext);
            // ifNameTgt = varSym.toSherrlocFmt();
                    // (env.ctxt.equals("") ? "" : env.ctxt + ".") + ((Name) target).id;
            // varSym = env.contractInfo.toVarInfo(id, annotation, isConst, loc);
            env.addVar(id, varSym);
            // env.varNameMap.add(((Name) target).id, ifNameTgt, varSym);
            if (annotation instanceof LabeledType) {
                String ifLabel = ((LabeledType) annotation).ifl.toSherrlocFmt();
                env.cons.add(new Constraint(new Inequality(ifLabel, Relation.EQ, varSym.labelToSherrlocFmt()), env.hypothesis, location, env.curContractSym.name,
                        "Variable " + varSym.name + " may be labeled incorrectly"));
            }
        } else {
            // ifNameTgt = ((Name) target).id;
            varSym = env.getVar(id);
        }
        logger.debug(varSym.name);
        SLCNameVar = varSym.toSherrlocFmt();
        SLCNameVarLbl = varSym.labelToSherrlocFmt();
        logger.debug(varSym.typeSym.name);
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
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        // String ifNameTgtLbl = ifNameTgt + "..lbl";
        // Context prevContext = env.prevContext;

        env.cons.add(new Constraint(new Inequality(ifNamePc, SLCNameVarLbl), env.hypothesis, location, env.curContractSym.name,
                "Integrity of control flow must be trusted to allow this assignment"));
        if (value != null) {
            env.context = curContext;
            Context tmp = value.genConsVisit(env, scopeContext.isContractLevel());
            String ifNameValue = tmp.valueLabelName;
            env.cons.add(new Constraint(new Inequality(ifNameValue, SLCNameVarLbl), env.hypothesis, value.location, env.curContractSym.name,
                    "Integrity of the value being assigned must be trusted to allow this assignment"));
            /*if (prevContext != null && prevContext.inLockName != null) {
                env.cons.add(new Constraint(new Inequality(tmp.lockName, tmp.inLockName), env.hypothesis, value.location, env.curContractSym.name,
                        "Lock should be maintained after execution of this operation"));
                // env.cons.add(new Constraint(new Inequality(tmp.lockName, CompareOperator.Eq, prevContext.lockName), env.hypothesis, location, env.curContractSym.name, "Lock should be maintained before execution of this operation"));
            }*/
            // prevContext = tmp;
            // env.prevContext.lockName = tmp.lockName;
        }
        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, curContext.inLockName), env.hypothesis, location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        } else if (!scopeContext.isContractLevel()) {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.lockName), env.hypothesis, location, env.curContractSym.name,
                    Utils.ERROR_MESSAGE_LOCK_IN_LAST_OPERATION));
        }

        return new Context(SLCNameVarLbl, curContext.lockName, curContext.inLockName);
    }

    public void findPrincipal(HashSet<String> principalSet) {
        if (annotation instanceof LabeledType) {
            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
        }
    }

    public void SolCodeGen(SolCode code) {
        if (value != null)
            code.addVarDef(annotation.toSolCode(), target.toSolCode(), isStatic, value.toSolCode());
        else
            code.addVarDef(annotation.toSolCode(), target.toSolCode(), isStatic);
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

    /*public boolean typeMatch(AnnAssign a) {
        return target.typeMatch(a.target) &&
                annotation.typeMatch(a.annotation) &&
                value.typeMatch(a.value) &&
                isConst == a.isConst &&
                simple == a.simple;
    }*/
}
