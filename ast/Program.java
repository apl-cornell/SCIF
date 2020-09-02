package ast;

import compile.SolCode;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public class Program extends Node {
    public String programName;
    HashSet<String> iptContracts; // imported contracts
    ArrayList<Contract> contracts;
    String contractName;
    public Program(HashSet<String> iptContracts, ArrayList<Contract> contracts) {
        programName = "anonymous";
        contractName = "UNKNOWN";
        if (!contracts.isEmpty())
            contractName = contracts.get(0).contractName;
        this.iptContracts = iptContracts;
        this.contracts = contracts;
    }
    public Program(ArrayList<Contract> contracts) {
        programName = "anonymous";
        contractName = "UNKNOWN";
        if (!contracts.isEmpty())
            contractName = contracts.get(0).contractName;
        this.iptContracts = new HashSet<>();
        this.contracts = contracts;
    }

    public String getFirstContractName() { return contractName; }
    public void setProgramName(String name) {
        programName = name;
    }

    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        for (Contract contract : contracts) {
            logger.debug("contract: " + contract.contractName + "\n" + env.getContract(contract.contractName));
            env.setGlobalSymTab(env.getContract(contract.contractName).symTab);
            env.setCurSymTab(env.globalSymTab);
            contract.NTCgenCons(env, now);
        }
        return now;
    }

    @Override
    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        for (String iptContract : iptContracts)
            if (!env.containsContract(iptContract)) {
                logger.debug("not containing imported contract: " + iptContract);
                return false;
            }
        for (Contract contract : contracts) {
            // env.setGlobalSymTab(new SymTab());
            env.setCurSymTab(env.globalSymTab);
            Utils.addBuiltInSyms(env.globalSymTab);
            if (!contract.NTCGlobalInfo(env, parent)) {
                logger.debug("GlobalInfo failed with: " + contract);
                return false;
            }
        }
        return true;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        Utils.addBuiltInSyms(contractSym.symTab);
        if (contracts.size() < 1) return;
        for (Contract contract : contracts) {
            if (!iptContracts.contains(contract.contractName)) {
                iptContracts.add(contract.contractName);
            }
        }
        // contractSym.iptContracts = iptContracts;
        for (Contract contract : contracts) {
            logger.debug("visit Contract: " + contract.contractName + "\n" + contractSym.symTab.getTypeSet());
            contract.globalInfoVisit(contractSym);
        }
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        for (Contract contract : contracts) {
            contract.genConsVisit(env);
        }
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        for (Contract contract : contracts) {
            contract.findPrincipal(principalSet);
        }
    }

    public void SolCodeGen(SolCode code) {
        code.addVersion(compile.Utils.SOLITIDY_VERSION);
        /*for (String contractName : iptContracts) {
            code.addImport(contractName);
        }*/
        for (Contract contract : contracts) {
            contract.SolCodeGen(code);
        }
    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(contracts);
        return rtn;
    }
}
