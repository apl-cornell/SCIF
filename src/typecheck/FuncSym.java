package typecheck;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FuncSym extends Sym {
    public String funcName;
    // public FuncLabels funcLabels;
    public List<VarSym> parameters;
    public TypeSym returnType;
    // public IfLabel returnLabel;

    private VarSym sender;
    public final Label external_pc, internal_pc, gamma, rtn;

    public Map<ExceptionTypeSym, String> exceptions;
    public CodeLocation location;

    public FuncSym(String funcName,
                   Label external_pc,
                   Label internal_pc,
                   Label gamma,
                   List<VarSym> parameters,
                   TypeSym returnType,
                   Label returnLabel,
                   Map<ExceptionTypeSym, String> exceptions,
                   ScopeContext defContext,
            VarSym sender, CodeLocation location) {
        super(funcName, defContext);
        this.funcName = funcName;
        this.sender = sender;
        // this.funcLabels = funcLabels;
        assert external_pc != null;
        this.external_pc = external_pc;
        this.internal_pc = internal_pc;
        this.gamma = gamma;
        this.parameters = parameters;
        this.returnType = returnType;
        this.rtn = returnLabel;
        // this.returnLabel = returnLabel;
        this.exceptions = exceptions;
        // this.scopeContext = scopeContext;
        this.location = location;
    }
    public FuncSym(String funcName,
                Label external_pc,
                Label internal_pc,
                Label gamma,
                   ArrayList<VarSym> parameters,
                   TypeSym returnType,
                Label returnLabel,
                   ScopeContext defContext,
            VarSym sender, CodeLocation location) {
        super(funcName, defContext);
        // this.typeName = funcName;
        this.funcName = funcName;
        this.sender = sender;
        // this.name = funcName;
        assert external_pc != null;
        this.external_pc = external_pc;
        this.internal_pc = internal_pc;
        this.gamma = gamma;
        this.parameters = parameters;
        this.returnType = returnType;
        this.rtn = returnLabel;
        this.exceptions = new HashMap<>();
        // this.defContext = defContext;
        this.location = location;
    }

    public String externalPcSLC() {
        return toSHErrLocFmt() + "." + "extpc";
        // return Utils.getLabelNameFuncCallPcBefore(toSHErrLocFmt());
    }
//    public String getLabelNameCallPcBefore(String namespace) {
//        if (!Objects.equals(namespace, ""))
//            namespace += ".";
//        return namespace + Utils.getLabelNameFuncCallPcBefore(toSHErrLocFmt());
//    }

    public String internalPcSLC() {
        return toSHErrLocFmt() + "." + "inpc";
        // return Utils.getLabelNameFuncCallPcAfter(toSHErrLocFmt());
    }

    public String endPcSLC() {
        return toSHErrLocFmt() + "." + "endpc";
        // return Utils.getLabelNameCallPcEnd(toSHErrLocFmt());
    }

//    public String getLabelNameCallPcAfter(String namespace) {
//        if (!Objects.equals(namespace, ""))
//            namespace += ".";
//        return namespace + Utils.getLabelNameFuncCallPcAfter(toSHErrLocFmt());
//    }

    public String returnSLC() {
        return toSHErrLocFmt() + "." + "returnV";
        //return Utils.getLabelNameFuncRtnValue(toSHErrLocFmt());
    }

//    public String getLabelNameRtnValue(String namespace) {
//        if (!Objects.equals(namespace, ""))
//            namespace += "..";
//        return namespace + Utils.getLabelNameFuncRtnValue(toSHErrLocFmt());
//    }
    /*public String getLabelNameRtnLock() {
        return Utils.getLabelNameFuncRtnLock(scopeContext.getSHErrLocName());
    }*/

    public String getLabelNameArg(int index) {
        return parameters.get(index).labelNameSLC();
        // return Utils.getLabelNameArgLabel(toSHErrLocFmt(), parameters.get(index));
    }

//    public String getLabelNameArg(String namespace, int index) {
//        if (!Objects.equals(namespace, ""))
//            namespace += ".";
//        return namespace + Utils.getLabelNameArgLabel(toSHErrLocFmt(), parameters.get(index));
//    }

//    public String getLabelNameException(ExceptionTypeSym exp) {
//
//        return Utils.getLabelNameFuncExpLabel(toSHErrLocFmt(), exp.name());
//    }

    // TODO: remove namespace
    public String getCallPcLabel(String namespace) {
        assert external_pc != null;
        return external_pc.toSHErrLocFmt();
    }


    public String getCallLockLabel(String namespace) {
        return gamma.toSHErrLocFmt();
    }

    public String getCallAfterLabel(String namespace) {
        return internal_pc.toSHErrLocFmt();
    }

    public String getRtnValueLabel(String namespace) {
        return rtn.toSHErrLocFmt();
    }

    public boolean isLValue() {
        return false;
    }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return getName();//genson.serialize(this);
    }

    public String getLabelNameCallGamma() {
        return toSHErrLocFmt() + "." + "gamma";
        //return Utils.getLabelNameFuncCallGamma(toSHErrLocFmt());
    }

    public String returnTypeSLC() {
        return toSHErrLocFmt() + "." + "returnT";
    }

    public VarSym sender() {
        return sender;
    }

    public Label externalPc() {
        return external_pc;
    }

    public Label internalPc() {
        return internal_pc;
    }

    public Label callGamma() {
        return gamma;
    }

    public Label getLabelArg(int i) {
        return parameters.get(i).ifl;
    }

    public Label endPc() {
        return rtn;
    }
//    public String getLabelNameCallGamma(String namespace) {
//        if (!Objects.equals(namespace, ""))
//            namespace += ".";
//        return namespace + Utils.getLabelNameFuncCallGamma(toSHErrLocFmt());
//    }
}
