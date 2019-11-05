package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Program extends Node {
    HashSet<String> iptContracts; // imported contracts
    ArrayList<Contract> contracts;
    public Program(HashSet<String> iptContracts, ArrayList<Contract> contracts) {
        this.iptContracts = iptContracts;
        this.contracts = contracts;
    }
    public Program(ArrayList<Contract> contracts) {
        this.iptContracts = new HashSet<>();
        this.contracts = contracts;
    }

    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        if (contracts.size() < 1) return;
        for (Contract contract : contracts) {
            if (!iptContracts.contains(contract.contractName)) {
                iptContracts.add(contract.contractName);
            }
        }
        contractInfo.iptContracts = iptContracts;
        for (Contract contract : contracts) {
            contract.globalInfoVisit(contractInfo);
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
}
