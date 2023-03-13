# Layout of a SCIF source file


<!--
    Here describes the layout of the source file:
        imports
        the contract:
            name, ifc labels, inheritance
            trust relations
            variables
            exceptions
            constructors
            methods
-->

A SCIF source file can contain multiple SCIF contracts definitions. This section serves as a quick overview of the SCIF file layout.

An example:

```scif
contract LocalTrust {
    final address addrA = 0x...;
    final address addrB = 0x...;
    final address addrC = 0x...;
    principal high;
    principal low;
    assume {
        high => this;
        this => low;
        addrA => high;
        addrB => low;
    }

    uint highValue{high};
    uint lowValue{low};
    exception X();
    exception Y(uint aug1, bool aug2);

    @public
    void setHigh{high}(uint v) {
        highValue = v;
    }

    @public
    void foo() {
        setHigh(0);
    }

    @public
    void setLow{low}(uint v) {
        lowValue = v;
    }
}
```

## Importing from other source files

SCIF supports import statements. By using an import statement like the following:

```scif
import "path-to-file";
```

All global symbols such as contacts are imported from the specified file. 

## Structure of a contract

Structure of SCIF contracts are similar to classes in object-oriented programming languages such as Java. Each contract can contain declarations of state variables, exceptions, constructors and methods. In addition, a SCIF contract supports declarations of local principals and local trust assumptions that help secure your program from muliple security vulnerabilities such as reentrancy vulnerabilities.

### contract name, contract label and inheritance

Each contract starts with a name, a label (optional), and inheritance declaration (optional).

```scif
contract LocalTrust[l] extends BaseContract {
```

The label `l` indicates the trust level of the contract code, and the inheritance declaration above specifies that `LocalTrust` inherits from `BaseContract`.

### state variables and local principals

State variables are variables that are stored persistently and used globally in the contract.

```scif
contract LocalTrust {
    final address addrA = 0x...;
    final address addrB = 0x...;
    final address addrC = 0x...;
    principal high;
    principal low;
```

The above code declare three final addresses state variables.

Local principals are local labels that can be used in other label declarations and serve as convinient tools to represent local trust entities. The above code declare two local principals `high` and `low`.

### local trust assumptions

Users can declare local trust assumptions to help the compiler better reasoning trust relationships.

```scif
    assume {
        high => this;
        this => low;
        addrA => high;
        addrB => low;
    }
```
The above clause declares four trust assumptions. `A => B` means that `A` is trusted by `B`.

### exceptions

Exceptions can be used to indicate special behaviors and scenarios during contract executions.

```scif
    exception X();
    exception Y(uint aug1, bool aug2);
```

The above code declare two exceptions `X` and `Y`. Notice that exceptions can carry information as parameters.

### constructor methods

Constructor methods are optional. They provide convenience when creating a new contract and can help build invariants.

```scif
    constructor(...) {
        ...
    }
```

A constructor carries arbitrary parameters and can manipulate state variables.

### methods

Users can define public or private methods as they wish. In the example code, we define three methods `setHigh`, `foo` and `setLow`. They are declared in a way similar to Java and with additional information flow label annotations.

See the corresponding methods in chapter language basics for more information. 