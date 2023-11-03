package compile.ast;

import java.util.List;

public class ExternalCall extends Call {
    Expression contractVar;

    public ExternalCall(Expression contractVar, String funcName, List<Expression> argValues) {
        super(funcName, argValues);
        this.contractVar = contractVar;
    }

    @Override
    public String toSolCode() {
        return contractVar.toSolCode() + "." + super.toSolCode();
    }
}
