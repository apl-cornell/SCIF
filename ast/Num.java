package ast;

import sherrlocUtils.Relation;
import typecheck.NTCContext;
import typecheck.NTCEnv;

public class Num<T extends Number> extends Literal {
    T value;
    public Num(T x) {
        value = x;
    }

    @Override
    public String toSolCode() {
        return value.toString();
    }
}
