package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.HashMap;
import java.util.HashSet;

public class Throw extends Statement {
    Call exception;
    public Throw(Call exception) {
        this.exception = exception;
    }
    public void findPrincipal(HashSet<String> principalSet) {

    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        String exceptionName;
        ExceptionTypeSym exceptionSym;
        if (!(exception.value instanceof Name)) {
            if (exception.value  instanceof Attribute) {
                // a.b(c), a must be a contract
                Attribute att = (Attribute) exception.value;
                // (att.value).(att.attr)
                String contractTypeName = ((Name)att.value).id;
                exceptionName = att.attr.id;
                ContractSym s = env.getContract(contractTypeName);
                logger.debug("contract " + contractTypeName + ": " + s.name);
                if (!(env.getExtSym(exceptionName, contractTypeName) == null)) {
                    System.err.println("a.b not found");
                    return null;
                }
                exceptionSym = s.getExceptionSym(exceptionName);
                if (exceptionSym == null) {
                    System.err.println("exception in a.b() not found");
                    return null;
                }
            } else {
                return null;
            }
        } else {
            // a(b)
            exceptionName = ((Name) exception.value).id;
            Sym s = env.getCurSym(exceptionName);
            if (s == null) {
                System.err.println("exception type not found");
                return null;
            }
            if (!(s instanceof ExceptionTypeSym)) {
                // err: type mismatch
                System.err.println("exception type mismatch");
                return null;
            }
            exceptionSym = ((ExceptionTypeSym) s);
        }
        // typecheck arguments
        for (int i = 0; i < exception.args.size(); ++i) {
            Expression arg = exception.args.get(i);
            TypeSym paraInfo = exceptionSym.parameters.get(i).typeSym;
            ScopeContext argContext = arg.NTCgenCons(env, now);
            now.mergeExceptions(argContext);
            String typeName = env.getSymName(paraInfo.name);
            env.addCons(argContext.genCons(typeName, Relation.GEQ, env, location));
        }
        // String rtnTypeName = exceptionSym.returnType.name;
        // env.addCons(now.genCons(env.getSymName(rtnTypeName), Relation.EQ, env, location));

        HashMap<ExceptionTypeSym, CodeLocation> callExceptionMap = new HashMap<>();
        callExceptionMap.put(exceptionSym, location);
        now.mergeExceptions(callExceptionMap);
        return now;
    }
}
