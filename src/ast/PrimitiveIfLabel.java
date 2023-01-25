package ast;

import java.util.Objects;
import typecheck.BuiltInT;
import typecheck.ContractSym;
import typecheck.ExpOutcome;
import typecheck.FuncSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Sym;
import typecheck.TypeSym;
import typecheck.Utils;
import typecheck.VarSym;
import typecheck.VisitEnv;

import java.util.ArrayList;
import java.util.HashSet;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

public class PrimitiveIfLabel extends IfLabel {

    private final Name value;
    VarSym valueSym;

    public PrimitiveIfLabel(Name value) {
        this.value = value;
        location = Utils.BUILTIN_LOCATION;
    }

    public Name value() {
        return value;
    }
//
//    @Override
//    public String toSHErrLocFmt(ScopeContext defContext) {
//        // TODO:
//        String rnt = null;
//        assert valueSym != null;
//        if (valueSym != null) {
//            String name = value.id;
////            if (name.equals(Utils.LABEL_BOTTOM)) {
////                rnt = Utils.SHERRLOC_BOTTOM;
////            } else if (name.equals(Utils.LABEL_TOP)) {
////                rnt = Utils.SHERRLOC_TOP;
////            } else {
////                rnt = valueSym.defContext.getSHErrLocName() + "." + name;
////            }
//            return valueSym.toSHErrLocFmt();
//        }
//        return rnt;
//    }

    public String toSherrlocFmt(String namespace) {
        String rnt = "";
        if (value != null) {
            String name = value.id;
            if (name.equals(Utils.LABEL_BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.LABEL_TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else {
                if (!Objects.equals(namespace, "")) {
                    namespace += ".";
                }
                rnt = namespace + name;
            }
        }
        return rnt;
    }

    public String toSherrlocFmt(String k, String v) {
        String rnt = "";
        if (value != null) {
            String name = value.id;
            if (name.equals(Utils.LABEL_BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.LABEL_TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else if (name.equals(k)) {
                rnt = v;
            } else {
                rnt = name;
            }
        }
        return rnt;
    }

    public String toSherrlocFmtApply(HashSet<String> strSet, int no) {
        String rnt = "";
        if (value != null) {
            String name = value.id;
            if (name.equals(Utils.LABEL_BOTTOM)) {
                rnt = Utils.SHERRLOC_BOTTOM;
            } else if (name.equals(Utils.LABEL_TOP)) {
                rnt = Utils.SHERRLOC_TOP;
            } else if (strSet.contains(name)) {
                rnt = name + ".apply" + no;
            } else {
                rnt = name;
            }
        }
        return rnt;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

//    @Override
//    public void findPrincipal(HashSet<String> principalSet) {
//        if (value != null) {
//            String name = value.id;
//            if (!name.equals(Utils.LABEL_TOP) && !name.equals(Utils.LABEL_BOTTOM)) {
//                principalSet.add(name);
//            }
//        }
//    }

//    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
//        if (value != null) {
//            String name = value.id;
//            if (!name.equals(Utils.LABEL_TOP) && !name.equals(Utils.LABEL_BOTTOM) && !name.equals(
//                    getRidOf)) {
//                principalSet.add(name);
//            }
//        }
//    }

    @Override
    public boolean typeMatch(IfLabel begin_pc) {
        if (!(begin_pc instanceof PrimitiveIfLabel)) {
            return false;
        }
        return value.typeMatch(((PrimitiveIfLabel) begin_pc).value);
    }

    public void replace(String k, String v) {
        if (value != null) {
            String name = value.id;
            if (name.equals(k)) {
                value.id = v;
            }
        }
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
//        if (value.id.equals(Utils.LABEL_BOTTOM) || value.id.equals(Utils.LABEL_TOP) || value.id.equals(Utils.LABEL_SENDER) || value.id.equals(Utils.LABEL_THIS)) {
//            return null;
//        }
        Sym s = env.getCurSym(value.id);
        logger.debug("Label Name: " + value.id);
        logger.debug(s.toString());
        if (s instanceof VarSym) {
            valueSym = (VarSym) s;
            ScopeContext now = new ScopeContext(this, parent);
            TypeSym typeSym = ((VarSym) s).typeSym;
            logger.debug(s.getName());
            if (!typeSym.getName().equals(Utils.BuiltinType2ID(BuiltInT.PRINCIPAL)) && !typeSym.getName().equals(Utils.BuiltinType2ID(BuiltInT.ADDRESS)) && !(typeSym instanceof ContractSym)) {
                throw new RuntimeException("Primitive non-address/principal ifc label " + value.id + " at " + "location: "
                        + location.toString());
            }
            logger.debug(now.toString());
            return now;
        } else {
            throw new RuntimeException("Primitive undefined ifc label " + value.id + " at " + "location: "
                    + location.toString());
        }
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(value);
        return rtn;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof PrimitiveIfLabel &&
                typeMatch((IfLabel) expression);
    }

    public String toString() {
        return value.id;
    }
}
