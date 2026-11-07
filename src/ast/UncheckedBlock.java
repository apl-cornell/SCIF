package ast;

import compile.CompileEnv;
import compile.ast.Type;
import compile.ast.UncheckedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;

public class UncheckedBlock extends Statement {

    Block body;

    public UncheckedBlock(List<Statement> body) {
        this.body = new Block(body);
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        body.genTypeConstraints(env, parent);
        return parent;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException  {
        return body.IFCVisit(env, tail_position);
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        result.add(new UncheckedStatement(body.solidityCodeGen(code)));
        return result;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(body);
        return rtn;
    }

    @Override
    public boolean exceptionHandlingFree() {
        return body.exceptionHandlingFree();
    }

    @Override
    protected Map<String,? extends Type> readMap(CompileEnv code) {
        return body.readMap(code);
    }

    @Override
    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        return body.writeMap(code);
    }
}
