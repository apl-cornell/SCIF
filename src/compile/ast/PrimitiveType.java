package compile.ast;

public class PrimitiveType implements Type {
    public String name() {
        return name;
    }
    String name;
    public PrimitiveType(String name) {
        this.name = name;
    }

    @Override
    public String solCode() {
        return name;
    }

    @Override
    public String solCode(boolean isLocal) {
        return name.equals("bytes") ? name + " memory" : name;
    }
}
