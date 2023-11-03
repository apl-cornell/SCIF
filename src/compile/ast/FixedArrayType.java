package compile.ast;

public class FixedArrayType extends ArrayType {
    int size;

    public FixedArrayType(Type baseType, int size) {
        super(baseType);
        this.size = size;
    }

    @Override
    public String solCode() {
        return baseType.solCode() + "[" + size + "]";
    }
}
