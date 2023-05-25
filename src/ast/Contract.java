package ast;

import compile.SolCode;
import java.util.List;
import javax.swing.plaf.nimbus.State;
import jdk.jshell.execution.Util;
import sherrloc.constraint.ast.Top;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Contract extends TopLayerNode {

    String contractName;
    String superContractName = "";
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
    }

    public Contract(String contractName, String superContractName, TrustSetting trustSetting,
            List<StateVariableDeclaration> varDeclarations,
            List<ExceptionDef> exceptionDefs,
            List<FunctionDef> methodDeclarations) {
        this.contractName = contractName;
        this.superContractName = superContractName;
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

    public boolean ntcInherit(InheritGraph graph) {
        // add an edge from superclass to this contract
        if (!superContractName.isEmpty()) {
            graph.addEdge(superContractName, contractName);
        }
        return true;
    }

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

    public void globalInfoVisit(ContractSym contractSym) {
        // contractSym.name = contractName;
//        contractSym.trustSetting = trustSetting;
        // contractSym.ifl = ifl;
        contractSym.addContract(contractName, contractSym);
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

        for (StateVariableDeclaration dec : varDeclarations) {
            dec.solidityCodeGen(code);
        }

        for (ExceptionDef expDef : exceptionDefs) {
            expDef.solidityCodeGen(code);
        }

        for (FunctionDef fDef : methodDeclarations) {
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

    public boolean codePasteContract(Map<String, Contract> contractMap, Map<String, Interface> interfaceMap) {
        // TODO: add exception
        if (superContractName.equals("")) {
            return true;
        }

        // check no functions with the same name
        // add other functions from superContract
        // trust_list is also inherited
        Contract superContract = contractMap.get(superContractName);
        if (superContract == null) {
            // TODO: superContract not found
            return false;
        }

        // inherit from superContract

        // trust_list
        ArrayList<TrustConstraint> newTrustCons = new ArrayList<>();
        newTrustCons.addAll(superContract.trustSetting.trust_list);
        newTrustCons.addAll(trustSetting.trust_list);
        trustSetting.trust_list = newTrustCons;

        // Statement
        HashMap<String, StateVariableDeclaration> varNames = new HashMap<>();
        HashMap<String, FunctionSig> funcNames = new HashMap<>();

        for (StateVariableDeclaration a : varDeclarations) {
            Name x = a.name();
            if (varNames.containsKey(x.id)) {
                //TODO: duplicate names
                return false;
            }
            varNames.put(x.id, a);
        }

        for (FunctionDef f : methodDeclarations) {
            if (varNames.containsKey(f.name) || funcNames.containsKey(f.name)) {
                //TODO: duplicate names
                return false;
            }
            funcNames.put(f.name, f);
        }

        ArrayList<StateVariableDeclaration> newStateVarDecs = new ArrayList<>();
        ArrayList<FunctionDef> newFuncDefs = new ArrayList<>();

        for (StateVariableDeclaration a : superContract.varDeclarations) {
            Name x = a.name();
            if (varNames.containsKey(x.id)) {
                // TODO: var being overridden
                return false;
            } else if (funcNames.containsKey(x.id)) {
                // TODO: func overridden by var
                return false;
            }

            newStateVarDecs.add(a);
        }
        newStateVarDecs.addAll(varDeclarations);

        for (FunctionDef f : methodDeclarations) {
            boolean overridden = false;
            if (funcNames.containsKey(f.name)) {
                if (!funcNames.get(f.name).typeMatch(f)) {
                    // TODO: func overridden type not match
                    return false;
                }
                overridden = true;
            } else if (varNames.containsKey(f.name)) {
                // TODO: var overridden by func
                return false;
            }

            if (!overridden) {
                newFuncDefs.add(f);
            }
        }
        newFuncDefs.addAll(methodDeclarations);

        varDeclarations = newStateVarDecs;
        methodDeclarations = newFuncDefs;
        return true;
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

        // @protected
        // @final
        // void send{this -> TOP; BOT}(address target, uint amount);
        List<Arg> args = new ArrayList<>();
        Arg tmparg = new Arg(
                "target",
                new LabeledType(Utils.ADDRESS_TYPE, labelThis),
                false,
                true
        );
        args.add(tmparg);
        tmparg = new Arg(
                "amount",
                new LabeledType(Utils.BuiltinType2ID(BuiltInT.UINT), labelThis),
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
                new LabeledType(new Type(Utils.BuiltinType2ID(BuiltInT.VOID))),
                false,
                true
        );
        methodDeclarations.add(sendDef);

        // @public
        // final
        // uint{TOP} balance{BOT -> TOP; TOP}(final address addr);
        args = new ArrayList<>();
        args.add(new Arg(
                "addr",
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM))),
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
                new LabeledType(Utils.BuiltinType2ID(BuiltInT.UINT), labelTop),
                false,
                true
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
                new LabeledType(contractName, new PrimitiveIfLabel(new Name(Utils.LABEL_THIS))),
                null,
                true,
                true,
                true
        );
        StateVariableDeclaration topDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_TOP),
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_TOP))),
                null,
                true,
                true,
                true
        );
        StateVariableDeclaration botDec = new StateVariableDeclaration(
                new Name(Utils.LABEL_BOTTOM),
                new LabeledType(Utils.ADDRESS_TYPE, new PrimitiveIfLabel(new Name(Utils.LABEL_BOTTOM))),
                null,
                true,
                true,
                true
        );
        ArrayList<StateVariableDeclaration> newDecs = new ArrayList<>();
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
