package typecheck;

import ast.FuncLabels;
import ast.IfLabel;

import java.util.ArrayList;
import java.util.HashSet;

import static typecheck.Utils.logger;

public class PolyFuncSym extends FuncSym {
    ArrayList<Integer> polyArgList;
    HashSet<String> polyArgs;
    int applyCounter;

    public PolyFuncSym(String funcName,
            Label external_pc,
            Label internal_pc,
            Label gamma, ArrayList<Integer> polyArgList, ArrayList<VarSym> parameters, TypeSym returnType,

            Label returnLabel, ScopeContext scopeContext, CodeLocation location) {
        super(funcName,
                external_pc,
                internal_pc,
                gamma,
                parameters, returnType, returnLabel, scopeContext, location);
        this.polyArgList = polyArgList;
        this.polyArgs = new HashSet<>();
        for (int i : polyArgList) {
            polyArgs.add(getLabelNameArg(i));
        }
        applyCounter = 0;
        // substitutePoly();
    }

    public void apply() {
        applyCounter += 1;
    }

    public String getLabelNameCallPcAfter() {
        String rtn = super.internalPcSLC();
        if (applyCounter > 0)
            return rtn + ".apply" + applyCounter;
        else
            return rtn;
    }
    /*@Override
    public String getLabelNameCallAfter() {
        String rtn = super.getLabelNameCallAfter();
        if (applyCounter > 0)
            return rtn + ".apply" + applyCounter;
        else
            return rtn;
    }*/
    public String getLabelNameRtnValue() {
        String rtn = super.returnSLC();
        if (applyCounter > 0)
            return rtn + ".apply" + applyCounter;
        else
            return rtn;
    }
    @Override
    public String getLabelNameArg(int index) {
        String rtn = super.getLabelNameArg(index);
        if (applyCounter > 0)
            return rtn + ".apply" + applyCounter;
        else
            return rtn;
    }

    /*@Override
    public String getCallLockLabel() {
        String rtn = "";
        if (funcLabels.begin_lock != null) {
            rtn = funcLabels.begin_lock.toSherrlocFmtApply(polyArgs, applyCounter);
        }
        else {
            return null;
        }
        return rtn;
    }
    @Override
    public String getRtnLockLabel() {
        String rtn = "";
        if (funcLabels.end_lock != null) {
            rtn = funcLabels.end_lock.toSherrlocFmtApply(polyArgs, applyCounter);
        }
        else {
            return null;
        }
        return rtn;
    }*/

    /*@Override
    public String getCallAfterLabel() {
        if (callLabel != null) {
            if (callLabel instanceof Autoendorse) {
                return ((Autoendorse) callLabel).to.toSherrlocFmtApply(polyArgs, applyCounter);
            } else {
                return callLabel.toSherrlocFmtApply(polyArgs, applyCounter);
            }
        }
        else {
            return null;
        }
    }*/


//    public String getArgLabel(int index) {
//        VarSym varSym = parameters.get(index);
//        if (varSym.ifl == null) return null;
//        return varSym.ifl.toSherrlocFmtApply(polyArgs, applyCounter);
//    }

//    public void substitutePoly() { //TODO: might need to copy
//        logger.debug("polyArgList size: " + polyArgList.size());
//        for (int index : polyArgList) {
//            String argName = parameters.get(index).getName();
//            String argLabelName = getLabelNameArg(index);
//            if (funcLabels.begin_pc != null)
//                funcLabels.begin_pc.replace(argName, argLabelName);
//            if (returnLabel != null)
//                returnLabel.replace(argName, argLabelName);
//            for (VarSym arg : parameters) {
//                if (arg.ifl != null)
//                    arg.ifl.replace(argName, argLabelName);
//            }
//        }
//    }
}
