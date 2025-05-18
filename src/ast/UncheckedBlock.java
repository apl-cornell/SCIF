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

    List<Statement> body;

    public UncheckedBlock(List<Statement> body) {
        this.body = body;
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        for (Statement s : body) {
            s.genTypeConstraints(env, parent);
        }

        return parent;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException  {

        int index = 0;
        PathOutcome ifo = null;
        for (Statement stmt: body) {
            ++index;
            String prevLambda = env.inContext.lambda;
            ifo = stmt.IFCVisit(env, index == body.size() && tail_position);
            PsiUnit normalUnit = ifo.getNormalPath();
            if (normalUnit == null) {
                break;
            }
            env.inContext = Utils.genNewContextAndConstraints(env, index == body.size() && tail_position, normalUnit.c, prevLambda, stmt.nextPcSHL(), stmt.location);
        }
        return ifo;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        List<compile.ast.Statement> block_body = new ArrayList<>();
        for (Statement stmt : body) {
            block_body.addAll(stmt.solidityCodeGen(code));
        }
        result.add(new UncheckedStatement(block_body));
        return result;
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = parent;
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(body);
        return rtn;
    }
    @Override
    public boolean exceptionHandlingFree() {
        for (Statement s: body) {
            if (!s.exceptionHandlingFree()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Map<String,? extends Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.readMap(code));
        }
        return result;
    }

    @Override
    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}
