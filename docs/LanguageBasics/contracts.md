# Contracts

Contracts in SCIF contain persistent state variables and methods that perform computations and operations.

## Structure of a contract

The declaration of a contract starts by specifying its name and label, followed by its body contained in a braces pair.

The body of a contract may contain the following:

* state variable declarations
* exception definitions
* local trust relationship specifications
* constructor definitions
* method definitions.

The following example shows a basic contract.

```Java
contract ContractName[this] {
    // state variables
    map(address, uint) balances;
    final address owner;

    // exceptions
    exception TransferFailure(address from, address to, uint amount);

    // local trust relationships
    localtrust {
        principal high, low;
        high => low;
        this => high;
        owner => this;
    }

    // constructors
    constructor(address owner) {
        this.owner = owner;
    }

    // methods
    @public 
    void transfer(address to, uint amount) throws (TransferFailure) {
        if (balances[msg.sender] >= amount) {
            balances[msg.sender] = balance[msg.sender] - amount;
            balances[to] = balance[to] + amount;
        } else {
            throw TransferFailure(msg.sender, to, amount);
        }
    }
}
```

## State variables

State variables are contract-level variables. They are stored in`storage` of a contract and thus are persistent.
A state variable can be declared as `final`, similar to Java's keyword `final`, meaning that this variable can only be initialized once in constructors and cannot be reassigned later.

## Exceptions

Exceptions are defined by specifying the name and the arguments of the exception.
Programmers can use `try/catch` clause to catch exceptions and use `throw` statement to throw them.

An exception must be caught in a method or be specified in the method's signature.

There are also some built-in exceptions:

* `OutOfGasException`: a method call throws this exception when the current transaction runs out of gas.
* `RevertException`: `revert` statement reverts all changes in current transaction and throw this exception.

## Local trust relationships

Programmers can declare principals that are not addresses and trust assumptions between principals in `localtrust` clause of a contract.

By specifying `A => B`, the trust relationship that principal `B` trusts principal `A` is put into the local assumptions when typechecking.
By specifying `A == B`, the trust relationship that principal `B` and principal `A` are mutually trusted is put into the local assumptions when typechecking.

Note that it is the programmers' responsibility to ensure the correctness of these assumptions. Failure to do so might result in security vulnerabilities.

## Methods

Programmers declare a method by specifying its return type, name, arguments, and its body.

In addition, methods can be annotated with decorators to specify their properties:

* `public`: this method is an entry point and can be called by external accounts.
* `private`: this method can only be called by other methods inside this contract.
* `payable`: when called, the caller can send ether associated with this call.
* `override`: meaning that this method is overriding an existing method of the super contract.

### Method labels

Labeling a method is more complicated than labeling a variable. 

A method is labeled as `{l1 -> l2; l3}` where `l1` represents the requirement of the caller's integrity level, `l2` represents the control flow integrity level at the beginning of the method, and `l3` represents the lock level this method respects.

## Inheritance

SCIF supports inheritance when defining a contract by using the keyword `extends`:

```Java
contract B extends A {
    ...
    @override
    @public 
    void foo() {
        ...
    }
}
```

Like in Java, contract `B` will inherit all state variables, exceptions, and methods from `A`.
`B` can also override `A`'s method by decorator `override`.


## Interface

<!-- TODO -->