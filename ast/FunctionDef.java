package ast;

import compile.SolCode;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class FunctionDef extends FunctionSig {
    ArrayList<Statement> body;
    public FunctionDef(String name, FuncLabels funcLabels, Arguments args, ArrayList<Statement> body, ArrayList<String> decoratorList, Type rtn) {
        super(name, funcLabels, args, decoratorList, rtn);
        this.body = body;
    }
    public FunctionDef(FunctionSig funcSig, ArrayList<Statement> body) {
        super(funcSig);
        this.body = body;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);

        // add args to local sym;
        String funcName = this.name;
        logger.debug("func: " + funcName);
        FuncSym funcSym = ((FuncSym) env.getCurSym(funcName));
        for (Arg arg : this.args.args) {
            arg.NTCgenCons(env, now);
        }
        if (funcSym.returnType != null) {
            env.addCons(new Constraint(new Inequality(rtnToSHErrLocFmt(), Relation.EQ, env.getSymName(funcSym.returnType.name)), env.globalHypothesis, location, env.curContractSym.name,
                    "Label of this method's return value"));
        }

        env.setCurSymTab(new SymTab(env.curSymTab));
        for (Statement stmt : body) {
            // logger.debug("stmt: " + stmt);
            stmt.NTCgenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        Context context = env.context;
        String funcName = name;

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        FuncSym funcSym = env.getFunc(funcName);
        Context curContext = new Context(ifNamePc, Utils.getLabelNameFuncRtnLock(funcName), Utils.getLabelNameInLock(location));


        String ifNameCall = funcSym.getLabelNameCallPcAfter();
        env.trustCons.add(new Constraint(new Inequality(ifNameCall, Relation.EQ, ifNamePc), env.hypothesis, funcLabels.to_pc.location, env.curContractSym.name,
                "Control flow of this method start with its call-after(second) label"));

        String ifNameContract = env.curContractSym.getLabelNameContract();
        env.trustCons.add(new Constraint(new Inequality(ifNameContract, ifNameCall), env.hypothesis, funcLabels.begin_pc.location, env.curContractSym.name,
                "This contract should be trusted enough to call this method"));

        String ifNameGamma = funcSym.getLabelNameCallGamma();
        env.trustCons.add(new Constraint(new Inequality(curContext.inLockName, ifNameCall), env.hypothesis, funcLabels.to_pc.location, env.curContractSym.name,
                "The statically locked integrity must be at least as trusted as initial pc integrity"));
        env.cons.add(new Constraint(new Inequality(Utils.makeJoin(curContext.inLockName, curContext.lockName), ifNameGamma), env.hypothesis, funcLabels.gamma_label.location, env.curContractSym.name,
                "This function does not maintain reentrancy locks as specified in signature", 1));

        Context funcBeginContext = new Context(curContext);
        env.context = funcBeginContext;


        env.incScopeLayer();
        args.genConsVisit(env, false);
        // Context prev = new Context(env.prevContext);//, prev2 = null;
        CodeLocation loc = null;
        int index = 0;
        for (Statement stmt : body) {
            ++index;
            if (stmt instanceof DynamicStatement) {
                //TODO: ifc check for dynamic statement
                continue;
            }
            Context tmp = stmt.genConsVisit(env, index == body.size() && tail_position);
            env.context = funcBeginContext;
        }
        env.decScopeLayer();

        return curContext;
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
            if (decoratorList.contains(Utils.PUBLIC_DECORATOR))
                pub = true;
            if (decoratorList.contains(Utils.PAYABLE_DECORATOR))
                payable = true;
        }
        String rtnTypeCode = "";
        if (rtn != null && !this.rtn.isVoid())
            rtnTypeCode = rtn.toSolCode();

        if (isConstructor) {
            code.enterConstructorDef(args.toSolCode(), body);
        }
        else {
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
            if (stmt instanceof Expression) {
                ((Expression) stmt).SolCodeGenStmt(code);
            }
            else {
                stmt.SolCodeGen(code);
            }
        }

        code.leaveFunctionDef();
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = super.children();
        rtn.addAll(body);
        return rtn;
    }
}
