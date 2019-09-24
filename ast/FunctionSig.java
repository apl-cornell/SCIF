package ast;

import typecheck.ContractInfo;
import typecheck.FuncInfo;
import typecheck.VarInfo;

import java.util.ArrayList;
import java.util.HashSet;

public class FunctionSig extends Statement {
    Expression name;
    Arguments args;
    ArrayList<Expression> decoratorList;
    Expression rnt;
    public FunctionSig(Expression name, Arguments args, ArrayList<Expression> decoratorList, Expression rnt) {
        this.name = name;
        this.args = args;
        this.decoratorList = decoratorList;
        this.rnt = rnt;
    }
    public void setDecoratorList(ArrayList<Expression> decoratorList) {
        this.decoratorList = decoratorList;
    }

    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        String funcId;
        IfLabel callLabel = null, returnLabel = null;
        if (name instanceof LabeledType) {
            funcId = ((LabeledType) name).x.id;
            callLabel = ((LabeledType) name).ifl;
        } else {
            funcId = ((Name) name).id;
        }
        if (rnt instanceof LabeledType) {
            returnLabel = ((LabeledType) rnt).ifl;
        }

        ArrayList<VarInfo> argsInfo = args.parseArgs(contractInfo);
        contractInfo.funcMap.put(funcId, new FuncInfo(funcId, callLabel, argsInfo, returnLabel, location));
    }
    public void findPrincipal(HashSet<String> principalSet) {
        if (name instanceof LabeledType) {
            ((LabeledType) name).ifl.findPrincipal(principalSet);
        }
        args.findPrincipal(principalSet);

        if (rnt instanceof LabeledType) {
            ((LabeledType) rnt).ifl.findPrincipal(principalSet);
        }
    }
}
