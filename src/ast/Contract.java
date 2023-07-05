package ast;

import compile.SolCode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Contract extends TopLayerNode {

    String contractName;
    String superContractName = "";
    final boolean extendsSuperContract;
    TrustSetting trustSetting;
    List<StateVariableDeclaration> varDeclarations;
    List<ExceptionDef> exceptionDefs;
    List<FunctionDef> methodDeclarations;

    public Contract(String contractName, TrustSetting trustSetting,
            List<StateVariableDeclaration> varDeclarations,
            List<ExceptionDef> exceptionDefs,
            List<FunctionDef> methodDeclarations) {
        this.contractName = contractName;
        this.trustSetting = trustSetting;
        this.trustSetting.labelTable.put("this", "address(this)");
        this.varDeclarations = varDeclarations;
        this.exceptionDefs = exceptionDefs;
        this.methodDeclarations = methodDeclarations;

        setDefault();
        extendsSuperContract = true;
    }

    public Contract(String contractName,
            String superContractName, boolean extendOrImplement,
            TrustSetting trustSetting,
            List<StateVariableDeclaration> varDeclarations,
            List<ExceptionDef> exceptionDefs,
            List<FunctionDef> methodDeclarations) {
        this.contractName = contractName;
        this.superContractName = superContractName;
        this.extendsSuperContract = extendOrImplement;
        this.trustSetting = trustSetting;
        this.trustSetting.labelTable.put("this", "address(this)");
        this.varDeclarations = varDeclarations;
        this.exceptionDefs = exceptionDefs;
        this.methodDeclarations = methodDeclarations;
        setDefault();
    }

    private void setDefault() {
        if (superContractName.isEmpty() && !contractName.equals(Utils.BASECONTRACTNAME)) {
            superContractName = Utils.BASECONTRACTNAME;
        }
    }
//
//    public boolean ntcInherit(InheritGraph graph) {
//        // add an edge from superclass to this contract
//        if (!superContractName.isEmpty()) {
//            graph.addEdge(superContractName, contractName);
//        }
//        return true;
//    }

    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // SymTab curSymTab = new SymTab(env.curSymTab());
        env.enterNewScope();
        Utils.addBuiltInTypes(env.curSymTab());
        // env.initSymTab(curSymTab);
        ContractSym contractSym = new ContractSym(contractName, env.curSymTab(), new ArrayList<>(), this);
        env.addGlobalSym(contractName, contractSym);
        env.setCurContractSym(contractSym);
        // Utils.addBuiltInASTNode(contractSym, env.globalSymTab(), trustSetting);

        for (StateVariableDeclaration dec : varDeclarations) {
            if (!dec.ntcGlobalInfo(env, now)) {
                return false;
            }
        }

        for (ExceptionDef def : exceptionDefs) {
            if (!def.ntcGlobalInfo(env, now)) {
                return false;
            }
        }

        for (FunctionDef fDef : methodDeclarations) {
            if (!fDef.ntcGlobalInfo(env, now)) {
                return false;
            }
        }

        env.exitNewScope();
        return true;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.setCurContractSym(env.getContract(contractName));

        for (StateVariableDeclaration dec : varDeclarations) {
            dec.ntcGenCons(env, now);
        }

        trustSetting.ntcGenCons(env, now);

        for (ExceptionDef def : exceptionDefs) {
            def.ntcGenCons(env, now);
        }

        for (FunctionDef fDef : methodDeclarations) {
            fDef.ntcGenCons(env, now);
        }

        return now;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        // contractSym.name = contractName;
//        contractSym.trustSetting = trustSetting;
        // contractSym.ifl = ifl;
        // contractSym.addContract(contractName, contractSym);
        Utils.addBuiltInTypes(contractSym.symTab);
        /*String name = "this";
        contractSym.addVar(name, contractSym.toVarSym(name,
                new LabeledType(contractName, new PrimitiveIfLabel(new Name("this"))), true, false,
                null, scopeContext));

         */

        // Utils.addBuiltInSymsIfc(contractSym);

        for (StateVariableDeclaration dec : varDeclarations) {
            dec.globalInfoVisit(contractSym);
        }

        trustSetting.globalInfoVisit(contractSym);

        for (ExceptionDef expDef : exceptionDefs) {
            expDef.globalInfoVisit(contractSym);
        }

        for (FunctionDef fDef : methodDeclarations) {
            fDef.globalInfoVisit(contractSym);
        }
    }

    public void genConsVisit(VisitEnv env, boolean tail_position) {
        //env.prevContext = new Context()
        // findPrincipal(env.principalSet);

        for (StateVariableDeclaration dec : varDeclarations) {
            dec.genConsVisit(env, tail_position);
        }
        trustSetting.genConsVisit(env, tail_position);

        for (ExceptionDef expDef : exceptionDefs) {
            expDef.genConsVisit(env, tail_position);
        }

        for (FunctionDef fDef : methodDeclarations) {
            fDef.genConsVisit(env, tail_position);
        }
    }

//    public void findPrincipal(HashSet<String> principalSet) {
//        for (TrustConstraint trustConstraint : trustSetting.trust_list) {
//            trustConstraint.findPrincipal(principalSet);
//        }
//        ifl.findPrincipal(principalSet);
//
//        for (StateVariableDeclaration dec : varDeclarations) {
//            dec.findPrincipal(principalSet);
//        }
//
//        for (ExceptionDef expDef : exceptionDefs) {
//            expDef.findPrincipal(principalSet);
//        }
//
//        for (FunctionDef fDef : methodDeclarations) {
//            fDef.findPrincipal(principalSet);
//        }
//    }

    public void solidityCodeGen(SolCode code) {
        // code.setDynamicOption(trustSetting);
        code.enterContractDef(contractName);

        for (StateVariableDeclaration dec : varDeclarations)
            if (!dec.isBuiltin()) {
                dec.solidityCodeGen(code);
            }
//
//        for (ExceptionDef expDef : exceptionDefs) {
//            expDef.solidityCodeGen(code);
//        }

        for (FunctionDef fDef : methodDeclarations)
            if (!fDef.isBuiltin()) {
                fDef.solidityCodeGen(code);
            }

        code.leaveContractDef();
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(trustSetting.trust_list);
        rtn.addAll(varDeclarations);
        rtn.addAll(exceptionDefs);
        rtn.addAll(methodDeclarations);
        return rtn;
    }

    public void codePasteContract(Map<String, Contract> contractMap, Map<String, Interface> interfaceMap) {
        if (superContractName.equals("")) {
            return;
        }

        // check no functions with the same name
        // add other functions from superContract
        // trust_list is also inherited
        Contract superContract = contractMap.get(superContractName);
        if (superContract == null) {
            Interface itrface = interfaceMap.get(superContractName);
            if (itrface == null) {
                assert false: superContractName;
                return;
            }
            // check that all methods are implemented

            Map<String, FunctionSig> funcMap = new HashMap<>();
            for (FunctionDef f: methodDeclarations) {
                funcMap.put(f.name, f);
            }
            for (FunctionSig f: itrface.funcSigs) {
                String name = f.name;
                if (funcMap.containsKey(name)) {
                    if (!f.typeMatch(funcMap.get(name))) {
                        assert false: f.signature() + " $ " + funcMap.get(name).signature();
                    }
                } else {
                    assert false: name;
                }
            }
            return;
        }

        // inherit from superContract

        // trust_list
        List<TrustConstraint> newTrustCons = new ArrayList<>();
        newTrustCons.addAll(superContract.trustSetting.trust_list);
        newTrustCons.addAll(trustSetting.trust_list);
        trustSetting.trust_list = newTrustCons;

        // Statement
        Map<String, StateVariableDeclaration> varNames = new HashMap<>();
        Map<String, ExceptionDef> expDefs = new HashMap<>();
        Map<String, FunctionSig> funcNames = new HashMap<>();
        Set<String> nameSet = new HashSet<>();

        for (StateVariableDeclaration a : varDeclarations) {
            Name x = a.name();
            assert !nameSet.contains(x.id);
            varNames.put(x.id, a);
            nameSet.add(x.id);
        }

        for (ExceptionDef exp: exceptionDefs) {
            assert !nameSet.contains(exp.exceptionName);
            nameSet.add(exp.exceptionName);
            expDefs.put(exp.exceptionName, exp);
        }

        for (FunctionDef f : methodDeclarations) {
            assert !nameSet.contains(f.name);
            nameSet.add(f.name);
            funcNames.put(f.name, f);
        }

        List<StateVariableDeclaration> newStateVarDecs = new ArrayList<>();
        List<ExceptionDef> newExpDefs = new ArrayList<>();
        List<FunctionDef> newFuncDefs = new ArrayList<>();

        for (StateVariableDeclaration a : superContract.varDeclarations) {
            Name x = a.name();
            assert !nameSet.contains(x.id);

            newStateVarDecs.add(a);
        }
        newStateVarDecs.addAll(varDeclarations);

        for (ExceptionDef exp: superContract.exceptionDefs) {
            assert !nameSet.contains(exp.exceptionName);
            newExpDefs.add(exp);
        }
        newExpDefs.addAll(exceptionDefs);

        for (FunctionDef f : superContract.methodDeclarations) {
            boolean overridden = false;
            if (funcNames.containsKey(f.name)) {
                assert funcNames.get(f.name).typeMatch(f);
                overridden = true;
            } else if (varNames.containsKey(f.name)) {
                // TODO: var overridden by func
                assert false;
            } else
                assert !expDefs.containsKey(f.name);

            if (!overridden) {
                newFuncDefs.add(f);
            }
        }
        newFuncDefs.addAll(methodDeclarations);

        varDeclarations = newStateVarDecs;
        exceptionDefs = newExpDefs;
        methodDeclarations = newFuncDefs;
    }

    public String toString() {
        return toSHErrLocFmt();
        //return genson.serialize(body.get();
    }

    public String getContractName() {
        return contractName;
    }

    protected void addBuiltIns() {
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
        methodDeclarations.add(sendDef);

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
                        labelTop,
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
        methodDeclarations.add(balanceDef);
    }

    private void addBuiltInExceptions() {
        // add exceptions:
        // exception{BOT} Error();
        ExceptionDef error = new ExceptionDef(
                Utils.EXCEPTION_ERROR_NAME,
                new Arguments()
        );
        exceptionDefs.add(error);
    }

    private void addBuiltInVars() {
        // add variables:
        // final This{this} this;
        StateVariableDeclaration thisDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_THIS),
                new LabeledType(contractName, new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)), CodeLocation.builtinCodeLocation()),
                null,
                true,
                true,
                true
        );
        thisDec.setLoc(CodeLocation.builtinCodeLocation());
        StateVariableDeclaration topDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_TOP),
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_TOP)), CodeLocation.builtinCodeLocation()),
                null,
                true,
                true,
                true
        );
        topDec.setLoc(CodeLocation.builtinCodeLocation());
        StateVariableDeclaration botDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_BOTTOM),
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM)), CodeLocation.builtinCodeLocation()),
                null,
                true,
                true,
                true
        );
        botDec.setLoc(CodeLocation.builtinCodeLocation());
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
