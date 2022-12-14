package ast;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import compile.SolCode;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import typecheck.*;

import java.util.ArrayList;

/**
 * An AST node
 */
public abstract class Node {

    /**
     * Record where the corresponding code is in original source file
     */
    CodeLocation location;

    public CodeLocation getLocation() {
        return location;
    }
    
    ScopeContext scopeContext;

    public void setLoc(CodeLocation location) {
        this.location = location;
    }

    public String locToString() {
        return this.location.toString();
    }

    public String toSHErrLocFmt() {
        return this.getClass().getSimpleName() + "" + location;
    }

    /**
     * take each statement as an expression, return the type (context) as result.
     */
    public abstract ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent);

    public abstract void solidityCodeGen(SolCode code);

    public List<Node> children() {
        return new ArrayList<>();
    }

    public void passScopeContext(ScopeContext parent) {
        scopeContext = parent;
        for (Node node : children()) {
            if (node != null) {
                node.passScopeContext(scopeContext);
            }
        }
    }

    /**
     * A Genson object for serialization
     */
    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true)
            .useRuntimeType(true).create();

    @Override
    public String toString() {
        return "TODO";
        //return genson.serialize(location);
    }

    protected static final Logger logger = LogManager.getLogger();

    public ScopeContext getScopeContext() {
        return scopeContext;
    }
}
