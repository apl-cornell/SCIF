package ast;

import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;
import utils.VarInfo;

import java.util.ArrayList;
import java.util.HashMap;

public class Expression extends Statement {
    public VarInfo getVarInfo(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        return null;
    }

}
