package typecheck;

import ast.Autoendorse;
import ast.FuncLabels;
import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import java.util.ArrayList;

public class FuncInfo {
    public String funcName;
    public FuncLabels funcLabels;
    public ArrayList<VarInfo> parameters;
    public TypeInfo returnType;
    public CodeLocation location;

    public FuncInfo(String funcName, FuncLabels funcLabels, ArrayList<VarInfo> parameters, TypeInfo returnType, CodeLocation location) {
        this.funcName = funcName;
        this.funcLabels = funcLabels;
        this.parameters = parameters;
        this.returnType = returnType;
        this.location = location;
    }

    public String getLabelNameCallPc() {
        return Utils.getLabelNameFuncCallPc(funcName);
    }

    /*public String getLabelNameCallAfter() {
        return Utils.getLabelNameFuncCallAfter(funcName);
    }*/

    public String getLabelNameReturn() {
        return Utils.getLabelNameFuncReturn(funcName);
    }

    public String getLabelNameArg(int index) {
        return Utils.getLabelNameArgLabel(funcName, parameters.get(index));
    }

    public String getCallPcLabel() {
        if (funcLabels.begin_pc != null) {
            return funcLabels.begin_pc.toSherrlocFmt();
        }
        else {
            return null;
        }

    }

    /*public String getCallBeforeLabel() {
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
    }*/

    /*public String getCallAfterLabel() {
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
    }*/

    public String getReturnLabel() {
        if (returnType.ifl != null) {
            return returnType.ifl.toSherrlocFmt();
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
