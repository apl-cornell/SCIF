package compile.ast;

import java.util.List;
import java.util.stream.Collectors;

public class LowLevelCall extends ExternalCall {
    FunctionSig functionSig;

    public LowLevelCall(FunctionSig functionSig, Expression contractVar, String funcName, List<Expression> argValues) {
        super(contractVar, funcName, argValues, null);
        this.functionSig = functionSig;
    }

    @Override
    public String toSolCode() {
        return contractVar.toSolCode() + ".call(abi.encodeWithSignature(\"" + functionSig.signature() + "\", " +
                argValues.stream().map(Expression::toSolCode).collect(Collectors.joining(", ")) +
                "))";
    }
}
