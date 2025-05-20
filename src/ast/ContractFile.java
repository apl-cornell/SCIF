package ast;

import compile.CompileEnv;
import compile.ast.Import;

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
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

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
    public void codePasteContract(String name, java.util.Map<String, List<TopLayerNode>> sourceFileMap) throws SemanticException {
        Contract contract = findContract(name);
        assert contract != null;

        // construct contract table and interface table this contract imported
        // mapping from local names to the object
        Map<String, Contract> importedContractMap = new HashMap<>(); // contract name -> AST contract
        Map<String, Interface> importedInterfaceMap = new HashMap<>();

        for (String path: iptContracts) {
            assert sourceFileMap.containsKey(path) : path;
            List<TopLayerNode> sources = sourceFileMap.get(path);
            for (TopLayerNode source : sources) {
                if (source instanceof Contract) {
                    importedContractMap.put(((Contract) source).getContractName(), (Contract) source);
                    iptContractNames.computeIfAbsent(path, k -> new ArrayList<>()).add(((Contract) source).getContractName());
                } else if (source instanceof Interface) {
                    importedInterfaceMap.put(((Interface) source).getContractName(), (Interface) source);
                    iptContractNames.computeIfAbsent(path, k -> new ArrayList<>()).add(((Interface) source).getContractName());
                } else {
                    assert false;
                }
            }
        }
        // adding contracts and interfaces under the same file and above the current contract
        String path = getSourceFilePath();
        assert sourceFileMap.containsKey(path) : path;
        List<TopLayerNode> sources = sourceFileMap.get(path);
        for (TopLayerNode source : sources) {
            if (source instanceof Contract) {
                if (contract == source) {
                    break;
                }
                importedContractMap.put(((Contract) source).getContractName(), (Contract) source);
                iptContractNames.computeIfAbsent(path, k -> new ArrayList<>()).add(((Contract) source).getContractName());
            } else if (source instanceof Interface) {
                importedInterfaceMap.put(((Interface) source).getContractName(), (Interface) source);
                iptContractNames.computeIfAbsent(path, k -> new ArrayList<>()).add(((Interface) source).getContractName());
            } else {
                assert false;
            }
        }
        contract.codePasteContract(importedContractMap, importedInterfaceMap); // have effects only if the contracts extends/implements smth
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
            originalImportPaths.put(path.toString(), contract);
            graph.addEdge(path.toString(), getSourceFilePath());
        }
        iptContracts = resolvedIptContracts;
        return true;
        // return contract.ntcInherit(graph);
    }
    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterSourceFile(getSourceFilePath(), getContractName());
//        // logger.debug("contract: " + contract.contractName + "\n" + env.getContract(
//                contract.contractName));
        env.setCurSymTab(env.currentSourceFileFullName(), env.currentContractName());
        contract.genTypeConstraints(env, now);
        return now;
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterSourceFile(getSourceFilePath(), getContractName());
        env.setNewCurSymTab();
        for (String iptContract : iptContracts) {
            for (String contractName : iptContractNames.get(iptContract)) {
                env.importContract(iptContract, contractName, location);
            }
            // env.importContract(iptContract, location); // TODO steph: might need to change this
        }

        // for all other contracts in the same file that's above self
        String path = getSourceFilePath();
        if (iptContractNames.containsKey(path)) {
            for (String contractName : iptContractNames.get(path)) {
                env.importContract(path, contractName, location);
            }
        }


        // env.setGlobalSymTab(new SymTab());
        // env.initCurSymTab();
        // Utils.addBuiltInASTNode(env.globalSymTab, contract.trustSetting);
        // assert env.getCurSym("TrustManager") != null: env.currentSourceFileFullName();
        if (!contract.ntcGlobalInfo(env, now)) {
//            // logger.debug("GlobalInfo failed with: " + contract);
            return false;
        }
        return true;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) throws SemanticException {
        if (contract == null) {
            return;
        }
        // ntcGlobalInfos.add(contract.contractName);
        // contractSym.iptContracts = iptContracts;
//        // logger.debug("visit Contract: " + contract.contractName + "\n"
//                + contractSym.symTab.getTypeSet());
        contract.globalInfoVisit(contractSym);
    }
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        contract.IFCVisit(env, tail_position);
        return null;
    }

    @Override
    public compile.ast.SourceFile solidityCodeGen(CompileEnv code) {
        List<Import> imports = new ArrayList<>();

        for (String contractName : originalImportPaths.keySet()) {
            if (!contract.contractName.equals(contractName) &&
                    !Utils.isBuiltInContractName(contractName)) {
                imports.add(
                        new Import(originalImportPaths.getOrDefault(contractName, contractName)));
            }
        }
        return new compile.ast.ContractFile(imports, contract.solidityCodeGen(code), firstInFile);
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