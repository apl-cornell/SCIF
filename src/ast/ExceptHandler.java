package ast;

import compile.CompileEnv;
import compile.ast.Assign;
import compile.ast.Expression;
import compile.ast.IfStatement;
import compile.ast.PrimitiveType;
import compile.ast.SingleVar;
import compile.ast.StructType;
import compile.ast.VarDec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import typecheck.*;

public class ExceptHandler extends Node {

    private LabeledType labeledType;
    private String name;
    private List<Statement> body;
    final private boolean acceptall;

    public ExceptHandler(LabeledType type, String name, List<Statement> body) {
        this.labeledType = type;
        this.name = name;
        this.body = body;
        acceptall = false;
    }

    /**
     * Create a handler that accept all other types of exceptions(try/catch) or failures(atomic/rescue)
     */
    public ExceptHandler(List<Statement> body) {
        this.body = body;
        acceptall = true;
    }

    public void setBody(List<Statement> body) {
        this.body = body;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();

        if (!acceptall) {
            VarSym var = env.newVarSym(name, labeledType, true, true, true, location, now);
            assert var != null : "Exception type " + labeledType.type().name() + " not found";
            env.addSym(name, var);
        }

        for (Statement s : body) {
            ScopeContext tmp = s.ntcGenCons(env, now);
        }
        env.exitNewScope();
        return now;
    }

    public IfStatement solidityCodeGen(CompileEnv code, Map<String, compile.ast.Type> writeMap, SingleVar dataVar) {
        List<compile.ast.Statement> solBody = new ArrayList<>();
        if (acceptall) {
            for (Statement s: body) {
                solBody.addAll(s.solidityCodeGen(code));
            }
            return code.testNonNormalPath(solBody);
        }

        // convert the expected exception type to id
        // assign related exception data to the local variable
        // create a local variable and assign data to it
        ExceptionTypeSym exceptionTypeSym = code.findExceptionTypeSym(labeledType.type().name());

        // remove pending exception state
        List<Expression> targets = writeMap.entrySet().stream().map(entry -> new SingleVar(entry.getKey())).collect(
                Collectors.toList());
        if (exceptionTypeSym.parameters().size() > 0) {
            // create local variable for exception info
            // we have a local name: this.name for the variable
            // define struct for all exceptions at the beginning
            StructType varType = new StructType(labeledType.type().name());
            String varName = name;
            solBody.add(new VarDec(varType, name));
            targets.add(new SingleVar(name));
            code.addLocalVar(varName, varType);
        }
        if (targets.size() > 0) {
            solBody.add(
                    new Assign(
                            targets,
                            code.decodeVarsAndException(exceptionTypeSym, writeMap, dataVar)));
        }


        for (Statement s: body) {
            solBody.addAll(s.solidityCodeGen(code));
        }

        IfStatement testException = code.testException(exceptionTypeSym, solBody);
        return testException;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(labeledType);
        rtn.addAll(body);
        return rtn;
    }

    public String getHandlerPcLabelName() {
        return scopeContext.getSHErrLocName() + "." + "handlerPcLabelName" + location.toString();
    }

    public String getHandlerLockLabelName() {
        return scopeContext.getSHErrLocName() + "." + "handlerLockLabelName" + location.toString();
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        int index = 0;
        PathOutcome so = new PathOutcome(new PsiUnit(beginContext));
        Utils.genConsStatments(body, env, so, tail_position);
//        for (Statement stmt : body) {
//            ++index;
//            so = stmt.genConsVisit(env, index == body.size() && tail_position);
//            PsiUnit normalUnit = so.getNormalPath();
//            if (normalUnit == null) {
//                break;
//            }
//            env.inContext = so.getNormalPath().c;
//            // env.prevContext.lambda = rightContext.lambda;
//        }
        return so;
    }

    public Type type() {
        return labeledType.type();
    }

    public String name() {
        return name;
    }

    protected Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        Map<String, compile.ast.Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.readMap(code));
        }
        return result;
    }

    protected Map<String,? extends compile.ast.Type> writeMap(CompileEnv code) {
        Map<String, compile.ast.Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}
