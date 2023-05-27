package ast;

import compile.SolCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import typecheck.ContractSym;
import typecheck.InheritGraph;
import typecheck.NTCEnv;
import typecheck.ScopeContext;

public class InterfaceFile extends SourceFile {
    private final Interface itrface;

    public InterfaceFile(String sourceFileName, Set<String> iptContracts,
            Interface itrface) {
        super(sourceFileName, iptContracts, itrface.contractName);
        this.itrface = itrface;
    }

    public InterfaceFile(String sourceFileName, Interface itrface) {
        super(sourceFileName, itrface.contractName);
        this.itrface = itrface;
    }

    public InterfaceFile(InterfaceFile source, boolean builtIn) {
        super(source, builtIn);
        this.itrface = source.itrface;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }

    @Override
    public List<Node> children() {
        return null;
    }

    @Override
    public SourceFile makeBuiltIn() {
        return null;
    }

    @Override
    public boolean containContract(String name) {
        return false;
    }

    @Override
    public boolean codePasteContract(String name, Map<String, Contract> contractMap,
            Map<String, Interface> interfaceMap) {
        return false;
    }

    @Override
    public boolean ntcInherit(InheritGraph graph) {
        return false;
    }

    @Override
    public boolean ntcGlobalInfo(NTCEnv env, ScopeContext parent) {
        return false;
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {

    }

    public Interface getInterface() {
        return itrface;
    }
}
