package ast;

import compile.CompileEnv;
import compile.ast.StructType;
import compile.ast.Type;
import compile.ast.VarDec;
import java.util.HashMap;
import java.util.List;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
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

        env.addCons(type.genTypeConstraints(tgt, Relation.EQ, env, location));
        if (value != null) {
            ScopeContext v = value.genTypeConstraints(env, now);
            env.addCons(tgt.genTypeConstraints(v, Relation.LEQ, env, location));
        } else if (isFinal) {
            throw new RuntimeException("final variable " + name + " not initialized at " + location);
        }
        return now;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
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
//        if (annotation.label() == null) {
//            annotation.setToDefault(env.curFuncExternalLabel);
//        }
        varSym = env.curContractSym().newVarSym(id, annotation, isStatic, isFinal, isBuiltIn, loc,
                scopeContext);
        try {
            env.addVar(id, varSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Already defined: " + id, location);
        }
        if (annotation != null) {
            if (annotation.label() != null) {
                varSym.setLabel(env.toLabel(annotation.label()));
            }
            env.cons.add(new Constraint(
                    new Inequality(varSym.labelNameSLC(), Relation.EQ, varSym.labelValueSLC()),
                    env.hypothesis(), location,
                    "Variable " + varSym.getName() + " may be labeled incorrectly"));
        }
        logger.debug(varSym.getName());
        SLCNameVar = varSym.toSHErrLocFmt();
        SLCNameVarLbl = varSym.labelNameSLC();
        logger.debug(varSym.typeSym.getName());

        // If the declared variable is a principal (final address/contract), add it to the principal list
        if ((varSym.isFinal &&
                (varSym.typeSym instanceof ContractSym || varSym.typeSym.getName().equals(Utils.ADDRESS_TYPE)))
                // || varSym.typeSym.name().equals(Utils.PRINCIPAL_TYPE)) Invalid to declare a non-global principal
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
                                "New principal declaration"
                        ));
            } else {
                throw new RuntimeException("A final address/Contract must be initialized to another final address/Contract: " + id);
            }
        }

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());

        env.cons.add(
                new Constraint(new Inequality(ifNamePc, SLCNameVarLbl), env.hypothesis(), location,
                        "Integrity of control flow must be trusted to allow this assignment"));

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }
        if (value != null) {
            env.inContext = beginContext;
            ExpOutcome valueOutcome = value.genIFConstraints(env, scopeContext.isContractLevel());
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

//    public void findPrincipal(HashSet<String> principalSet) {
//        if (annotation instanceof LabeledType) {
//            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
//        }
//    }

    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        Type varType =
                annotation.type().solidityCodeGen(code);
        if (varType instanceof StructType) {
            ((StructType) varType).setStorage();
        }
//            isContractType ? new ContractType(annotation.type().name) :
//                new PrimitiveType(annotation.type().name);
        String varName = target.id;
        List<compile.ast.Statement> result = new ArrayList<>();
        if (value != null) {
            compile.ast.Expression valueExp;
            valueExp = value.solidityCodeGen(result, code);
            result.add(new VarDec(varType, varName,
                     valueExp));
        } else {
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
