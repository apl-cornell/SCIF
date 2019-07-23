package ast;

import utils.*;

import java.util.ArrayList;
import java.util.Collection;
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

    public ArrayList<VarInfo> globalInfoVisit() {
        ArrayList<VarInfo> rnt = new ArrayList<>();
        for (Arg arg : args) {
            rnt.add(arg.globalInfoVisit());
        }
        return rnt;
    }
    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {

        for (Arg arg : args) {
            arg.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        for (Arg arg : args) {
            arg.findPrincipal(principalSet);
        }
    }
}
