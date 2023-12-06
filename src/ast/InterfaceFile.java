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
import typecheck.ScopeContext;
import typecheck.Utils;

public class InterfaceFile extends SourceFile {
    private final Interface itrface;

    public InterfaceFile(String sourceFileName, Set<String> iptContracts,
            Interface itrface) {
        super(sourceFileName, iptContracts, itrface.contractName);
        this.itrface = itrface;
    }

    public InterfaceFile(String sourceFileName, Interface itrface) {
        super(sourceFileName, itrface.contractName);
        this.itrface = itrface;
    }

    public InterfaceFile(InterfaceFile source, boolean builtIn) {
        super(source, builtIn);
        this.itrface = source.itrface;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // env.setGlobalSymTab(env.getContract(contractName).symTab);
        // env.initCurSymTab();
        env.setCurSymTab(env.currentSourceFileFullName());
        itrface.ntcGenCons(env, now);
        return now;
    }

    @Override
    public compile.ast.SourceFile solidityCodeGen(CompileEnv code) {
        List<Import> imports = new ArrayList<>();
        for (String contractName : originalImportPaths.keySet()) {
            if (!itrface.contractName.equals(contractName) &&
                !Utils.isBuiltInContractName(contractName))  {
                imports.add(
                        new Import(originalImportPaths.getOrDefault(contractName, contractName)));
            }
        }
        return new compile.ast.InterfaceFile(imports, itrface.solidityCodeGen(code));
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(itrface);
        return rtn;
    }

    @Override
    public SourceFile makeBuiltIn() {
        return new InterfaceFile(this, true);
    }

    @Override
    public boolean containContract(String fullPath) {
        return sourceFilePath.toString().equals(fullPath);
    }

    @Override
    public void codePasteContract(String name, Map<String, Contract> contractMap, Map<String, Interface> interfaceMap) {
        assert sourceFilePath.toString().equals(name);

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
        itrface.codePasteContract(importedContractMap, importedInterfaceMap);
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
        // return itrface.ntcInherit(graph);
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterSourceFile(getSourceFilePath());
        env.setNewCurSymTab();
        for (String iptContract : iptContracts) {
            env.importContract(iptContract);
        }
        if (!itrface.ntcGlobalInfo(env, now)) {
            logger.debug("GlobalInfo failed with: " + itrface);
            return false;
        }
        return true;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        // iptContracts.add(itrface.contractName);
        itrface.globalInfoVisit(contractSym);
    }

    public Interface getInterface() {
        return itrface;
    }

    @Override
    public void addBuiltIns() {
        addBuiltInImports();
        itrface.addBuiltIns();
    }

}
