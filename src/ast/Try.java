package ast;

import compile.CompileEnv;
import compile.CompileEnv.ScopeType;
import compile.ast.Assign;
import compile.ast.BinaryExpression;
import compile.ast.Call;
import compile.ast.Function;
import compile.ast.IfStatement;
import compile.ast.Literal;
import compile.ast.Revert;
import compile.ast.SingleVar;
import compile.ast.Type;
import compile.ast.VarDec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;

public class Try extends Statement {

    List<Statement> body;
    List<ExceptHandler> handlers;

    // ArrayList<Statement> orelse;
    // ArrayList<Statement> finalbody;
    public Try(List<Statement> body,
            List<ExceptHandler> handlers) {//, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        this.body = body;
        this.handlers = handlers;
        // this.orelse = orelse;
        // this.finalbody = finalbody;
    }


    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // consider to be a new scope
        // must contain at least one Statement
        ScopeContext now = new ScopeContext(this, parent);
        env.enterNewScope();
        ScopeContext rtn = null;

        // ScopeContext tryclause = new ScopeContext(this, now);
        ScopeContext tmp;

        //TODO: inc scope layer
        for (ExceptHandler h : handlers) {
            ExceptionTypeSym t = env.getExceptionTypeSym(h.type());
            assert t != null: "Exception not found: " + h.type().name + " at " + location.errString();
            now.addException(t, true);
        }

        for (Statement s : body) {
            tmp = s.generateConstraints(env, now);
        }
        env.exitNewScope();

        for (ExceptHandler h : handlers) {
            tmp = h.generateConstraints(env, parent);
        }
        return now;
    }

    @Override
    public List<compile.ast.Statement> solidityCodeGen(CompileEnv code) {

        List<compile.ast.Statement> solBody = new ArrayList<>();

        Map<String, compile.ast.Type> readMap, writeMap;
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
        Function newTempFunction = code.makeMethod(body, readMap, writeMap, ScopeType.TRY);
        code.exitVarScope();
        code.addTemporaryFunction(newTempFunction);

        // generate an internal call: stat, data = newTempFunction(...);
        // UINT: stat
        // BYTES: data
        SingleVar statVar = new SingleVar(code.newTempVarName());
        SingleVar dataVar = new SingleVar(code.newTempVarName());
        solBody.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_UINT, statVar.name()));
        solBody.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BYTES, dataVar.name()));
        solBody.add(new Assign(
                List.of(statVar, dataVar),
                new Call(newTempFunction.funcName(), newTempFunction.argNames().stream().map(name -> new SingleVar(name)).collect(
                        Collectors.toList()))
        ));

        IfStatement handlingBranches;

        // if should return method arguments
        IfStatement ifShouldReturn = new IfStatement(
                new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar, new Literal(compile.Utils.RETURNCODE_RETURN)),
                code.genMethodReturn(dataVar),
                List.of(new Revert(new Literal("Unexpected Exception")))
                );

        List<compile.ast.Statement> elseBody = new ArrayList<>();

        IfStatement ifNormalEnd = new IfStatement(
                new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL, statVar, new Literal(compile.Utils.RETURNCODE_NORMAL)),
                writeMap.isEmpty() ? new ArrayList<>() : List.of(new Assign(
                        writeMap.entrySet().stream().map(entry -> new SingleVar(entry.getKey())).collect(
                                Collectors.toList()),
                        code.decodeVars(writeMap, dataVar)
                        )),
                List.of(ifShouldReturn)
                );

        handlingBranches = ifNormalEnd;
        code.setCurrentStatVar(statVar);
        code.enterNewVarScope();
        // test if there are any matching exceptions
        for (ExceptHandler handler : handlers) {
            handlingBranches = new IfStatement((IfStatement) handler.solidityCodeGen(code, writeMap, dataVar), List.of(handlingBranches));
        }
        code.exitVarScope();

        solBody.add(handlingBranches);
        return solBody;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

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
        Utils.genConsStmtsWithException(body, env, so, psi, false);
//        for (Statement s : body) {
//            so = s.genConsVisit(env, false);
//            psi.joinExe(so);
//            // env.inContext = new Context(so.getNormalPath().c.pc, beginContext.lambda);
//            env.inContext = new Context(so.getNormalPath().c);
//
//        }
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
                env.inContext = Utils.genNewContextAndConstraints(env, false, expUnit.c, beginContext.lambda, h.nextPcSHL(), h.location);
//                env.inContext = new Context(expUnit.c.pc, beginContext.lambda);
                PathOutcome ho = h.genConsVisit(env, tail_position);
                psi.join(ho);
            }
            env.decScopeLayer();
            // cTry = new Context(Utils.makeJoin(cTry.pc, env.outContext.outPcName), Utils.makeJoin(cTry.lambda, env.outContext.lockName));
        }

        typecheck.Utils.contextFlow(env, psi.getNormalPath().c, endContext, location);

        /*Utils.contextFlow(env, cTry, endContext, location);
        env.outContext = endContext;*/
        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
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
        ArrayList<Node> rtn = new ArrayList<>();
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
