package typecheck;

import ast.Autoendorse;
import ast.FuncLabels;
import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import java.util.ArrayList;

public class FuncInfo extends Type {
    public String funcName;
    public FuncLabels funcLabels;
    public ArrayList<VarInfo> parameters;
    public TypeInfo returnType;
    public CodeLocation location;

    public FuncInfo(String funcName, FuncLabels funcLabels, ArrayList<VarInfo> parameters, TypeInfo returnType, CodeLocation location) {
        this.typeName = funcName;
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

    public String getLabelNameRtnValue() {
        return Utils.getLabelNameFuncRtnValue(funcName);
    }
    public String getLabelNameRtnLock() {
        return Utils.getLabelNameFuncRtnLock(funcName);
    }
    public String getLabelNameCallLock() {
        return Utils.getLabelNameFuncCallLock(funcName);
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
    public String getCallLockLabel() {
        if (funcLabels.begin_lock != null) {
            return funcLabels.begin_lock.toSherrlocFmt();
        }
        else {
            return null;
        }

    }
    public String getRtnLockLabel() {
        if (funcLabels.end_lock != null) {
            return funcLabels.end_lock.toSherrlocFmt();
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

    public String getRtnValueLabel() {
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
