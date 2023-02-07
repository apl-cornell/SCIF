package ast;

import compile.SolCode;
import java.util.ArrayList;
import typecheck.CodeLocation;
import typecheck.Context;
import typecheck.ContractSym;
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

/**
 * A node that represents a state variable declaration in a contract.
 * Should be labeled or be assigned a default label.
 */
public class StateVariableDeclaration extends TopLayerNode {

    private Name name;
    private LabeledType type;
    private Expression value;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isBuiltIn = false;

    public StateVariableDeclaration(Name name, LabeledType type, Expression value,
            boolean isConst, boolean isFinal) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.location = Utils.BUILTIN_LOCATION;
        this.type.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    public StateVariableDeclaration(Name name, LabeledType type, Expression value,
            boolean isConst, boolean isFinal, boolean isBuiltIn) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.location = Utils.BUILTIN_LOCATION;
        this.isBuiltIn = isBuiltIn;
        this.type.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    private void setToDefault(IfLabel lbl) {
        type.setToDefault(lbl);
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        // ScopeContext now = new ScopeContext(this, parent);
        // ScopeContext tgt = new ScopeContext(name, now);

        String vname = name.id;
        VarSym varSym = env.toVarSym(vname, type, isStatic, isFinal, isBuiltIn, location, parent);
        // assert varSym.ifl != null;
        env.globalSymTab().add(vname, varSym);
        return true;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);

        ScopeContext vtype = type.ntcGenCons(env, now);
        ScopeContext tgt = name.ntcGenCons(env, now);

        logger.debug("1: \n" + env + "\n2: " + name.toSolCode() + "\n" + tgt);
        env.addCons(vtype.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.ntcGenCons(env, now);
            env.addCons(tgt.genCons(v, Relation.LEQ, env, location));
        } else if (isFinal && !isBuiltIn) {
            throw new RuntimeException("final variable " + name.id + " not initialized");
        }
        return now;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        String id = name.id;
        CodeLocation loc = location;
        System.err.println(id + " " + type.ifl);
        VarSym varSym =
                contractSym.toVarSym(id, type, isStatic, isFinal, isBuiltIn, loc, contractSym.defContext());
        contractSym.addVar(id, varSym);
        varSym.setLabel(contractSym.toLabel(type.ifl));
        assert varSym.ifl != null;
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        logger.debug("entering AnnAssign: \n");
        // logger.debug(this.toString() + "\n");
        String SLCNameVar, SLCNameVarLbl;
        VarSym varSym;
        String id = name.id;
        logger.debug(scopeContext.toString() + " | " + scopeContext.isContractLevel());

        varSym = env.getVar(id);
        logger.debug(varSym.getName());
        SLCNameVar = varSym.toSHErrLocFmt();
        SLCNameVarLbl = varSym.labelNameSLC();
        logger.debug(varSym.typeSym.getName());
        if ((varSym.isFinal &&
                (varSym.typeSym instanceof ContractSym || varSym.typeSym.getName().equals(Utils.ADDRESS_TYPE)))) {
            env.addPrincipal(varSym);

            VarSym equalPrincipal = null;
            boolean correctInit = true;
            if (value instanceof Name) { // assigned as another variable
                // Nothing to check
                equalPrincipal = env.getVar(((Name) value).id);
                assert equalPrincipal != null : ((Name) value).id;
            } else if (value instanceof Call && ((Call) value).isCast(env)) { // assigned as another contract
                // check if it is a cast to a final address variable
                Call cast = (Call) value;
                Expression arg = cast.getArgAt(0);
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
            } else {
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
            } else if (!isBuiltIn) {
                throw new RuntimeException("A final address/Contract must be initialized to another final address/Contract: " + id);
            }


        } else if (varSym.typeSym.getName().equals(Utils.PRINCIPAL_TYPE)) {
            env.addPrincipal(varSym);
        }

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        // String ifNameTgtLbl = ifNameTgt + "..lbl";
        // Context prevContext = env.prevContext;

        env.cons.add(
                new Constraint(new Inequality(ifNamePc, SLCNameVarLbl), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of control flow must be trusted to allow this assignment"));

        //env.outContext = endContext;

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

    public void solidityCodeGen(SolCode code) {
        if (value != null) {
            code.addVarDef(type.toSolCode(), type.toSolCode(), isStatic, value.toSolCode());
        } else {
            code.addVarDef(type.toSolCode(), type.toSolCode(), isStatic);
        }
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(name);
        rtn.add(type);
        if (value != null) {
            rtn.add(value);
        }
        return rtn;
    }

    public Name name() {
        return name;
    }

    /*public boolean typeMatch(AnnAssign a) {
        return target.typeMatch(a.target) &&
                annotation.typeMatch(a.annotation) &&
                value.typeMatch(a.value) &&
                isConst == a.isConst &&
                simple == a.simple;

     */

}
