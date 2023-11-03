package ast;

import compile.CompileEnv;
import compile.CompileEnv.ScopeType;
import compile.ast.PrimitiveType;
import compile.ast.SolNode;
import compile.ast.StructDef;
import compile.ast.VarDec;
import java.util.stream.Collectors;
import typecheck.*;

import java.util.ArrayList;

public class ExceptionDef extends TopLayerNode {

    String exceptionName;
    Arguments arguments;
    boolean isBuiltIn = false;

    public ExceptionDef(String name, Arguments members) {
        exceptionName = name;
        this.arguments = members;
        arguments.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }
    public ExceptionDef(String name, Arguments members, Boolean isBuiltIn) {
        exceptionName = name;
        this.arguments = members;
        this.isBuiltIn = isBuiltIn;
        arguments.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        // exceptionType.setContractName(env.curContractSym().getName());
        env.addSym(exceptionName,
                env.newExceptionType(exceptionName, arguments, parent));
        return true;
    }

    /**
     * Collecting global info.
     * Creating an exception symbol in the symbol table of the current contract
     * @param contractSym
     */
    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        // exceptionType.setContractName(contractSym.getName());
        contractSym.addType(exceptionName,
                contractSym.toExceptionType(exceptionName, arguments, contractSym.defContext()));
        // contractSym.addType(exceptionType, contractSym.toExceptionType(exceptionType, arguments));

    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);

        // ExceptionTypeSym expSym = env.newExceptionType(exceptionName, arguments, parent);
        // env.addSym(exceptionName, expSym);
        return now;
    }

    public StructDef solidityCodeGen(CompileEnv code) {
        return new StructDef(exceptionName, arguments.solidityCodeGen(code).stream().map(arg -> new VarDec(
                (PrimitiveType) arg.type(), arg.name())).collect(Collectors.toList()));
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

    public boolean isBuiltIn() {
        return isBuiltIn;
    }
}
