package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FunctionSig extends Statement {
    public String name;
    public FuncLabels funcLabels;
    public Arguments args;
    public ArrayList<String> decoratorList;
    public Type rtn;
    public FunctionSig(String name, FuncLabels funcLabels, Arguments args, ArrayList<String> decoratorList, Type rtn) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = decoratorList;
        this.rtn = rtn;
    }
    public FunctionSig(FunctionSig funcSig) {
        this.name = funcSig.name;
        this.funcLabels = funcSig.funcLabels;
        this.args = funcSig.args;
        this.decoratorList = funcSig.decoratorList;
        this.rtn = funcSig.rtn;
    }

    public void setDecoratorList(ArrayList<String> decoratorList) {
        this.decoratorList = decoratorList;
    }

    @Override
    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ArrayList<VarSym> argsInfo = args.parseArgs(env, now);
        env.addSym(name, new FuncSym(name, funcLabels, argsInfo, env.toTypeSym(rtn), null, location));
        return true;

    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        ArrayList<VarSym> argsInfo = args.parseArgs(contractSym);
        IfLabel ifl = null;
        if (rtn instanceof LabeledType)
            ifl = ((LabeledType) rtn).ifl;
        contractSym.symTab.add(name, new FuncSym(name, funcLabels, argsInfo, contractSym.toTypeSym(rtn), ifl, location));
    }
    public void findPrincipal(HashSet<String> principalSet) {
        if (funcLabels != null) {
            funcLabels.findPrincipal(principalSet);
        }
        args.findPrincipal(principalSet);

        if (rtn instanceof LabeledType) {
            if (rtn instanceof DepMap) {
                ((DepMap) rtn).findPrincipal(principalSet);
            } else {
                ((LabeledType) rtn).ifl.findPrincipal(principalSet);
            }
        }
    }

    public String rtnToSHErrLocFmt() {
        return toSHErrLocFmt() + ".RTN";
    }
    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children())
            node.passScopeContext(scopeContext);
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        if (funcLabels != null)
            rtn.add(funcLabels);
        rtn.add(args);
        if (this.rtn != null)
            rtn.add(this.rtn);
        return rtn;
    }
}
