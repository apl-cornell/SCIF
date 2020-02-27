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
    public boolean NTCGlobalInfo(NTCEnv env, NTCContext parent) {
        NTCContext now = new NTCContext(this, parent);
        ArrayList<VarInfo> argsInfo = args.parseArgs(env, now);
        env.addSym(name, new FuncSym(name, new FuncInfo(name, funcLabels, argsInfo, env.toTypeInfo(rtn, false), location)));
        return true;

    }

    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        ArrayList<VarInfo> argsInfo = args.parseArgs(contractInfo);
        contractInfo.funcMap.put(name, new FuncInfo(name, funcLabels, argsInfo, contractInfo.toTypeInfo(rtn, false), location));
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
}
