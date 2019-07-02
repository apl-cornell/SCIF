package utils;

import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import utils.CodeLocation;
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

    public String getIfNameCallLabel() {
        return Utils.getIfNameFuncCall(funcName);
    }
    public String getIfNameReturnLabel() {
        return Utils.getIfNameFuncReturn(funcName);
    }

    public String getIfNameArgLabel(int index) {
        return Utils.getIfNameArgLabel(funcName, parameters.get(index));
    }


    public String getCallLabel() {
        if (callLabel != null) {
            return callLabel.toSherrlocFmt();
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
