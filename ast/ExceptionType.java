package ast;

public class ExceptionType extends Type {
    Type type;
    String contractName;
    public ExceptionType(Type type) {
        super(type.name);
        this.type = type;
        this.contractName = "";
    }
    public ExceptionType(Type type, String contractName) {
        super(contractName + "." + type.name);
        this.type = type;
        this.contractName = contractName;
    }

    public boolean isLocal(String name) {
        return name.equals(contractName);
    }

    public String getName() {
        return type.name;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String name) {
        contractName = name;
    }
}
