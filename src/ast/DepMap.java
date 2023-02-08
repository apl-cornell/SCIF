package ast;

import java.util.ArrayList;
import typecheck.DepMapTypeSym;
import typecheck.MapTypeSym;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.SymTab;
import typecheck.TypeSym;
import typecheck.VarSym;

public class DepMap extends Map {
    final private String keyName;
    final private IfLabel valueLabel;
    public DepMap(Type keyType, String keyName, LabeledType valueType) {
        super(keyType, valueType.type());
        this.keyName = keyName;
        this.valueLabel = valueType.label();
    }

    @Override
    public boolean typeMatch(Expression annotation) {
        return annotation instanceof DepMap &&
                super.typeMatch(annotation) &&
                keyName.equals(((DepMap) annotation).keyName) &&
                valueLabel.typeMatch(((DepMap) annotation).valueLabel);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = super.children();
        rtn.add(valueLabel);
        return rtn;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        keyType.ntcGenCons(env, parent);

        TypeSym keyTypeSym = env.toTypeSym(keyType, now);
        VarSym keyVarSym = new VarSym(
                keyName,
                keyTypeSym,
                null,
                keyType.location,
                now,
                false,
                true,
                true
        );
        env.setCurSymTab(new SymTab(env.curSymTab()));
        env.addSym(keyName, keyVarSym);
        valueType.ntcGenCons(env, now);

        env.setCurSymTab(env.curSymTab().getParent());

        DepMapTypeSym typeSym = (DepMapTypeSym) env.toTypeSym(this, scopeContext);
        assert typeSym != null : name;
        env.addCons(now.genEqualCons(typeSym, env, location, "Improper type is specified"));
        return now;
    }

    public String keyName() {
        return keyName;
    }
}
