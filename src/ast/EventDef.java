package ast;

import compile.CompileEnv;
import compile.ast.Argument;
import typecheck.*;
import typecheck.exceptions.SemanticException;

import java.util.ArrayList;
import java.util.List;

public class EventDef extends TopLayerNode {

    String eventName;
    Arguments arguments;
    boolean isBuiltIn = false;

    public EventDef(String name, Arguments members) {
        eventName = name;
        this.arguments = members;
        // set all args with label this (not sure if needed)
        arguments.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    public EventDef(String name, Arguments members, Boolean isBuiltIn) {
        eventName = name;
        this.arguments = members;
        this.isBuiltIn = isBuiltIn;
        // set all args with label this (not sure if needed)
        arguments.setToDefault(new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        // for regular typecheck
        ScopeContext now = new ScopeContext(this, parent);

        return now;
    }

    @Override
    public void globalInfoVisit(InterfaceSym contractSym) throws SemanticException {
        contractSym.addType(eventName,
                contractSym.toEventType(eventName, arguments, contractSym.defContext()),
                location);
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) throws SemanticException {
        try {
            env.addSym(eventName, env.newEventType(eventName, arguments, parent));
        } catch (SymTab.AlreadyDefined e) {
            throw new SemanticException("Event already defined: " + eventName,
                    location);
        }
        return true;
    }

    public compile.ast.Event solidityCodeGen(CompileEnv code) {
        code.enterNewVarScope();
        List<Argument> args = arguments.solidityCodeGen(code);
        code.exitVarScope();
        return new compile.ast.Event(eventName, args);
    }

    @Override
    public List<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(arguments);
        return rtn;
    }

    public PathOutcome IFCVisit(VisitEnv env, boolean tail_position) throws SemanticException {
        return null;
    }

    public String toString() {
        return "";
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    public boolean typeMatch(EventDef eventDef) {
        return eventName.equals(eventDef.eventName) &&
                arguments.typeMatch(eventDef.arguments);
    }

}
