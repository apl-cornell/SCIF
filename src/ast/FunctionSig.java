package ast;

import compile.SolCode;
import compile.Utils;
import java.util.Arrays;
import java.util.List;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FunctionSig extends TopLayerNode {

    public String getName() {
        return name;
    }

    String name;
    FuncLabels funcLabels;
    Arguments args;

    /**
     * a list of decorators, e.g., <code>public</code>, <code>private</code>. Can be null after
     * initialization and need to be set to default
     */
    List<String> decoratorList;
    Type rtn;
    List<ExceptionType> exceptionList;
    boolean isConstructor;

    /**
     * @param name          local name of this method
     * @param funcLabels    information flow labels of this method <code>{a -> b; c; d}</code>
     * @param args          arguments of this method
     * @param decoratorList a list of decorators, e.g., <code>public</code>, <code>private</code>.
     *                      will be set to <code>private</code> if no visibility specified.
     * @param rtn           Type of the return value
     */
    public FunctionSig(String name, FuncLabels funcLabels, Arguments args,
            List<String> decoratorList, Type rtn, boolean isConstructor) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = setToDefault(decoratorList);
        this.rtn = rtn;
        this.exceptionList = new ArrayList<>();
        this.isConstructor = isConstructor;

        funcLabels.setToDefault(isConstructor, this.decoratorList);
    }

    private List<String> setToDefault(List<String> decoratorList) {
        if (decoratorList == null) {
            return new ArrayList<>(Arrays.asList(Utils.PRIVATE_DECORATOR));
        } else {
            boolean isPublic = false, isPrivate = false;
            if (decoratorList.contains(Utils.PUBLIC_DECORATOR)) {
                isPublic = true;
            }
            if (decoratorList.contains(Utils.PRIVATE_DECORATOR)) {
                isPrivate = true;
            }
            if (!(isPrivate && isPublic)) {
                decoratorList.add(Utils.PRIVATE_DECORATOR);
            } else if (isPrivate && isPublic) {
                return null;
                // TODO: throw new Exception("a method can not be both public and private");
            }
            return decoratorList;
        }
    }

    public FunctionSig(String name, FuncLabels funcLabels, Arguments args,
            List<String> decoratorList, Type rtn, List<ExceptionType> exceptionList,
            boolean isConstructor) {
        this.name = name;
        this.funcLabels = funcLabels;
        this.args = args;
        this.decoratorList = setToDefault(decoratorList);
        this.rtn = rtn;
        this.exceptionList = exceptionList;
        this.isConstructor = isConstructor;

        funcLabels.setToDefault(isConstructor, this.decoratorList);
    }

    public FunctionSig(FunctionSig funcSig) {
        this.name = funcSig.name;
        this.funcLabels = funcSig.funcLabels;
        this.args = funcSig.args;
        this.decoratorList = funcSig.decoratorList;
        this.rtn = funcSig.rtn;
        this.exceptionList = funcSig.exceptionList;
        this.isConstructor = funcSig.isConstructor;
    }

    public void setDecoratorList(List<String> decoratorList) {
        this.decoratorList = decoratorList;
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ArrayList<VarSym> argsInfo = args.parseArgs(env, now);
        HashMap<ExceptionTypeSym, String> exceptions = new HashMap<>();
        for (ExceptionType t : exceptionList) {
            t.setContractName(env.curContractSym.name);
            ExceptionTypeSym t1 = env.toExceptionTypeSym(t);
            assert t1 != null;
            // System.err.println("add func exp: " +  t1.name);
            // ExceptionTypeSym exceptionType = env.get(t);
            exceptions.put(t1, null);
        }
        env.addSym(name,
                new FuncSym(name, funcLabels, argsInfo, env.toTypeSym(rtn), null, exceptions,
                        scopeContext, location));
        return true;

    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {
        ArrayList<VarSym> argsInfo = args.parseArgs(contractSym);
        IfLabel ifl = null;
        if (rtn instanceof LabeledType) {
            ifl = ((LabeledType) rtn).ifl;
        }
        HashMap<ExceptionTypeSym, String> exceptions = new HashMap<>();
        for (ExceptionType t : exceptionList) {
            IfLabel label = null;
            /*if (t.type instanceof  LabeledType)
                label = ((LabeledType) t.type).ifl;*/
            ExceptionTypeSym exceptionTypeSym = contractSym.getExceptionSym(t.type.name);
            assert exceptionTypeSym != null;
            exceptions.put(exceptionTypeSym,
                    typecheck.Utils.getLabelNameFuncExpLabel(scopeContext.getSHErrLocName(),
                            t.getName()));
        }
        contractSym.symTab.add(name,
                new FuncSym(name, funcLabels, argsInfo, contractSym.toTypeSym(rtn), ifl, exceptions,
                        scopeContext, location));
    }

    public void findPrincipal(HashSet<String> principalSet) {
        if (funcLabels != null) {
            funcLabels.findPrincipal(principalSet);
        }
        args.findPrincipal(principalSet);

        if (rtn instanceof LabeledType) {
            if (rtn instanceof DepMap) {
                ((DepMap) rtn).findPrincipal(principalSet);
            } else {
                ((LabeledType) rtn).ifl.findPrincipal(principalSet);
            }
        }
    }

    public String rtnToSHErrLocFmt() {
        return toSHErrLocFmt() + ".RTN";
    }

    @Override
    public void passScopeContext(ScopeContext parent) {
        scopeContext = new ScopeContext(this, parent);
        for (Node node : children()) {
            if (node != null) //TODO: remove null check
            {
                node.passScopeContext(scopeContext);
            }
        }
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {

        return null;
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        if (funcLabels != null) {
            rtn.add(funcLabels);
        }
        rtn.add(args);
        if (this.rtn != null) {
            rtn.add(this.rtn);
        }
        return rtn;
    }

    public boolean typeMatch(FunctionSig f) {
        if (!f.name.equals(name)) {
            return false;
        }
        if (!f.funcLabels.typeMatch(funcLabels)) {
            return false;
        }
        if (!f.args.typeMatch(args)) {
            return false;
        }

        if (!(decoratorList == null && f.decoratorList == null)) {
            if (decoratorList == null || f.decoratorList == null
                    || decoratorList.size() != f.decoratorList.size()) {
                return false;
            }
            int index = 0;
            while (index < decoratorList.size()) {
                if (!decoratorList.get(index).equals(f.decoratorList.get(index))) {
                    return false;
                }
                ++index;
            }
        }
        if (!f.rtn.typeMatch(rtn)) {
            return false;
        }
        return true;
    }
}
