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
            env.addCons(new Constraint(new Inequality(rtnToSHErrLocFmt(), Relation.EQ, env.getSymName(funcSym.returnType.name)), env.globalHypothesis, location));
        }

        env.setCurSymTab(new SymTab(env.curSymTab));
        for (Statement stmt : body) {
            stmt.NTCgenCons(env, now);
        }
        env.curSymTab = env.curSymTab.getParent();
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        // String originalCtxt = env.ctxt;
        Context originalContext = env.prevContext;
        String funcName = name;
        // env.ctxt += funcName;// + location.toString();

        args.genConsVisit(env);

        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        FuncSym funcSym = env.getFunc(funcName);


        String ifNameCall = funcSym.getLabelNameCallPcAfter();
        env.cons.add(new Constraint(new Inequality(ifNameCall, ifNamePc), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameCall), env.hypothesis, location));

        String ifNameRtn = funcSym.getLabelNameRtnValue();
        env.cons.add(new Constraint(new Inequality(ifNameCall, ifNameRtn), env.hypothesis, location));

        String ifNameContract = env.curContractSym.getLabelNameContract();
        env.cons.add(new Constraint(new Inequality(ifNameContract, ifNameCall), env.hypothesis, location));

        String ifNameCallLock = funcSym.getLabelNameCallLock();

        Context funcBeginContext = new Context(ifNamePc, ifNameCallLock);
        env.prevContext = funcBeginContext;


        env.incScopeLayer();
        args.genConsVisit(env);
        Context prev = new Context(env.prevContext), prev2 = null;
        CodeLocation loc = null;
        for (Statement stmt : body) {
            if (prev2 != null) {
                env.cons.add(new Constraint(new Inequality(prev.lockName, Relation.LEQ, prev2.lockName), env.hypothesis, loc));
            }
            Context tmp = stmt.genConsVisit(env);
            env.prevContext = tmp;
            prev2 = prev;
            loc = stmt.location;
            prev = new Context(tmp);
        }
        env.decScopeLayer();

        String ifNameGammaLock = funcSym.getLabelNameCallGamma();
        env.cons.add(new Constraint(new Inequality(prev.lockName, ifNameGammaLock), env.hypothesis, loc));

        /*if (rtn instanceof LabeledType) {
            if (rtn instanceof DepMap) {
                ((DepMap) rtn).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) rtn).ifl.findPrincipal(env.principalSet);
            }
        }*/
        // env.ctxt = originalCtxt;
        // don't recover
        // env.prevContext = originalContext;
        return env.prevContext;
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
        if (rtn != null)
            rtnTypeCode = rtn.toSolCode();

        code.enterFunctionDef(name, args.toSolCode(), rtnTypeCode, pub, payable);

        /*
            f{pc}(x_i{l_i}) from sender
            assert sender => pc, l_i
         */
        code.enterFuncCheck(name);
        for (Statement stmt : body) {
            stmt.SolCodeGen(code);
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
