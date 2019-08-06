package typecheck;

import ast.Autoendorse;
import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import java.util.ArrayList;

public class FuncInfo {
    public String funcName;
    public IfLabel callLabel;
    public ArrayList<VarInfo> parameters;
    public IfLabel returnLabel;
    public CodeLocation location;

    public FuncInfo(String funcName, IfLabel callLabel, ArrayList<VarInfo> parameters, IfLabel returnLabel, CodeLocation location) {
        this.funcName = funcName;
        this.callLabel = callLabel;
        this.parameters = parameters;
        this.returnLabel = returnLabel;
        this.location = location;
    }

    public String getLabelNameCallBefore() {
        return Utils.getLabelNameFuncCallBefore(funcName);
    }

    public String getLabelNameCallAfter() {
        return Utils.getLabelNameFuncCallAfter(funcName);
    }

    public String getLabelNameReturn() {
        return Utils.getLabelNameFuncReturn(funcName);
    }

    public String getLabelNameArg(int index) {
        return Utils.getLabelNameArgLabel(funcName, parameters.get(index));
    }


    public String getCallBeforeLabel() {
        if (callLabel != null) {
            if (callLabel instanceof Autoendorse) {
                return ((Autoendorse) callLabel).from.toSherrlocFmt();
            } else {
                return callLabel.toSherrlocFmt();
            }
        }
        else {
            return null;
        }
    }

    public String getCallAfterLabel() {
        if (callLabel != null) {
            if (callLabel instanceof Autoendorse) {
                return ((Autoendorse) callLabel).to.toSherrlocFmt();
            } else {
                return callLabel.toSherrlocFmt();
            }
        }
        else {
            return null;
        }
    }

    public String getReturnLabel() {
        if (returnLabel != null) {
            return returnLabel.toSherrlocFmt();
        } else {
            return null;
        }
    }


    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return genson.serialize(this);
    }
}
