package ast;

import compile.CompileEnv;
import compile.ast.SolNode;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

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
    public void globalInfoVisit(InterfaceSym contractSym) {
        // assuming there is no double declaration

        contractSym.addType(structName, contractSym.toStructType(structName, members));

    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        assert false;
        return null;
    }

    public SolNode solidityCodeGen(CompileEnv code) {
        assert false;
        return null;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(members);
        return rtn;
    }

}
