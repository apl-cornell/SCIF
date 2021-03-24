package typecheck;

import ast.FuncLabels;
import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import java.util.ArrayList;

public class FuncSym extends Sym {
    public String funcName;
    public FuncLabels funcLabels;
    public ArrayList<VarSym> parameters;
    public TypeSym returnType;
    public IfLabel returnLabel;
    public CodeLocation location;
    public ScopeContext scopeContext;

    public FuncSym(String funcName,
                   FuncLabels funcLabels,
                   ArrayList<VarSym> parameters,
                   TypeSym returnType,
                   IfLabel returnLabel,
                   ScopeContext scopeContext,
                   CodeLocation location) {
        // this.typeName = funcName;
        this.funcName = funcName;
        this.name = funcName;
        this.funcLabels = funcLabels;
        this.parameters = parameters;
        this.returnType = returnType;
        this.returnLabel = returnLabel;
        this.scopeContext = scopeContext;
        this.location = location;
    }

    public String getLabelNameCallPcBefore() {
        return Utils.getLabelNameFuncCallPcBefore(scopeContext.getSHErrLocName());
    }

    public String getLabelNameCallPcAfter() {
        return Utils.getLabelNameFuncCallPcAfter(scopeContext.getSHErrLocName());
    }

    public String getLabelNameRtnValue() {
        return Utils.getLabelNameFuncRtnValue(scopeContext.getSHErrLocName());
    }
    public String getLabelNameRtnLock() {
        return Utils.getLabelNameFuncRtnLock(scopeContext.getSHErrLocName());
    }
    public String getLabelNameCallLock() {
        return Utils.getLabelNameFuncCallLock(scopeContext.getSHErrLocName());
    }

    public String getLabelNameArg(int index) {
        return Utils.getLabelNameArgLabel(scopeContext.getSHErrLocName(), parameters.get(index));
    }

    public String getCallPcLabel() {
        if (funcLabels != null && funcLabels.begin_pc != null) {
            return funcLabels.begin_pc.toSherrlocFmt();
        }
        else {
            return null;
        }

    }
    public String getCallLockLabel() {
        if (funcLabels != null && funcLabels.gamma_label != null) {
            return funcLabels.gamma_label.toSherrlocFmt();
        }
        else {
            return null;
        }

    }


    /*public String getRtnLockLabel() {
        if (funcLabels != null && funcLabels.gamma_label != null) {
            return funcLabels.gamma_label.toSherrlocFmt();
        }
        else {
            return null;
        }

    }*/

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

    public String getCallAfterLabel() {
        if (funcLabels != null && funcLabels.to_pc != null) {
            return funcLabels.to_pc.toSherrlocFmt();
        }
        else {
            return null;
        }
    }

    public String getRtnValueLabel() {
        if (returnLabel != null) {
            return returnLabel.toSherrlocFmt();
        } else {
            return null;
        }
    }

    public boolean isLValue() {
        return false;
    }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return genson.serialize(this);
    }

    public String getLabelNameCallGamma() {
        return Utils.getLabelNameFuncCallGamma(scopeContext.getSHErrLocName());
    }
}
