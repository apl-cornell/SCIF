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
    CodeLocation location = new CodeLocation();

    public CodeLocation getLocation() {
        return location;
    }

    /**
     * Represent the scope of where the current node is defined
     */
    ScopeContext scopeContext;

    public void setLoc(CodeLocation location) {
        this.location = location;
    }

    public String locToString() {
        return this.location.toString();
    }

    public String toSHErrLocFmt() {
        return scopeContext + "." + this.getClass().getSimpleName() + "" + location;
    }

    /**
     * take each statement as an expression, return the type (context) as result.
     */
    public abstract ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent);

    public abstract void solidityCodeGen(SolCode code);

    /**
     * Return a list of AST nodes that are the current node's kids
     */
    public List<Node> children() {
        return new ArrayList<>();
    }

    /**
     * set the scopeContext of the current node, and update children's scopeContext
     * @param parent
     */
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
        return "Node";
        //return genson.serialize(location);
    }

    protected static final Logger logger = LogManager.getLogger();

    public ScopeContext getScopeContext() {
        return scopeContext;
    }
}
