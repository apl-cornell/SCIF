package ast;

import compile.SolCode;
import java.util.ArrayList;
import java.util.List;
import typecheck.Context;
import typecheck.ContractSym;
import typecheck.ExceptionTypeSym;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Sym;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Relation;

public class Revert extends Statement {

    final String message;

    public Revert(String message) {
        this.message = message;
    }
    public Revert() {
        this.message = "";
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return parent;
    }

    @Override
    public void solidityCodeGen(SolCode code) {
        
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));
        PathOutcome psi = new PathOutcome(new PsiUnit(beginContext));

        return psi;
    }
}
