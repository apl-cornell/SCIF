package ast;

import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

/**
 * Represent an expression value[index].
 * value is of type map or array, and index's type should be right accordingly.
 */
public class Subscript extends TrailerExpr {

    Expression index;

    public Subscript(Expression v, Expression i) {
        value = v;
        index = i;
    }

    @Override
    public VarSym getVarInfo(NTCEnv env) {
        VarSym valueVarSym = value.getVarInfo(env);
        assert valueVarSym.typeSym instanceof MapTypeSym;

        VarSym subscriptVarSym = new VarSym(
                valueVarSym.getName() + ".sub",
                ((MapTypeSym) valueVarSym.typeSym).valueType,
                null,
                location,
                valueVarSym.defContext(),
                false,
                false,
                true
        );
        return subscriptVarSym;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        VarSym valueVarSym = value.getVarInfo(env);
        ScopeContext idx = index.ntcGenCons(env, now);
        value.ntcGenCons(env, now);

        if (valueVarSym.typeSym instanceof DepMapTypeSym) {
            // index must match, and must be a final address/contract or a principal
            boolean validIndex = true;
            if (index instanceof Name) {
                VarSym indexSym = index.getVarInfo(env);
                if (!indexSym.isPrincipalVar()) {
                    validIndex = false;
                }
            } else {
                validIndex = false;
            }
            if (!validIndex) {
                throw new RuntimeException("Must use a final address/contract to access a dependent map: " + ((Name) index).id);
            }
            return now;
        } else if (valueVarSym.typeSym instanceof MapTypeSym) {
            MapTypeSym typeInfo = (MapTypeSym) valueVarSym.typeSym;
            // index matches the keytype
            env.addCons(idx.genCons(typeInfo.keyType.getName(), Relation.LEQ, env, location));
            // valueType matches the result exp
            env.addCons(now.genCons(typeInfo.valueType.getName(), Relation.EQ, env, location));
            return now;
        } else {
            throw new RuntimeException("Subscript: value type not found: " + value);
        }
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));
        VarSym valueVarSym = value.getVarInfo(env, false);
        String ifNameValue = valueVarSym.labelNameSLC();
        // String ifNameRtnValue = ifNameValue + "." + "Subscript" + location.toString();
        String ifNameRtnValue = toSHErrLocFmt();
        ExpOutcome io = index.genConsVisit(env, tail_position);


        // String ifNameRtnLock = "";
        if (valueVarSym.typeSym instanceof DepMapTypeSym) {
            // value[index] where value is of dependent map type
            // precondition: the type of index and value match; index is a final address/contract or a principal
            // the result value of this expression has the label dependent to index

            DepMapTypeSym typeSym = (DepMapTypeSym) valueVarSym.typeSym;
            VarSym indexVarSym = index.getVarInfo(env, tail_position);
            logger.debug("subscript/DepMap:");
            logger.debug("lookup at: " + index.toString());
            logger.debug(indexVarSym.toString());
            String ifNameIndex = indexVarSym.toSHErrLocFmt();

            if (indexVarSym.isPrincipalVar()) {
                logger.debug("typename {} to {}", valueVarSym.typeSym.getName(), ifNameIndex);
                String ifDepMapValue = (valueVarSym).ifl.toSHErrLocFmt(typeSym.key().toSHErrLocFmt(),
                        ifNameIndex);

                env.cons.add(
                        new Constraint(new Inequality(ifDepMapValue, Relation.EQ, ifNameRtnValue),
                                env.hypothesis(), location, env.curContractSym().getName(),
                                "Integrity level of the subscript value is not trustworthy enough"));
                return new ExpOutcome(ifNameRtnValue, io.psi);

            } else {
                assert false;
                return null;
            }
        } else {
            Context indexContext = io.psi.getNormalPath().c;
            String ifNameIndex = io.valueLabelName;
            //ifNameRtnValue =
            //        scopeContext.getSHErrLocName() + "." + "Subscript" + location.toString();
            env.cons.add(new Constraint(new Inequality(ifNameValue, Relation.EQ, ifNameRtnValue),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    "Integrity level of the subscript value is not trustworthy enough"));

            // env.cons.add(new Constraint(new Inequality(ifNameIndex, ifNameRtnValue), env.hypothesis, location));
            // ifNameRtnLock = indexContext.lambda;
            return new ExpOutcome(ifNameRtnValue, io.psi);
        }
    }

    public VarSym getVarInfo(VisitEnv env, boolean tail_position) {
        VarSym rtnVarSym = null;
        VarSym valueVarSym = value.getVarInfo(env, false);
        String ifNameValue = valueVarSym.labelNameSLC();
        String ifNameRtn = ifNameValue + "." + "Subscript" + location.toString();
        if (valueVarSym.typeSym instanceof DepMapTypeSym) {
            assert false;
            VarSym indexVarSym = index.getVarInfo(env, tail_position);
            String ifNameIndex = indexVarSym.toSHErrLocFmt();
            if (indexVarSym.typeSym.getName().equals(Utils.ADDRESS_TYPE)) {

                TypeSym rtnTypeSym = ((DepMapTypeSym) valueVarSym.typeSym).valueType;
                rtnVarSym = new VarSym(ifNameRtn, rtnTypeSym, valueVarSym.ifl, location,
                        valueVarSym.defContext(), false, false, false);

//                String ifDepMapValue = (valueVarSym).ifl.toSHErrLocFmt(valueVarSym.typeSym.name(),
//                        ifNameIndex);

//                env.cons.add(
//                        new Constraint(new Inequality(ifDepMapValue, ifNameRtn), env.hypothesis,
//                                location, env.curContractSym.name(),
//                                "Label of the subscript variable"));

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
                    new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis(), location,
                            env.curContractSym().getName(),
                            "Label of the subscript variable"));

            env.cons.add(
                    new Constraint(new Inequality(ifNameIndex, ifNameRtn), env.hypothesis(), location,
                            env.curContractSym().getName(),
                            "Label of the subscript index value"));

            TypeSym rtnTypeSym = new BuiltinTypeSym(ifNameRtn);
            //TODO: more careful thoughts
            rtnVarSym = new VarSym(ifNameRtn, rtnTypeSym, valueVarSym.ifl, location,
                    valueVarSym.defContext(), false, false, false);
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
