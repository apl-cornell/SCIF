package ast;

import compile.SolCode;
import java.util.HashSet;
import typecheck.ContractSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class StructDef extends TopLayerNode {

    String structName;
    ArrayList<AnnAssign> members;

    public StructDef(String structName, ArrayList<AnnAssign> members) {
        this.members = members;
        this.structName = structName;
    }

    //TODO: struct def NTCgenCons

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        // assuming there is no double declaration

        contractSym.addType(structName, contractSym.toStructType(structName, members));

    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

//    @Override
//    public void findPrincipal(HashSet<String> principalSet) {
//
//    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(members);
        return rtn;
    }

}
