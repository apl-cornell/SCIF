package typecheck;

import ast.*;

import java.util.ArrayList;
import java.util.List;

public class ContractSym extends InterfaceSym {

    public ContractSym(String name,
            SymTab symTab,
            // HashSet<String> iptContracts, HashMap<String, Type> typeMap, HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap,
            List<Assumption> assumptions,
            // Label label,
            Contract contract,
            VarSym anySym) {
        super(name, symTab, assumptions, contract.getScopeContext(), anySym);
        astNode = contract;
    }

    public ContractSym(String contractName, Contract contract) {
        super(contractName, contract.getScopeContext());
        astNode = contract;
    }



    public Contract getContractNode() {
        return (Contract) astNode;
    }

    public FuncSym getConstructorSym() {
        Sym s = symTab.lookup(Utils.CONSTRUCTOR_KEYWORD);
        assert s instanceof FuncSym;
        return (FuncSym) s;
    }
}
