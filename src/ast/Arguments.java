package ast;

import compile.CompileEnv;
import compile.ast.Argument;
import compile.ast.SolNode;
import java.util.List;
import java.util.stream.Collectors;
import typecheck.*;
import typecheck.exceptions.SemanticException;

import java.util.ArrayList;

public class Arguments extends Node {

    private List<Arg> args;

    //TODO: deaults cons generate
    //TODO: kwonlyargs, varargs and kwarg
    public Arguments(List<Arg> args) {
        this.args = args;
    }

    public Arguments() {
        this.args = new ArrayList<>();
    }

    public void setToDefault(IfLabel ifl) {
        for (Arg arg : args) {
            arg.setToDefault(ifl);
        }
    }

    public void merge(Arguments y) {
        if (y.args != null) {
            this.args.addAll(y.args);
        }
    }

    public ArrayList<VarSym> parseArgs(NTCEnv env, ScopeContext parent)
            throws SemanticException
    {
        // ScopeContext now = new ScopeContext(this, parent);
        ArrayList<VarSym> rnt = new ArrayList<>();
        for (Arg arg : args) {
            rnt.add(arg.parseArg(env, parent));
        }
        return rnt;
    }

    public List<VarSym> parseArgs(InterfaceSym interfaceSym) throws SemanticException {
        List<VarSym> rnt = new ArrayList<>();
        for (Arg arg : args) {
            rnt.add(arg.parseArg(interfaceSym));
        }
        return rnt;
    }

    public void genConsVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        int index = 0;
        for (Arg arg : args) {
            ++index;
            arg.genConsVisit(env, index == args.size() && tail_position);
        }
    }

    public String toSolCode() {
        List<String> solArgs = new ArrayList<>();
        for (Arg arg : args) {
            solArgs.add(arg.toSolCode());
        }
        return String.join(", ", solArgs);
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) {
        return null;
    }

    public List<Argument> solidityCodeGen(CompileEnv code) {
        return args.stream().map(arg -> arg.solidityCodeGen(code)).collect(Collectors.toList());
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        if (args != null) {
            rtn.addAll(args);
        }
        return rtn;
    }

    public boolean typeMatch(Arguments arguments) {
        boolean bothArgsNull = arguments.args == null && args == null;

        if (!bothArgsNull) {
            if (args == null || arguments.args == null || args.size() != arguments.args.size()) {
                return false;
            }
            int index = 0;
            while (index < args.size()) {
                if (!args.get(index).typeMatch(arguments.args.get(index))) {
                    return false;
                }
                ++index;
            }
        }
        return true;
    }

    public Iterable<Arg> args() {
        return args;
    }

    public boolean empty() {
        return args.size() == 0;
    }
}
