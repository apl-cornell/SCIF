package typecheck;

public class ContractTypeSym extends TypeSym {
    ContractSym contractSym = null;


    public ContractTypeSym(String typeName, ContractSym contractSym) {
        this.name = typeName;
        this.contractSym = contractSym;
    }
}
