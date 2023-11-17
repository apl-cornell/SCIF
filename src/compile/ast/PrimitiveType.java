package compile.ast;

import compile.Utils;

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
        if (name.equals("uint")) {
            return "uint256";
        }
        return name;
    }

    @Override
    public String solCode(boolean isLocal) {
         if (isLocal && !isPrimitive()) {
             return solCode() + " memory";
         }
         return solCode();
    }

    private boolean isPrimitive() {
        return name.equals(Utils.PRIMITIVE_TYPE_UINT_NAME) ||
                name.equals(Utils.PRIMITIVE_TYPE_BOOL_NAME) ||
                name.equals(Utils.PRIMITIVE_TYPE_VOID_NAME) ||
                name.equals(Utils.PRIMITIVE_TYPE_ADDRESS_NAME);
    }
}
