package ast;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import compile.SolCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Node {
    public CodeLocation location;
    public Node() {
    }
    public void setLoc(CodeLocation location) {
        this.location = location;
    }
    public String locToString() {
        return this.location.toString();
    }

    static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return genson.serialize(this);
    }

    public String toSHErrLocFmt() {
        return this.getClass().getSimpleName() + "" + location;
    }

    public void globalInfoVisit(ContractInfo contractInfo) {
        //Do nothing
    }

    public Context genConsVisit(VisitEnv env) {
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
    }

    public boolean NTCGlobalInfo(NTCEnv env, NTCContext parent) {
        return false;
    }

    /* take each statement as an expression, return the type (context) as result. */
    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        /* not supposed to call this implementation */
        return null;
    }

    public void SolCodeGen(SolCode code) {
        // npt supposed to be called
        return;
    }
    protected static final Logger logger = LogManager.getLogger();

}
