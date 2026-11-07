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
import typecheck.sherrlocUtils.Relation;

public class New extends Expression {
    Call constructor_call;

    /**
     * The node this expression resolves to when it builds a struct value instead of creating a
     * contract, as determined by the regular type check; every later pass is its. Null when it
     * creates a contract.
     */
    private Expression form = null;
    /** The principal of the contract this expression creates, once IFC-visited. */
    private String createdPrincipal = null;

    public New(Call constructor_call) {
        this.constructor_call = constructor_call;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        // check constructor_call to be a valid contract constructor call
        if (!(constructor_call.value instanceof Name)) {
            throw new SemanticException("new must name a contract or struct type in this contract's scope",
                    constructor_call.value.location);
        }
        String name = ((Name) constructor_call.value).id;
        Sym sym = env.getCurSym(name);
        if (!(sym instanceof ContractSym) && !(sym instanceof StructTypeSym)) {
            throw new SemanticException("not a contract or struct type: " + name,
                    constructor_call.value.location);
        }
        if (sym instanceof StructTypeSym) {
            form = new StructConstruction((Name) constructor_call.value, constructor_call.args);
            form.setLoc(location);
            form.scopeContext = scopeContext;
            return form.genTypeConstraints(env, parent);
        }
        String contractName = name;
        FuncSym constructorSym = ((ContractSym) sym).getConstructorSym();
        if (env.inConstructor()  && !env.superCalled()) {
            throw new SemanticException("should not call methods before called super in constructor: " + contractName,
                    location);
        }
        if (constructor_call.args.size() != constructorSym.parameters.size()) {
            throw new SemanticException("number of values provided " + constructor_call.args.size() +
                    " does not match the number of arguments of the called method: " +
                    constructorSym.parameters.size(), location);
        }
        this.constructor_call.funcSym = constructorSym;
        if (constructor_call.callSpec != null) {
            constructor_call.callSpec.genTypeConstraints(env, now);
        }
        // type-check arguments
        for (int i = 0; i < constructor_call.args.size(); ++i) {
            Expression arg = constructor_call.args.get(i);
            TypeSym paraInfo = constructorSym.parameters.get(i).typeSym;
            ScopeContext argContext = arg.genTypeConstraints(env, now);
            String typeName = paraInfo.toSHErrLocFmt();
            env.addCons(argContext.genTypeConstraints(typeName, Relation.GEQ, env, arg.location, "The " + Utils.ordNumString(i + 1) + " argument must have the type of its constructor parameter"));
        }
        String rtnTypeName = sym.toSHErrLocFmt();
        env.addCons(now.genTypeConstraints(rtnTypeName, Relation.EQ, env, location, "new returns the contract type it names"));

        return now;
    }

    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position) throws SemanticException {
        if (form != null) {
            return form.genIFConstraints(env, tail_position);
        }
        ExpOutcome result = newIFConstraints(env, tail_position);
        // A contract's constructor may call back and assign to any field, so fields hold
        // new principals afterwards.
        env.havocFields();
        return result;
    }

    @Override
    public String principalName(VisitEnv env) {
        return createdPrincipal;
    }

    private ExpOutcome newIFConstraints(VisitEnv env, boolean tail_position) throws SemanticException {
        // creating contract requires pc and arguments to be of integirty level `this`
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        Map<String, String> dependentLabelMapping = new HashMap<>();

        List<String> argValueLabelNames = new ArrayList<>();

        PathOutcome psi = new PathOutcome(new PsiUnit(endContext));
        ExpOutcome ao = null;

        List<String> argPrincipalNames = new ArrayList<>();
        for (Expression arg : constructor_call.args) {
            ao = arg.genIFConstraints(env, false);
            psi.joinExe(ao.psi);
            argValueLabelNames.add(ao.valueLabelName);
            argPrincipalNames.add(arg.principalName(env));
            env.inContext = new Context(
                    Utils.joinLabels(ao.psi.getNormalPath().c.pc, beginContext.pc),
                    beginContext.lambda);
        }

        if (constructor_call.callSpec != null) {
            PathOutcome co = constructor_call.callSpec.genIFConstraints(env, false);
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
        // The new contract is a principal about which nothing is known; in the
        // constructor's labels, its `this` is that principal.
        createdPrincipal = env.freshPrincipal(toSHErrLocFmt() + "..new");
        typecheck.InterfaceSym createdSym =
                env.getContract(((Name) constructor_call.value).id);
        dependentLabelMapping.put(createdSym.thisSym().toSHErrLocFmt(), createdPrincipal);

        ifFuncCallPcBefore = funcSym.externalPc();
        ifFuncCallPcAfter = funcSym.internalPc();
        ifFuncGammaLock = funcSym.callGamma();

        for (int i = 0; i < constructor_call.args.size(); ++i) {
            Expression arg = constructor_call.args.get(i);
            VarSym argSym = funcSym.parameters.get(i);
            if (argSym.isPrincipalTyped() && argPrincipalNames.get(i) != null) {
                dependentLabelMapping.put(argSym.toSHErrLocFmt(), argPrincipalNames.get(i));
            }

            // env.prevContext = prevContext = tmp;
            String ifNameArgValue = argValueLabelNames.get(i);
            Label ifArgLabel = funcSym.getLabelArg(i);
            assert ifArgLabel != null : argSym.getName();
            env.checkFlow(ifNameArgValue, ifArgLabel.toSHErrLocFmt(dependentLabelMapping),
                    arg.location, "The " + Utils.ordNumString(i + 1)
                    + " argument must be trusted at the label of its parameter");
            env.checkFlow(ifNamePc, ifArgLabel.toSHErrLocFmt(dependentLabelMapping),
                    arg.location, "Control flow must be trusted at the label of the "
                    + Utils.ordNumString(i + 1) + " parameter");
        }

        // pc_2 dropped
        // PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
        //         Utils.joinLabels(psi.getNormalPath().c.pc,
        //                 funcSym.endPc().toSHErrLocFmt(dependentLabelMapping)),
        //         Utils.joinLabels(funcSym.getLabelNameCallGamma(), funcSym.internalPcSLC())
        // )));
        PathOutcome expPsi = new PathOutcome(new PsiUnit(new Context(
                Utils.joinLabels(psi.getNormalPath().c.pc,
                        funcSym.endPc().toSHErrLocFmt(dependentLabelMapping)),
                funcSym.getLabelNameCallGamma()
        )));

        typecheck.Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);
        env.checkFlow(ifNamePc, ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping),
                location, "Control flow must be trusted at the external begin label of the constructor");
        // pc_2 dropped
        // env.cons.add(new Constraint(
        //         new Inequality(ifFuncCallPcBefore.toSHErrLocFmt(dependentLabelMapping),
        //                 Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping),
        //                         beginContext.lambda)), env.hypothesis(),
        //         location, env.curContractSym().getName(),
        //         "Calling this function does not respect static reentrancy locks"));

        // pc_2 dropped
        // env.cons.add(new Constraint(
        //         new Inequality(
        //                 Utils.joinLabels(ifFuncCallPcAfter.toSHErrLocFmt(dependentLabelMapping),
        //                         ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping)),
        //                 Relation.EQ, endContext.lambda), env.hypothesis(), location,
        //         env.curContractSym().getName(),
        //         typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        env.checkEqual(ifFuncGammaLock.toSHErrLocFmt(dependentLabelMapping),
                endContext.lambda, location,
                "The constructor maintains only its reentrancy lock label");

        if (!tail_position) {
            env.checkFlow(psi.getNormalPath().c.lambda, beginContext.lambda, location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }

        // Creating a contract does not make it trusted: the reference is trusted
        // no more than the new contract, and no more than the control flow that
        // decided to create it.
        String ifNameRtn = Utils.joinLabels(ifNamePc, createdPrincipal);
        psi.joinExe(expPsi);

        return new ExpOutcome(ifNameRtn, psi);

    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        if (form != null) {
            return form.solidityCodeGen(result, code);
        }
        assert constructor_call.value instanceof Name;
        String contractName = ((Name) constructor_call.value).id;
        List<compile.ast.Expression> argExps = new ArrayList<>();
        for (Expression arg: constructor_call.args) {
            argExps.add(arg.solidityCodeGen(result, code));
        }
        return new compile.ast.New(contractName, argExps, constructor_call.callSpec != null ? constructor_call.callSpec.solidityCodeGen(result, code) : null);
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof New && constructor_call.typeMatch(((New) expression).constructor_call);
    }

    @Override
    public List<Node> children() {
        return List.of(constructor_call);
    }

    @Override
    public Map<String, compile.ast.Type> readMap(CompileEnv code) {
        if (form != null) {
            return form.readMap(code);
        }
        Map<String, compile.ast.Type> result = new HashMap<>();
        for (Expression arg : constructor_call.args) {
            result.putAll(arg.readMap(code));
        }
        if (constructor_call.callSpec != null) {
            result.putAll(constructor_call.callSpec.readMap(code));
        }
        return result;
    }

    @Override
    protected TypeSym staticType(VisitEnv env) {
        return form != null ? form.type() : env.getContract(((Name) constructor_call.value).id);
    }
}
