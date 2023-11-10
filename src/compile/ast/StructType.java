package compile.ast;

public class StructType implements Type {
    String name;
    public StructType(String name) {
        this.name = name;
    }

    @Override
    public String solCode() {
        return name;
    }

    @Override
    public String solCode(boolean isLocal) {
        return solCode() + (isLocal ?  " memory" : "");
    }
}
