package typecheck;

import ast.Autoendorse;
import ast.IfLabel;

import java.util.ArrayList;
import java.util.HashSet;

import static typecheck.Utils.logger;

public class PolyFuncInfo extends FuncInfo {
    ArrayList<Integer> polyArgList;
    HashSet<String> polyArgs;
    int applyCounter;

    public PolyFuncInfo(String funcName, ArrayList<Integer> polyArgList, IfLabel callLabel, ArrayList<VarInfo> parameters, IfLabel returnLabel, CodeLocation location) {
        super(funcName, callLabel, parameters, returnLabel, location);
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
    public String getLabelNameCallBefore() {
        String rtn = super.getLabelNameCallBefore();
        if (applyCounter > 0)
            return rtn + ".apply" + applyCounter;
        else
            return rtn;
    }
    @Override
    public String getLabelNameCallAfter() {
        String rtn = super.getLabelNameCallAfter();
        if (applyCounter > 0)
            return rtn + ".apply" + applyCounter;
        else
            return rtn;
    }
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
    public String getCallBeforeLabel() {
        logger.debug("entering polyfuncinfo - getCallBeforeLabel");
        String rtn = "";
        if (callLabel != null) {
            if (callLabel instanceof Autoendorse) {
                rtn = ((Autoendorse) callLabel).from.toSherrlocFmtApply(polyArgs, applyCounter);
            } else {
                rtn = callLabel.toSherrlocFmtApply(polyArgs, applyCounter);
            }
        }
        else {
            return null;
        }
        logger.debug("getcallbeforelabel finished: " + rtn);
        return rtn;
    }

    @Override
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
    }

    @Override
    public String getReturnLabel() {
        if (returnLabel != null) {
            return returnLabel.toSherrlocFmtApply(polyArgs, applyCounter);
        } else {
            return null;
        }
    }

    public String getArgLabel(int index) {
        VarInfo varInfo = parameters.get(index);
        if (varInfo.type.ifl == null) return null;
        return varInfo.type.ifl.toSherrlocFmtApply(polyArgs, applyCounter);
    }

    public void substitutePoly() {
        logger.debug("polyArgList size: " + polyArgList.size());
        for (int index : polyArgList) {
            String argName = parameters.get(index).localName;
            String argLabelName = getLabelNameArg(index);
            if (callLabel != null)
                callLabel.replace(argName, argLabelName);
            if (returnLabel != null)
                returnLabel.replace(argName, argLabelName);
            for (VarInfo arg : parameters) {
                if (arg.type.ifl != null)
                    arg.type.ifl.replace(argName, argLabelName);
            }
        }
    }
}
