package ast;

import compile.SolCode;
import java.util.List;
import typecheck.*;

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

    public ArrayList<VarSym> parseArgs(NTCEnv env, ScopeContext parent) {
        // ScopeContext now = new ScopeContext(this, parent);
        ArrayList<VarSym> rnt = new ArrayList<>();
        for (Arg arg : args) {
            rnt.add(arg.parseArg(env, parent));
        }
        return rnt;
    }

    public ArrayList<VarSym> parseArgs(ContractSym contractSym) {
        ArrayList<VarSym> rnt = new ArrayList<>();
        for (Arg arg : args) {
            rnt.add(arg.parseArg(contractSym));
        }
        return rnt;
    }

    public void genConsVisit(VisitEnv env, boolean tail_position) {
        int index = 0;
        for (Arg arg : args) {
            ++index;
            arg.genConsVisit(env, index == args.size() && tail_position);
        }
    }

    public String toSolCode() {
        String rtn = "";
        boolean first = true;
        for (Arg arg : args) {
            if (!first) {
                rtn += ", ";
            } else {
                first = false;
            }
            // rnt += "type name"
            rtn += arg.toSolCode();
        }
        return rtn;
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
}
