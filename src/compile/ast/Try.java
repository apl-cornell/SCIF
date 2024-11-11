package compile.ast;

import jflex.base.Pair;

import static compile.Utils.addLine;

import java.util.ArrayList;
import java.util.List;

public class Try implements Statement {
    ExternalCall externalCall;
    List<Pair<String, List<Statement>>> catchBlocks;

    public Try(ExternalCall externalCall, List<Pair<String, List<Statement>>> catchBlocks) {
        this.externalCall = externalCall;
        this.catchBlocks = catchBlocks;
    }

    @Override
    public List<String> toSolCode(int indentLevel) {
        List<String> result = new ArrayList<>();
        addLine(result, "Try " + externalCall.toSolCode() + " {", indentLevel);
        for (Pair<String, List<Statement>> catchBlock : catchBlocks) {
            String exception = catchBlock.fst;
            List<Statement> body = catchBlock.snd;
            addLine(result, "} Catch " + exception + " {", indentLevel);
            for (Statement s : body) {
                result.addAll(s.toSolCode(indentLevel + 1));
            }
        }
        addLine(result, "}", indentLevel);
        return result;
    }
}
