package compile.ast;

import static compile.Utils.addLine;

import compile.CompileEnv;
import compile.Utils;
import java.util.ArrayList;
import java.util.List;

public class InterfaceFile implements SourceFile {
    List<Import> imports;

    Interface itrface;
    boolean firstInFile;


    public InterfaceFile(List<Import> imports, Interface itrface, boolean firstInFile) {
        this.imports = imports;
        this.itrface = itrface;
        this.firstInFile = firstInFile;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, Utils.version(Utils.DEFAULT_SOLITIDY_VERSION), indentLevel);
        if (firstInFile) {
            for (Import imp: imports) {
                result.addAll(imp.toSolCode(indentLevel));
            }
        }
        result.addAll(itrface.toSolCode(indentLevel));
        return result;
    }

    @Override
    public void addStats(CompileEnv env) {
        // pass
    }

    @Override
    public boolean firstInFile() {
        return firstInFile;
    }
}