package typecheck;

public class ScopedName {
    String name;
    String contractName;

    public ScopedName(String name, String contractName) {
        this.name = name;
        this.contractName = contractName;
    }
}
