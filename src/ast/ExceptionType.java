package ast;

import typecheck.Utils;

public class ExceptionType extends Type {
    LabeledType type;
    String contractName;
    public ExceptionType(Type type) {
        super(type.name);
        if (type instanceof LabeledType) {
            this.type = (LabeledType) type;
        } else {
            this.type = new LabeledType(type.name, new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
        }
        this.contractName = "";
    }
    public ExceptionType(Type type, String contractName) {
        super(contractName + "." + type.name);
        if (type instanceof LabeledType) {
            this.type = (LabeledType) type;
        } else {
            this.type = new LabeledType(type.name, new PrimitiveIfLabel(new Name(Utils.LABEL_THIS)));
        }
        this.contractName = contractName;
    }


    public boolean isLocal(String localContract) {
        return contractName.equals("") || contractName.equals(localContract);
    }

    public String getName() {
        return type.name;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String name) {
        contractName = name;
    }
}
