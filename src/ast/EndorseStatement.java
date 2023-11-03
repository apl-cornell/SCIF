package ast;

import compile.CompileEnv;
import compile.Utils;
import compile.ast.IfStatement;
import compile.ast.Literal;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.plaf.nimbus.State;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class EndorseStatement extends Statement {

    List<Name> expressionList;
    IfLabel from, to;
    List<Statement> body;

    public EndorseStatement(List<Name> expressionList, IfLabel from, IfLabel to,
            List<Statement> body) {
        this.expressionList = expressionList;
        this.from = from;
        this.to = to;
        this.body = body;
    }

    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // TODO
        return parent;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        for (Statement s: body) {
            result.addAll(s.solidityCodeGen(code));
        }
        return List.of(new IfStatement(new Literal(Utils.SOL_TRUE), result));
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO

        return null;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.addAll(expressionList);
        rtn.add(from);
        rtn.add(to);
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
    protected java.util.Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.readMap(code));
        }
        return result;
    }

    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}
