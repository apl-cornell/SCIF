package ast;

import compile.SolCode;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class AnnAssign extends Statement {

    private final Expression target;
    private LabeledType annotation;
    private Expression value;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isBuiltIn = false;

    public AnnAssign(Expression target, LabeledType annotation, Expression value,
            boolean isConst, boolean isFinal) {
        this.target = target;
        this.annotation = annotation;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
    }
    public AnnAssign(Expression target, LabeledType annotation, Expression value,
            boolean isConst, boolean isFinal, boolean isBuiltIn) {
        this.target = target;
        this.annotation = annotation;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.isBuiltIn = isBuiltIn;
    }

    public void setType(LabeledType type) {
        this.annotation = type;
    }

    public void setToDefault(IfLabel lbl) {
        annotation.setToDefault(lbl);
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public VarSym toVarInfo(ContractSym contractSym) {
        IfLabel ifl = null;
        if (annotation != null) {
            ifl = ((LabeledType) annotation).ifl;
        }
        return contractSym.toVarSym(((Name) target).id, annotation, isStatic, isFinal, isBuiltIn, location,
                scopeContext);
    }

    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        if (!(target instanceof Name)) {
            return false;
        }
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = new ScopeContext(target, now);

        String name = ((Name) target).id;
        env.globalSymTab().add(name,
                env.toVarSym(name, annotation, isStatic, isFinal, isBuiltIn, location, tgt));
        return true;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        String name = ((Name) target).id;
        env.addSym(name,
                new VarSym(env.toVarSym(name, annotation, isStatic, isFinal, isBuiltIn, location, now)));
        ScopeContext type = annotation.ntcGenCons(env, now);
        ScopeContext tgt = target.ntcGenCons(env, now);

        logger.debug("1: \n" + env + "\n2: " + target.toSolCode() + "\n" + tgt);
        env.addCons(type.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.ntcGenCons(env, now);
            env.addCons(tgt.genCons(v, Relation.LEQ, env, location));
        } else if (isFinal) {
            throw new RuntimeException("final variable " + name + " not initialized at " + location);
        }
        return now;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        logger.debug("entering AnnAssign: \n");
        // logger.debug(this.toString() + "\n");
        String SLCNameVar, SLCNameVarLbl;
        VarSym varSym;
        String id = ((Name) target).id;
        logger.debug(scopeContext.toString() + " | " + scopeContext.isContractLevel());
        CodeLocation loc = location;
        varSym = env.curContractSym().toVarSym(id, annotation, isStatic, isFinal, isBuiltIn, loc,
                scopeContext);
        env.addVar(id, varSym);
        if (annotation != null) {
            env.cons.add(new Constraint(
                    new Inequality(varSym.labelNameSLC(), Relation.EQ, varSym.labelValueSLC()),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    "Variable " + varSym.getName() + " may be labeled incorrectly"));
        }
        logger.debug(varSym.getName());
        SLCNameVar = varSym.toSHErrLocFmt();
        SLCNameVarLbl = varSym.labelNameSLC();
        logger.debug(varSym.typeSym.getName());

        // If the declared variable is a principal (final address/contract), add it to the principal list
        if ((varSym.isFinal &&
                (varSym.typeSym instanceof ContractSym || varSym.typeSym.getName().equals(Utils.ADDRESS_TYPE)))
                // || varSym.typeSym.getName().equals(Utils.PRINCIPAL_TYPE)) Invalid to declare a non-global principal
                ) {
            env.addPrincipal(varSym);

            VarSym equalPrincipal = null;
            boolean correctInit = true;
            if (value instanceof Name) { // assigned as another variable
                // Nothing to check
                equalPrincipal = env.getVar(((Name) value).id);
            } else if (value instanceof Call call && call.isCast(env)) { // assigned as another contract
                // check if it is a cast to a final address variable
                Expression arg = call.getArgAt(0);
                if (arg instanceof Name) {
                    VarSym sym = env.getVar(((Name) arg).id);
                    if (!sym.isFinal) {
                        correctInit = false;
                    } else {
                        equalPrincipal = sym;
                    }
                } else {
                    correctInit = false;
                }
            } else if (!isBuiltIn) {
                correctInit = false;
            }

            if (correctInit) {
                // add equivalence assumption to the trust set
                env.addTrustConstraint(
                        new Constraint(
                                new Inequality(SLCNameVar, CompareOperator.Eq, equalPrincipal.toSHErrLocFmt()),
                                env.hypothesis(),
                                location,
                                env.curContractSym().getName(),
                                "New principal declaration"
                        ));
            } else {
                throw new RuntimeException("A final address/Contract must be initialized to another final address/Contract: " + id);
            }
        }

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.cons.add(
                new Constraint(new Inequality(ifNamePc, SLCNameVarLbl), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of control flow must be trusted to allow this assignment"));

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        if (value != null) {
            env.inContext = beginContext;
            ExpOutcome valueOutcome = value.genConsVisit(env, scopeContext.isContractLevel());
            env.cons.add(new Constraint(new Inequality(valueOutcome.valueLabelName, SLCNameVarLbl),
                    env.hypothesis(), value.location, env.curContractSym().getName(),
                    "Integrity of the value being assigned must be trusted to allow this assignment"));
            typecheck.Utils.contextFlow(env, valueOutcome.psi.getNormalPath().c, endContext,
                    value.location);
            valueOutcome.psi.set(Utils.getNormalPathException(), endContext);
            return valueOutcome.psi;
        } else {
            return new PathOutcome(new PsiUnit(endContext));
        }

    }

//    public void findPrincipal(HashSet<String> principalSet) {
//        if (annotation instanceof LabeledType) {
//            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
//        }
//    }

    public void solidityCodeGen(SolCode code) {
        if (value != null) {
            code.addVarDef(annotation.toSolCode(), target.toSolCode(), isStatic, value.toSolCode());
        } else {
            code.addVarDef(annotation.toSolCode(), target.toSolCode(), isStatic);
        }
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(target);
        rtn.add(annotation);
        if (value != null) {
            rtn.add(value);
        }
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
