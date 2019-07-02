package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Arg extends Node {
    Name name;
    Expression annotation;
    public Arg(Name name, Expression annotation) {
        this.name = name;
        this.annotation = annotation;
    }

    public VarInfo globalInfoVisit() {
        if (annotation instanceof LabeledType)
            return new VarInfo(name.id, ((LabeledType) annotation).ifl, location);
        else
            return new VarInfo(name.id, null, location);
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifName = ctxt + "." + name.id;
        varNameMap.add(name.id, ifName);
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        if (annotation instanceof LabeledType) {
            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
        }
    }

}
