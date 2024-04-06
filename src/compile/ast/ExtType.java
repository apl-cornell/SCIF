package compile.ast;

public class ExtType extends StructType {

    String contractName;

    public ExtType(String contractName, String name) {
        super(name);
        this.contractName = contractName;
    }

    @Override
    public String solCode() {
        return contractName + "." + name;
    }
}
