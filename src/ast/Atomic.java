package ast;

import compile.CompileEnv;
import compile.CompileEnv.ScopeType;
import compile.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import compile.ast.Assign;
import compile.ast.FunctionSig;
import compile.ast.Literal;
import compile.ast.Revert;
import compile.ast.Type;
import typecheck.*;
import typecheck.exceptions.SemanticException;

public class Atomic extends Statement {

    Block body;
    List<ExceptHandler> handlers;

    // ArrayList<Statement> orelse;
    // ArrayList<Statement> finalbody;
    public Atomic(List<Statement> body,
            List<ExceptHandler> handlers) {//, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        this.body = new Block(body);
        this.handlers = handlers;
        // this.orelse = orelse;
        // this.finalbody = finalbody;
    }


    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        env.enterAtomic();
        ScopeContext rtn = null;

        // ScopeContext tryclause = new ScopeContext(this, now);
        ScopeContext tmp;
        ScopeContext atomicScope = new ScopeContext(this, parent);
        atomicScope.clearExceptions();

//        for (ExceptHandler h : handlers) {
//            ExceptionTypeSym t = env.getExceptionTypeSym(h.type());
//            assert t != null: "exception type not found: " + h.type().name;
//            now.addException(t, false);
//        }

        body.genTypeConstraints(env, atomicScope);
        env.exitNewScope();
        env.exitAtomic();

        for (ExceptHandler h : handlers) {
            tmp = h.genTypeConstraints(env, parent);
        }
        return atomicScope;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {

        // convert the internal code into a "public" (potentially "external" for optimization purpose) method
        // unless the clause only contains one external call
        // all modified local variables need to be returned and assigned to the corresponding variables if terminated normally


//        if (body.size() == 1 && body.get(0) instanceof CallStatement) {
//            // if there is only one method call in the body
//            CallStatement callStatement = (CallStatement) body.get(0);
//            if (callStatement.allArgumentsAreValues()) {
//                // if all arguments are values
//                // statVar, dataVar = externalcall();
//                // if statVar != 0, got to handler
//                // else continue
//                List<compile.ast.Statement> result = new ArrayList<>();
//                SingleVar statVar = new SingleVar(code.newTempVarName());
//                SingleVar dataVar = new SingleVar(code.newTempVarName());
//                result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_UINT, statVar.name()));
//                result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BYTES, dataVar.name()));
//                SingleVar tempVar = null;
//                if (!funcSym.returnType.isVoid()) {
//                    tempVar = new SingleVar(code.newTempVarName());
//                    result.add(new VarDec(new PrimitiveType(funcSym.returnType.getName()), tempVar.name()));
//                }
//                result.add(new Assign(
//                        List.of(statVar, dataVar),
//                        callExp
//                ));
//            }
//        }

        List<compile.ast.Statement> solBody = new ArrayList<>();
        LowLevelCall lowLevelCall = null;
        boolean noException = false;

        Map<String, Type> readMap, writeMap;
        readMap = new HashMap<>();
        writeMap = new HashMap<>();

        List<Statement> stmts = body.statements();
        if (stmts.size() == 1 && stmts.get(0) instanceof CallStatement callStmt && callStmt.call.value instanceof Attribute) {
            // if it is a single call
            Call call = callStmt.call;
            List<compile.ast.Statement> result = callStmt.solidityCodeGen(code);
            if (result.size() == 1 && result.get(0) instanceof ExternalCall) {
                ExternalCall externalCall = (ExternalCall) result.get(0);
//                    LowLevelCall lowLevelCall = new LowLevelCall();

                FuncSym funcSym = call.funcSym;
                List<compile.ast.Expression> argValues = externalCall.getArgValues();
                noException = funcSym.exceptions.isEmpty();
                List<Argument> args = new ArrayList<>();

                for (int i = 0; i < argValues.size(); i++) {
                    TypeSym typeSym = funcSym.parameters.get(i).typeSym;
                    Type type = typeSym.getType();
                    args.add(new Argument(type, ""));
                }
                FunctionSig functionSig = new FunctionSig(externalCall.funcName(), args, new PrimitiveType(""), false, false);
                try {
                    SingleVar singleVar = new SingleVar("address(" + externalCall.getContractVar().toSolCode() + ")");
                    lowLevelCall = new LowLevelCall(functionSig, singleVar, externalCall.funcName(), argValues);
                } catch (Exception e) {
                    lowLevelCall = null;
                }
            }
        }

        if (lowLevelCall == null) {
            // find out read and written local variables
            readMap.putAll(body.readMap(code));
            writeMap.putAll(body.writeMap(code));
            readMap.putAll(writeMap);
            code.enterNewVarScope();
            Function newTempFunction = code.makeMethod(body.statements(), readMap, writeMap, ScopeType.ATOMIC);
            lowLevelCall = new LowLevelCall(newTempFunction, compile.Utils.THIS_ADDRESS,
                    newTempFunction.funcName(), newTempFunction.argNames().stream().map(name -> new SingleVar(name)).collect(
                    Collectors.toList()));
            code.exitVarScope();
            code.addTemporaryFunction(newTempFunction);
        }

        // generate an internal call: stat, data = newTempFunction(...);
        // UINT: stat
        // BYTES: data
        SingleVar succVar = new SingleVar(code.newTempVarName());
        SingleVar dataVar = new SingleVar(code.newTempVarName());
        SingleVar statVar = new SingleVar(code.newTempVarName());
        solBody.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BOOL, succVar.name()));
        solBody.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_UINT, statVar.name()));
        solBody.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BYTES, dataVar.name()));
        solBody.add(new Assign(
                List.of(succVar, dataVar),
                lowLevelCall
        ));

        List<compile.ast.Statement> ifNotRevertedBody;
        if (noException) {
            ifNotRevertedBody = new ArrayList<>();
        } else {
            ifNotRevertedBody = code.splitStatAndData(dataVar, statVar);
            // if should return method arguments
            IfStatement ifShouldReturn = new IfStatement(
                    new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar,
                            new Literal(compile.Utils.RETURNCODE_RETURN)),
                    code.genMethodReturn(dataVar)
            );

            IfStatement ifNormalEnd = new IfStatement(
                    new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar,
                            new Literal(compile.Utils.RETURNCODE_NORMAL)),
                    writeMap.isEmpty() ? new ArrayList<>() : List.of(new Assign(
                            writeMap.entrySet().stream().map(entry -> new SingleVar(entry.getKey()))
                                    .collect(
                                            Collectors.toList()),
                            code.decodeVars(writeMap, dataVar)
                    )),
                    List.of(ifShouldReturn)
            );
            ifNotRevertedBody.add(ifNormalEnd);
        }

        List<compile.ast.Statement> ifRevertedBody = new ArrayList<>(
                List.of(new Assign(statVar, new Literal(compile.Utils.RETURNCODE_FAILURE))));

        IfStatement handlingBranches;
        handlingBranches = null;

        Revert nohandler = new Revert();
        code.setCurrentStatVar(statVar);
        // test if there are any matching exceptions
        code.enterNewVarScope();
        for (ExceptHandler handler : handlers) {
            handlingBranches = handlingBranches == null ?
                    new IfStatement(handler.solidityCodeGen(code, writeMap, dataVar),
                            List.of(nohandler)) :
                    new IfStatement(handler.solidityCodeGen(code, writeMap, dataVar),
                            List.of(handlingBranches));
        }
        code.exitVarScope();
        if (handlingBranches == null) {
            ifRevertedBody.add(nohandler);
        } else {
            ifRevertedBody.add(handlingBranches);
        }

        IfStatement ifNotReverted = new IfStatement(
                succVar,
                ifNotRevertedBody,
                ifRevertedBody);
        solBody.add(ifNotReverted);

        return solBody;
    }

    @Override
    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(Utils.getLabelNamePc(toSHErrLocFmt()),
                Utils.getLabelNameLock(toSHErrLocFmt()));

        // add new exceptions to psi
        /*HashMap<ExceptionTypeSym, PsiUnit> oldPsi = env.psi;
        HashMap<ExceptionTypeSym, PsiUnit> newPsi = new HashMap<>();
        for (HashMap.Entry<ExceptionTypeSym, PsiUnit> p : oldPsi.entrySet()) {
            newPsi.put(p.getKey(), new PsiUnit(p.getValue().pc, p.getValue().lambda, p.getValue().inTx));
        }*/
        /*boolean inTx = this instanceof Extry;
        for (ExceptHandler h : handlers) {
            newPsi.put(env.toExceptionTypeSym(h.type), new PsiUnit(h.getHandlerPcLabelName(), h.getHandlerLockLabelName(), inTx));
            env.cons.add(new Constraint(new Inequality(h.getHandlerLockLabelName(), beginContext.lambda), env.hypothesis, location, env.curContractSym.name,
                    "Try clause should maintain locks when throwing exception " + h.name));
        }*/

        //env.psi = newPsi;
        PathOutcome psi = new PathOutcome();
        PathOutcome input = new PathOutcome();
        Map<String, String> versionsBefore = env.saveVersions();
        env.incScopeLayer();
        psi.join(body.IFCVisit(env, tail_position));
        env.decScopeLayer();
        for (ExceptHandler h : handlers) {
            ExceptionTypeSym expSym = h.exceptionType(env);
            PsiUnit u = psi.psi.get(expSym);

            if (u != null) {
                env.checkFlow(u.c.lambda, beginContext.lambda, location,
                        "The atomic block must maintain the reentrancy lock when it throws " + h.type().name());
            /*env.cons.add(new Constraint(new Inequality(u.c.pc, h.), env.hypothesis, location, env.curContractSym.name,
                    "Try clause should maintain locks when throwing exception " + h.name));*/

                input.set(expSym, u);
                // psi.set(expSym, (Context) null);
                psi.remove(expSym);
            }
        }
        //Context cTry = env.outContext;

        //env.psi = oldPsi;
        // A handler may be entered from any point in the body.
        List<Map<String, String>> exitVersions = new ArrayList<>();
        exitVersions.add(env.saveVersions());
        java.util.Set<String> assignedInBody = ast.Assign.assignedNames(body.statements());
        for (ExceptHandler h : handlers) {
            env.restoreVersions(versionsBefore);
            env.havoc(assignedInBody);
            env.incScopeLayer();
            //env.inContext = new Context(h.getHandlerPcLabelName(), beginContext.lambda);
            ExceptionTypeSym expSym = h.exceptionType(env);
            if (expSym == null) {
                // Nothing is known about where a failure came from, so a handler for
                // failures runs with untrusted control flow.
                env.inContext = new Context(env.curContractSym().any().toSHErrLocFmt(),
                        beginContext.lambda);
                PathOutcome ho = h.genConsVisit(env, tail_position);
                psi.join(ho);
                exitVersions.add(env.saveVersions());
            } else if (input.psi.get(expSym) instanceof PsiUnit expUnit) {
                env.inContext = new Context(expUnit.c.pc, beginContext.lambda);
                PathOutcome ho = h.genConsVisit(env, tail_position);
                psi.join(ho);
                exitVersions.add(env.saveVersions());
            }
            env.decScopeLayer();
            // cTry = new Context(Utils.makeJoin(cTry.pc, env.outContext.outPcName), Utils.makeJoin(cTry.lambda, env.outContext.lockName));
        }
        env.mergeVersions(exitVersions);

        Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);

        /*Utils.contextFlow(env, cTry, endContext, location);
        env.outContext = endContext;*/
        if (!tail_position) {
            env.checkFlow(endContext.lambda, beginContext.lambda, location,
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
        }

        return psi;
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            node.passScopeContext(scopeContext);
        }
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(body);
        rtn.addAll(handlers);
        return rtn;
    }

    @Override
    public boolean exceptionHandlingFree() {
        return false;
    }

    @Override
    protected Map<String,? extends Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>(body.readMap(code));
        for (ExceptHandler s: handlers) {
            result.putAll(s.readMap(code));
        }
        return result;
    }

    @Override
    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>(body.writeMap(code));
        for (ExceptHandler s: handlers) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}
