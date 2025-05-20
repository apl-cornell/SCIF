package ast;

import compile.CompileEnv;
import java.util.ArrayList;
import java.util.List;
import typecheck.*;

public class DynamicStatement extends Statement {

    Call call;

    public DynamicStatement(Call call) {
        this.call = call;
    }

    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) {
        //TODO: check arguments
        return parent;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        assert false;
        return null;
    }
//
//    public String toSolCode() {
//        // logger.debug("toSOl: DynamicStatement");
//        return call.toSolCode();
//    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(call);
        return rtn;
    }
}
