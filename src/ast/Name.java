package ast;

import compile.CompileEnv;
import compile.ast.SingleVar;
import compile.ast.Statement;
import compile.ast.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;
import typecheck.exceptions.NameNotFoundException;

public class Name extends Variable {

    public String id;

    //Context ctx;
    public Name(String x) {
        id = x;
        location = CodeLocation.builtinCodeLocation();
        // id = new ScopedName(name, "");
        // ctx = null;
    }
    /*public Name(String name, Context y) {
        id = name;
        ctx = y;
    }*/


    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        Sym s = env.getCurSym(id);
        logger.debug("Name: " + id);
        // logger.debug(s.toString());
        if (s instanceof FuncSym) {
            assert false : id + " at " + location.errString();
            return null;
        } else if (s instanceof VarSym) {
            ScopeContext now = new ScopeContext(this, parent);
            TypeSym typeSym = ((VarSym) s).typeSym;
            logger.debug(s.getName());
            env.addCons(now.genEqualCons(typeSym, env, location, "The variable is of improper type"));
            logger.debug(now.toString());
            return now;
        } else if (s instanceof TypeSym) {
            assert false;
            return null;
        }
        throw new NameNotFoundException(id, location);
    }

    @Override
    public List<Node> children() {
        return new ArrayList<>();
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        // assuming the name would be a variable name
        logger.debug("Name: " + id);
        String ifNameRtn = env.getVar(id).labelNameSLC();
        return new ExpOutcome(ifNameRtn, new PathOutcome(new PsiUnit(env.inContext)));
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new SingleVar(id);
    }

    @Override
    public VarSym getVarInfo(NTCEnv env) {
        return ((VarSym) env.getCurSym(id));
    }

    @Override
    public VarSym getVarInfo(VisitEnv env, boolean tail_position, Map<String, String> dependentMapping) {
        VarSym rnt = env.getVar(id);
        return rnt;
    }


//    public String toSolCode() {
//        return id;
//    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Name &&
                ((Name) expression).id.equals(id);
    }

    public boolean typeMatch(Name value) {
        return id.equals(value.id);
    }


    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        Type varType = code.getLocalVarType(id);
        if (varType != null) {
            result.put(id, varType);
        }
        return result;
    }

    public Map<String,? extends Type> writeMap(CompileEnv code) {
        return readMap(code);
    }
}
