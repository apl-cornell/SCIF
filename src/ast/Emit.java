package ast;

import compile.CompileEnv;
import typecheck.*;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Relation;

import java.util.ArrayList;
import java.util.List;

public class Emit extends Statement {

    Call event;

    public Emit(Call event) {
        this.event = event;
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        // check event must of form a(b)
        String eventName;
        EventTypeSym eventTypeSym;
        if (event.value instanceof Name) {
            // a(b)
            eventName = ((Name) event.value).id;
            Sym s = env.getCurSym(eventName);
            if (s == null) {
                System.err.println("event type not found");
                return null;
            }
            if (!(s instanceof EventTypeSym)) {
                System.err.println("event type mismatch");
                return null;
            }
            eventTypeSym = (EventTypeSym) s;
        } else {
            System.err.println("event illegal");
            return null;
        }

        // check number of parameters
        // TODO stpeh assert?
        assert eventTypeSym.parameters().size() == event.args.size() : "ummatched parameter number for event";

        // typecheck arguments
        // TODO steph double check with correctness
        for (int i = 0; i < event.args.size(); i++) {
            Expression arg = event.args.get(i);
            TypeSym paraInfo = eventTypeSym.parameters().get(i).typeSym;
            assert !now.getSHErrLocName().startsWith("null");
            ScopeContext argContext = arg.genTypeConstraints(env, now);
            String typeNameSLC = paraInfo.toSHErrLocFmt();
            Constraint argCon = argContext.genTypeConstraints(typeNameSLC, Relation.GEQ, env, arg.location);
            env.addCons(argCon);
        }


        return now;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
        List<compile.ast.Statement> result = new ArrayList<>();
//        compile.ast.Call call = (compile.ast.Call) event.solidityCodeGen(result, code);
//        result.add(new compile.ast.Emit(call));

        assert event.value instanceof Name : "internal error: event is malformed"; // internal
        String eventName = ((Name) event.value).id;
        EventTypeSym eventTypeSym = code.findEventTypeSym(eventName);
        List<compile.ast.Expression> args = new ArrayList<>();
        for (int i = 0; i < event.args.size(); i++) {
            Expression arg = event.args.get(i);
            args.add(arg.solidityCodeGen(result, code));
        }
        result.add(new compile.ast.Emit(eventName, args));
        return result;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        // TODO placeholder
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        return new PathOutcome(new PsiUnit(endContext));
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(event);
        return rtn;
    }
}
