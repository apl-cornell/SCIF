package ast;

import java.util.ArrayList;

public class StructDef extends Statement {
    String structName;
    ArrayList<AnnAssign> members;
    public StructDef(String structName, ArrayList<AnnAssign> members) {
        this.members= members;
        this.structName = structName;
    }
}
