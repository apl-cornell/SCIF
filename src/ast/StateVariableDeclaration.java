package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import compile.ast.Type;
import compile.ast.VarDec;
import java.util.ArrayList;
import java.util.List;

import typecheck.*;
import typecheck.exceptions.SemanticException;
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
    private boolean isFinal, isTransient;
    private boolean isBuiltin = false;

    public StateVariableDeclaration(Name name, LabeledType type, Expression value,
            boolean isConst, boolean isFinal, boolean isTransient) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.isTransient = isTransient;
        this.location = Utils.BUILTIN_LOCATION;
        this.type.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    public StateVariableDeclaration(Name name, LabeledType type, Expression value,
            boolean isConst, boolean isFinal, boolean isTransient, boolean isBuiltIn) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.isTransient = isTransient;
        this.location = Utils.BUILTIN_LOCATION;
        this.isBuiltin = isBuiltIn;
        this.type.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    private void setToDefault(IfLabel lbl) {
        type.setToDefault(lbl);
    }

//    public void setFinal(boolean isFinal) {
//        this.isFinal = isFinal;
//    }

//    public void setStatic(boolean isStatic) {
//        this.isStatic = isStatic;
//    }

    public void ntcGlobalInfo(NTCEnv env, ScopeContext parent)
        throws SemanticException
    {
        // ScopeContext now = new ScopeContext(this, parent);
        // ScopeContext tgt = new ScopeContext(name, now);

        String vname = name.id;
        VarSym varSym = env.newVarSym(vname, type, isStatic, isFinal, isBuiltin, location, parent, true);
        // assert varSym.ifl != null;
        try {
            env.addSym(vname, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new RuntimeException(e); // can't happen?
        }
        if (type.label() != null) {
            varSym.setLabel(env.newLabel(type.label()));
        }
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);

        ScopeContext vtype = type.genTypeConstraints(env, now);
        ScopeContext tgt = name.genTypeConstraints(env, now);

        env.addCons(vtype.genTypeConstraints(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.genTypeConstraints(env, now);
            env.addCons(tgt.genTypeConstraints(v, Relation.LEQ, env, location));
        }
        // The label must mean the same thing wherever the field is used, so it may not
        // name a principal that can change.
        if (env.getCurSym(name.id) instanceof VarSym field && field.ifl != null) {
            for (VarSym p : field.ifl.principalSyms()) {
                if (p.isMutablePrincipal()) {
                    throw new SemanticException("the label of field " + name.id
                            + " names " + p.getName() + ", which is not final", field.ifl.location());
                }
            }
        }
        return now;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) throws SemanticException {
        String id = name.id;
        CodeLocation loc = location;
        VarSym varSym = contractSym.newVarSym(id, type, isStatic, isFinal,
                                isBuiltin, loc, contractSym.defContext());
        contractSym.addVar(id, varSym, loc);
        if (type.label() != null) {
            varSym.setLabel(contractSym.newLabel(type.label()));
        }
        assert varSym.ifl != null;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        // logger.debug("entering AnnAssign: \n");
        // // logger.debug(this.toString() + "\n");
        String SLCNameVar, SLCNameVarLbl;
        VarSym varSym;
        String id = name.id;
        // logger.debug(scopeContext.toString() + " | " + scopeContext.isContractLevel());

        varSym = env.getVar(id);
        // logger.debug(varSym.getName());
        SLCNameVar = varSym.toSHErrLocFmt();
        SLCNameVarLbl = varSym.labelNameSLC();
        // logger.debug(varSym.typeSym.getName());
        // A field initialized here starts out naming the principal it is initialized to.
        // Only built-in fields have initializers; others are initialized in a constructor.
        if (varSym.isPrincipalTyped()) {
            env.addPrincipal(varSym);
            String init = value == null ? null : value.principalName(env);
            if (init != null) {
                env.assumeSamePrincipal(SLCNameVar, init);
            }
        } else if (varSym.typeSym.getName().equals(Utils.PRINCIPAL_TYPE)) {
            env.addPrincipal(varSym);
        }

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        // String ifNameTgtLbl = ifNameTgt + "..lbl";
        // Context prevContext = env.prevContext;

        env.checkFlow(ifNamePc, SLCNameVarLbl, location,
                "Integrity of control flow must be trusted to allow this assignment");

        //env.outContext = endContext;

        if (!tail_position) {
            env.checkFlow(endContext.lambda, beginContext.lambda, location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }
        if (value != null) {
            env.inContext = beginContext;
            ExpOutcome valueOutcome = value.genIFConstraints(env, scopeContext.isContractLevel());
            env.checkFlow(valueOutcome.valueLabelName, SLCNameVarLbl, value.location,
                    "Integrity of the value being assigned must be trusted to allow this assignment");
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
            return new VarDec(false, isTransient, varType, varName, valueExp);
        } else {
            return new VarDec(false, isTransient, varType, varName);
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

    /** Whether this field is final. */
    public boolean isFinal() {
        return isFinal;
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
        return new VarDec(varType, varName);
    }

    public VarSym toVarInfo(InterfaceSym interfaceSym) throws SemanticException {
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
