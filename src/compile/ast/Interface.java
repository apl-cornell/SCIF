package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class Interface implements SolNode {
    String interfaceName;
    List<FunctionSig> functionSigs;

    public Interface(String interfaceName, List<FunctionSig> functionSigs) {
        this.interfaceName = interfaceName;
        this.functionSigs = functionSigs;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "interface " + interfaceName + " {", indentLevel);
        for (FunctionSig functionSig: functionSigs) {
            result.addAll(functionSig.toSolCode(indentLevel + 1));
        }
        addLine(result, "}", indentLevel);
        return result;
    }

}
