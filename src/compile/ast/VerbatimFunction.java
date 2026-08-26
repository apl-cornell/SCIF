package compile.ast;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

/**
 * A contract member function whose text is pre-rendered — loaded from
 * a Solidity resource file and emitted as-is (indented). Used for the
 * fixed runtime functions (label checks, closure packing) whose bodies
 * include constructs the structured nodes cannot express, e.g. inline
 * assembly.
 */
public class VerbatimFunction extends Function {
    private final List<String> lines;

    public VerbatimFunction(String name, List<String> lines) {
        super(name, new ArrayList<>(), new PrimitiveType(compile.Utils.PRIMITIVE_TYPE_VOID_NAME),
                false, false, new ArrayList<>());
        this.lines = lines;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            addLine(result, line, indentLevel);
        }
        return result;
    }
}
