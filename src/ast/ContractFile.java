package ast;

import compile.SolCode;
import java.util.ArrayList;
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

    public Contract findContract(String name) {
        if (contract.contractName.equals(name)) {
            return contract;
        }
        return null;
    }

    @Override
    public boolean containContract(String name) {
        return findContract(name) != null;
    }

    @Override
    public ContractFile makeBuiltIn() {
        return new ContractFile(this, true);
    }

    @Override
    public boolean codePasteContract(String name, java.util.Map<String, Contract> contractMap, Map<String, Interface> interfaceMap) {
        Contract contract = findContract(name);
        if (contract == null) {
            return false;
        }
        return contract.codePasteContract(contractMap, interfaceMap);
    }

    @Override
    public boolean ntcInherit(InheritGraph graph) {
        for (String name: iptContracts) {
            graph.addEdge(name, contractName);
        }
        return contract.ntcInherit(graph);
    }
    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        logger.debug("contract: " + contract.contractName + "\n" + env.getContract(
                contract.contractName));
        env.setGlobalSymTab(env.getContract(contract.contractName).symTab);
        env.initCurSymTab();
        contract.ntcGenCons(env, now);
        return now;
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        for (String iptContract : iptContracts) {
            if (!env.containsContract(iptContract) && !env.containsInterface(iptContract)) {
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

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) {
        if (contract == null) {
            return;
        }
        iptContracts.add(contract.contractName);
        // contractSym.iptContracts = iptContracts;
        logger.debug("visit Contract: " + contract.contractName + "\n"
                + contractSym.symTab.getTypeSet());
        contract.globalInfoVisit(contractSym);
    }
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        contract.genConsVisit(env, tail_position);
        return null;
    }
    public void solidityCodeGen(SolCode code) {
        code.addVersion(compile.Utils.SOLITIDY_VERSION);


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
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(contract);
        return rtn;
    }

    public Contract getContract() {
        return contract;
    }

    @Override
    public void addBuiltIns() {
        contract.addBuiltIns();
    }
}
