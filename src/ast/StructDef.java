package ast;

import compile.CompileEnv;
import compile.ast.SolNode;
import java.util.List;
import java.util.stream.Collectors;
import typecheck.InterfaceSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.StructTypeSym;
import typecheck.Sym;
import typecheck.VarSym;
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

    /**
     * Resolves the labels of the members of this struct's type in {@code contractSym}, whose
     * state variables, including the built-in principals, must already be declared.
     */
    public void resolveMemberLabels(InterfaceSym contractSym) throws SemanticException {
        StructTypeSym structSym = (StructTypeSym) contractSym.lookupSym(structName);
        for (int i = 0; i < members.size(); ++i) {
            structSym.members().get(i).setLabel(contractSym.newLabel(members.get(i).label()));
        }
    }

    @Override
    public void ntcGlobalInfo(NTCEnv env, ScopeContext parent) throws SemanticException {
        if (members.isEmpty()) {
            throw new SemanticException("struct " + structName + " has no members", location);
        }
        Sym existing = env.getCurSym(structName);
        if (existing != null && !(existing instanceof StructTypeSym)) {
            throw new SemanticException("name already defined: " + structName, location);
        }
        env.addType(structName, env.toStructType(structName, members));
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent)
            throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        for (StateVariableDeclaration member : members) {
            member.label().genTypeConstraints(env, now);
            // Like the label of a field, a member's label must mean the same thing wherever
            // the struct is used, so it may not name a principal that can change.
            checkNamesFinal(member.label(), env);
        }
        return now;
    }

    private void checkNamesFinal(IfLabel label, NTCEnv env) throws SemanticException {
        if (label instanceof ComplexIfLabel c) {
            checkNamesFinal(c.getLeft(), env);
            checkNamesFinal(c.getRight(), env);
        } else if (label instanceof PrimitiveIfLabel p
                && env.getCurSym(p.value().id) instanceof VarSym v && v.isMutablePrincipal()) {
            throw new SemanticException("the label of a member of struct " + structName
                    + " names " + v.getName() + ", which is not final", label.location());
        }
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
