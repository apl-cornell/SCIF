package compile.ast;

public class Argument {

    public Type type() {
        return type;
    }

    public String name() {
        return name;
    }

    Type type;
    String name;

    public Argument(Type type, String name) {
        this.type = type;
        this.name = name;
    }
}
