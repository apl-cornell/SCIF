package ast;

import compile.SolCode;
import typecheck.*;

import java.util.*;
import java.util.Map;

// A program only contains one contract
public abstract class SourceFile extends Node {
    static Map<String, String> sourceFileNameIds = new HashMap<>();
    static int idCounter = 0;

    final String sourceFileFullName; // e.g., "A.scif"
    final String sourceFileNameId;
    protected final Set<String> iptContracts; // imported contracts
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
    private static String sourceFileNameId(String sourceFileName) {
        if (!sourceFileNameIds.containsKey(sourceFileName)) {
            String id = "s" + idCounter;
            ++idCounter;
            sourceFileNameIds.put(sourceFileName, id);
        }
        return sourceFileNameIds.get(sourceFileName);
    }

    public SourceFile(String sourceFileName, Set<String> iptContracts, String contractName) {
        this.sourceFileFullName = sourceFileName;
        this.sourceFileNameId = sourceFileNameId(sourceFileName);
        this.contractName = contractName;
        this.iptContracts = iptContracts;
        sourceCode = null;
        builtIn = false;
    }

    public SourceFile(String sourceFileName, String contractName) {
        this.sourceFileFullName = sourceFileName;
        this.sourceFileNameId = sourceFileNameId(sourceFileName);
        this.contractName = contractName;
        this.iptContracts = new HashSet<>();
        this.sourceCode = null;
        builtIn = false;
    }

    abstract public SourceFile makeBuiltIn();
    public SourceFile(SourceFile source, boolean builtIn) {
        this.sourceFileFullName = source.sourceFileFullName;
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

    abstract public boolean containContract(String name);

    abstract public boolean codePasteContract(String name, Map<String, Contract> contractMap, Map<String, Interface> interfaceMap);

    abstract public boolean ntcInherit(InheritGraph graph);

    /**
     *  Generate constraints for regular typechecking in the current source file.
     */
    // abstract public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent);
    abstract public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent);

    abstract public void globalInfoVisit(ContractSym contractSym);


    public void findPrincipal(HashSet<String> principalSet) {
        // contract.findPrincipal(principalSet);
    }

    public String getSourceFileFullName() {
        return sourceFileFullName;
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
}
