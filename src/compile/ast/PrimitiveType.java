package compile.ast;

import typecheck.Utils;

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
         if (name.equals("bytes") && isLocal) {
             return name + " memory";
         }
         return solCode();
    }
}
