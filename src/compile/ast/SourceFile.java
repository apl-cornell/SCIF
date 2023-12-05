package compile.ast;

import compile.CompileEnv;
import java.util.List;

public interface SourceFile extends SolNode {

    void addStats(CompileEnv env);
}
