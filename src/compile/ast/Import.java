package compile.ast;

import compile.Utils;
import java.util.List;

public class Import implements SolNode {
    String filename;

    public Import(String filename) {
        this.filename = filename;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        return List.of(Utils.addIndent("import \"" + filename + "\";", indentLevel));
    }
}
