package ast;

import typecheck.CodeLocation;
import typecheck.ContractInfo;

import java.util.ArrayList;

public class StructDef extends Statement {
    String structName;
    ArrayList<AnnAssign> members;
    public StructDef(String structName, ArrayList<AnnAssign> members) {
        this.members= members;
        this.structName = structName;
    }

    //TODO: struct def NTCgenCons

    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        // assuming there is no double declaration

        contractInfo.typeMap.put(structName, contractInfo.toStructType(structName, members));

    }

}
