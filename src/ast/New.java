package ast;

import compile.CompileEnv;
import compile.ast.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.Context;
import typecheck.ContractSym;
import typecheck.ExpOutcome;
import typecheck.FuncSym;
import typecheck.Label;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.StructTypeSym;
import typecheck.Sym;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

public class New extends Expression {
    Call constructor_call;

    boolean isConstructor = false;

    public New(Call constructor_call) {
        this.constructor_call = constructor_call;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        // check constructor_call to be a valid contract constructor call
        assert constructor_call.value instanceof Name : "Contract name must be in the current namespace: " + location.errString();
        String name = ((Name) constructor_call.value).id;
        Sym sym = env.getCurSym(name);
        assert sym instanceof ContractSym || sym instanceof StructTypeSym : "Not a contract or struct type: " + location.errString();
        if (sym instanceof ContractSym) {
            isConstructor = true;
            String contractName = name;
            FuncSym constructorSym = ((ContractSym) sym).getConstructorSym();
            assert !env.inConstructor() || env.superCalled() :
                    "should not call methods before called super in constructor: " + contractName
                            + " at " + location.toString();
            assert constructor_call.args.size() == constructorSym.parameters.size() :
                    "number of values provided does not match the number of arguments of the called method: "
                            + contractName + " at " + location.toString();
            this.constructor_call.funcSym = constructorSym;
            if (constructor_call.callSpec != null) {
                constructor_call.callSpec.genTypeConstraints(env, now);
            }
            // typecheck arguments
            for (int i = 0; i < constructor_call.args.size(); ++i) {
                Expression arg = constructor_call.args.get(i);
                TypeSym paraInfo = constructorSym.parameters.get(i).typeSym;
                ScopeContext argContext = arg.genTypeConstraints(env, now);
                String typeName = paraInfo.toSHErrLocFmt();
                env.addCons(argContext.genTypeConstraints(typeName, Relation.GEQ, env, arg.location));
            }
            String rtnTypeName = sym.toSHErrLocFmt();
            env.addCons(now.genTypeConstraints(rtnTypeName, Relation.EQ, env, location));

            return now;
        } else {
            String structName = name;
            StructTypeSym structTypeSym = (StructTypeSym) sym;
            assert constructor_call.args.size() == structTypeSym.merberSize() :
                    "number of values provided does not match the number of arguments of the called method: "
                            + structTypeSym.getName() + " at " + location.toString();
            // this.constructor_call.funcSym = structTypeSym;
            // typecheck arguments
            for (int i = 0; i < constructor_call.args.size(); ++i) {
                Expression arg = constructor_call.args.get(i);
                TypeSym paraInfo = structTypeSym.getMemberVarInfo(structName, i).typeSym;
                ScopeContext argContext = arg.genTypeConstraints(env, now);
                String typeName = paraInfo.toSHErrLocFmt();
                env.addCons(argContext.genTypeConstraints(typeName, Relation.GEQ, env, arg.location));
            }
            String rtnTypeName = structTypeSym.toSHErrLocFmt();
            env.addCons(now.genTypeConstraints(rtnTypeName, Relation.EQ, env, location));

            return now;
        }
    }

    @Override
    public ExpOutcome genIFConstraints(VisitEnv env, boolean tail_position) {
        if (!isConstructor) {
            // creating a struct var returns a join of its arg values
            Context beginContext = env.inContext;
            Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                    typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

            List<String> argValueLabelNames = new ArrayList<>();
            String ifNameRtn = scopeContext.getSHErrLocName() + "." + "newStruct" + location.toString();

            PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
            ExpOutcome ao = null;

            for (Expression arg : constructor_call.args) {
                ao = arg.genIFConstraints(env, false);
                psi.joinExe(ao.psi);
                argValueLabelNames.add(ao.valueLabelName);
                env.inContext = new Context(
                        Utils.joinLabels(ao.psi.getNormalPath().c.pc, beginContext.pc),
                        beginContext.lambda);
                env.addTrustConstraint(new Constraint(new Inequality(ao.valueLabelName, ifNameRtn), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of left hand expression doesn't flow to value of this binary operation"));
            }

            return new ExpOutcome(ifNameRtn, psi);
        } else {
            // creating contract requires pc and arguments to be of integirty level `this`
            Context beginContext = env.inContext;
            Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                    typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
            Map<String, String> dependentLabelMapping = new HashMap<>();

            List<String> argValueLabelNames = new ArrayList<>();

            PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
            ExpOutcome ao = null;

            for (Expression arg : constructor_call.args) {
                ao = arg.genIFConstraints(env, false);
                psi.joinExe(ao.psi);
                argValueLabelNames.add(ao.valueLabelName);
                env.inContext = new Context(
                        Utils.joinLabels(ao.psi.getNormalPath().c.pc, beginContext.pc),
                        beginContext.lambda);
            }

            if (constructor_call.callSpec != null) {
                PathOutcome co = constructor_call.callSpec.genConsVisit(env, false);
                psi.joinExe(co);
                env.inContext = typecheck.Utils.genNewContextAndConstraints(env, false, co.getNormalPath().c, beginContext.lambda, constructor_call.callSpec.nextPcSHL(), constructor_call.callSpec.location);

            }

            String ifNamePc;
            FuncSym funcSym;
            Label ifFuncCallPcBefore, ifFuncCallPcAfter, ifFuncGammaLock;
            ExpOutcome vo = null;

            assert constructor_call.value instanceof Name;
            funcSym = constructor_call.funcSym;

            ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
            dependentLabelMapping.put(funcSym.sender().toSHErrLocFmt(),
                    env.thisSym().toSHErrLocFmt());

            ifFuncCallPcBefore = funcSym.externalPc();
            ifFuncCallPcAfter = funcSym.internalPc();
            ifFuncGammaLock = funcSym.callGamma();

            for (int i = 0; i < constructor_call.args.size(); ++i) {
                Expression arg = constructor_call.args.get(i);
                VarSym argSym = funcSym.parameters.get(i);
                if (argSym.isPrincipalVar()) {

                    if (arg instanceof Name) {
                        VarSym valueSym = (VarSym) env.getVar(((Name) arg).id);
                        if (valueSym.isPrincipalVar()) {
                            dependentLabelMapping.put(argSym.toSHErrLocFmt(),
                                    valueSym.toSHErrLocFmt());
                        }
                    }
                }

                // env.prevContext = prevContext = tmp;
                String ifNameArgValue = argValueLabelNames.get(i);
                Label ifArgLabel = funcSym.getLabelArg(i);
                assert ifArgLabel != null : argSym.getName();
                env.cons.add(
                        new Constraint(
                                new Inequality(
                                        ifNameArgValue,
                                        Relation.LEQ,
                                        ifArgLabel.toSHErrLocFmt(dependentLabelMapping)
                                ),
                                env.hypothesis(), arg.location, env.curContractSym().getName(),
                                "Input to the " + Utils.ordNumString(i + 1)
                                        + " argument must be trusted enough")
                );
                env.cons.add(
                        new Constraint(
                                new Inequality(
                                        ifNamePc,
                                        Relation.LEQ,
                                        ifArgLabel.toSHErrLocFmt(dependentLabelMapping)),
                                env.hypothesis(), arg.location, env.curContractSym().getName(),
                                "Current control flow must be trusted to feed the "
                                        + Utils.ordNumString(i + 1)
                                        + "-th argument value")
                );
            }

            PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
                    Utils.joinLabels(psi.getNormalPath().c.pc,
                            funcSym.endPc().toSHErrLocFmt(dependentLabelMapping)),
                    Utils.joinLabels(funcSym.getLabelNameCallGamma(), funcSym.internalPcSLC())
            )));

            typecheck.Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
            env.cons.add(
                    new Constraint(new Inequality(ifNamePc,
                            ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping)),
                            env.hypothesis(),
                            location, env.curContractSym().getName(),
                            "Current control flow must be trusted to call this method"));
            env.cons.add(new Constraint(
                    new Inequality(ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping),
                            Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping),
                                    beginContext.lambda)), env.hypothesis(),
                    location, env.curContractSym().getName(),
                    "Calling this function does not respect static reentrancy locks"));

            env.cons.add(new Constraint(
                    new Inequality(
                            Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping),
                                    ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping)),
                            Relation.EQ, endContext.lambda), env.hypothesis(), location,
                    env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

            if (!tail_position) {
                env.cons.add(new Constraint(
                        new Inequality(psi.getNormalPath().c.lambda, beginContext.lambda),
                        env.hypothesis(), location, env.curContractSym().getName(),
                        typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
            }

            String ifNameFuncRtnValue = funcSym.rtn.toSHErrLocFmt(dependentLabelMapping);
            // String ifNameFuncRtnLock = funcSym.getLabelNameRtnLock();
            psi.joinExe(expPsi);

            return new ExpOutcome(ifNameFuncRtnValue, psi);

        }
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        assert constructor_call.value instanceof Name;
        String contractName = ((Name) constructor_call.value).id;
        List<compile.ast.Expression> argExps = new ArrayList<>();
        for (Expression arg: constructor_call.args) {
            argExps.add(arg.solidityCodeGen(result, code));
        }
        return isConstructor ? new compile.ast.New(contractName, argExps, constructor_call.callSpec != null ? constructor_call.callSpec.solidityCodeGen(result, code) : null) : new compile.ast.Call(contractName, argExps);
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof New && constructor_call.typeMatch(((New) expression).constructor_call);
    }

    @Override
    public List<Node> children() {
        return List.of(constructor_call);
    }
}
