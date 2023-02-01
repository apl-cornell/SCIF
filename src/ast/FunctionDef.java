package ast;

import compile.SolCode;
import java.util.List;
import java.util.Map.Entry;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FunctionDef extends FunctionSig {

    List<Statement> body;

    public FunctionDef(String name, FuncLabels funcLabels, Arguments args,
            List<Statement> body, List<String> decoratorList, Type rtn, boolean isConstructor) {
        super(name, funcLabels, args, decoratorList, rtn, isConstructor);
        this.body = body;
    }
    public FunctionDef(String name, FuncLabels funcLabels, Arguments args,
            List<Statement> body, List<String> decoratorList, Type rtn, boolean isConstructor, boolean isBuiltIn) {
        super(name, funcLabels, args, decoratorList, rtn, isConstructor, isBuiltIn);
        this.body = body;
    }

    public FunctionDef(FunctionSig funcSig, List<Statement> body) {
        super(funcSig);
        this.body = body;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        env.setCurSymTab(new SymTab(env.curSymTab()));
        // add args to local sym;
        String funcName = this.name;
        logger.debug("func: " + funcName);
        FuncSym funcSym = ((FuncSym) env.getCurSym(funcName));
        HashMap<ExceptionTypeSym, Boolean> exceptionTypeSyms = new HashMap<>();
        for (HashMap.Entry<ExceptionTypeSym, String> t : funcSym.exceptions.entrySet()) {
            exceptionTypeSyms.put(t.getKey(), true);
            // System.err.println("func sig exp: " + t.name);
        }

        ScopeContext now = new ScopeContext(this, parent, exceptionTypeSyms);
        // now.printExceptionSet();

        // add built-in vars
        addBuiltInVars(env.curSymTab(), now);

        for (Arg arg : this.args.args()) {
            arg.ntcGenCons(env, now);
        }
        funcLabels.ntcGenCons(env, now);
        if (funcSym.returnType != null) {
            env.addCons(new Constraint(new Inequality(funcSym.returnTypeSLC(), Relation.EQ,
                    funcSym.returnType.toSHErrLocFmt()), env.globalHypothesis(), location,
                    env.curContractSym().getName(),
                    "Label of this method's return value"));
        }

        if (!isBuiltIn()) {
            // TODO: add support for signatures
            for (Statement stmt : body) {
                // logger.debug("stmt: " + stmt);
                stmt.ntcGenCons(env, now);
            }
        }
        env.setCurSymTab(env.curSymTab().getParent());
        return now;
    }


    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        if (isBuiltIn()) return null;
        env.incScopeLayer();
        addBuiltInVars(env.curSymTab, scopeContext);

        for (Entry<String, VarSym> entry: env.curSymTab.getVars().entrySet()) {
            VarSym varSym = entry.getValue();
            if (varSym.isFinal && (varSym.typeSym instanceof ContractSym || varSym.typeSym.getName().equals(Utils.ADDRESS_TYPE))) {
                env.addPrincipal(varSym);
            }
        }

        String funcLocalName = name;

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        FuncSym funcSym = env.getFunc(funcLocalName);
        String funcFullName = funcSym.toSHErrLocFmt();
        // Context curContext = new Context(ifNamePc, Utils.getLabelNameFuncRtnLock(funcName), Utils.getLabelNameInLock(location));
        String inLockName = Utils.getLabelNameInLock(funcFullName);
        String outLockName = Utils.getLabelNameFuncRtnLock(funcFullName);
        String outPcName = Utils.getLabelNameFuncRtnPc(funcFullName);
        Context curContext = new Context(ifNamePc, inLockName);

        String ifNameCall = funcSym.internalPcSLC();
        env.addTrustConstraint(
                new Constraint(new Inequality(ifNameCall, Relation.EQ, ifNamePc), env.hypothesis(),
                        funcLabels.to_pc.location, env.curContractSym.getName(),
                        "Control flow of this method start with its call-after(second) label"));

        String ifNameContract = env.curContractSym.getLabelNameContract();
        env.addTrustConstraint(new Constraint(new Inequality(ifNameContract, ifNameCall), env.hypothesis(),
                funcLabels.begin_pc.location, env.curContractSym.getName(),
                "This contract should be trusted enough to call this method"));

        String ifNameGamma = funcSym.getLabelNameCallGamma();
        env.addTrustConstraint(new Constraint(new Inequality(inLockName, ifNamePc), env.hypothesis(),
                funcLabels.to_pc.location, env.curContractSym.getName(),
                "The statically locked integrity must be at least as trusted as initial pc integrity"));
        env.cons.add(
                new Constraint(new Inequality(Utils.joinLabels(inLockName, outLockName), ifNameGamma),
                        env.hypothesis(), funcLabels.gamma_label.location, env.curContractSym.getName(),
                        "This function does not maintain reentrancy locks as specified in signature",
                        1));

        HashMap<ExceptionTypeSym, PsiUnit> psi = new HashMap<>();
        for (Map.Entry<ExceptionTypeSym, String> exp : funcSym.exceptions.entrySet()) {
            psi.put(exp.getKey(), new PsiUnit(
                    //new Context(funcSym.getLabelNameException(exp.getKey()), ifNameGamma), true));
                    new Context(exp.getKey().labelNameSLC(), ifNameGamma), true));
        }

        Context funcBeginContext = curContext;
        PsiUnit funcEndContext = new PsiUnit(outPcName, outLockName);
        // env.inContext = funcBeginContext;
        // env.outContext = funcEndContext;

        args.genConsVisit(env, false);
        // Context prev = new Context(env.prevContext);//, prev2 = null;
        CodeLocation loc = null;
        PathOutcome CO = null;
        int index = 0;
        for (Statement stmt : body) {
            if (index == 0) {
                env.inContext = funcBeginContext;
            }
            // Context CO = new Context(Utils.getLabelNamePc(stmt.location), Utils.getLabelNameLock(stmt.location));
            // env.outContext = CO;
            ++index;
            if (stmt instanceof DynamicStatement) {
                //TODO: ifc check for dynamic statement
                continue;
            }
            CO = stmt.genConsVisit(env, index == body.size() && tail_position);
            //Context CO = env.outContext;
            env.inContext = new Context(CO.getNormalPath().c);
        }
        if (body.size() > 0) {
            assert CO != null;
            Utils.contextFlow(env, CO.getNormalPath().c, funcEndContext.c,
                    body.get(body.size() - 1).location);
        }

        env.decScopeLayer();

        return null;
    }
    /*public void findPrincipal(HashSet<String> principalSet) {
        if (sig.name instanceof LabeledType) {
            ((LabeledType) sig.name).ifl.findPrincipal(principalSet);
        }
        sig.args.findPrincipal(principalSet);

        if (sig.rnt instanceof LabeledType) {
            ((LabeledType) sig.rnt).ifl.findPrincipal(principalSet);
        }
    }*/

    public void SolCodeGen(SolCode code) {
        boolean pub = false;
        boolean payable = false;
        if (decoratorList != null) {
            if (decoratorList.contains(Utils.PUBLIC_DECORATOR)) {
                pub = true;
            }
            if (decoratorList.contains(Utils.PAYABLE_DECORATOR)) {
                payable = true;
            }
        }
        String rtnTypeCode = "";
        if (rtn != null && !this.rtn.isVoid()) {
            rtnTypeCode = rtn.toSolCode();
        }

        if (isConstructor) {
            code.enterConstructorDef(args.toSolCode(), body);
        } else {
            code.enterFunctionDef(name, args.toSolCode(), rtnTypeCode, pub, payable);
        }

        /*
            f{pc}(x_i{l_i}) from sender
            assert sender => pc, l_i
         */
        if (!isConstructor) {
            code.enterFuncCheck(funcLabels, args);
        }
        for (Statement stmt : body) {
            if (stmt instanceof DynamicStatement) {
                continue;
            }
            /*if (stmt instanceof Expression) {
                ((Expression) stmt).SolCodeGenStmt(code);
            }*/

            stmt.solidityCodeGen(code);
        }

        code.leaveFunctionDef();
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = super.children();
        rtn.addAll(body);
        return rtn;
    }
//
//    @Override
//    public String toSHErrLocFmt() {
//        return this.getClass().getSimpleName() + "." + getName() + "." + location;
//    }
}
