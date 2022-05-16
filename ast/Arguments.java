package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class Arguments extends Node {
    ArrayList<Arg> args;
    ArrayList<Expression> defaults; //corresponds to the last size(defualts) args
    //TODO: deaults cons generate
    //TODO: kwonlyargs, varargs and kwarg
    public Arguments(ArrayList<Arg> args, ArrayList<Expression> defaults) {
        this.args = args;
        this.defaults = defaults;
    }
    public Arguments() {
        this.args = new ArrayList<>();
        this.defaults = null;
    }
    public void merge(Arguments y) {
        if (y.args != null) {
            this.args.addAll(y.args);
        }
        if (y.defaults != null) {
            this.defaults.addAll(y.defaults);
        }
    }

    public ArrayList<VarSym> parseArgs(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ArrayList<VarSym> rnt = new ArrayList<>();
        for (Arg arg : args) {
            rnt.add(arg.parseArg(env, now));
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
    public void findPrincipal(HashSet<String> principalSet) {
        for (Arg arg : args) {
            arg.findPrincipal(principalSet);
        }
    }

    public String toSolCode() {
        String rtn = "";
        boolean first = true;
        for (Arg arg : args) {
            if (!first) {
                rtn += ", ";
            } else
                first = false;
            // rnt += "type name"
            rtn += arg.toSolCode();
        }
        return rtn;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        if (args != null)
            rtn.addAll(args);
        if (defaults != null)
            rtn.addAll(defaults);
        return rtn;
    }

    public boolean typeMatch(Arguments arguments) {
        boolean bothArgsNull = arguments.args == null && args == null;
        boolean bothDefaultsNull = defaults == null && arguments.defaults == null;

        if (!bothArgsNull) {
            if (args == null || arguments.args == null || args.size() != arguments.args.size())
                return false;
            int index = 0;
            while (index < args.size()) {
                if (!args.get(index).typeMatch(arguments.args.get(index)))
                    return false;
                ++index;
            }
        }

        if (!bothDefaultsNull) {
            if (defaults == null || arguments.defaults == null || defaults.size() != arguments.defaults.size())
                return false;
            int index = 0;
            while (index < defaults.size()) {
                if (!defaults.get(index).typeMatch(arguments.defaults.get(index)))
                    return false;
                ++index;
            }
        }

        return true;
    }
}
