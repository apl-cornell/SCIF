package typecheck;

public class ArrayTypeSym extends TypeSym {
    // types of key and value have no labels
    private final int size;
    public TypeSym valueType;

    /*public MapTypeSym() {
    }*/

    public ArrayTypeSym(int size, TypeSym valueType, ScopeContext defContext) {
        super(getArrayName(size, valueType), defContext);
        this.size = size;
        this.valueType = valueType;
        // this.name = name();
    }

    public ArrayTypeSym(ArrayTypeSym arrayTypeSym) {
        super(arrayTypeSym.getName(), arrayTypeSym.defContext());
        size = arrayTypeSym.size;
        valueType = arrayTypeSym.valueType;
        // name = name();
    }

//    @Override
//    public String name() {
//        return this.name;
//        // return "Map." + keyType.name() + "." + valueType.name();
//    }

    public static String getArrayName(int size, TypeSym valueType) {
        return "A" + size + "." + valueType.getName();
    }
}
