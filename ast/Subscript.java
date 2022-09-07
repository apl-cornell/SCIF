package ast;

import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Subscript extends TrailerExpr {

    Expression index; //TODO: to be slice

    public Subscript(Expression v, Expression i, Context c) {
        value = v;
        index = i;
    }

    //TODO: getVarInfo(NTCEnv)
    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        VarSym valueVarSym = value.getVarInfo(env);
        ScopeContext idx = index.ntcGenCons(env, now);
        value.ntcGenCons(env, now);
        //TODO: support DepMap

        if (valueVarSym.typeSym instanceof DepMapTypeSym) {
            System.err.println("Subscript: DepMap to be support");
            return null;
        } else if (valueVarSym.typeSym instanceof MapTypeSym) {
            MapTypeSym typeInfo = (MapTypeSym) valueVarSym.typeSym;
            // index matches the keytype
            env.addCons(idx.genCons(typeInfo.keyType.name, Relation.LEQ, env, location));
            // valueType matches the result exp
            env.addCons(now.genCons(typeInfo.valueType.name, Relation.EQ, env, location));
            return now;
        } else {
            System.err.println("Subscript: value type not found");
            return null;
        }
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(location),
                typecheck.Utils.getLabelNameLock(location));
        VarSym valueVarSym = value.getVarInfo(env, false);
        String ifNameValue = valueVarSym.labelToSherrlocFmt();
        String ifNameRtnValue = ifNameValue + "." + "Subscript" + location.toString();

        // String ifNameRtnLock = "";
        if (valueVarSym.typeSym instanceof DepMapTypeSym) {
            VarSym indexVarSym = index.getVarInfo(env, tail_position);
            logger.debug("subscript/DepMap:");
            logger.debug("lookup at: " + index.toString());
            logger.debug(indexVarSym.toString());
            String ifNameIndex = indexVarSym.toSherrlocFmt();

            if (indexVarSym.typeSym.name.equals(Utils.ADDRESSTYPE)) {
                logger.debug("typename {} to {}", valueVarSym.typeSym.name, ifNameIndex);
                //System.err.println("typename " + valueVarInfo.type.typeName + " to " + ifNameIndex);
                // String ifDepMapIndexReq = ((DepMapTypeSym) valueVarSym.ifl.toSherrlocFmt(valueVarSym.typeSym.type.typeName, ifNameIndex);
                String ifDepMapValue = (valueVarSym).ifl.toSherrlocFmt(valueVarSym.typeSym.name,
                        ifNameIndex);
                // env.cons.add(new Constraint(new Inequality(ifNameIndex + "..lbl", ifDepMapIndexReq), env.hypothesis, location));

                env.cons.add(
                        new Constraint(new Inequality(ifDepMapValue, Relation.EQ, ifNameRtnValue),
                                env.hypothesis, location, env.curContractSym.name,
                                "Integrity level of the subscript value is not trustworthy enough"));

                // ifNameRtnLock = env.prevContext.lambda;
                return null;
            } else {
                logger.error("non-address type variable as index to access DEPMAP @{}",
                        locToString());
                //System.out.println("ERROR: non-address type variable as index to access DEPMAP @" + locToString());
                return null;
            }
        } else {
            ExpOutcome io = index.genConsVisit(env, tail_position);
            Context indexContext = io.psi.getNormalPath().c;
            String ifNameIndex = io.valueLabelName;
            ifNameRtnValue =
                    scopeContext.getSHErrLocName() + "." + "Subscript" + location.toString();
            env.cons.add(new Constraint(new Inequality(ifNameValue, Relation.EQ, ifNameRtnValue),
                    env.hypothesis, location, env.curContractSym.name,
                    "Integrity level of the subscript value is not trustworthy enough"));

            // env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtnValue), env.hypothesis, location));
            // ifNameRtnLock = indexContext.lambda;
            return new ExpOutcome(ifNameRtnValue, io.psi);
        }
    }

    public VarSym getVarInfo(VisitEnv env, boolean tail_position) {
        VarSym rtnVarSym = null;
        VarSym valueVarSym = value.getVarInfo(env, false);
        String ifNameValue = valueVarSym.labelToSherrlocFmt();
        String ifNameRtn = ifNameValue + "." + "Subscript" + location.toString();
        if (valueVarSym.typeSym instanceof DepMapTypeSym) {
            VarSym indexVarSym = index.getVarInfo(env, tail_position);
            String ifNameIndex = indexVarSym.toSherrlocFmt();
            if (indexVarSym.typeSym.name.equals(Utils.ADDRESSTYPE)) {

                TypeSym rtnTypeSym = ((DepMapTypeSym) valueVarSym.typeSym).valueType;
                rtnVarSym = new VarSym(ifNameRtn, rtnTypeSym, valueVarSym.ifl, location,
                        valueVarSym.defContext, false, false);
                //assert rtnVarSym != null;
                rtnVarSym.replace(valueVarSym.typeSym.name, ifNameIndex);

                // String ifDepMapIndexReq = ((DepMapTypeSym) valueVarSym.typeSym).keyType.ifl.toSherrlocFmt(valueVarSym.typeSym.type.typeName, ifNameIndex);
                String ifDepMapValue = (valueVarSym).ifl.toSherrlocFmt(valueVarSym.typeSym.name,
                        ifNameIndex);
                // env.cons.add(new Constraint(new Inequality(ifNameIndex + "..lbl", ifDepMapIndexReq), env.hypothesis, location));

                env.cons.add(
                        new Constraint(new Inequality(ifDepMapValue, ifNameRtn), env.hypothesis,
                                location, env.curContractSym.name,
                                "Label of the subscript variable"));

            } else {
                logger.error("non-address type variable as index to access DEPMAP @{}",
                        locToString());
                //System.out.println("ERROR: non-address type variable as index to access DEPMAP @" + locToString());
                return null;
            }
        } else {
            String ifNameIndex = index.genConsVisit(env, tail_position).valueLabelName;
            ifNameRtn = scopeContext.getSHErrLocName() + "." + "Subscript" + location.toString();
            env.cons.add(
                    new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location,
                            env.curContractSym.name,
                            "Label of the subscript variable"));

            env.cons.add(
                    new Constraint(new Inequality(ifNameIndex, ifNameRtn), env.hypothesis, location,
                            env.curContractSym.name,
                            "Label of the subscript index value"));

            TypeSym rtnTypeSym = new BuiltinTypeSym(ifNameRtn);
            //TODO: more careful thoughts
            rtnVarSym = new VarSym(ifNameRtn, rtnTypeSym, valueVarSym.ifl, location,
                    valueVarSym.defContext, false, false);
        }
        return rtnVarSym;
    }

    @Override
    public String toSolCode() {
        String i = index.toSolCode();
        String v = value.toSolCode();
        return v + "[" + i + "]";
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        rtn.add(index);
        return rtn;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Subscript &&
                super.typeMatch(expression) &&
                index.typeMatch(((Subscript) expression).index);
    }
}
