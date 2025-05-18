package ast;

import compile.CompileEnv;
import compile.ast.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.PathOutcome;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

public abstract class Statement extends Node {


    /** Generate SHErrLoc information-flow constraints for this statement. */
    public abstract PathOutcome genConsVisit(VisitEnv env, boolean tail_position) throws SemanticException;

    public abstract List<compile.ast.Statement> solidityCodeGen(CompileEnv code);
    public String toString() {
        return "";
    }

    /**
     *  return Solidity statement without the delimiter.
     */
//    public String toSolCode() {
//        assert false;
//        return "";
//    }

    public boolean exceptionHandlingFree() {
        return true;
    }

    protected Map<String,? extends Type> readMap(CompileEnv code) {
        return new HashMap<>();
    }

    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        return new HashMap<>();
    }
}
