package ast;

import compile.CompileEnv;
import compile.ast.StructDef;
import compile.ast.VarDec;

import java.util.stream.Collectors;
import typecheck.*;
import typecheck.exceptions.SemanticException;

import java.util.ArrayList;

public class ExceptionDef extends TopLayerNode {

    String exceptionName;
    String namespace;
    Arguments arguments;
    boolean isBuiltIn = false;
    public ExceptionDef(String name, Arguments members) {
        this(name, members, false);
    }
    public ExceptionDef(String name, Arguments members, Boolean isBuiltIn) {
        exceptionName = name;
        this.arguments = members;
        this.isBuiltIn = isBuiltIn;
        // Like fields, unlabeled data is trusted by this contract.
        arguments.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    @Override
    public void ntcGlobalInfo(NTCEnv env, ScopeContext parent)
        throws SemanticException
    {
        // exceptionType.setContractName(env.curContractSym().getName());
        ExceptionTypeSym expSym = env.newExceptionType(namespace, exceptionName, arguments, parent);
        try {
            env.addSym(exceptionName, expSym);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Exception already defined: " + exceptionName,
                    location);
        }
        // A parameter's label may name principal-typed parameters of the exception.
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        try {
            for (VarSym param : expSym.parameters()) {
                env.addSym(param.getName(), param);
            }
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Parameter already defined: " + e.id, location);
        }
        for (Arg arg : arguments.args()) {
            arg.annotation.label().genTypeConstraints(env, now);
        }
        env.exitNewScope();
    }

    /**
     * Collecting global info.
     * Creating an exception symbol in the symbol table of the current contract
     * @param contractSym
     */
    @Override
    public void globalInfoVisit(InterfaceSym contractSym) throws SemanticException {
        // exceptionType.setContractName(contractSym.getName());
        contractSym.addType(exceptionName,
                contractSym.toExceptionType(namespace, exceptionName, arguments, contractSym.defContext()),
                location);
        // contractSym.addType(exceptionType, contractSym.toExceptionType(exceptionType, arguments));
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);

        // ExceptionTypeSym expSym = env.newExceptionType(exceptionName, arguments, parent);
        // env.addSym(exceptionName, expSym);
        return now;
    }

    public StructDef solidityCodeGen(CompileEnv code) {
        code.enterNewVarScope();
        StructDef result = new StructDef(exceptionName, arguments.solidityCodeGen(code).stream().map(arg -> new VarDec(
                arg.type(), arg.name())).collect(Collectors.toList()));
        code.exitVarScope();
        return result;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(arguments);
        return rtn;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    public String toString() {
        // return genson.serialize(exceptionName);
        return "";
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    public boolean typeMatch(ExceptionDef exceptionDef) {
        return exceptionName.equals(exceptionDef.exceptionName) &&
                arguments.typeMatch(exceptionDef.arguments);
    }

    public void setNamespace(String name) {
        namespace = name;
    }
}
