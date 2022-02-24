package ast;

import compile.Utils;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FunctionSig extends Statement {
    public String name;
    public FuncLabels funcLabels;
    public Arguments args;
    public ArrayList<String> decoratorList;
    public Type rtn;
    public ArrayList<ExceptionType> exceptionList;
    public boolean isConstructor;
    public FunctionSig(String name, FuncLabels funcLabels, Arguments args, ArrayList<String> decoratorList, Type rtn) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = decoratorList;
        this.rtn = rtn;
        this.exceptionList = new ArrayList<>();
        if (name.equals(Utils.CONSTRUCTOR_NAME)) {
            isConstructor = true;
        }

        funcLabels.setToDefault(isConstructor, decoratorList);
    }
    public FunctionSig(String name, FuncLabels funcLabels, Arguments args, ArrayList<String> decoratorList, Type rtn, ArrayList<ExceptionType> exceptionList) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = decoratorList;
        this.rtn = rtn;
        this.exceptionList = exceptionList;
        if (name.equals(Utils.CONSTRUCTOR_NAME)) {
            isConstructor = true;
        }

        funcLabels.setToDefault(isConstructor, decoratorList);
    }
    public FunctionSig(FunctionSig funcSig) {
        this.name = funcSig.name;
        this.funcLabels = funcSig.funcLabels;
        this.args = funcSig.args;
        this.decoratorList = funcSig.decoratorList;
        this.rtn = funcSig.rtn;
        this.exceptionList = funcSig.exceptionList;
        this.isConstructor = funcSig.isConstructor;
    }

    public void setDecoratorList(ArrayList<String> decoratorList) {
        this.decoratorList = decoratorList;
    }

    @Override
    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ArrayList<VarSym> argsInfo = args.parseArgs(env, now);
        ArrayList<ExceptionTypeSym> exceptions = new ArrayList<>();
        for (ExceptionType t : exceptionList) {
            t.setContractName(env.curContractSym.name);
            ExceptionTypeSym t1 = env.toExceptionTypeSym(t);
            // System.err.println("add func exp: " +  t1.name);
            // ExceptionTypeSym exceptionType = env.get(t);
            exceptions.add(t1);
        }
        env.addSym(name, new FuncSym(name, funcLabels, argsInfo, env.toTypeSym(rtn), null, exceptions, scopeContext, location));
        return true;

    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        ArrayList<VarSym> argsInfo = args.parseArgs(contractSym);
        IfLabel ifl = null;
        if (rtn instanceof LabeledType)
            ifl = ((LabeledType) rtn).ifl;
        ArrayList<TypeWithLabel> exceptions = new ArrayList<>();
        /*for ( t : exceptionList) {
            IfLabel label = null;
            if (t instanceof  LabeledType)
                label = ((LabeledType) rtn).ifl;
            TypeSym exceptionType = contractSym.toTypeSym(t);
            exceptions.add(new TypeWithLabel(exceptionType, label));
        }*/
        //contractSym.symTab.add(name, new FuncSym(name, funcLabels, argsInfo, contractSym.toTypeSym(rtn), ifl, exceptions, scopeContext, location));
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
        for (Node node : children()) {
            if (node != null) //TODO: remove null check
                node.passScopeContext(scopeContext);
        }
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

    public boolean typeMatch(FunctionSig f) {
        if (!f.name.equals(name))
            return false;
        if (!f.funcLabels.typeMatch(funcLabels))
            return false;
        if (!f.args.typeMatch(args))
            return false;

        if (!(decoratorList == null && f.decoratorList == null)) {
            if (decoratorList == null || f.decoratorList == null || decoratorList.size() != f.decoratorList.size())
                return false;
            int index = 0;
            while (index < decoratorList.size()) {
                if (!decoratorList.get(index).equals(f.decoratorList.get(index)))
                    return false;
                ++index;
            }
        }
        if (!f.rtn.typeMatch(rtn))
            return false;
        return true;
    }
}
