package ast;

import typecheck.ContractSym;
import typecheck.PathOutcome;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class StructDef extends FirstLayerStatement {
    String structName;
    ArrayList<AnnAssign> members;
    public StructDef(String structName, ArrayList<AnnAssign> members) {
        this.members= members;
        this.structName = structName;
    }

    //TODO: struct def NTCgenCons

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        // assuming there is no double declaration

        contractSym.addType(structName, contractSym.toStructType(structName, members));

    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(members);
        return rtn;
    }

}
