package ast;

import compile.CompileEnv;
import compile.ast.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class Delete extends Statement {

    List<Expression> targets;

    public Delete(List<Expression> targets) {
        this.targets = targets;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        assert false;
        return null;
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.addAll(targets);
        return rtn;
    }
    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Expression target: targets) {
            result.putAll(target.readMap(code));
        }
        return result;
    }
    @Override
    public java.util.Map<String, compile.ast.Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Expression target: targets) {
            result.putAll(target.writeMap(code));
        }
        return result;
    }
}
