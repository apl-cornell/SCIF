package typecheck;

import ast.ExceptionType;
import ast.FuncLabels;
import ast.IfLabel;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class FuncSym extends Sym {
    public String funcName;
    public FuncLabels funcLabels;
    public ArrayList<VarSym> parameters;
    public TypeSym returnType;
    public IfLabel returnLabel;
    public HashMap<ExceptionTypeSym, String> exceptions;
    public CodeLocation location;
    public ScopeContext scopeContext;

    public FuncSym(String funcName,
                   FuncLabels funcLabels,
                   ArrayList<VarSym> parameters,
                   TypeSym returnType,
                   IfLabel returnLabel,
                   HashMap<ExceptionTypeSym, String> exceptions,
                   ScopeContext scopeContext,
                   CodeLocation location) {
        super(funcName);
        // this.typeName = funcName;
        this.funcName = funcName;
        // this.name = funcName;
        this.funcLabels = funcLabels;
        this.parameters = parameters;
        this.returnType = returnType;
        this.returnLabel = returnLabel;
        this.exceptions = exceptions;
        this.scopeContext = scopeContext;
        this.location = location;
    }
    public FuncSym(String funcName,
                   FuncLabels funcLabels,
                   ArrayList<VarSym> parameters,
                   TypeSym returnType,
                   IfLabel returnLabel,
                   ScopeContext scopeContext,
                   CodeLocation location) {
        super(funcName);
        // this.typeName = funcName;
        this.funcName = funcName;
        // this.name = funcName;
        this.funcLabels = funcLabels;
        this.parameters = parameters;
        this.returnType = returnType;
        this.returnLabel = returnLabel;
        this.exceptions = new HashMap<>();
        this.scopeContext = scopeContext;
        this.location = location;
    }

    public String getLabelNameCallPcBefore() {
        return Utils.getLabelNameFuncCallPcBefore(scopeContext.getSHErrLocName());
    }
    public String getLabelNameCallPcBefore(String namespace) {
        if (namespace != "")
            namespace += ".";
        return namespace + Utils.getLabelNameFuncCallPcBefore(scopeContext.getSHErrLocName());
    }

    public String getLabelNameCallPcAfter() {
        return Utils.getLabelNameFuncCallPcAfter(scopeContext.getSHErrLocName());
    }

    public String getLabelNameCallPcEnd() {
        return Utils.getLabelNameCallPcEnd(scopeContext.getSHErrLocName());
    }

    public String getLabelNameCallPcAfter(String namespace) {
        if (!Objects.equals(namespace, ""))
            namespace += ".";
        return namespace + Utils.getLabelNameFuncCallPcAfter(scopeContext.getSHErrLocName());
    }

    public String getLabelNameRtnValue() {
        return Utils.getLabelNameFuncRtnValue(scopeContext.getSHErrLocName());
    }
    public String getLabelNameRtnValue(String namespace) {
        if (!Objects.equals(namespace, ""))
            namespace += "..";
        return namespace + Utils.getLabelNameFuncRtnValue(scopeContext.getSHErrLocName());
    }
    /*public String getLabelNameRtnLock() {
        return Utils.getLabelNameFuncRtnLock(scopeContext.getSHErrLocName());
    }*/
    public String getLabelNameCallLock() {
        return Utils.getLabelNameFuncCallLock(scopeContext.getSHErrLocName());
    }

    public String getLabelNameArg(int index) {
        return Utils.getLabelNameArgLabel(scopeContext.getSHErrLocName(), parameters.get(index));
    }

    public String getLabelNameArg(String namespace, int index) {
        if (!Objects.equals(namespace, ""))
            namespace += ".";
        return namespace + Utils.getLabelNameArgLabel(scopeContext.getSHErrLocName(), parameters.get(index));
    }

    public String getLabelNameException(ExceptionTypeSym exp) {
        return Utils.getLabelNameFuncExpLabel(scopeContext.getSHErrLocName(), exp.getName());
    }
    public String getCallPcLabel(String namespace) {
        if (funcLabels != null && funcLabels.begin_pc != null) {
            return namespace + "." + funcLabels.begin_pc.toSherrlocFmt(scopeContext);
        }
        else {
            return null;
        }
    }


    public String getCallLockLabel(String namespace) {
        if (funcLabels != null && funcLabels.gamma_label != null) {
            return namespace + "." + funcLabels.gamma_label.toSherrlocFmt(scopeContext);
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


    public String getCallAfterLabel(String namespace) {
        if (funcLabels != null && funcLabels.to_pc != null) {
            return namespace + "." + funcLabels.to_pc.toSherrlocFmt(scopeContext);
        }
        else {
            return null;
        }
    }

    public String getRtnValueLabel(String namespace) {
        if (returnLabel != null) {
            return namespace + "." + returnLabel.toSherrlocFmt(scopeContext);
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
    public String getLabelNameCallGamma(String namespace) {
        if (!Objects.equals(namespace, ""))
            namespace += ".";
        return namespace + Utils.getLabelNameFuncCallGamma(scopeContext.getSHErrLocName());
    }
}
