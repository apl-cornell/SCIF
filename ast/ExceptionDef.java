package ast;

import typecheck.*;

import java.util.ArrayList;

public class ExceptionDef extends Statement {
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

    public void globalInfoVisit(ContractSym contractSym) {
        exceptionType.setContractName(contractSym.name);
        contractSym.addType(exceptionType.x, contractSym.toExceptionType(exceptionType.x, arguments));
        // contractSym.addType(exceptionType, contractSym.toExceptionType(exceptionType, arguments));

    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(arguments);
        return rtn;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    public String toString() {
        // return genson.serialize(exceptionName);
        return "";
    }
}
