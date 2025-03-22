package compile.ast;

import static compile.Utils.addLine;

import ast.Type;
import java.util.ArrayList;
import java.util.List;

public class StructDef extends Type {
    List<VarDec> members;

    public StructDef(String name, List<VarDec> members) {
        super(name);
        this.members = members;
        for (VarDec member: members) {
            member.isLocal = false;
        }
    }

    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "struct " + name() + " {", indentLevel);
        for (VarDec varDec: members) {
            result.addAll(varDec.toSolCode(indentLevel + 1));
        }
        addLine(result, "}", indentLevel);
        return result;
    }
}
