package ast;

import compile.SolCode;
import typecheck.*;

import java.util.*;

// As for now, multiple contracts in one Program might cause problem
public class Program extends Node {
    public String programName;
    Set<String> iptContracts; // imported contracts
    Contract contract;
    String contractName;
    String filePath;
    List<String> sourceCode;
    public Program(HashSet<String> iptContracts, Contract contract) {
        programName = "anonymous";
        contractName = "UNKNOWN";
        if (contract != null)
            contractName = contract.contractName;
        this.iptContracts = iptContracts;
        this.contract = contract;
        sourceCode = null;
    }
    public Program(Contract contract) {
        programName = "anonymous";
        contractName = "UNKNOWN";
        if (contract != null)
            contractName = contract.contractName;
        this.iptContracts = new HashSet<>();
        this.contract = contract;
        this.sourceCode = null;
    }

    public String getContractName() { return contractName; }
    public void setSourceCode(List<String> sourceCode) { this.sourceCode = sourceCode; }
    public String getSourceCodeLine(int i) {
        return sourceCode.get(i);
    }
    public Contract findContract(String name) {
        if (contract.contractName.equals(name))
            return contract;
        return null;
    }
    public boolean containContract(String name) {
        return findContract(name) != null;
    }
    public void setProgramName(String name) {
        programName = name;
    }

    public boolean codePasteContract(String name, HashMap<String, Contract> contractMap) {
        Contract contract = findContract(name);
        if (contract == null) return false;
        return contract.codePasteContract(contractMap);
    }

    public boolean NTCinherit(InheritGraph graph) {
        return contract.NTCinherit(graph);
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        logger.debug("contract: " + contract.contractName + "\n" + env.getContract(contract.contractName));
        env.setGlobalSymTab(env.getContract(contract.contractName).symTab);
        env.setCurSymTab(env.globalSymTab);
        contract.NTCgenCons(env, now);
        return now;
    }

    @Override
    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        for (String iptContract : iptContracts)
            if (!env.containsContract(iptContract)) {
                logger.debug("not containing imported contract: " + iptContract);
                return false;
            }
        // env.setGlobalSymTab(new SymTab());
        env.setCurSymTab(env.globalSymTab);
        // Utils.addBuiltInSyms(env.globalSymTab, contract.trustSetting);
        if (!contract.NTCGlobalInfo(env, parent)) {
            logger.debug("GlobalInfo failed with: " + contract);
            return false;
        }
        return true;
    }

    public void globalInfoVisit(ContractSym contractSym) {
        Utils.addBuiltInSyms(contractSym.symTab, null);
        if (contract == null) return;
        if (!iptContracts.contains(contract.contractName)) {
            iptContracts.add(contract.contractName);
        }
        // contractSym.iptContracts = iptContracts;
        logger.debug("visit Contract: " + contract.contractName + "\n" + contractSym.symTab.getTypeSet());
        contract.globalInfoVisit(contractSym);
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        contract.genConsVisit(env, tail_position);
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        contract.findPrincipal(principalSet);
    }

    public void SolCodeGen(SolCode code) {
        code.addVersion(compile.Utils.SOLITIDY_VERSION);

        //TODO: deal with baseContract imports
        code.addImport(Utils.PATH_TO_BASECONTRACTCENTRALIZED);

        for (String contractName : iptContracts) {
            boolean exists = false;
            if (contract.contractName.equals(contractName)) {
                exists = true;
                break;
            }
            if (!exists)
                code.addImport(contractName);
        }
        contract.SolCodeGen(code);
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

    public String getProgramName() {
        return programName;
    }
}
