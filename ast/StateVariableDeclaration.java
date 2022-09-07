package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.HashSet;
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

public class StateVariableDeclaration extends TopLayerNode {

    Name name;
    Type type;
    Expression value;
    boolean isStatic;
    boolean isFinal;

    public StateVariableDeclaration(Name name, Type type, Expression value,
            boolean isConst, boolean isFinal) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setToDefault(IfLabel lbl) {
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

    public VarSym toVarInfo(ContractSym contractSym) {
        IfLabel ifl = null;
        if (type instanceof LabeledType) {
            ifl = ((LabeledType) type).ifl;
        }
        return contractSym.toVarSym(name.id, type, isStatic, isFinal, location,
                scopeContext);
    }

    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext tgt = new ScopeContext(name, now);

        String vname = name.id;
        env.globalSymTab.add(vname,
                env.toVarSym(vname, type, isStatic, isFinal, location, tgt));
        return true;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // if (!parent.isContractLevel()) {
        String vname = name.id;
        env.addSym(vname,
                new VarSym(env.toVarSym(vname, type, isStatic, isFinal, location, now)));
        // }
        ScopeContext vtype = type.ntcGenCons(env, now);
        ScopeContext tgt = name.ntcGenCons(env, now);

        logger.debug("1: \n" + env + "\n2: " + name.toSolCode() + "\n" + tgt);
        env.cons.add(vtype.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.ntcGenCons(env, now);
            env.cons.add(tgt.genCons(v, Relation.LEQ, env, location));
        }
        return now;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        String id = name.id;
        CodeLocation loc = location;
        contractSym.addVar(id,
                contractSym.toVarSym(id, type, isStatic, isFinal, loc, scopeContext));
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(location),
                typecheck.Utils.getLabelNameLock(location));

        logger.debug("entering AnnAssign: \n");
        // logger.debug(this.toString() + "\n");
        String SLCNameVar, SLCNameVarLbl;
        VarSym varSym;
        String id = name.id;
        logger.debug(scopeContext.toString() + " | " + scopeContext.isContractLevel());
        if (!scopeContext.isContractLevel()) {
            CodeLocation loc = location;
            varSym = env.curContractSym.toVarSym(id, type, isStatic, isFinal, loc,
                    scopeContext);
            // ifNameTgt = varSym.toSherrlocFmt();
            // (env.ctxt.equals("") ? "" : env.ctxt + ".") + ((Name) target).id;
            // varSym = env.contractInfo.toVarInfo(id, annotation, isConst, loc);
            env.addVar(id, varSym);
            // env.varNameMap.add(((Name) target).id, ifNameTgt, varSym);
            if (type instanceof LabeledType) {
                String ifLabel = ((LabeledType) type).ifl.toSherrlocFmt();
                env.cons.add(new Constraint(
                        new Inequality(ifLabel, Relation.EQ, varSym.labelToSherrlocFmt()),
                        env.hypothesis, location, env.curContractSym.name,
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

        if (type instanceof LabeledType) {
            if (type instanceof DepMap) {
                ((DepMap) type).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) type).ifl.findPrincipal(env.principalSet);
            }
        }
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        // String ifNameTgtLbl = ifNameTgt + "..lbl";
        // Context prevContext = env.prevContext;

        env.cons.add(
                new Constraint(new Inequality(ifNamePc, SLCNameVarLbl), env.hypothesis, location,
                        env.curContractSym.name,
                        "Integrity of control flow must be trusted to allow this assignment"));

        //env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        if (value != null) {
            env.inContext = beginContext;
            ExpOutcome valueOutcome = value.genConsVisit(env, scopeContext.isContractLevel());
            env.cons.add(new Constraint(new Inequality(valueOutcome.valueLabelName, SLCNameVarLbl),
                    env.hypothesis, value.location, env.curContractSym.name,
                    "Integrity of the value being assigned must be trusted to allow this assignment"));
            typecheck.Utils.contextFlow(env, valueOutcome.psi.getNormalPath().c, endContext,
                    value.location);
            valueOutcome.psi.set(Utils.getNormalPathException(), endContext);
            return valueOutcome.psi;
        } else {
            return new PathOutcome(new PsiUnit(endContext));
        }

    }

    public void findPrincipal(HashSet<String> principalSet) {
        if (type instanceof LabeledType) {
            ((LabeledType) type).ifl.findPrincipal(principalSet);
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

    /*public boolean typeMatch(AnnAssign a) {
        return target.typeMatch(a.target) &&
                annotation.typeMatch(a.annotation) &&
                value.typeMatch(a.value) &&
                isConst == a.isConst &&
                simple == a.simple;

     */

}
