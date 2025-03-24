package typecheck;

import compile.ast.PrimitiveType;
import compile.ast.Type;

public class BuiltinTypeSym extends TypeSym {
    public BuiltinTypeSym(String typeName) {
        super(typeName, Utils.globalScopeContext());
        // this.name = typeName;
    }

    @Override
    public boolean isVoid() {
        return getName().equals(Utils.BuiltinType2ID(BuiltInT.VOID));
    }

    @Override
    public Type getType() {
        return new PrimitiveType(this.getName());
    }
}
