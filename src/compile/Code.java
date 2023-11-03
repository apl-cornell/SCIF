package compile;

import java.util.ArrayList;
import java.util.List;

public class Code {
    List<CodeToken> code = new ArrayList<>();

    public void addLine(String line) {
        if (line.equals("assert(true);") || line.equals("assert(!false);")) {
            line = "// " + line;
        }
        code.add(new CodeLine(line));
    }

    void addIndent() {
        code.add(new CodeIndent());
    }

    void decIndent() {
        code.add(new CodeIndent(-1));
    }

    public void enterContractDef(String contractName) {
        addLine("contract " + contractName + " {");
        addIndent();
    }

    public void enterInterfaceDef(String name) {
        addLine("interface " + name + " {");
        addIndent();
    }

    public void leaveContractDef() {
        decIndent();
        addLine("}");
    }

    public void leaveInterfaceDef() {
        decIndent();
        addLine("}");
    }
}
