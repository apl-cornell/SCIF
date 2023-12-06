package compile.ast;

public class ArrayType implements Type {
    Type baseType;

    public ArrayType(Type baseType) {
        this.baseType = baseType;
    }

    @Override
    public String solCode() {
        return baseType.solCode() + "[]";
    }
    @Override
    public String solCode(boolean isLocal) {
        return solCode() + (isLocal ?  " memory" : "");
    }
}
