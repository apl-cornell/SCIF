package typecheck;

import ast.Autoendorse;
import ast.FuncLabels;
import ast.IfLabel;

import java.util.ArrayList;
import java.util.HashSet;

import static typecheck.Utils.logger;

public class PolyFuncInfo extends FuncInfo {
    ArrayList<Integer> polyArgList;
    HashSet<String> polyArgs;
    int applyCounter;

    public PolyFuncInfo(String funcName, FuncLabels funcLabels, ArrayList<Integer> polyArgList, ArrayList<VarInfo> parameters, TypeInfo returnType, CodeLocation location) {
        super(funcName, funcLabels, parameters, returnType, location);
        this.polyArgList = polyArgList;
        this.polyArgs = new HashSet<>();
        for (int i : polyArgList) {
            polyArgs.add(getLabelNameArg(i));
        }
        applyCounter = 0;
        substitutePoly();
    }

    public void apply() {
        applyCounter += 1;
    }

    @Override
    public String getLabelNameCallPc() {
        String rtn = super.getLabelNameCallPc();
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
    @Override
    public String getLabelNameReturn() {
        String rtn = super.getLabelNameReturn();
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

    @Override
    public String getCallPcLabel() {
        logger.debug("entering polyfuncinfo - getCallBeforeLabel");
        String rtn = "";
        if (funcLabels.begin_pc != null) {
            rtn = funcLabels.begin_pc.toSherrlocFmtApply(polyArgs, applyCounter);
        }
        else {
            return null;
        }
        logger.debug("getcallbeforelabel finished: " + rtn);
        return rtn;
    }

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

    @Override
    public String getReturnLabel() {
        if (returnType.ifl != null) {
            return returnType.ifl.toSherrlocFmtApply(polyArgs, applyCounter);
        } else {
            return null;
        }
    }

    public String getArgLabel(int index) {
        VarInfo varInfo = parameters.get(index);
        if (varInfo.typeInfo.ifl == null) return null;
        return varInfo.typeInfo.ifl.toSherrlocFmtApply(polyArgs, applyCounter);
    }

    public void substitutePoly() { //TODO: might need to copy
        logger.debug("polyArgList size: " + polyArgList.size());
        for (int index : polyArgList) {
            String argName = parameters.get(index).localName;
            String argLabelName = getLabelNameArg(index);
            if (funcLabels.begin_pc != null)
                funcLabels.begin_pc.replace(argName, argLabelName);
            if (returnType.ifl != null)
                returnType.ifl.replace(argName, argLabelName);
            for (VarInfo arg : parameters) {
                if (arg.typeInfo.ifl != null)
                    arg.typeInfo.ifl.replace(argName, argLabelName);
            }
        }
    }
}
