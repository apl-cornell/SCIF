package compile.ast;

import static compile.Utils.addLine;

import compile.Utils;
import java.util.ArrayList;
import java.util.List;

public class ContractFile implements SourceFile {
    List<Import> imports;

    Contract contract;

    public ContractFile(List<Import> imports, Contract contract) {
        this.imports = imports;
        this.contract = contract;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, Utils.version(Utils.DEFAULT_SOLITIDY_VERSION), indentLevel);
        for (Import imp: imports) {
            result.addAll(imp.toSolCode(indentLevel));
        }
        result.addAll(contract.toSolCode(indentLevel));
        return result;
    }
}
