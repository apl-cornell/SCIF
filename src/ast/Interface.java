package ast;

import compile.CompileEnv;
import compile.ast.SolNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import typecheck.BuiltInT;
import typecheck.CodeLocation;
import typecheck.InterfaceSym;
import typecheck.ScopeContext;
import typecheck.NTCEnv;

import java.util.ArrayList;
import java.util.HashSet;
import typecheck.Utils;

public class Interface extends TopLayerNode {

    String contractName;
    String superContractName;
    TrustSetting trustSetting;
    List<StructDef> structDefs;
    List<StateVariableDeclaration> varDeclarations;

    List<ExceptionDef> exceptionDefs;
    List<FunctionSig> funcSigs;

    public Interface(String contractName,
            String superContractName,
            List<StructDef> structDefs,
            List<ExceptionDef> exceptionDefs,
            List<FunctionSig> funcSigs) {
        this.contractName = contractName;
        this.superContractName = superContractName;
        this.structDefs = structDefs;
        this.exceptionDefs = exceptionDefs;
        this.funcSigs = funcSigs;

        varDeclarations = new ArrayList<>();
        trustSetting = new TrustSetting();
        /*for (FunctionSig f: funcSigs) {
            f.setPublic();
        }*/
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // SymTab curSymTab = new SymTab(env.curSymTab());
        env.enterNewScope();
        Utils.addBuiltInTypes(env.curSymTab());
        InterfaceSym interfaceSym = new InterfaceSym(contractName, env.curSymTab(), new ArrayList<>(), this);
        env.addContractSym(env.currentSourceFileFullName(), interfaceSym);
        env.addSym(contractName, interfaceSym);
        env.setCurContractSym(interfaceSym);
        // Utils.addBuiltInASTNode(contractSym, env.globalSymTab(), trustSetting);

        for (StructDef def: structDefs) {
            if (!def.ntcGlobalInfo(env, now)) {
                return false;
            }
        }

        for (StateVariableDeclaration varDef: varDeclarations) {
            if (!varDef.ntcGlobalInfo(env, now)) {
                return false;
            }
        }

        for (ExceptionDef def : exceptionDefs) {
            if (!def.ntcGlobalInfo(env, now)) {
                return false;
            }
        }

        for (FunctionSig fDef : funcSigs) {
            if (!fDef.ntcGlobalInfo(env, now)) {
                return false;
            }
        }

        env.exitNewScope();
        return true;
    }


    public void globalInfoVisit(InterfaceSym contractSym) {
        // contractSym.addContract(contractName, contractSym);
        Utils.addBuiltInTypes(contractSym.symTab);

        for (StructDef def: structDefs) {
            def.globalInfoVisit(contractSym);
        }

        for (StateVariableDeclaration dec : varDeclarations) {
            dec.globalInfoVisit(contractSym);
        }

        trustSetting.globalInfoVisit(contractSym);

        for (ExceptionDef expDef : exceptionDefs) {
            expDef.globalInfoVisit(contractSym);
        }


        for (FunctionSig stmt : funcSigs) {
            stmt.globalInfoVisit(contractSym);
        }

    }

//    public void findPrincipal(HashSet<String> principalSet) {
//        for (TrustConstraint trustConstraint : trustSetting.trust_list) {
//            trustConstraint.findPrincipal(principalSet);
//        }
//        for (FunctionSig stmt : funcSigs) {
//            stmt.findPrincipal(principalSet);
//        }
//    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    public compile.ast.Interface solidityCodeGen(CompileEnv code) {
        List<compile.ast.StructDef> structAndExcDefs = new ArrayList<>();
        for (StructDef structDef: structDefs) {
            structAndExcDefs.add(structDef.solidityCodeGen(code));
        }

        for (ExceptionDef exceptionDef: exceptionDefs) {
            if (!exceptionDef.arguments.empty()) {
                structAndExcDefs.add(exceptionDef.solidityCodeGen(code));
            }
        }

        List<compile.ast.FunctionSig> functionSigs = new ArrayList<>();
        for (FunctionSig functionSig: funcSigs)
            if (!functionSig.isBuiltIn()) {
                functionSigs.add(functionSig.solidityCodeGen(code));
            }

        return new compile.ast.Interface(contractName, structAndExcDefs, functionSigs);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(structDefs);
        rtn.addAll(exceptionDefs);
        rtn.addAll(funcSigs);
        return rtn;
    }
    public String getContractName() {
        return contractName;
    }
//
//    public boolean ntcInherit(InheritGraph graph) {
//        // add an edge from superclass to this contract
//        if (!superContractName.isEmpty()) {
//            graph.addEdge(superContractName, contractName);
//        }
//        return true;
//    }

    public void codePasteContract(Map<String, Contract> contractMap, Map<String, Interface> interfaceMap) {
        if (superContractName.isEmpty()) {
            return;
        }

        Interface superContract = interfaceMap.get(superContractName);
        if (superContract == null) {
            assert false: "super contract not found: " + superContractName;
            return;
        }

        // check that there are no duplicates
        Set<String> nameSet = new HashSet<>();
        for (StructDef structDef: structDefs) {
            nameSet.add(structDef.structName);
        }
        for (ExceptionDef exp: exceptionDefs) {
            nameSet.add(exp.exceptionName);
        }
        for (FunctionSig f: funcSigs) {
            nameSet.add(f.name);
        }

        for (StructDef structDef: superContract.structDefs) {
            if (structDef.isBuiltIn()) continue;
            if (nameSet.contains(structDef.structName)) {
                assert false: structDef.structName;
            }
        }
        for (ExceptionDef exp: superContract.exceptionDefs) {
            if (exp.isBuiltIn()) continue;
            if (nameSet.contains(exp.exceptionName)) {
                assert false: exp.exceptionName;
            }
        }
        for (FunctionSig f: superContract.funcSigs) {
            if (f.isBuiltIn()) continue;
            if (nameSet.contains(f.name)) {
                assert false: f.name + " from " + contractName;
            }
        }

        // paste exceptions and methods
        List<StructDef> newStrDefs = new ArrayList<>();
        List<ExceptionDef> newExpDefs = new ArrayList<>();
        List<FunctionSig> newFuncSigs = new ArrayList<>();

        newStrDefs.addAll(superContract.structDefs);
        newStrDefs.addAll(structDefs);
        for (StructDef structDef: structDefs) {
            if (structDef.isBuiltIn()) continue;
            newStrDefs.add(structDef);
        }
        newExpDefs.addAll(superContract.exceptionDefs);
//        newExpDefs.addAll(exceptionDefs);
        for (ExceptionDef exceptionDef: exceptionDefs) {
            if (exceptionDef.isBuiltIn()) continue;
            newExpDefs.add(exceptionDef);
        }
        newFuncSigs.addAll(superContract.funcSigs);
        for (FunctionSig functionSig: funcSigs) {
            if (functionSig.isBuiltIn()) continue;
            newFuncSigs.add(functionSig);
        }

        structDefs = newStrDefs;
        exceptionDefs = newExpDefs;
        funcSigs = newFuncSigs;
    }

    public void addBuiltIns() {
        addBuiltInTrustSettings();
        addBuiltInVars();
        addBuiltInExceptions();
        addBuiltInMethods();
    }



    private void addBuiltInMethods() {
        // add methods:
        final IfLabel labelThis = new PrimitiveIfLabel(new Name(Utils.LABEL_THIS));
        final IfLabel labelTop = new PrimitiveIfLabel(new Name(Utils.LABEL_TOP));
        final IfLabel labelBot = new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM));
        labelThis.setLoc(CodeLocation.builtinCodeLocation());
        labelTop.setLoc(CodeLocation.builtinCodeLocation());
        labelBot.setLoc(CodeLocation.builtinCodeLocation());

        // @protected
        // @final
        // void send{this -> TOP; BOT}(address target, uint amount);
        List<Arg> args = new ArrayList<>();
        Arg tmparg = new Arg(
                "target",
                new LabeledType(Utils.ADDRESS_TYPE, labelThis, CodeLocation.builtinCodeLocation()),
                false,
                true
        );
        args.add(tmparg);
        tmparg = new Arg(
                "amount",
                new LabeledType(Utils.BuiltinType2ID(BuiltInT.UINT), labelThis, CodeLocation.builtinCodeLocation()),
                false,
                true
        );
        args.add(tmparg);
        int count = 0;
        for (Arg arg : args) {
            CodeLocation location = new CodeLocation(1, count, "Builtin");
            arg.setLoc(location);
            arg.annotation.setLoc(location);
            arg.annotation.type().setLoc(location);
            ++count;
        }
        List<String> decs = new ArrayList<>();
        decs.add(Utils.PROTECTED_DECORATOR);
        decs.add(Utils.FINAL_DECORATOR);
        FunctionDef sendDef = new FunctionDef(
                Utils.METHOD_SEND_NAME,
                new FuncLabels(
                        labelThis,
                        labelTop,
                        labelBot,
                        labelBot
                ),
                new Arguments(args),
                new ArrayList<>(),
                decs,
                Utils.builtinLabeldType(BuiltInT.VOID),
                false,
                true,
                CodeLocation.builtinCodeLocation()
        );
        funcSigs.add(sendDef);

        // @public
        // final
        // uint{TOP} balance{BOT -> TOP; TOP}(final address addr);
        args = new ArrayList<>();
        args.add(new Arg(
                "addr",
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM)), CodeLocation.builtinCodeLocation()),
                false,
                true
        ));
        count = 0;
        for (Arg arg : args) {
            CodeLocation location = new CodeLocation(1, count, "Builtin");
            arg.setLoc(location);
            arg.annotation.setLoc(location);
            arg.annotation.type().setLoc(location);
            ++count;
        }
        decs = new ArrayList<>();
        decs.add(Utils.PUBLIC_DECORATOR);
        decs.add(Utils.FINAL_DECORATOR);
        FunctionDef balanceDef = new FunctionDef(
                Utils.METHOD_BALANCE_NAME,
                new FuncLabels(
                        labelBot,
                        labelBot,
                        labelTop,
                        labelTop
                ),
                new Arguments(args),
                new ArrayList<>(),
                decs,
                new LabeledType(Utils.BuiltinType2ID(BuiltInT.UINT), labelTop, CodeLocation.builtinCodeLocation()),
                false,
                true,
                CodeLocation.builtinCodeLocation()
        );
        funcSigs.add(balanceDef);
    }

    private void addBuiltInExceptions() {
        // add exceptions:
        // exception{BOT} Error();
//        ExceptionDef error = new ExceptionDef(
//                Utils.EXCEPTION_ERROR_NAME,
//                new Arguments(),
//                true
//        );
//        exceptionDefs.add(error);
    }

    private void addBuiltInVars() {
        // add variables:
        // final This{this} this;
        StateVariableDeclaration thisDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_THIS),
                new LabeledType(contractName, new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)), CodeLocation.builtinCodeLocation(0, 1)),
                null,
                true,
                true,
                true
        );
        thisDec.name().setLoc(CodeLocation.builtinCodeLocation(0, 0));
        thisDec.setLoc(CodeLocation.builtinCodeLocation(0, 0));
        StateVariableDeclaration topDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_TOP),
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_TOP)), CodeLocation.builtinCodeLocation(1, 1)),
                null,
                true,
                true,
                true
        );
        topDec.name().setLoc(CodeLocation.builtinCodeLocation(1, 0));
        topDec.setLoc(CodeLocation.builtinCodeLocation(1, 0));
        StateVariableDeclaration botDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_BOTTOM),
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM)), CodeLocation.builtinCodeLocation(2, 1)),
                null,
                true,
                true,
                true
        );
        botDec.name().setLoc(CodeLocation.builtinCodeLocation(2, 0));
        botDec.setLoc(CodeLocation.builtinCodeLocation(2, 0));
        List<StateVariableDeclaration> newDecs = new ArrayList<>();
        newDecs.add(topDec);
        newDecs.add(botDec);
        newDecs.add(thisDec);
        newDecs.addAll(varDeclarations);
        varDeclarations = newDecs;
    }

    private void addBuiltInTrustSettings() {
        trustSetting.addBuiltIns();
    }
}
