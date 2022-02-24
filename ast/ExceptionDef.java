package ast;

import typecheck.ContractSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

import java.util.ArrayList;

public class ExceptionDef extends NonFirstLayerStatement {
    ExceptionType exceptionType;
    Arguments arguments;
    public ExceptionDef(String name, Arguments members) {
        this.arguments = members;
        this.exceptionType = new ExceptionType(new Type(name));
    }


    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        /*if (exceptionType.isLocal(env.curContractSym.name)) {
        }*/
        exceptionType.setContractName(env.curContractSym.name);
        env.globalSymTab.add(exceptionType.x, env.toExceptionType(exceptionType.x, arguments, parent));
        return true;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        // contractSym.addType(exceptionType, contractSym.toExceptionType(exceptionType, arguments));

    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(arguments);
        return rtn;
    }

    public String toString() {
        // return genson.serialize(exceptionName);
        return "";
    }
}
