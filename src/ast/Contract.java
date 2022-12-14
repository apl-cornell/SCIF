package ast;

import compile.SolCode;
import java.util.List;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Contract extends Node {

    String contractName;
    String superContractName = "";
    TrustSetting trustSetting;
    IfLabel ifl;
    List<StateVariableDeclaration> varDeclarations;
    List<ExceptionDef> exceptionDefs;
    List<FunctionDef> methodDeclarations;

    public Contract(String contractName, TrustSetting trustSetting,
            List<StateVariableDeclaration> varDeclarations,
            List<ExceptionDef> exceptionDefs,
            List<FunctionDef> methodDeclarations,
            IfLabel ifl) {
        this.contractName = contractName;
        this.trustSetting = trustSetting;
        this.trustSetting.labelTable.put("this", "address(this)");
        this.varDeclarations = varDeclarations;
        this.exceptionDefs = exceptionDefs;
        this.methodDeclarations = methodDeclarations;
        this.ifl = ifl;
        setDefault();
    }

    public Contract(String contractName, String superContractName, TrustSetting trustSetting,
            List<StateVariableDeclaration> varDeclarations,
            List<ExceptionDef> exceptionDefs,
            List<FunctionDef> methodDeclarations,
            IfLabel ifl) {
        this.contractName = contractName;
        this.superContractName = superContractName;
        this.trustSetting = trustSetting;
        this.trustSetting.labelTable.put("this", "address(this)");
        this.varDeclarations = varDeclarations;
        this.exceptionDefs = exceptionDefs;
        this.methodDeclarations = methodDeclarations;
        this.ifl = ifl;
        setDefault();
    }

    private void setDefault() {
        if (ifl == null) {
            ifl = new PrimitiveIfLabel(new Name(Utils.LABEL_THIS));
        }
    }

    public boolean ntcInherit(InheritGraph graph) {
        // add an edge from superclass to this contract
        if (!superContractName.equals("")) {
            graph.addEdge(superContractName, contractName);
        }
        return true;
    }

    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.setCurSymTab(new SymTab(env.curSymTab()));
        Utils.addBuiltInSyms(env.globalSymTab(), trustSetting);
        /*
            add built-in variable "this"
         */
        //String name = "this";
        //env.globalSymTab.add(name, new VarSym(env.toVarSym(name, new LabeledType(contractName, new PrimitiveIfLabel(new Name("this"))), true, new CodeLocation(), now)));
        ContractSym contractSym = new ContractSym(contractName, env.curSymTab(), trustSetting, ifl);
        env.addGlobalSym(contractName, contractSym);
        env.setCurContractSym(contractSym);

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

        env.setCurSymTab(env.curSymTab().getParent());
        return true;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.setCurContractSym(env.getContract(contractName));

        for (StateVariableDeclaration dec : varDeclarations) {
            dec.ntcGenCons(env, now);
        }

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
        contractSym.trustSetting = trustSetting;
        contractSym.ifl = ifl;
        contractSym.addContract(contractName, contractSym);
        String name = "this";
        contractSym.addVar(name, contractSym.toVarSym(name,
                new LabeledType(contractName, new PrimitiveIfLabel(new Name("this"))), true, false,
                null, scopeContext));

        for (StateVariableDeclaration dec : varDeclarations) {
            dec.globalInfoVisit(contractSym);
        }

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
        code.setDynamicOption(trustSetting);
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

    public boolean codePasteContract(HashMap<String, Contract> contractMap) {
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
            Name x = a.name;
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
            Name x = a.name;
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
        return "TODO";
        //return genson.serialize(body.get();
    }

    public String getContractName() {
        return contractName;
    }
}
