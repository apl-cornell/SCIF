package ast;

import compile.CompileEnv;
import compile.ast.Type;

import java.util.List;
import java.util.Map;
import typecheck.*;

import java.util.ArrayList;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

public class Assert extends Statement {

    Expression test;
    Expression msg;

    public Assert(Expression test, Expression msg) {
        this.test = test;
        this.msg = msg;
    }


    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        //TODO: exceptions
        if (test instanceof FlowsToExp flowsToExp) {
            Name left = (Name) flowsToExp.lhs, right = (Name) flowsToExp.rhs;
            if (env.containsVar(left.id) && env.containsVar(right.id)) {

                VarSym l = env.getVar(left.id), r = env.getVar(right.id);
//                // logger.debug(l.toString());
//                // logger.debug(r.toString());
                Inequality hypo = new Inequality(l.toSHErrLocFmt(), Relation.LEQ,
                        r.toSHErrLocFmt());

                env.hypothesis().add(hypo);
            }

        }
        return test.genIFConstraints(env, tail_position).psi;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = scopeContext;
        ScopeContext rtn = null;
        rtn = test.genTypeConstraints(env, now);
        Constraint testCon = rtn.genTypeConstraints(Utils.BuiltinType2ID(BuiltInT.BOOL), Relation.EQ, env, test.location);
        env.addCons(testCon);
        return now;
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
