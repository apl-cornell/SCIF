package ast;

import compile.SolCode;
import typecheck.*;

import java.util.*;
import java.util.Map;

// A program only contains one contract
public class SourceFile extends Node {
    static Map<String, String> sourceFileNameIds = new HashMap<>();
    static int idCounter = 0;

    private String sourceFileFullName; // e.g., "A.scif"
    private String sourceFileNameId;
    private Set<String> iptContracts; // imported contracts
    private Contract contract;
    private String contractName;
    private String filePath;

    /*
        @sourceCode represents the source code in lines
     */
    private List<String> sourceCode;

    public SourceFile(String sourceFileName, Set<String> iptContracts, Contract contract) {
        this.sourceFileFullName = sourceFileName;
        this.sourceFileNameId = sourceFileNameId(sourceFileName);
        contractName = contract.contractName;
        this.iptContracts = iptContracts;
        this.contract = contract;
        sourceCode = null;
    }

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

    public SourceFile(String sourceFileName, Contract contract) {
        this.sourceFileFullName = sourceFileName;
        this.sourceFileNameId = sourceFileNameId(sourceFileName);
        contractName = contract.contractName;
        this.iptContracts = new HashSet<>();
        this.contract = contract;
        this.sourceCode = null;
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

    public Contract findContract(String name) {
        if (contract.contractName.equals(name)) {
            return contract;
        }
        return null;
    }

    public boolean containContract(String name) {
        return findContract(name) != null;
    }

    public boolean codePasteContract(String name, HashMap<String, Contract> contractMap) {
        Contract contract = findContract(name);
        if (contract == null) {
            return false;
        }
        return contract.codePasteContract(contractMap);
    }

    public boolean ntcInherit(InheritGraph graph) {
        return contract.ntcInherit(graph);
    }

    /**
     *  Generate constraints for regular typechecking in the current source file.
     */
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        logger.debug("contract: " + contract.contractName + "\n" + env.getContract(
                contract.contractName));
        env.setGlobalSymTab(env.getContract(contract.contractName).symTab);
        env.initCurSymTab();
        contract.ntcGenCons(env, now);
        return now;
    }

    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        for (String iptContract : iptContracts) {
            if (!env.containsContract(iptContract)) {
                logger.debug("not containing imported contract: " + iptContract);
                return false;
            }
        }
        // env.setGlobalSymTab(new SymTab());
        env.initCurSymTab();
        // Utils.addBuiltInASTNode(env.globalSymTab, contract.trustSetting);
        if (!contract.ntcGlobalInfo(env, now)) {
            logger.debug("GlobalInfo failed with: " + contract);
            return false;
        }
        return true;
    }

    public void globalInfoVisit(ContractSym contractSym) {
        if (contract == null) {
            return;
        }
        if (!iptContracts.contains(contract.contractName)) {
            iptContracts.add(contract.contractName);
        }
        // contractSym.iptContracts = iptContracts;
        logger.debug("visit Contract: " + contract.contractName + "\n"
                + contractSym.symTab.getTypeSet());
        contract.globalInfoVisit(contractSym);
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        contract.genConsVisit(env, tail_position);
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        // contract.findPrincipal(principalSet);
    }

    public void solidityCodeGen(SolCode code) {
        code.addVersion(compile.Utils.SOLITIDY_VERSION);

        //TODO: deal with baseContract imports
        code.addImport(Utils.PATH_TO_BASECONTRACTCENTRALIZED);

        for (String contractName : iptContracts) {
            boolean exists = false;
            if (contract.contractName.equals(contractName)) {
                exists = true;
                break;
            }
            if (!exists) {
                code.addImport(contractName);
            }
        }
        contract.solidityCodeGen(code);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(contract);
        return rtn;
    }

    public Contract getContract() {
        return contract;
    }

    public String getSourceFileFullName() {
        return sourceFileFullName;
    }

    /**
     * Add built-in variables, trust assumptions, exceptions, and methods in the AST with this as the root.
     */
    public void addBuiltIns() {
        contract.addBuiltIns();
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
}
