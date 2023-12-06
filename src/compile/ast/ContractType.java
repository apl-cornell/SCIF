package compile.ast;

public class ContractType implements Type {
    String name;

    public ContractType(String name) {
        this.name = name;
    }

    @Override
    public String solCode() {
        return name;
    }

    @Override
    public String solCode(boolean isLocal) {
        return solCode();
    }
}
