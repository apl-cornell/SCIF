package compile;

import static compile.Utils.decodeCall;
import static compile.Utils.encodeCall;

import ast.*;
import compile.ast.Argument;
import compile.ast.BinaryExpression;
import compile.ast.Function;
import compile.ast.IfStatement;
import compile.ast.SingleVar;
import compile.ast.VarDec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import typecheck.EventTypeSym;
import typecheck.ExceptionTypeSym;
import typecheck.Utils;

public class CompileEnv {
    private Map<String, String> labelTable; // map label names to on-chain addresses
    public typecheck.DynamicSystemOption dynamicSystemOption;

    Map<String, compile.ast.Type> globalvars;
    Stack<Map<String, compile.ast.Type>> localvars;
    ExceptionManager exceptionManager;
    TemporaryNameManager temporaryNameManager;
    Map<String, ExceptionTypeSym> exceptionSymTable;
    Map<String, EventTypeSym> eventSymTable;
    List<Function> temporaryFunctions;
    Stats stats = new Stats();


    /**
        Wrap up statements in body as a method.
        If scope is TRY, create an internal method and return a call to it;
        If scope is ATOMIC, create a public method and return a low level call to it.
     */
    public Function makeMethod(List<Statement> body, Map<String, compile.ast.Type> readMap,
            Map<String, compile.ast.Type> writeMap, ScopeType scope) {
        assert scope == ScopeType.TRY || scope == ScopeType.ATOMIC;
        pushScope(scope);
        // make try block a call
        setWriteMap(writeMap);

        List<compile.ast.Statement> funcBody = new ArrayList<>();
        for (Statement s: body) {
            funcBody.addAll(s.solidityCodeGen(this));
        }
        funcBody.add(new compile.ast.Return(
                List.of(new compile.ast.Literal("0"), encodeVars())
        ));
        popScope();

        String methodName = newTempVarName();
        Function newMethod = null;
        if (scope == ScopeType.TRY) {
            newMethod =
                    new Function(methodName,
                            readMap.entrySet().stream().map(entry -> new Argument(entry.getValue(), entry.getKey())).collect(
                                    Collectors.toList()),
                            compile.Utils.UNIVERSAL_RETURN_TYPE,
                            false,
                            false,
                            funcBody
                            );
        } else if (scope == ScopeType.ATOMIC) {
            funcBody.add(0, new compile.ast.Assert(new compile.ast.Literal("address(this) == msg.sender")));
            newMethod =
                    new Function(methodName,
                            readMap.entrySet().stream().map(entry -> new Argument(entry.getValue(), entry.getKey())).collect(
                                    Collectors.toList()),
                            compile.Utils.UNIVERSAL_RETURN_TYPE,
                            true,
                            false,
                            funcBody
                    );
        }
        assert newMethod != null;
        return newMethod;
    }

    private void setWriteMap(Map<String,compile.ast.Type> writeMap) {
        this.writeMap = writeMap;
    }

    public void addTemporaryFunction(Function newTempFunction) {
        temporaryFunctions.add(newTempFunction);
    }

    public List<compile.ast.Statement> genMethodReturn(SingleVar dataVar) {
        if (currentScope() != ScopeType.METHOD) {
            return List.of(
                    new compile.ast.Return(List.of(new compile.ast.Literal(compile.Utils.RETURNCODE_RETURN), dataVar))
            );
        } else {
            if (currentMethod.exceptionFree()) {
                if (currentMethod.returnVoid()) {
                    return List.of(new compile.ast.Return());
                } else {
                    compile.ast.Expression decodeData = decodeVars(
                            List.of(currentReturnType),
                            dataVar
                    );
                    return List.of(new compile.ast.Return(
                            decodeData
                    ));
                }

            } else {
                return List.of(
                        new compile.ast.Return(
                                List.of(new compile.ast.Literal(compile.Utils.RETURNCODE_NORMAL),
                                        dataVar))
                );
            }
        }
    }

    public void initLocalVars() {
        localvars.clear();;
    }

    public void setExceptionMap(Map<String, ExceptionTypeSym> exceptionTypeSymMap) {
        this.exceptionSymTable = exceptionTypeSymMap;
    }

    public void setEventMap(Map<String, EventTypeSym> eventTypeSymMap) {
        this.eventSymTable = eventTypeSymMap;
    }

    public Collection<? extends Function> tempFunctions() {
        return temporaryFunctions;
    }

    public void clearTempFunctions() {
        temporaryFunctions.clear();
    }

    public List<compile.ast.Statement> splitStatAndData(SingleVar dataVar, SingleVar statVar) {
        SingleVar dataVar1 = new SingleVar(newTempVarName());
        SingleVar dataVar2 = new SingleVar(newTempVarName());
        SingleVar data2LenVar = new SingleVar(newTempVarName());
        List<compile.ast.Statement> result = new ArrayList<>();
        result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BYTES, dataVar1.name(),
                new compile.ast.Literal("new bytes(32)")));
        result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_UINT, data2LenVar.name(),
                new BinaryExpression(compile.Utils.toBinOp(BinaryOperator.Sub),
                        new compile.ast.Literal(dataVar.name() + ".length"), new compile.ast.Literal("32"))));
        result.add(new VarDec(compile.Utils.PRIMITIVE_TYPE_BYTES, dataVar2.name(),
                new compile.ast.Literal("new bytes(" + data2LenVar.name() + ")")));
        compile.ast.For parseData1 = new compile.ast.For(
                "uint $i = 0",
                new BinaryExpression(compile.Utils.toCompareOp(CompareOperator.Lt), new SingleVar("$i"), new compile.ast.Literal("32")),
                "++$i",
                List.of(new compile.ast.Assign(new compile.ast.Subscript(dataVar1, new SingleVar("$i")),
                        new compile.ast.Subscript(dataVar, new SingleVar("$i"))))
        );
        compile.ast.For parseData2 = new compile.ast.For(
                "uint $i = 32",
                new BinaryExpression(compile.Utils.toCompareOp(CompareOperator.Lt), new SingleVar("$i"), new compile.ast.Literal(dataVar.name() + ".length")),
                "++$i",
                List.of(new compile.ast.Assign(new compile.ast.Subscript(dataVar2,
                                                    new BinaryExpression(compile.Utils.toBinOp(BinaryOperator.Sub), new SingleVar("$i"), new compile.ast.Literal("32"))),
                        new compile.ast.Subscript(dataVar, new SingleVar("$i"))))
        );
        result.add(parseData1);
        result.add(new compile.ast.Assign(statVar, new compile.ast.Literal(decodeCall(dataVar1.name(), "(uint)"))));
        result.add(parseData2);
        result.add(new compile.ast.Assign(dataVar, dataVar2));
        return result;
    }

    public void clearExceptionManager() {
        exceptionManager.clear();
    }

    public void countEndorse() {
        stats.endorse();
    }
    public void countEndorse(int size) {
        stats.endorse(size);
    }

    public int endorseCount() {
        return stats.endorseCount();
    }

    public int dynamicCallCount() {
        return stats.dynamicCallCount();
    }

    public enum ScopeType {
        CONTRACT, METHOD, TRY, ATOMIC
    }

    Stack<ScopeType> scopeStack; // whether it is generating code for a SCIF method
    FunctionDef currentMethod;
    compile.ast.Type currentReturnType;
    Map<String, compile.ast.Type> writeMap;

    public void setCurrentStatVar(SingleVar currentStatVar) {
        this.currentStatVar = currentStatVar;
    }

    SingleVar currentStatVar;

    public void setCurrentMethod(FunctionDef method, compile.ast.Type type) {
        currentMethod = method;
        currentReturnType = type;
    }
    public ScopeType currentScope() {
        return scopeStack.peek();
    }
    public void pushScope(ScopeType newScope) {
        scopeStack.push(newScope);
    }
    public void popScope() {
        scopeStack.pop();
    }

    public CompileEnv() {
        // code = new ArrayList<>();
        labelTable = new HashMap<>();
        labelTable.put("this", "address(this)");
        // labelTable.put("sender", "msg.sender");
        globalvars = new HashMap<>();
        localvars = new Stack<>();
        scopeStack = new Stack<>();
        // indentWidth = 4;
        // indentLevel = 0;
//        unitIndent = "";
//        for (int i = 0; i < indentWidth; ++i) {
//            unitIndent += " ";
//        }
        // currentIndent = "";
        exceptionManager = new ExceptionManager();
        temporaryNameManager = new TemporaryNameManager();
        temporaryFunctions = new ArrayList<>();
    }

    /**
     * return a dynamic check on whether l trusts r, assuming l and r are both primitive addresses.
     */
    compile.ast.Expression ifTrust(String l, String r) {
        if (l.equals(r)) {
            return new compile.ast.Literal("true");
            // return "true";
        }
        if (r.equals(Utils.LABEL_BOTTOM)) {
            return new compile.ast.Literal("false");
        }

        List<compile.ast.Expression> argValues = new ArrayList<>();
        argValues.add(new SingleVar(labelTable.containsKey(l) ? labelTable.get(l) : l));
        argValues.add(new SingleVar(labelTable.containsKey(r) ? labelTable.get(r) : r));
        stats.trusts();
        return new compile.ast.Call(compile.Utils.TRUSTS_CALL, argValues);
//            return new compile.ast.ExternalCall(new compile.ast.Call("getTrustManager"),
//                        "trusts", argValues
//                        );
            // return "().trusts(" + l + ", " + r + ")";

    }
    compile.ast.Call ifUnlocked(String l) {
        stats.bypassLocks();
        return new compile.ast.Call(compile.Utils.BYPASSLOCK_CALL, List.of(new SingleVar(labelTable.containsKey(l) ? labelTable.get(l) : l)));
//        return new ExternalCall(new compile.ast.Call("getLockManager"),
//                "locked",
//                List.of(new SingleVar(l))
//        );
        // return "getLockManager().locked(" + l + ")";
    }

    public static String toIterations(List<String> iters) {
        return String.join(", ", iters);
    }

    public static String genVarDef(String type, String name, boolean isFinal) {
        return type + " " + (isFinal ? "constant" : "") + name;
    }

    public static String genVarDef(String type, String name, boolean isFinal, String value) {
        return type + " " + (isFinal ? "constant" : "") + name + " = " + value;
    }

    public static String genAssign(String target, String value) {
        return target + " = " + value;
    }

    public static String genInterfaceType(Type type) {
        return (type.isPrimitive())? type.toSolCode() : type.toSolCode() + " memory";
    }



    public CodeLine codeAssign(String target, String value) {
        return new CodeLine(genAssign(target, value) + ";");
    }

    public CodeToken codeBreak() {
        return new CodeLine("break;");
    }

    public String codeReturn(String value) {
        return ("return " + value + ";");
    }

//
//    public void enterFunctionDef(String name, String args, String returnType, boolean isPublic,
//            boolean isPayable) {
//        addFunctionHeader(name, args, returnType, isPublic, isPayable);
//        addLine("{");
//        addIndent();
//        addFunctionBulitInVarDefs();
//    }
//
//    private void addFunctionBulitInVarDefs() {
//        addLine("uint " + Utils.RESULT_VAR_NAME + " = 0;");
//        addLine("uint " + Utils.EXCEPTION_RECORDER_NAME + " = 0;");
//        addLine("bytes " + Utils.EXCEPTION_RECORDER_DATA_NAME + ";");
//    }
//
//    public void addFunctionSig(String name, String args, String returnType, boolean isPublic, boolean isPayable) {
//        addFunctionHeader(name, args, returnType, isPublic, isPayable);
//        addLine(";");
//    }
//
//    void addFunctionHeader(String name, String args, String returnType, boolean isPublic, boolean isPayable) {
//        addLine("function " + name + "(" + args + ")");
//        addIndent();
//        if (isPublic) {
//            addLine("external");
//        } else {
//            addLine("internal");
//        }
//        if (isPayable) {
//            addLine(Utils.PAYABLE_DECORATOR);
//        }
//        if (!returnType.isEmpty()) {
//            addLine("returns (" + returnType + ")");
//        }
//        decIndent();
//    }
//
//    public void enterConstructorDef(String args, List<Statement> body) {
//        addLine(genConstructorSig(args) + " {");
//        addIndent();
//    }
//
//    public void addConstructorSig(String args) {
//        addLine(genConstructorSig(args) + ";");
//    }
//
//    static String genConstructorSig(String args) {
//        return "constructor (" + args + ")";
//    }
//
//    public void leaveFunctionDef() {
//        decIndent();
//        addLine("}");
//    }
//
//    public void enterIf(String cond) {
//        addLine("if (" + cond + ") {");
//        addIndent();
//    }
//
//    public void leaveIf() {
//        decIndent();
//        addLine("}");
//    }
//
//    public void enterElse() {
//        addLine("else {");
//        addIndent();
//    }
//
//    public void leaveElse() {
//        decIndent();
//        addLine("}");
//    }
//
//    public void enterWhile(String cond) {
//        addLine("while (" + cond + ") {");
//        addIndent();
//    }
//
//    public void leaveWhile() {
//        decIndent();
//        addLine("}");
//    }
//
//    public void enterFor(String newVars, String test, String iters) {
//        addLine("for (" + newVars + "; " + test + "; " + iters + ") {");
//        addIndent();
//    }
//
//    public void leaveFor() {
//        decIndent();
//        addLine("}");
//    }
//
//    public void addVarDef(String type, String name, boolean isConst, String value) {
//        addLine(type + " " + name + " = " + value + ";");
//    }
//
//    public void addVarDef(String type, String name, boolean isConst) {
//        addLine(type + " " + name + ";");
//    }

    public static String toAttribute(String value, String attr) {
        return value + "." + attr;
    }
//
//    public static String toBinOp(String left, String op, String right) {
//        return "(" + left + " " + op + " " + right + ")";
//    }
//
//    public static String toBoolOp(String left, String op, String right) {
//        return "(" + left + " " + op + " " + right + ")";
//    }
//
//    public static String toCompareOp(String left, String op, String right) {
//        return "(" + left + " " + op + " " + right + ")";
//    }
//
//    public static String toFunctionCall(String name, String args) {
//        return name + "(" + args + ")";
//    }
//
//    public void output(File outputFile) {
//        try {
//            BufferedWriter codeFile = new BufferedWriter(new FileWriter(outputFile));
//            for (String line : code) {
//                codeFile.write(line + "\n");
//            }
//            codeFile.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void display() {
//        for (String line : code) {
//            System.out.println(line);
//        }
//    }

    /**
     * Generate proper dynamic checks for the trustworthiness for the caller and dynamic locks
     * @param funcLabels method labels
     * @param args arguments
     * @return generated Solidity statements
     */
    public List<compile.ast.Statement> enterFuncCheck(FuncLabels funcLabels, Arguments args) {
        /*
            f{pc_f->pc_t, lambda}(x_i{l_i}) from sender
            assert for any l in lock set, pc_f => pc_t join l
            TODO: check the correctness to simplify it to pc_f => l in L
            assert sender => pc, l_i
            TODO: add constaints to limit every arguments' label to be no more trusted than sender/pc
         */
        // TODO: make labels unique
        List<compile.ast.Statement> result = new ArrayList<>();

        if (!funcLabels.begin_pc.equals(funcLabels.to_pc)) {
            result.add(new compile.ast.Assert(checkIfUnlocked(funcLabels.begin_pc, funcLabels.to_pc)));
            // addLine(assertExp("!" +  + ";");
        }
        result.add(new compile.ast.Assert(checkIfTrustSender(funcLabels.begin_pc)));
        // addLine(assertExp(checkIfTrustSender(funcLabels.begin_pc)) + ";");
        return result;

    }

    private compile.ast.Expression checkIfTrustSender(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();
            if (name.equals(typecheck.Utils.LABEL_BOTTOM) || name.equals(Utils.LABEL_SENDER)) {
                return new compile.ast.Literal("true");
//                return "true"; //TODO
            }
            String addr = labelTable.containsKey(name) ? labelTable.get(name) : ((PrimitiveIfLabel) l).value().id;
            // return "ifTrust(" + addr + ", msg.sender)";
            return ifTrust(addr, "msg.sender");
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).getOp() == IfOperator.JOIN) {
            return new BinaryExpression("||",
                    checkIfTrustSender(((ComplexIfLabel) l).getLeft()),
                    checkIfTrustSender(((ComplexIfLabel) l).getRight()));
//            return "(" + checkIfTrustSender(((ComplexIfLabel) l).getLeft()) + " || "
//                    + checkIfTrustSender(((ComplexIfLabel) l).getRight()) + ")";
        } else {
            // TODO: error report;
            assert false;
            return null;
        }
    }

    private compile.ast.Expression checkIfUnlocked(IfLabel l_1, IfLabel l_2) {
        // check if l_1 => l_2 join l
        // (l_a => l_c or l_b => l_c) implies (l_a ⨅ l_b => l_c) not necessary
        // (l_a => l_c and l_b => l_c) implies (l_a ⨆ l_b => l_c) necessary
        // (l_a => l_b and l_a => l_c) implies (l_a => l_b ⨅ l_c) necessary
        // (l_a => l_b or l_a => l_c) implies (l_a => l_b ⨆ l_c) not necessary
        String name_1, name_2;
        if (!(l_2 instanceof PrimitiveIfLabel)) {
            //TODO: error report
            assert false;
            return null;
        } else {
            name_2 = ((PrimitiveIfLabel) l_2).toString();
        }
        if (l_1 instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            name_1 = ((PrimitiveIfLabel) l_1).toString();

            if (name_1.equals(name_2)) {
                return new compile.ast.Literal("false");
                // return "false";
            }
            String addr_1 = labelTable.containsKey(name_1) ? labelTable.get(name_1) : ((PrimitiveIfLabel) l_1).value().id;
            String addr_2 = labelTable.containsKey(name_2) ? labelTable.get(name_2) : ((PrimitiveIfLabel) l_2).value().id;

            return new BinaryExpression("||", ifUnlocked(addr_2), ifTrust(addr_2, addr_1));
            // return "(" + ifLocked(addr_1) + " && !" + ifTrust(addr_2, addr_1) + ")";// + name;
        } else if (l_1 instanceof ComplexIfLabel
                && ((ComplexIfLabel) l_1).getOp() == IfOperator.JOIN) {
            return new BinaryExpression("&&",
                    checkIfUnlocked(((ComplexIfLabel) l_1).getLeft(), l_2),
                    checkIfUnlocked(((ComplexIfLabel) l_1).getRight(), l_2));
//            return "(" + checkIfLocked(((ComplexIfLabel) l_1).getLeft(), l_2) + " && "
//                    + checkIfLocked(
//                    ((ComplexIfLabel) l_1).getRight(), l_2) + ")";
        } else {
            // TODO: error report;
            assert false;
            return null;
        }
    }

    public List<compile.ast.Statement> putDynamicLock(IfLabel l, List<compile.ast.Statement> body) {
        /*
            lock(l)
         */
        List<compile.ast.Statement> result = new ArrayList<>();
        result.add(new compile.ast.Assert(lock(l)));
        // addLine(assertExp(lock(l)) + ";");
        result.addAll(body);
        result.add(new compile.ast.Assert(unlock(l)));
        return result;
    }

    private compile.ast.Expression lock(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).value().id;
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) {
                return new compile.ast.Literal("false");
//                return "false";//TODO: should make a nop statement
            }
            String addr = labelTable.containsKey(name) ? labelTable.get(name) : ((PrimitiveIfLabel) l).value().id;
            return lock(addr);
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).getOp() == IfOperator.MEET) {
            return new BinaryExpression("&&",
                    lock(((ComplexIfLabel) l).getLeft()),
                    lock(((ComplexIfLabel) l).getRight()));
//            return "(" + lock(((ComplexIfLabel) l).getLeft()) + " && " + lock(
//                    ((ComplexIfLabel) l).getRight())
//                    + ")";
        } else {
            // TODO: error report;
            assert false;
            return null;
        }
    }

    public compile.ast.Call lock(String addr) {
        stats.acquireLock();
        return new compile.ast.Call(compile.Utils.LOCK_CALL,
                List.of(new SingleVar(labelTable.containsKey(addr) ? labelTable.get(addr) : addr)));
//        return new ExternalCall(new compile.ast.Call("getLockManager"),
//                "addlock", Arrays.asList(new SingleVar("addr"))
//                );
//        return "getLockManager().addlock(" + addr + ")";
    }

    public compile.ast.Call unlock(String addr) {
        stats.releaseLock();
        return new compile.ast.Call(compile.Utils.UNLOCK_CALL,
                List.of(new SingleVar(labelTable.containsKey(addr) ? labelTable.get(addr) : addr)));
//        return new ExternalCall(new compile.ast.Call("getLockManager"),
//                "unlock", Arrays.asList(new SingleVar("addr"))
//                );
        // return "getLockManager().unlock(" + addr + ")";
    }

    private compile.ast.Expression unlock(IfLabel l) {
        if (l instanceof PrimitiveIfLabel) {
            // TODO: error report when missing this entry
            String name = ((PrimitiveIfLabel) l).toString();
            if (name.equals(typecheck.Utils.LABEL_BOTTOM)) {
                return new compile.ast.Literal("false");
                // return "false";//TODO: should make a nop statement
            }
            String addr = labelTable.containsKey(name) ? labelTable.get(name) : ((PrimitiveIfLabel) l).value().id;
            return unlock(addr);
        } else if (l instanceof ComplexIfLabel && ((ComplexIfLabel) l).getOp() == IfOperator.MEET) {
            return new BinaryExpression("&&",
                    unlock(((ComplexIfLabel) l).getLeft()),
                    unlock(((ComplexIfLabel) l).getRight()));
//            return "(" + unlock(((ComplexIfLabel) l).getLeft()) + " && " + unlock(
//                    ((ComplexIfLabel) l).getRight()) + ")";
        } else {
            // TODO: error report;
            assert false;
            return null;
        }
    }

    public void setDynamicOption(TrustSetting trustSetting) {
        dynamicSystemOption = trustSetting.getDynamicSystemOption();
        labelTable = trustSetting.getLabelTable();
    }

    public boolean exceptionFree() {
        return exceptionManager.exceptionFree();
    }

    public IfStatement testNormalPath(List<compile.ast.Statement> body) {
        return new IfStatement(
                new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL,
                        currentStatVar,
                        new compile.ast.Literal("0")),
                body
        );
//        addLine("if (" + typecheck.Utils.EXCEPTION_RECORDER_NAME + " == 0 ) {");
//        addIndent();
    }
    public IfStatement testNonNormalPath(List<compile.ast.Statement> body) {
        return new IfStatement(
                new BinaryExpression(compile.Utils.SOL_BOOL_NONEQUAL,
                        currentStatVar,
                        new compile.ast.Literal("0")),
                body
        );
//        addLine("if (" + typecheck.Utils.EXCEPTION_RECORDER_NAME + " == 0 ) {");
//        addIndent();
    }

    public IfStatement testException(ExceptionTypeSym exceptionTypeSym, List<compile.ast.Statement> body) {
        int exceptionId = getExceptionId(exceptionTypeSym);
        return new IfStatement(
                new BinaryExpression(compile.Utils.SOL_BOOL_EQUAL,
                        currentStatVar,
                        new compile.ast.Literal(Integer.toString(exceptionId))),
                body
        );
    }

    public int getExceptionId(ExceptionTypeSym exceptionTypeSym) {
        return exceptionManager.id(exceptionTypeSym);
    }

//    public void removeExceptionFree() {
//        exceptionManager.removeExceptionFree();
//    }

//    public void setExceptionFree() {
//        exceptionManager.setExceptionFree();
//    }

    /*
        Given the type of the exception exceptionTypeSym,
        parse its argument from bytes byteName to struct
     */


    public compile.ast.Expression decodeVarsAndException(ExceptionTypeSym exceptionTypeSym, Map<String, compile.ast.Type> writeMap, compile.ast.Expression data) {
        if (exceptionTypeSym.parameters().size() == 0) {
            return decodeVars(writeMap, data);
        }

        if (writeMap.isEmpty()) {
        /*for (VarSym varSym : exceptionTypeSym.parameters()) {
            // varSym.typeSym.name varSym.name
            names.add(name + "." + varSym.getName());
        }*/
            //
            return new compile.ast.Literal(decodeCall(data.toSolCode(), "(" + exceptionTypeSym.getName() + ")"));
        } else {
            String varsString = "(" + writeMap.values().stream().map(
                compile.ast.Type::solCode).collect(
                Collectors.joining(", ")) + ", " + exceptionTypeSym.getName() + ")";
            return new compile.ast.Literal(decodeCall(data.toSolCode(), varsString));

        }
    }

    public static compile.ast.Literal encodeException(ExceptionTypeSym exceptionTypeSym, List<compile.ast.Expression> args) {
        if (args.size() == 0) {
            return new compile.ast.Literal("\"\"");
        }
        String exceptionString = exceptionTypeSym.getName() + "(" +
                String.join(", ", args.stream().map(arg -> arg.toSolCode()).collect(Collectors.toList())) + ")";
        return new compile.ast.Literal(encodeCall(exceptionString));
    }
    public compile.ast.Literal encodeVarsAndException(ExceptionTypeSym exceptionTypeSym, List<compile.ast.Expression> args) {
        if (writeMap.isEmpty()) {
            return encodeException(exceptionTypeSym, args);
        }
        String varsString = String.join(", ", writeMap.entrySet().stream().map(entry -> entry.getKey()).collect(
                Collectors.toList()));
        if (args.size() == 0) {
            return new compile.ast.Literal(encodeCall(varsString));
        } else {
            String exceptionString = exceptionTypeSym.getName() + "(" +
                    String.join(", ", args.stream().map(arg -> arg.toSolCode()).collect(Collectors.toList()));
            return new compile.ast.Literal(encodeCall(varsString + ", " + exceptionString));
        }
    }
    public compile.ast.Literal encodeVars() {
        if (writeMap.isEmpty()) {
            return new compile.ast.Literal("\"\"");
        }
        String varsString = String.join(", ", writeMap.entrySet().stream().map(entry -> entry.getKey()).collect(
                Collectors.toList()));
        return new compile.ast.Literal(encodeCall(varsString));
    }
    public compile.ast.Expression decodeVars(Map<String, compile.ast.Type> varsMap, compile.ast.Expression data) {
        String varsString = "(" + varsMap.values().stream().map(
                compile.ast.Type::solCode).collect(
                Collectors.joining(", ")) + ")";
        return new compile.ast.Literal(decodeCall(data.toSolCode(), varsString));
    }
    public compile.ast.Expression decodeVars(List<compile.ast.Type> typeList, compile.ast.Expression data) {
        String varsString = "(" + typeList.stream().map(compile.ast.Type::solCode).collect(
                Collectors.joining(", ")) + ")";
        return new compile.ast.Literal(decodeCall(data.toSolCode(), varsString));
    }

    public ExceptionTypeSym findExceptionTypeSym(String name) {
        assert exceptionSymTable.containsKey(name);
        return exceptionSymTable.get(name);
    }

    public EventTypeSym findEventTypeSym(String name) {
        assert eventSymTable.containsKey(name);
        return eventSymTable.get(name);
    }

    public String newTempVarName() {
        return temporaryNameManager.newVarName();
    }

    public void addGlobalVar(String name, compile.ast.Type type) {
        globalvars.put(name, type);
    }

    public void addLocalVar(String name, compile.ast.Type type) {
        localvars.peek().put(name, type);
    }

    public boolean isLocal(String name) {
        return localvars.peek().containsKey(name);
    }

    public compile.ast.Type getLocalVarType(String name) {
        ListIterator<Map<String, compile.ast.Type>> li = localvars.listIterator(localvars.size());
        while (li.hasPrevious()) {
            Map<String, compile.ast.Type> currentMap = li.previous();
            if (currentMap.containsKey(name)) {
                return currentMap.get(name);
            }
        }
        return null;
    }

    public void enterNewVarScope() {
        localvars.push(new HashMap<>());
    }

    public void exitVarScope() {
        localvars.pop();
    }

    public List<compile.ast.Statement> compileReturn(compile.ast.Expression value) {
        List<compile.ast.Statement> result = new ArrayList<>();
        if (currentScope() == ScopeType.METHOD && currentMethod.exceptionFree()) {
            if (value == null) {
                result.add(new compile.ast.Return());
            } else {
                result.add(new compile.ast.Return(value));
            }
        } else {
            compile.ast.Expression return_stat_code;
            if (currentScope() == ScopeType.METHOD) {
                // return 0, return_values
                return_stat_code = new compile.ast.Literal(compile.Utils.RETURNCODE_NORMAL);
            } else {
                // return 10000, return_values
                return_stat_code = new compile.ast.Literal(compile.Utils.RETURNCODE_RETURN);
            }
            // encode values to bytes
            if (currentMethod.returnVoid()) {
                result.add(new compile.ast.Return(List.of(return_stat_code, new compile.ast.Literal("\"\""))));
            } else {
                result.add(new compile.ast.Return(List.of(return_stat_code, new compile.ast.Literal(encodeCall(value.toSolCode())))));
            }
        }
        return result;
    }
}
