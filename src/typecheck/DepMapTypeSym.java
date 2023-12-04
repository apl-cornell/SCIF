package typecheck;

import ast.LabeledType;

/**
 *  Represent a dependant map type.
 *  A dependant map is written as map(x: t0, t1{l}),
 *  where t0 and t1 are types of the key and value,
 *  x is a name that can be used in the later label l
 *  that describe the integrity level of the value corresponding to the key.
 */
public class DepMapTypeSym extends MapTypeSym {

    private VarSym key;

    public DepMapTypeSym(TypeSym keyType, String keyName, TypeSym valueType, Label valueLabel, ScopeContext defContext, ScopeContext newContext) {
        super(keyType, valueType, defContext);
        key = new VarSym(
                keyName,
                keyType,
                null,
                newContext.cur().getLocation(),
                newContext,
                false,
                true,
                true
        );
        this.valueLabel = valueLabel;
    }

    public DepMapTypeSym(DepMapTypeSym depMapTypeInfo) {
        super(depMapTypeInfo);
        this.key = depMapTypeInfo.key;
        this.valueLabel = depMapTypeInfo.valueLabel;
    }

    public VarSym key() {
        return key;
    }
    public Label label() {
        return valueLabel;
    }
}
