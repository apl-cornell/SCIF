package ast;

import compile.CompileEnv;
import compile.ast.Assign;
import compile.ast.Expression;
import compile.ast.IfStatement;
import compile.ast.SingleVar;
import compile.ast.StructType;
import compile.ast.VarDec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import typecheck.*;
import typecheck.exceptions.SemanticException;

public class ExceptHandler extends Node {

    private LabeledType labeledType;
    private String name;
    private Block body;
    final private boolean acceptall;

    public ExceptHandler(LabeledType type, String name, List<Statement> body) {
        this.labeledType = type;
        this.name = name;
        this.body = new Block(body);
        acceptall = false;
    }

    /**
     * Create a handler that accept all other types of exceptions(try/catch) or failures(atomic/rescue)
     */
    public ExceptHandler(List<Statement> body) {
        this.body = new Block(body);
        acceptall = true;
    }


    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();

        if (!acceptall) {
            VarSym var = env.newVarSym(name, labeledType, true, true, true, location, now);
            if (var == null) {
                throw new SemanticException("Exception type " + labeledType.type().name() + " not found",
                        location);
            }
            try {
                env.addSym(name, var);
            } catch (SymTab.AlreadyDefined e) {
                throw new SemanticException("Already have handler for " + name, location);
            }
        }

        body.genTypeConstraints(env, now);
        env.exitNewScope();
        return now;
    }

    public IfStatement solidityCodeGen(CompileEnv code, Map<String, compile.ast.Type> writeMap, SingleVar dataVar) {
        List<compile.ast.Statement> solBody = new ArrayList<>();
        if (acceptall) {
            solBody.addAll(body.solidityCodeGen(code));
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


        solBody.addAll(body.solidityCodeGen(code));

        IfStatement testException = code.testException(exceptionTypeSym, solBody);
        return testException;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(labeledType);
        rtn.add(body);
        return rtn;
    }

    public String getHandlerPcLabelName() {
        return scopeContext.getSHErrLocName() + "." + "handlerPcLabelName" + location.toString();
    }

    public String getHandlerLockLabelName() {
        return scopeContext.getSHErrLocName() + "." + "handlerLockLabelName" + location.toString();
    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        if (!acceptall) {
            declareCaughtVar(env, beginContext.pc);
        }
        int index = 0;
        PathOutcome so = body.IFCVisit(env, tail_position);
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

    /**
     * Declares this handler's variable in the current scope of {@code env}, bound by control
     * flow at {@code handlerPc}. If the variable's type has a label, that label is its label,
     * and the control flow must be trusted at it; otherwise its label is {@code handlerPc}.
     * An exception has a single label, so its fields are read at the variable's label.
     */
    private void declareCaughtVar(VisitEnv env, String handlerPc) throws SemanticException {
        Label declared = labeledType.label() == null ? null : env.toLabel(labeledType.label());
        VarSym var = new VarSym(name, exceptionType(env), declared, location, scopeContext,
                false, true, false);
        try {
            env.addVar(name, var);
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Already defined: " + name, location);
        }
        if (declared == null) {
            env.checkEqual(var.labelNameSLC(), handlerPc, location,
                    "The label of handler variable " + name + " may be wrong");
        } else {
            env.checkEqual(var.labelNameSLC(), declared.toSHErrLocFmt(), location,
                    "The label of handler variable " + name + " may be wrong");
            env.checkFlow(handlerPc, declared.toSHErrLocFmt(), location,
                    "Control flow in the handler must be trusted at the label of " + name);
        }
    }

    public String name() {
        return name;
    }

    /**
     * The exception type this handler catches, or null if it catches every failure.
     */
    public ExceptionTypeSym exceptionType(VisitEnv env) {
        return acceptall ? null : env.getExp(labeledType.type().name());
    }

    protected Map<String,? extends compile.ast.Type> readMap(CompileEnv code) {
        return body.readMap(code);
    }

    protected Map<String,? extends compile.ast.Type> writeMap(CompileEnv code) {
        return body.writeMap(code);
    }
}
