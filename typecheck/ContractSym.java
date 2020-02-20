package typecheck;

public class ContractSym extends Sym {
    ContractInfo contractInfo;
    public ContractSym(String id, ContractInfo contractInfo) {
        this.name = id;
        this.contractInfo = contractInfo;
    }

    public boolean isLValue() {
        return false;
    }
}
