# Expressions and Statements

## Contract creation

Like creating a new object in regular object-oriented language, a contract can be created by another contract using `new` keyword. 
For example, the following statement creates a contract of type `C`:

```scif
C newContract = new C(args);
```

The full code of the contract being created must be provided during compile time to get rid of loop creation-dependencies.

## Unary expressions

The following unary operators are provided in SCIF: `!` for logical negation; `~` for bitwise negation; `-` for integer negation.

Unary operators have the highest precedence when evaluating expressions.

## Binary expressions

Binary operators provided in SCIF for booleans, bytes and integers are like in Java: `&`, `|`, for bitewise and and or; `<<`, `>>` for bitwise left shift and right shift; `&&`, `||` for logical and and or; `==`, `!=` for logical equality and inequality. `+`, `-`, `*`, `/`, and `%` for integer addition, subtraction, multiplication, division and modulo operation.

`address` type and contracts can also be compared using `==` and `!=`.

## Method calls

Method calls in SCIF contains internal method calls and external method calls.

### Internal method calls

Methods within the same contract can be called using their names. For example,

```scif
contract C {
    @private 
    uint f(uint i) {
        return i + 1;
    }

    @public
    uint g() {
        return f(10);
    }
}
```

In method `g` above, method `f` is called internally.

Internal method calls are compiled into simple jumps inside the EVM. So they are much more efficient than external method calls.

### Extrernal method calls

```scif
contract D {
    C c;
    @private
    uint h() {
        return c.g() + 10;
    }
}
```

In contract `D`, `c`'s public method `g` is called by specifying the contract `c` where the method belongs to. This is an external method call, and a new sub-transaction is created when doing so.

A contract can also call its own public methods externally by using expressions like `this.h()`.

## Assignments

Variables can be assigned values using assignment operator `=`.

## Control structures

The following contrcol structure keywords are supported in SCIF: `if`, `else`, `while`, `for`, `break`, `continue`, and `return`.
They carry similar semantics from Java.

SCIF also supports exception handling by providing `try`/`catch` statements.

## Scoping and declarations

Scoping in SCIF is similar to in Java. Variables are visible from the point after their declaration until the end of the smallest scope block (usually a `{}` block) that contains the declaration.

Extrenal contracts can be imported by keyword `import`. For example, the following code imports a new contract `ContractD` from the specified source file.

```scif
import ContractD from "./ContractD.scif";
```

Variables have initial default values: `false` for a `bool`; `0` for integers and bytes;

## Exceptions

Exception types are declared at contract level and can be thrown and caught by `throw` and `try`/`catch` statements. Any uncaught exceptions in a method must be specified in the signature of the method.
