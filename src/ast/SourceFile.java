package ast;

import static typecheck.Utils.BUILTIN_FILES;

import compile.CompileEnv;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import typecheck.*;

import java.util.*;
import java.util.Map;

// A program only contains one contract
public abstract class SourceFile extends Node {
    static Map<String, String> sourceFileNameIds = new HashMap<>();
    static int idCounter = 0;

    final Path sourceFilePath;
    // final String sourceFileFullName; // e.g., "A.scif"
    final String sourceFileNameId;

    /*
        imported files;
        resolved to absolute path after inheritance graph generation;
        joined with super contracts' imports after code pasting
     */
    protected Set<String> iptContracts;
    protected final String contractName;

    /*
        @sourceCode represents the source code in lines
     */
    private List<String> sourceCode;
    private final boolean builtIn;

    /**
     * return s + id
     * @param sourceFileName
     * @return
     */
    public static String sourceFileNameId(String sourceFileName) {
        if (!sourceFileNameIds.containsKey(sourceFileName)) {
            String id = "s" + idCounter;
            ++idCounter;
            sourceFileNameIds.put(sourceFileName, id);
        }
        return sourceFileNameIds.get(sourceFileName);
    }

    public SourceFile(String sourceFilePath, Set<String> iptContracts, String contractName) {
        this.sourceFilePath = FileSystems.getDefault().getPath(sourceFilePath);
        this.sourceFileNameId = sourceFileNameId(this.sourceFilePath.toString());
        this.contractName = contractName;
        this.iptContracts = iptContracts;
        sourceCode = null;
        builtIn = false;
    }

    public SourceFile(String sourceFilePath, String contractName) {
        this.sourceFilePath = FileSystems.getDefault().getPath(sourceFilePath);
        this.sourceFileNameId = sourceFileNameId(sourceFilePath);
        this.contractName = contractName;
        this.iptContracts = new HashSet<>();
        this.sourceCode = null;
        builtIn = false;
    }

    abstract public SourceFile makeBuiltIn();
    public SourceFile(SourceFile source, boolean builtIn) {
        this.sourceFilePath = source.sourceFilePath;
        this.sourceFileNameId = source.sourceFileNameId;
        this.contractName = source.contractName;
        this.iptContracts = source.iptContracts;
        this.sourceCode = source.sourceCode;
        this.builtIn = builtIn;
    }

    public String getContractName() {
        return contractName;
    }

    public String getSourceCodeLine(int i) {
        return sourceCode.get(i);
    }

    public void setSourceCode(List<String> sourceCode) {
        this.sourceCode = sourceCode;
    }

    abstract public boolean containContract(String fullPath);

    abstract public void codePasteContract(String name, Map<String, Contract> contractMap, Map<String, Interface> interfaceMap);

    abstract public boolean ntcAddImportEdges(InheritGraph graph);

    /**
     *  Generate constraints for regular typechecking in the current source file.
     */
    // abstract public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent);
    abstract public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent);

    abstract public void globalInfoVisit(InterfaceSym contractSym);


    public void findPrincipal(HashSet<String> principalSet) {
        // contract.findPrincipal(principalSet);
    }

    public String getSourceFilePath() {
        return sourceFilePath.toString();
    }

    public String getSourceFileId() {
        return sourceFileNameId;
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            if (node != null) {
                node.passScopeContext(scopeContext);
            }
        }
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    /**
     * Add built-in variables, trust assumptions, exceptions, and methods in the AST with this as the root.
     */
    abstract public void addBuiltIns();

    void addBuiltInImports() {
        if (isBuiltIn()) return;
        for (File importFile: BUILTIN_FILES) {
            iptContracts.add(importFile.toString());
            System.err.println(contractName + " imports " + importFile.toString());
        }
    }

    public void updateImports(Map<String, SourceFile> fileMap) {
        Set<String> newIptContracts = new HashSet<>(iptContracts);
        for (String x: iptContracts) {
            SourceFile file = fileMap.get(x);
            assert file != null: x;
            newIptContracts.addAll(file.iptContracts);
        }
        iptContracts = newIptContracts;
    }

    public Set<String> importPaths() {
        return Collections.unmodifiableSet(iptContracts);
    }

    public abstract compile.ast.SourceFile solidityCodeGen(CompileEnv code);
}
