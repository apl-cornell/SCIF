package ast;

import typecheck.ContractInfo;
import typecheck.FuncInfo;
import typecheck.VarInfo;

import java.util.ArrayList;
import java.util.HashSet;

public class FunctionSig extends Statement {
    String name;
    FuncLabels funcLabels;
    Arguments args;
    ArrayList<String> decoratorList;
    Type rtn;
    public FunctionSig(String name, FuncLabels funcLabels, Arguments args, ArrayList<String> decoratorList, Type rnt) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = decoratorList;
        this.rtn = rnt;
    }
    public void setDecoratorList(ArrayList<String> decoratorList) {
        this.decoratorList = decoratorList;
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
}
