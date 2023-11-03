package ast;

import compile.CompileEnv;
import compile.ast.SingleVar;
import compile.ast.Type;
import java.beans.PersistenceDelegate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import typecheck.*;

import java.util.ArrayList;

public class Assert extends Statement {

    Expression test;
    Expression msg;

    public Assert(Expression test, Expression msg) {
        this.test = test;
        this.msg = msg;
    }


    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO: exceptions
        test.genConsVisit(env, tail_position);
        return null;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
        result.add(new compile.ast.Assert(test.solidityCodeGen(result, code)));
        return result;
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(test);
        rtn.add(msg);
        return rtn;
    }

    @Override
    protected Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = test.readMap(code);
        if (msg != null) {
            result.putAll(msg.readMap(code));
        }
        return result;
    }

}
