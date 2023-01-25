package ast;

import compile.SolCode;
import java.util.HashSet;
import typecheck.*;

import java.util.ArrayList;

public class ExceptionDef extends TopLayerNode {

    ExceptionType exceptionType;
    Arguments arguments;

    public ExceptionDef(String name, IfLabel ifl, Arguments members) {
        this.arguments = members;
        if (ifl == null) {
            this.exceptionType = new ExceptionType(new Type(name));
        } else {
            this.exceptionType = new ExceptionType(new LabeledType(name, ifl));
        }
        arguments.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        exceptionType.setContractName(env.curContractSym().getName());
        env.globalSymTab().add(exceptionType.name,
                env.toExceptionType(exceptionType.name, arguments, parent));
        return true;
    }

    /**
     * Collecting global info.
     * Creating an exception symbol in the symbol table of the current contract
     * @param contractSym
     */
    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        exceptionType.setContractName(contractSym.getName());
        contractSym.addType(exceptionType.name,
                contractSym.toExceptionType(exceptionType.name, exceptionType.type.ifl, arguments, contractSym.defContext()));
        // contractSym.addType(exceptionType, contractSym.toExceptionType(exceptionType, arguments));

    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(arguments);
        return rtn;
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    public String toString() {
        // return genson.serialize(exceptionName);
        return "";
    }
}
