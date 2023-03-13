# Types

SCIF is statically typed, meaning that each variable and method type needs to be specified at compile time. However, SCIF provides inference mechanisms to infer labels for programmer's convenience.

## Primitive types

* `bool`: Boolean type where the possible values are `true` and `false`.
* `uint`: Unsigned integer type where the possible values are between 0 and 2^256 - 1.
* `byte`: Byte type where the possible values are between 0 and 2^8 - 1.
* `address`: Address type represents an Ethereum account address using a 20-byte value.

## Arrays

`T[n]` represents the type of an array of fixed size `n` and element type `T`. For example, `uint[10]` represents the type of a `uint` array of size 10.

Indices are zero-based.

## Maps

`map(keyType, valueType)` represents a map type that maps from `keyType` to `valueType`. For example, `map(address, uint)` maps from `address` to `uint`.

`keyType` can be any primitive type, while `valueType` can be any type, including maps and user-defined classes.

Values in a map `m` can be accessed through expressions `m[k]`.

## Classes and Contracts

<!-- 
    TODO: add detailed explanations of classes.
 -->

* Contract Types: Every defined contract can be explicitly converted from and to the `address` type.

## Labels

Each variable type in SCIF is associated with a label representing its integrity level. `T{l}` describes a type `T` associated with the label `l`.

For example:

```scif
uint{trusted} x;
uint{untrusted} y;
x = y; // compile error
y = x; // pass
```

`x` is labeled as `trusted` while `y` is labeled as `untrusted`. So when `x` is reassigned to `y`, the compiler will not compile because there is an integrity failure that an untrusted value is assigned to a trusted variable.

If a label is not specified when declaring a variable, the compiler will either infer a label from the context or assign a default label to it.
