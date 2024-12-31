package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import compile.ast.Type;
import compile.ast.VarDec;
import java.util.ArrayList;
import java.util.List;
import typecheck.CodeLocation;
import typecheck.Context;
import typecheck.ContractSym;
import typecheck.ExpOutcome;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
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
    private boolean isBuiltin = false;

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
        this.isBuiltin = isBuiltIn;
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

    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent)
        throws SemanticException
    {
        // ScopeContext now = new ScopeContext(this, parent);
        // ScopeContext tgt = new ScopeContext(name, now);

        String vname = name.id;
        VarSym varSym = env.newVarSym(vname, type, isStatic, isFinal, isBuiltin, location, parent, true);
        // assert varSym.ifl != null;
        env.addSym(vname, varSym);
        if (type.label() != null) {
            varSym.setLabel(env.newLabel(type.label()));
        }
        return true;
    }

    @Override
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);

        ScopeContext vtype = type.generateConstraints(env, now);
        ScopeContext tgt = name.generateConstraints(env, now);

        env.addCons(vtype.genCons(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.generateConstraints(env, now);
            env.addCons(tgt.genCons(v, Relation.LEQ, env, location));
        } else if (isFinal && !isBuiltin) {
            throw new RuntimeException("final variable " + name.id + " not initialized");
        }
        return now;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        String id = name.id;
        CodeLocation loc = location;
        VarSym varSym =
                contractSym.newVarSym(id, type, isStatic, isFinal, isBuiltin, loc, contractSym.defContext());
        contractSym.addVar(id, varSym);
        if (type.label() != null) {
            varSym.setLabel(contractSym.newLabel(type.label()));
        }
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
                                "New principal declaration"
                        ));
            } else if (!isBuiltin) {
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
                        "Integrity of control flow must be trusted to allow this assignment"));

        //env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        if (value != null) {
            env.inContext = beginContext;
            ExpOutcome valueOutcome = value.genConsVisit(env, scopeContext.isContractLevel());
            env.cons.add(new Constraint(new Inequality(valueOutcome.valueLabelName, SLCNameVarLbl),
                    env.hypothesis(), value.location,
                    "Integrity of the value being assigned must be trusted to allow this assignment"));
            typecheck.Utils.contextFlow(env, valueOutcome.psi.getNormalPath().c, endContext,
                    value.location);
            valueOutcome.psi.set(Utils.getNormalPathException(), endContext);
            return valueOutcome.psi;
        } else {
            return new PathOutcome(new PsiUnit(endContext));
        }

    }

    public VarDec solidityCodeGen(CompileEnv code) {
        Type varType = type.type().solidityCodeGen(code);
        String varName = name.id;
        code.addGlobalVar(varName, varType);
        if (value != null) {
            List<Statement> result = new ArrayList<>();
            compile.ast.Expression valueExp = value.solidityCodeGen(result, code);
            assert result.size() == 0;
            return new VarDec(false, varType, varName, valueExp);
        } else {
            return new VarDec(false, varType, varName);
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

    public boolean isBuiltIn() {
        return isBuiltin;
    }

    /*public boolean typeMatch(AnnAssign a) {
        return target.typeMatch(a.target) &&
                annotation.typeMatch(a.annotation) &&
                value.typeMatch(a.value) &&
                isConst == a.isConst &&
                simple == a.simple;

     */


    public VarDec solidityCodeGenStruct(CompileEnv code) {
        assert value == null;
        Type varType = type.type().solidityCodeGen(code);
        String varName = name.id;
        return new VarDec(false, varType, varName);
    }

    public VarSym toVarInfo(InterfaceSym interfaceSym) {
        IfLabel ifl = null;
        if (type != null) {
            ifl = type.label();
        }
        return interfaceSym.newVarSym(((Name) name).id, type, isStatic, isFinal, isBuiltin, location,
                scopeContext);
    }

    public boolean typeMatch(StateVariableDeclaration stateVariableDeclaration) {
        return name.typeMatch(stateVariableDeclaration.name) &&
                type.typeMatch(stateVariableDeclaration.type) &&
                (value == stateVariableDeclaration.value || value.typeMatch(stateVariableDeclaration.value));
    }
}
