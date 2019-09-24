package typecheck;

public class ContractType extends Type {
    ContractInfo contractInfo = null;
    public ContractType(String typeName) {
        this.typeName = typeName;
    }
}
