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

    Name name;
    LabeledType type;
    Expression value;
    boolean isStatic;
    boolean isFinal;

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

//    public void setType(LabeledType type) {
//        this.type = type;
//    }

    private void setToDefault(IfLabel lbl) {
        type.setToDefault(lbl);
    }

    /*
    public void setTarget(Expression target) {
        this.target = target;
    }
    */

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
        VarSym varSym = env.toVarSym(vname, type, isStatic, isFinal, location, parent);
        // assert varSym.ifl != null;
        env.globalSymTab().add(vname, varSym);
        return true;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // if (!parent.isContractLevel()) {
//        String vname = name.id;
//        env.addSym(vname,
//                new VarSym(env.toVarSym(vname, type, isStatic, isFinal, location, now)));
        // }
        ScopeContext vtype = type.ntcGenCons(env, now);
        ScopeContext tgt = name.ntcGenCons(env, now);

        logger.debug("1: \n" + env + "\n2: " + name.toSolCode() + "\n" + tgt);
        env.addCons(vtype.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.ntcGenCons(env, now);
            env.addCons(tgt.genCons(v, Relation.LEQ, env, location));
        }
        return now;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        String id = name.id;
        CodeLocation loc = location;
        System.err.println(id + " " + type.ifl);
        VarSym varSym =
                contractSym.toVarSym(id, type, isStatic, isFinal, loc, contractSym.defContext());
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
//        if (!scopeContext.isContractLevel()) {
//            CodeLocation loc = location;
//            varSym = env.curContractSym.toVarSym(id, type, isStatic, isFinal, loc,
//                    scopeContext);
//            // ifNameTgt = varSym.toSHErrLocFmt();
//            // (env.ctxt.equals("") ? "" : env.ctxt + ".") + ((Name) target).id;
//            // varSym = env.contractInfo.toVarInfo(id, annotation, isConst, loc);
//            env.addVar(id, varSym);
//            // env.varNameMap.add(((Name) target).id, ifNameTgt, varSym);
//            if (type instanceof LabeledType) {
//                // String ifLabelValue = ((LabeledType) type).ifl.toSHErrLocFmt(scopeContext);
//                env.cons.add(new Constraint(
//                        new Inequality(varSym.labelNameSLC(), Relation.EQ, varSym.labelValueSLC()),
//                        env.hypothesis, location, env.curContractSym.getName(),
//                        "Variable " + varSym.getName() + " may be labeled incorrectly"));
//            }
//        } else {
            // ifNameTgt = ((Name) target).id;
            varSym = env.getVar(id);
//        }
        logger.debug(varSym.getName());
        SLCNameVar = varSym.toSHErrLocFmt();
        SLCNameVarLbl = varSym.labelNameSLC();
        logger.debug(varSym.typeSym.getName());
        if (varSym.typeSym.getName().equals(Utils.ADDRESSTYPE) || varSym.typeSym.getName().equals(Utils.PRINCIPAL_TYPE)) {
            env.principalSet().add(varSym);
        }

        /*if (type instanceof LabeledType) {
            if (type instanceof DepMap) {
                ((DepMap) type).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) type).ifl.findPrincipal(env.principalSet);
            }
        }*/
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        // String ifNameTgtLbl = ifNameTgt + "..lbl";
        // Context prevContext = env.prevContext;

        env.cons.add(
                new Constraint(new Inequality(ifNamePc, SLCNameVarLbl), env.hypothesis, location,
                        env.curContractSym.getName(),
                        "Integrity of control flow must be trusted to allow this assignment"));

        //env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis, location, env.curContractSym.getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        if (value != null) {
            env.inContext = beginContext;
            ExpOutcome valueOutcome = value.genConsVisit(env, scopeContext.isContractLevel());
            env.cons.add(new Constraint(new Inequality(valueOutcome.valueLabelName, SLCNameVarLbl),
                    env.hypothesis, value.location, env.curContractSym.getName(),
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
//        if (type instanceof LabeledType) {
//            ((LabeledType) type).ifl.findPrincipal(principalSet);
//        }
//    }

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

    /*public boolean typeMatch(AnnAssign a) {
        return target.typeMatch(a.target) &&
                annotation.typeMatch(a.annotation) &&
                value.typeMatch(a.value) &&
                isConst == a.isConst &&
                simple == a.simple;

     */

}
