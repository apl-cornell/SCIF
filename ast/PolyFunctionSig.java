package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class PolyFunctionSig extends FunctionSig {
    ArrayList<Integer> polyArgs;

    public PolyFunctionSig(Expression name, ArrayList<String> polyArgs, Arguments args, ArrayList<Expression> decoratorList, Expression rnt) {
        super(name, args, decoratorList, rnt);
        this.polyArgs = new ArrayList<>();
        HashMap<String, Integer> argToIndex = new HashMap<>();
        for (int i = 0; i < args.args.size(); ++i) {
            Arg arg = args.args.get(i);
            argToIndex.put(arg.name.id, i);
            logger.debug("argToIndex: " + arg.name.id + " " + i);
        }
        for (String polyArgName : polyArgs) {
            //TODO: error handling : no such argName
            logger.debug("lookupIndex: " + polyArgName);
            this.polyArgs.add(argToIndex.get(polyArgName));
        }
    }


    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        String funcId;
        IfLabel callLabel = null, returnLabel = null;
        if (name instanceof LabeledType) {
            funcId = ((LabeledType) name).x.id;
            callLabel = ((LabeledType) name).ifl;
        } else {
            funcId = ((Name) name).id;
        }
        if (rnt instanceof LabeledType) {
            returnLabel = ((LabeledType) rnt).ifl;
        }
        ArrayList<VarInfo> argsInfo = args.parseArgs(contractInfo);
        logger.debug("creating polyFuncInfo with polyarg size: " + polyArgs.size());
        contractInfo.funcMap.put(funcId, new PolyFuncInfo(funcId, polyArgs, callLabel, argsInfo, returnLabel, location));
    }



    /*@Override
    public String genConsVisit(VisitEnv env) {
        super.genConsVisit(env);

        return null;
    }
    /*
        String originalCtxt = env.ctxt;
        String funcName = "";
        if (name instanceof LabeledType) {
            funcName = ((LabeledType) name).x.id;

            //((LabeledType) name).ifl.findPrincipal(env.principalSet);
        } else {
            funcName = ((Name) name).id;
        }
        FuncInfo funcInfo = env.funcMap.get(funcName);
        if (funcInfo.callLabel != null)
            funcInfo.callLabel.findPrincipal(env.principalSet);
        env.ctxt += funcName;// + location.toString();

        for (VarInfo arg : funcInfo.parameters) {
            if (arg.type.ifl != null) {
                arg.type.ifl.
            }
        }
        args.genConsVisit(env);

        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        FuncInfo funcInfo = env.funcMap.get(funcName);

        if (name instanceof LabeledType) {
            String ifNameCall = funcInfo.getLabelNameCallAfter();
            env.cons.add(new Constraint(new Inequality(ifNameCall, ifNamePc), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameCall), env.hypothesis, location));

        }

        env.varNameMap.incLayer();
        args.genConsVisit(env);
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        env.varNameMap.decLayer();


        if (rnt instanceof LabeledType) {
            if (rnt instanceof DepMap) {
                ((DepMap) rnt).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) rnt).ifl.findPrincipal(env.principalSet);
            }
        }
        env.ctxt = originalCtxt;
        return null;
    }

 */
}
