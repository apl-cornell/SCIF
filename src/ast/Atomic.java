package ast;

import compile.CompileEnv;
import compile.CompileEnv.ScopeType;
import compile.ast.Assign;
import compile.ast.BinaryExpression;
import compile.ast.Call;
import compile.ast.Function;
import compile.ast.IfStatement;
import compile.ast.Literal;
import compile.ast.LowLevelCall;
import compile.ast.PrimitiveType;
import compile.ast.Revert;
import compile.ast.SingleVar;
import compile.ast.Type;
import compile.ast.VarDec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import typecheck.Context;
import typecheck.ExceptionTypeSym;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.PsiUnit;
import typecheck.ScopeContext;
import typecheck.Utils;
import typecheck.VisitEnv;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;

public class Atomic extends Statement {

    List<Statement> body;
    List<ExceptHandler> handlers;

    // ArrayList<Statement> orelse;
    // ArrayList<Statement> finalbody;
    public Atomic(List<Statement> body,
            List<ExceptHandler> handlers) {//, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        this.body = body;
        this.handlers = handlers;
        // this.orelse = orelse;
        // this.finalbody = finalbody;
    }


    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        env.enterAtomic();
        ScopeContext rtn = null;

        // ScopeContext tryclause = new ScopeContext(this, now);
        ScopeContext tmp;

//        for (ExceptHandler h : handlers) {
//            ExceptionTypeSym t = env.getExceptionTypeSym(h.type());
//            assert t != null: "exception type not found: " + h.type().name;
//            now.addException(t, false);
//        }

        for (Statement s : body) {
            assert !now.getSHErrLocName().startsWith("null");
            tmp = s.ntcGenCons(env, now);
        }
        env.exitNewScope();
        env.exitAtomic();

        for (ExceptHandler h : handlers) {
            tmp = h.ntcGenCons(env, parent);
        }
        return now;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {
//
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

        // convert the internal code into a "public" (potentially "external" for optimization purpose) method
        // unless the clause only contains one external call
        // all modified local variables need to be returned and assigned to the corresponding variables if terminated normally



        List<compile.ast.Statement> solBody = new ArrayList<>();

        Map<String, Type> readMap, writeMap;
        readMap = new HashMap<>();
        writeMap = new HashMap<>();

        // find out read and written local variables
        for (Statement s : body) {
            readMap.putAll(s.readMap(code));
            writeMap.putAll(s.writeMap(code));
            // solBody.addAll(s.solidityCodeGen(code));
        }
        readMap.putAll(writeMap);
        code.enterNewVarScope();
        Function newTempFunction = code.makeMethod(body, readMap, writeMap, ScopeType.ATOMIC);
        LowLevelCall externalCall = new LowLevelCall(newTempFunction, compile.Utils.THIS_ADDRESS,
                newTempFunction.funcName(), newTempFunction.argNames().stream().map(name -> new SingleVar(name)).collect(
                Collectors.toList()));
        code.exitVarScope();
        code.addTemporaryFunction(newTempFunction);

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
                externalCall
        ));

        List<compile.ast.Statement> ifNotRevertedBody = code.splitStatAndData(dataVar, statVar);
        // if should return method arguments
        IfStatement ifShouldReturn = new IfStatement(
                new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar, new Literal(compile.Utils.RETURNCODE_RETURN)),
                code.genMethodReturn(dataVar)
        );

        IfStatement ifNormalEnd = new IfStatement(
                new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar, new Literal(compile.Utils.RETURNCODE_NORMAL)),
                writeMap.isEmpty() ? new ArrayList<>() : List.of(new Assign(
                        writeMap.entrySet().stream().map(entry -> new SingleVar(entry.getKey())).collect(
                                Collectors.toList()),
                        code.decodeVars(writeMap, dataVar)
                )),
                List.of(ifShouldReturn)
        );
        ifNotRevertedBody.add(ifNormalEnd);
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
                    new IfStatement(handler.solidityCodeGen(code, writeMap, dataVar), List.of(nohandler)) :
                    new IfStatement(handler.solidityCodeGen(code, writeMap, dataVar), List.of(handlingBranches));
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
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
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
        env.incScopeLayer();
        PathOutcome so = new PathOutcome(new PsiUnit(beginContext));
        Utils.genConsStatmentsWithException(body, env, so, psi, tail_position);
        env.decScopeLayer();
        for (ExceptHandler h : handlers) {
            ExceptionTypeSym expSym = env.getExp(h.name());
            PsiUnit u = psi.psi.get(expSym);

            if (u != null) {
                env.cons.add(
                        new Constraint(new Inequality(u.c.lambda, beginContext.lambda), env.hypothesis(),
                                location, env.curContractSym().getName(),
                                "Try clause should maintain locks when throwing exception " + h.name()));
            /*env.cons.add(new Constraint(new Inequality(u.c.pc, h.), env.hypothesis, location, env.curContractSym.name,
                    "Try clause should maintain locks when throwing exception " + h.name));*/

                input.set(expSym, u);
                // psi.set(expSym, (Context) null);
                psi.remove(expSym);
            }
        }
        //Context cTry = env.outContext;

        //env.psi = oldPsi;
        for (ExceptHandler h : handlers) {
            env.incScopeLayer();
            //env.inContext = new Context(h.getHandlerPcLabelName(), beginContext.lambda);
            ExceptionTypeSym expSym = env.getExp(h.name());
            PsiUnit expUnit = input.psi.get(expSym);
            if (expUnit != null) {
                env.inContext = new Context(expUnit.c.pc, beginContext.lambda);
                PathOutcome ho = h.genConsVisit(env, tail_position);
                psi.join(ho);
            }
            env.decScopeLayer();
            // cTry = new Context(Utils.makeJoin(cTry.pc, env.outContext.outPcName), Utils.makeJoin(cTry.lambda, env.outContext.lockName));
        }

        Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);

        /*Utils.contextFlow(env, cTry, endContext, location);
        env.outContext = endContext;*/
        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
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
        rtn.addAll(body);
        rtn.addAll(handlers);
        return rtn;
    }

    @Override
    public boolean exceptionHandlingFree() {
        return false;
    }

    @Override
    protected Map<String,? extends Type> readMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.readMap(code));
        }
        for (ExceptHandler s: handlers) {
            result.putAll(s.readMap(code));
        }
        return result;
    }

    @Override
    protected Map<String,? extends Type> writeMap(CompileEnv code) {
        Map<String, Type> result = new HashMap<>();
        for (Statement s: body) {
            result.putAll(s.writeMap(code));
        }
        for (ExceptHandler s: handlers) {
            result.putAll(s.writeMap(code));
        }
        return result;
    }
}
