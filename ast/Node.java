package ast;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Node {
    CodeLocation location;
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

    public void globalInfoVisit(HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap) {
        //Do nothing
    }

    public String genConsVisit(VisitEnv env) {
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
    }

    protected static final Logger logger = LogManager.getLogger();

}
