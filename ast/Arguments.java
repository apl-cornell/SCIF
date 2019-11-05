package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
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

    public ArrayList<VarInfo> parseArgs(ContractInfo contractInfo) {
        ArrayList<VarInfo> rnt = new ArrayList<>();
        for (Arg arg : args) {
            rnt.add(arg.parseArg(contractInfo));
        }
        return rnt;
    }
    @Override
    public Context genConsVisit(VisitEnv env) {

        for (Arg arg : args) {
            arg.genConsVisit(env);
        }
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        for (Arg arg : args) {
            arg.findPrincipal(principalSet);
        }
    }
}
