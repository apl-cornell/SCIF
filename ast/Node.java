package ast;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import compile.SolCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class Node {
    public CodeLocation location;
    public ScopeContext scopeContext;
    public Node() {
    }
    public void setLoc(CodeLocation location) {
        this.location = location;
    }
    public String locToString() {
        return this.location.toString();
    }


    public String toSHErrLocFmt() {
        return this.getClass().getSimpleName() + "" + location;
    }

    //public abstract void globalInfoVisit(ContractSym contractSym);
    //public abstract PathOutcome genConsVisit(VisitEnv env, boolean tail_position);

    public void findPrincipal(HashSet<String> principalSet) {
    }

    public boolean NTCGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

    /* take each statement as an expression, return the type (context) as result. */
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        /* not supposed to call this implementation */
        return null;
    }

    public boolean NTCinherit(InheritGraph graph) {
        return false;
    }

    public void SolCodeGen(SolCode code) {
        // npt supposed to be called
        return;
    }

    public ArrayList<Node> children() {
        return new ArrayList<>();
    };


    public void passScopeContext(ScopeContext parent) {
        scopeContext = parent;
        for (Node node : children())
            if (node != null)
                node.passScopeContext(scopeContext);
    }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return "TODO";
        //return genson.serialize(location);
    }
    protected static final Logger logger = LogManager.getLogger();

}
