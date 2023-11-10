package ast;

import compile.CompileEnv;
import compile.ast.Import;
import compile.ast.SolNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import typecheck.InheritGraph;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class ContractFile extends SourceFile {
    private final Contract contract;

    public ContractFile(String sourceFileName, Set<String> iptContracts, Contract contract) {
        super(sourceFileName, iptContracts, contract.contractName);
        this.contract = contract;
    }

    public ContractFile(String sourceFileName, Contract contract) {
        super(sourceFileName, contract.contractName);
        this.contract = contract;
    }

    public ContractFile(ContractFile source, boolean builtIn) {
        super(source, builtIn);
        this.contract = source.contract;
    }

    public Contract findContract(String fullPath) {
        if (sourceFilePath.toString().equals(fullPath)) {
            return contract;
        }
        return null;
    }

    @Override
    public boolean containContract(String fullPath) {
        return findContract(fullPath) != null;
    }

    @Override
    public ContractFile makeBuiltIn() {
        return new ContractFile(this, true);
    }

    @Override
    public void codePasteContract(String name, java.util.Map<String, Contract> contractMap, Map<String, Interface> interfaceMap) {
        Contract contract = findContract(name);
        assert contract != null;

        // construct contract table and interface table this contract imported
        // mapping from local names to the object
        Map<String, Contract> importedContractMap = new HashMap<>();
        Map<String, Interface> importedInterfaceMap = new HashMap<>();

        for (String path : iptContracts) {
            assert contractMap.containsKey(path) || interfaceMap.containsKey(path) : path;
            if (contractMap.containsKey(path)) {
                importedContractMap.put(contractMap.get(path).getContractName(), contractMap.get(path));
            } else if (interfaceMap.containsKey(path)) {
                importedInterfaceMap.put(interfaceMap.get(path).getContractName(), interfaceMap.get(path));
            }
        }
        contract.codePasteContract(importedContractMap, importedInterfaceMap);
    }

    @Override
    public boolean ntcAddImportEdges(InheritGraph graph) {
        Set<String> resolvedIptContracts = new HashSet<>();
        for (String contract: iptContracts) {
            Path path = Paths.get(contract);
            if (!path.isAbsolute()) {
                path = sourceFilePath.getParent().resolve(path).normalize().toAbsolutePath();
                // System.err.println(contract + " -> " + path + " from " + sourceFilePath);
            }
            resolvedIptContracts.add(path.toString());
            graph.addEdge(path.toString(), getSourceFilePath());
        }
        iptContracts = resolvedIptContracts;
        return true;
        // return contract.ntcInherit(graph);
    }
    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterSourceFile(getSourceFilePath());
        logger.debug("contract: " + contract.contractName + "\n" + env.getContract(
                contract.contractName));
        env.setCurSymTab(env.currentSourceFileFullName());
        contract.ntcGenCons(env, now);
        return now;
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterSourceFile(getSourceFilePath());
        env.setNewCurSymTab();
        for (String iptContract : iptContracts) {
            env.importContract(iptContract);
        }
        // env.setGlobalSymTab(new SymTab());
        // env.initCurSymTab();
        // Utils.addBuiltInASTNode(env.globalSymTab, contract.trustSetting);
        // assert env.getCurSym("TrustManager") != null: env.currentSourceFileFullName();
        if (!contract.ntcGlobalInfo(env, now)) {
            logger.debug("GlobalInfo failed with: " + contract);
            return false;
        }
        return true;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        if (contract == null) {
            return;
        }
        // iptContracts.add(contract.contractName);
        // contractSym.iptContracts = iptContracts;
        logger.debug("visit Contract: " + contract.contractName + "\n"
                + contractSym.symTab.getTypeSet());
        contract.globalInfoVisit(contractSym);
    }
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        contract.genConsVisit(env, tail_position);
        return null;
    }

    @Override
    public compile.ast.SourceFile solidityCodeGen(CompileEnv code) {
        List<Import> imports = new ArrayList<>();

        for (String contractName : iptContracts) {
            if (!contract.contractName.equals(contractName)) {
                imports.add(new Import(contractName));
            }
        }
        return new compile.ast.ContractFile(imports, contract.solidityCodeGen(code));
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(contract);
        return rtn;
    }

    public Contract getContract() {
        return contract;
    }

    @Override
    public void addBuiltIns() {
        addBuiltInImports();
        contract.addBuiltIns();
    }

}
