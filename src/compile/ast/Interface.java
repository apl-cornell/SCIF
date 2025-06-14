package compile.ast;

import ast.EventDef;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class Interface implements SolNode {
    String interfaceName;
    List<StructDef> structDefs;
    List<Event> eventDefs;
    List<FunctionSig> functionSigs;

    public Interface(String interfaceName, List<StructDef> structDefs, List<Event> eventDefs, List<FunctionSig> functionSigs) {
        this.interfaceName = interfaceName;
        this.structDefs = structDefs;
        this.eventDefs = eventDefs;
        this.functionSigs = functionSigs;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "interface " + interfaceName + " {", indentLevel);
        for (StructDef structDef: structDefs) {
            result.addAll(structDef.toSolCode(indentLevel + 1));
        }
        for (Event eventDef: eventDefs) {
            result.addAll(eventDef.toSolCode(indentLevel + 1));
        }
        for (FunctionSig functionSig: functionSigs) {
            result.addAll(functionSig.toSolCode(indentLevel + 1));
        }
        addLine(result, "}", indentLevel);
        return result;
    }

}
