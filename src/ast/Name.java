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


    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        Sym s = env.getCurSym(id);
        // logger.debug("Name: " + id);
        // // logger.debug(s.toString());
        if (s instanceof FuncSym) {
            throw new SemanticException("method " + id + " cannot be used as a value", location);
        } else if (s instanceof VarSym) {
            ScopeContext now = new ScopeContext(this, parent);
            TypeSym typeSym = ((VarSym) s).typeSym;
            // logger.debug(s.getName());
            env.addCons(now.genEqualCons(typeSym, env, location, "Variable `" + id + "` has the wrong type"));
            // logger.debug(now.toString());
            return now;
        } else if (s instanceof TypeSym) {
            throw new SemanticException("type " + id + " cannot be used as a value", location);
        }
        throw new NameNotFoundException(id, location);
    }

    @Override
    public List<Node> children() {
        return new ArrayList<>();
    }

    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position) {
        VarSym var = env.getVar(id);
        String ifNameRtn = var.labelNameSLC();
        return new ExpOutcome(ifNameRtn, new PathOutcome(new PsiUnit(env.inContext)));
    }

    @Override
    public String principalName(VisitEnv env) {
        if (env.getCurSym(id) instanceof VarSym v && v.isPrincipalTyped()) {
            return env.principalName(v);
        }
        return null;
    }

    @Override
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        return new SingleVar(id);
    }

    @Override
    public VarSym getVarInfo(NTCEnv env) {
        return env.getCurSym(id) instanceof VarSym v ? v : null;
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

    @Override
    protected TypeSym staticType(VisitEnv env) {
        return env.getVar(id).typeSym;
    }
}
