package compile.ast;

public class StructType implements Type {
    String name;
    boolean isStorage = false;
    public StructType(String name) {
        this.name = name;
    }
    public StructType(String name, boolean isStorage) {
        this.name = name;
        this.isStorage = isStorage;
    }
    public void setStorage() {
        isStorage = true;
    }

    @Override
    public String solCode() {
        return name;
    }

    @Override
    public String solCode(boolean isLocal) {
        return solCode() + (isStorage ?
                " storage" : (isLocal ?  " memory" : ""));
    }
}
