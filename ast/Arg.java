package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Arg extends Node {
    String name;
    Type annotation;
    public Arg(String name, Type annotation) {
        this.name = name;
        this.annotation = annotation;
    }

    public VarInfo parseArg(ContractInfo contractInfo) {
        return contractInfo.toVarInfo(name + "?", name, annotation, false, location);
    }

    @Override
    public Context genConsVisit(VisitEnv env) {

        if (annotation instanceof LabeledType) {
            if (annotation instanceof DepMap) {
                ((DepMap) annotation).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) annotation).ifl.findPrincipal(env.principalSet);
            }
        }
        String ifName = env.ctxt + "." + name;
        VarInfo varInfo = env.contractInfo.toVarInfo(ifName, name, annotation, false, location);
        env.varNameMap.add(name, ifName, varInfo);

        if (varInfo.typeInfo.type.typeName.equals(Utils.ADDRESSTYPE)) {
            env.principalSet.add(varInfo.toSherrlocFmt());
        }

        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        if (annotation instanceof LabeledType) {
            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
        }
    }

}
