package ast;

import typecheck.ContractInfo;

import java.util.ArrayList;

public class Interface extends Node {
    String contractName;
    public ArrayList<FunctionSig> funcSigs;
    public Interface(String contractName, ArrayList<FunctionSig> funcSigs) {
        this.contractName = contractName;
        this.funcSigs = funcSigs;
    }
    @Override
    public void globalInfoVisit(ContractInfo contractInfo) {
        contractInfo.name = contractName;
        for (Statement stmt : funcSigs) {
            stmt.globalInfoVisit(contractInfo);
        }
    }
}
