package ast;

import compile.CompileEnv;
import compile.ast.StructType;
import compile.ast.Type;
import compile.ast.VarDec;
import java.util.HashMap;
import java.util.List;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class AnnAssign extends Statement {

    private final Name target;
    private LabeledType annotation;
    private Expression value;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isBuiltIn = false;
    private boolean isContractType = false;

    public AnnAssign(Name target, LabeledType annotation, Expression value,
            boolean isConst, boolean isFinal) {
        this.target = target;
        this.annotation = annotation;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
//        setToDefault();
    }
    public AnnAssign(Name target, LabeledType annotation, Expression value,
            boolean isConst, boolean isFinal, boolean isBuiltIn) {
        this.target = target;
        this.annotation = annotation;
        this.value = value;
        this.isStatic = isConst;
        this.isFinal = isFinal;
        this.isBuiltIn = isBuiltIn;
//        setToDefault();
    }

    public void setType(LabeledType type) {
        this.annotation = type;
    }

    public void setToDefault() {
        annotation.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public VarSym toVarInfo(InterfaceSym contractSym) throws SemanticException {
        IfLabel ifl = null;
        if (annotation != null) {
            ifl = annotation.label();
        }
        return contractSym.newVarSym(((Name) target).id, annotation, isStatic, isFinal, isBuiltIn, location,
                scopeContext);
    }
//
//    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
//        if (!(target instanceof Name)) {
//            return false;
//        }
//        ScopeContext now = new ScopeContext(this, parent);
//        ScopeContext tgt = new ScopeContext(target, now);
//
//        String name = ((Name) target).id;
//        env.globalSymTab().add(name,
//                env.newVarSym(name, annotation, isStatic, isFinal, isBuiltIn, location, tgt));
//        return true;
//    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        String name = ((Name) target).id;
        Sym symValue = env.getCurSym(name);
        if (symValue != null && !symValue.isGlobal()) {
            throw new SemanticException("local duplicate variable name " + name, location);
        }
        VarSym varSym = new VarSym(env.newVarSym(name, annotation, isStatic, isFinal, isBuiltIn, location, now));
//        isContractType = varSym.typeSym instanceof InterfaceSym;
        try {
            env.addSym(name, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Already defined: " + name, location);
        }
        if (annotation.label() != null) {
            varSym.setLabel(env.newLabel(annotation.label()));
        }
        ScopeContext type = annotation.genTypeConstraints(env, now);
        ScopeContext tgt = target.genTypeConstraints(env, now);

        env.addCons(type.genTypeConstraints(tgt, Relation.EQ, env, location, "Variable " + target.id + " has its declared type"));
        if (value != null) {
            ScopeContext v = value.genTypeConstraints(env, now);
            env.addCons(tgt.genTypeConstraints(v, Relation.LEQ, env, location, "The initial value must have the declared type of " + target.id));
        } else if (isFinal) {
            throw new SemanticException("final variable " + name + " not initialized", location);
        }
        return now;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

//        // logger.debug("entering AnnAssign: \n");
        // // logger.debug(this.toString() + "\n");
        String SLCNameVar, SLCNameVarLbl;
        VarSym varSym;
        String id = ((Name) target).id;
        // logger.debug(scopeContext.toString() + " | " + scopeContext.isContractLevel());
        CodeLocation loc = location;
//        if (annotation.label() == null) {
//            annotation.setToDefault(env.curFuncExternalLabel);
//        }
        varSym = env.curContractSym().newVarSym(id, annotation, isStatic, isFinal, isBuiltIn, loc,
                scopeContext);
        if (varSym.typeSym instanceof typecheck.ClosureTypeSym cs
                && annotation.type() instanceof ast.ClosureType astClosure) {
            cs.resolveLabels(env, astClosure);
        }
        try {
            env.addVar(id, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Already defined: " + id, location);
        }
        if (annotation != null) {
            if (annotation.label() != null) {
                varSym.setLabel(env.toLabel(annotation.label()));
            }
            env.checkEqual(varSym.labelNameSLC(), varSym.labelValueSLC(), location, "The label of variable "
                    + varSym.getName() + " may be wrong");
        }
//        // logger.debug(varSym.getName());
        SLCNameVar = varSym.toSHErrLocFmt();
        SLCNameVarLbl = varSym.labelNameSLC();
//        // logger.debug(varSym.typeSym.getName());

        if (varSym.isPrincipalTyped()) {
            env.addPrincipal(varSym);
        }

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.checkFlow(ifNamePc, SLCNameVarLbl, location,
                "Control flow must be trusted at the label of " + target.id + " to initialize it");

        if (!tail_position) {
            env.checkFlow(endContext.lambda, beginContext.lambda, location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }
        if (value != null) {
            env.inContext = beginContext;
            ExpOutcome valueOutcome = value.genIFConstraints(env, scopeContext.isContractLevel());
            // A principal-typed variable starts out naming the principal it is initialized to.
            String init = varSym.isPrincipalTyped() ? value.principalName(env) : null;
            if (init != null) {
                env.assumeSamePrincipal(SLCNameVar, init);
            }
            env.checkFlow(valueOutcome.valueLabelName, SLCNameVarLbl, value.location,
                    "The initial value must be trusted at the label of " + target.id);

            if (varSym.typeSym instanceof typecheck.ClosureTypeSym targetCs) {
                ClosureCreation.checkFlowInto(value, targetCs, new java.util.HashMap<>(), env,
                        value.location,
                        "The closure's labels must be compatible with the declared type of " + target.id);
            }
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

    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        Type varType =
                annotation.type().solidityCodeGen(code);
//            isContractType ? new ContractType(annotation.type().name) :
//                new PrimitiveType(annotation.type().name);
        String varName = target.id;
        List<compile.ast.Statement> result = new ArrayList<>();
        if (value != null) {
            compile.ast.Expression valueExp;
            valueExp = value.solidityCodeGen(result, code);
            if (varType instanceof StructType && !(valueExp instanceof compile.ast.Call)) {
                ((StructType) varType).setStorage();
            }
            result.add(new VarDec(varType, varName,
                     valueExp));
        } else {
            if (varType instanceof StructType) {
                ((StructType) varType).setStorage();
            }
            result.add(new VarDec(varType, varName));
        }
        code.addLocalVar(varName, varType);
        return result;
    }

//    @Override
//    public String toSolCode() {
//        if (value != null) {
//            return CompileEnv.genVarDef(annotation.toSolCode(), target.toSolCode(), isFinal,
//                    value.toSolCode());
//        } else {
//            return CompileEnv.genVarDef(annotation.toSolCode(), target.toSolCode(), isFinal);
//        }
//    }

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

    @Override
    protected java.util.Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        return value == null ? new HashMap<>() : value.readMap(code);
    }
}
