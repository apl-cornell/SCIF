package ast;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import utils.*;

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

    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
    }
}
