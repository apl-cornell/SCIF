package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class Contract implements SolNode {
    String contractName;
    List<VarDec> stateVarDecs;
    List<StructDef> structDefs;
    List<Function> functions;

    public Contract(String contractName, List<VarDec> stateVarDecs, List<StructDef> structDefs, List<Function> functions) {
        this.contractName = contractName;
        this.stateVarDecs = stateVarDecs;
        this.structDefs = structDefs;
        this.functions = functions;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "contract " + contractName + "{", indentLevel);
        for (StructDef structDef: structDefs) {
            result.addAll(structDef.toSolCode(indentLevel + 1));
        }
        for (VarDec varDec: stateVarDecs) {
            result.addAll(varDec.toSolCode(indentLevel + 1));
        }
        for (Function function: functions) {
            result.addAll(function.toSolCode(indentLevel + 1));
        }
        addLine(result, "}", indentLevel);
        return result;
    }
}
