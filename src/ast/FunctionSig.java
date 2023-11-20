package ast;

import compile.CompileEnv;
import compile.Utils;
import compile.ast.Argument;
import compile.ast.Type;
import java.util.List;
import java.util.Map.Entry;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FunctionSig extends TopLayerNode {

    boolean isPublic;

    public String getName() {
        return name;
    }

    String name;
    FuncLabels funcLabels;
    Arguments args;

    /**
     * a list of decorators, e.g., <code>public</code>, <code>private</code>. Can be null after
     * initialization and need to be set to default
     */
    List<String> decoratorList;
    LabeledType rtn;
    List<LabeledType> exceptionList;
    private boolean isConstructor, isNative;

    final private boolean isBuiltin;
    FuncSym funcSym;

    /**
     * @param name          local name of this method
     * @param funcLabels    information flow labels of this method <code>{a -> b; c; d}</code>
     * @param args          arguments of this method
     * @param decoratorList a list of decorators, e.g., <code>public</code>, <code>private</code>.
     *                      will be set to <code>private</code> if no visibility specified.
     * @param rtn           Type of the return value
     */
    public FunctionSig(String name, FuncLabels funcLabels, Arguments args,
            List<String> decoratorList, LabeledType rtn, boolean isConstructor,
            CodeLocation location) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = setToDefault(decoratorList);
        this.rtn = rtn;
        this.exceptionList = new ArrayList<>();
        this.isConstructor = isConstructor;
        this.isBuiltin = false;
        this.location = location;
        setDefault();
    }
    public FunctionSig(String name, FuncLabels funcLabels, Arguments args,
            List<String> decoratorList, LabeledType rtn, boolean isConstructor, boolean isBuiltIn,
            CodeLocation location) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = setToDefault(decoratorList);
        this.rtn = rtn;
        this.exceptionList = new ArrayList<>();
        this.isConstructor = isConstructor;
        this.isBuiltin = isBuiltIn;
        this.location = location;
        setDefault();
    }

    private void setDefault() {
        funcLabels.setToDefault(isConstructor, this.decoratorList, location);
        args.setToDefault(funcLabels.begin_pc);
        if (rtn != null && rtn.label() != null) {

            // pass
        } else {
            if (name.equals(Utils.CONSTRUCTOR_NAME)) {
                // return void{any}
                rtn = new LabeledType(new ast.Type(typecheck.Utils.VOID_TYPE), new PrimitiveIfLabel(new Name(
                        typecheck.Utils.LABEL_SENDER)));
            } else {
                assert rtn != null;
                rtn = new LabeledType(rtn.type(),
                        new PrimitiveIfLabel(new Name(typecheck.Utils.LABEL_THIS)));
            }
        }
        for (LabeledType exception : exceptionList) {
            exception.setToDefault(rtn.label());
        }
    }

    private List<String> setToDefault(List<String> decoratorList) {
        if (decoratorList == null) {
            return List.of(Utils.PRIVATE_DECORATOR);
        } else {
            boolean isPublic = false, isPrivate = false;
            for (String dec: decoratorList) {
                if (dec.equals(Utils.PUBLIC_DECORATOR)) {
                    isPublic = true;
                    this.isPublic = true;
                }
                if (dec.equals(Utils.PRIVATE_DECORATOR)) {
                    isPrivate = true;
                }
                if (dec.equals(Utils.NATIVE_DECORATOR)) {
                    isNative = true;
                }
            }
            if (!(isPrivate || isPublic)) {
                decoratorList.add(Utils.PRIVATE_DECORATOR);
            } else if (isPrivate && isPublic) {
                assert false : "method " + name + " can not be both public and private";
            }

            return decoratorList;
        }
    }

    public FunctionSig(String name, FuncLabels funcLabels, Arguments args,
            List<String> decoratorList, LabeledType rtn, List<LabeledType> exceptionList,
            boolean isConstructor, CodeLocation location) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = setToDefault(decoratorList);
        this.rtn = rtn;
        this.exceptionList = exceptionList;
        this.isConstructor = isConstructor;
        this.isBuiltin = false;
        this.location = location;
        setDefault();
    }

    public FunctionSig(FunctionSig funcSig) {
        this.name = funcSig.name;
        this.funcLabels = funcSig.funcLabels;
        this.args = funcSig.args;
        this.decoratorList = funcSig.decoratorList;
        this.rtn = funcSig.rtn;
        this.exceptionList = funcSig.exceptionList;
        this.isConstructor = funcSig.isConstructor;
        this.isNative = funcSig.isNative;
        this.isPublic = funcSig.isPublic;
        this.isBuiltin = false;
        this.location = funcSig.location;
        setDefault();
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        SymTab contractSymTab = env.curSymTab();
        env.enterNewScope();
        ScopeContext now = new ScopeContext(this, parent);
        addBuiltInVars(env.curSymTab(), now);
        VarSym sender = (VarSym) env.getCurSym(typecheck.Utils.LABEL_SENDER);

        List<VarSym> argsInfo = args.parseArgs(env, now);
        Map<ExceptionTypeSym, String> exceptions = new HashMap<>();
        for (LabeledType t : exceptionList) {
            ExceptionTypeSym t1 = env.getExceptionTypeSym(t.type());
            assert t1 != null: t.type().name;
            exceptions.put(t1, null);
        }
        funcSym = new FuncSym(name,
                isPublic,
                isBuiltin,
                signature(),
                env.newLabel(funcLabels.begin_pc),
                env.newLabel(funcLabels.to_pc),
                env.newLabel(funcLabels.gamma_label),
                argsInfo, env.toTypeSym(rtn.type(), now),
                env.newLabel(rtn.label()),
                exceptions,
                parent, sender, location);
        if (!returnVoid()) {
            addBuiltInResult(env.curSymTab(), now);
        }
        contractSymTab.add(name, funcSym);
        env.exitNewScope();
        return true;

    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        SymTab realContractSymTab = contractSym.symTab;
        contractSym.symTab = new SymTab(contractSym.symTab);
        addBuiltInVars(contractSym.symTab, scopeContext);
        VarSym sender = (VarSym) contractSym.symTab.lookup(typecheck.Utils.LABEL_SENDER);
        List<VarSym> argsInfo = args.parseArgs(contractSym);
        Label ifl = null;
        if (rtn != null) {
            ifl = contractSym.newLabel(((LabeledType) rtn).label());
        }
        Map<ExceptionTypeSym, String> exceptions = new HashMap<>();
        for (LabeledType t : exceptionList) {
            IfLabel label = null;
            /*if (t.type instanceof  LabeledType)
                label = ((LabeledType) t.type).ifl;*/
            ExceptionTypeSym exceptionTypeSym = contractSym.getExceptionSym(t.type().name);
            assert exceptionTypeSym != null;
            exceptions.put(exceptionTypeSym,
                    typecheck.Utils.getLabelNameFuncExpLabel(scopeContext.getSHErrLocName(),
                            t.type().name()));
        }
//      System.err.println("adding method: " + name);
        funcSym =
                new FuncSym(name,
                        isPublic,
                        isBuiltin,
                        signature(),
                        contractSym.newLabel(funcLabels.begin_pc),
                        contractSym.newLabel(funcLabels.to_pc),
                        contractSym.newLabel(funcLabels.gamma_label),
                        argsInfo, contractSym.toTypeSym(rtn.type(), scopeContext), ifl, exceptions,
                        contractSym.defContext(), sender, location);
        if (!returnVoid()) {
            addBuiltInResult(contractSym.symTab, scopeContext);
        }
        realContractSymTab.add(name, funcSym);
        contractSym.symTab = contractSym.symTab.getParent();
    }

//    public String rtnToSHErrLocFmt() {
//        return toSHErrLocFmt() + ".RTN";
//    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        passScopeContext();
    }

    public void passScopeContext() {
        for (Node node : children()) {
            if (node != null) //TODO: remove null check
            {
                node.passScopeContext(scopeContext);
            }
        }
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    public compile.ast.FunctionSig solidityCodeGen(CompileEnv code) {
        code.enterNewVarScope();
        boolean isPublic = false, isPayable = false;
        if (decoratorList != null) {
            if (decoratorList.contains(typecheck.Utils.PUBLIC_DECORATOR)) {
                isPublic = true;
            }
            if (decoratorList.contains(typecheck.Utils.PAYABLE_DECORATOR)) {
                isPayable = true;
            }
        }

        if (isConstructor) {
            assert false;
            code.exitVarScope();
            return null;
        } else {
            String methodName = isPublic ? typecheck.Utils.methodNameHash(name, this) : name;
            List<Argument> arguments = args.solidityCodeGen(code);
            Type returnType, originalReturnType = rtn.type().solidityCodeGen(code);
            if (exceptionFree()) {
                returnType = originalReturnType;
            } else {
                returnType = compile.Utils.UNIVERSAL_RETURN_TYPE;
            }
            code.exitVarScope();
            return new compile.ast.FunctionSig(methodName, arguments, returnType, isPublic, isPayable);
        }
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {

        assert false;
        return null;
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        if (funcLabels != null) {
            rtn.add(funcLabels);
        }
        rtn.add(args);
        if (this.rtn != null) {
            rtn.add(this.rtn);
        }
        return rtn;
    }

    public boolean typeMatch(FunctionSig f) {
        if (true) {
            return f.signature().equals(signature());
        }
        if (!f.name.equals(name)) {
            return false;
        }
        if (!f.funcLabels.typeMatch(funcLabels)) {
            return false;
        }
        if (!f.args.typeMatch(args)) {
            return false;
        }

        if (!(decoratorList == null && f.decoratorList == null)) {
            if (decoratorList == null || f.decoratorList == null
                    || decoratorList.size() != f.decoratorList.size()) {
                return false;
            }
            int index = 0;
            while (index < decoratorList.size()) {
                if (!decoratorList.get(index).equals(f.decoratorList.get(index))) {
                    return false;
                }
                ++index;
            }
        }
        if (!f.rtn.typeMatch(rtn)) {
            return false;
        }
        return true;
    }

    public boolean isBuiltIn() {
        return isBuiltin;
    }


    protected void addBuiltInVars(SymTab curSymTab, ScopeContext now) {
        // final address{this} sender;
        VarSym varSender =
                new VarSym(
                        typecheck.Utils.LABEL_SENDER,
                        (TypeSym) curSymTab.lookup(typecheck.Utils.ADDRESS_TYPE),
                        null,
                        typecheck.Utils.BUILTIN_LOCATION,
                        now,
                        true,
                        true,
                        true
                );
        PrimitiveLabel labelSender = new PrimitiveLabel(varSender, typecheck.Utils.BUILTIN_LOCATION);
        varSender.setLabel(labelSender);
        curSymTab.add(typecheck.Utils.LABEL_SENDER, varSender);

        // final uint{sender} value;
        curSymTab.add(typecheck.Utils.LABEL_PAYVALUE,
                new VarSym(
                        typecheck.Utils.LABEL_PAYVALUE,
                        (TypeSym) curSymTab.lookup(typecheck.Utils.BuiltinType2ID(BuiltInT.UINT)),
                        labelSender,
                        typecheck.Utils.BUILTIN_LOCATION,
                        now,
                        true,
                        true,
                        true
                ));
        // final uint{sender} value;

        // other built-in vars
        for (Entry<String, String> entry: typecheck.Utils.BUILTIN_INMETHOD_VARS.entrySet()) {
            String varName = entry.getKey();
            String typeName = entry.getValue();
            VarSym varSym = new VarSym(varName, (TypeSym) curSymTab.lookup(typeName), labelSender,
                    typecheck.Utils.BUILTIN_LOCATION, now, true, true, true);
            curSymTab.add(varName, varSym);
        }
    }

    protected void addBuiltInResult(SymTab curSymTab, ScopeContext now) {
        assert !returnVoid();
        curSymTab.add(typecheck.Utils.RESULT_VARNAME,
                new VarSym(
                        typecheck.Utils.RESULT_VARNAME,
                        funcSym.returnType,
                        funcSym.rtn,
                        typecheck.Utils.BUILTIN_LOCATION,
                        now,
                        false,
                        false,
                        true
                ));

    }

    public void setPublic() {
        decoratorList.removeIf(d -> d.equals(Utils.PRIVATE_DECORATOR));
        decoratorList.add(Utils.PUBLIC_DECORATOR);
    }

    public String signature() {
        StringBuilder sb = new StringBuilder();
        String separator = "$";
        sb.append(name);
        for (String s: decoratorList) {
            if (s.equals(Utils.NATIVE_DECORATOR)) continue;
            sb.append(s);
        }
        sb.append(separator);
        sb.append(funcLabels.begin_pc);
        sb.append(funcLabels.to_pc);
        sb.append(funcLabels.gamma_label);
        sb.append(separator);

        for (Arg arg: args.args()) {
            sb.append(arg.isFinal ? "F" : "NF");
            sb.append(arg.annotation.type().name);
            sb.append(arg.annotation.label());
        }
        sb.append(separator);
        for (LabeledType exp: exceptionList) {
            sb.append(exp.type().name);
            sb.append(exp.label());
        }
        sb.append(separator);
        sb.append(rtn.type().name);
        sb.append(rtn.label());
        return sb.toString();
    }

    public boolean exceptionFree() {
        return exceptionList.size() == 0;
    }

    public boolean returnVoid() {
        return rtn.type().isVoid();
    }

    public void changeName(String genSuperName) {
        name = genSuperName;
    }

    public boolean isConstructor() {
        return isConstructor;
    }
    public boolean isNative() {
        return isNative;
    }

    public void makeNonConstructor() {
        isConstructor = false;
    }
}
