package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Expression extends Statement {
    public VarInfo getVarInfo(VisitEnv env) {
        return new VarInfo();
    }
    public VarInfo getVarInfo(NTCEnv env) {
        return new VarInfo();
    }

    public String toSolCode() { return "unknown exp"; }
}
