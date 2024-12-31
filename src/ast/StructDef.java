package ast;

import compile.CompileEnv;
import compile.ast.SolNode;
import java.util.List;
import java.util.stream.Collectors;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.exceptions.SemanticException;

import java.util.ArrayList;

public class StructDef extends TopLayerNode {

    String structName;
    List<StateVariableDeclaration> members;
    boolean isBuiltIn = false;

    public StructDef(String structName, List<StateVariableDeclaration> members) {
        this.members = members;
        this.structName = structName;
    }

    //TODO: struct def NTCgenCons

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) throws SemanticException {
        // assuming there is no double declaration

        contractSym.addType(structName, contractSym.toStructType(structName, members),
                location);
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) throws SemanticException {
        assert members.size() > 0: "struct should have at least one member: " + structName + " at " + location.errString();
        env.addType(structName, env.toStructType(structName, members));
        return true;
    }

    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
//        assert false;
        return null;
    }

    public compile.ast.StructDef solidityCodeGen(CompileEnv code) {
        return new compile.ast.StructDef(structName, members.stream().map(m -> m.solidityCodeGenStruct(code)).collect(
                Collectors.toList()));
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(members);
        return rtn;
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    public boolean typeMatch(StructDef structDef) {
        if (!structName.equals(structDef.structName)) {
            return false;
        }
        int i = 0;
        while (i < members.size()) {
            if (!members.get(i).typeMatch(structDef.members.get(i))) {
                return false;
            }
            ++i;
        }
        return true;
    }
}
