# Types

SCIF is statically typed. Every variable, parameter and return value has a declared type, and its declaration can also carry a label (see [Information Flow Control](/LanguageBasics/information-flow-control)).

## Primitive types

- `bool`: `true` or `false`.
- `uint`: an unsigned 256-bit integer, from 0 to 2²⁵⁶ − 1. It is the only integer type.
- `address`: a 20-byte account address.
- `bytes`: a dynamically sized byte array.
- `string`: a dynamically sized UTF-8 string.

`void` is used only as the return type of a function that returns nothing.

## Literals

Integer literals are written in decimal, from `0` to `2147483647` (2³¹ − 1), and the largest `uint` value is available as `UINT_MAX`. The boolean literals are `true` and `false`. String literals appear only in `revert` statements.

## Contract and interface types

Every contract and interface name is a type, whose values refer to deployed contracts. To call the function `balanceOf` of a contract value `token`, write `token.balanceOf(account)`.

`address(c)` converts the contract value `c` to its address. Conversely, `C(addr)`, where `C` is the name of a contract or interface, converts the address `addr` to a value of type `C`: for example, `IToken(addr)` refers to the contract at address `addr` through the interface `IToken`.

## Arrays

`T[]` is a dynamically sized array of elements of type `T`, indexed from zero with `a[i]`. `a.length()` returns the number of elements, and for an array stored in a state variable, `a.push(x)` appends `x` and `a.pop()` removes the last element.

## Maps

`map(K, V)` maps keys of type `K` to values of type `V`. Every key initially maps to the zero value of `V`, and `m[k]` reads or writes the entry for the key `k`.

Each entry carries the label of the map variable. For example, `map(address, uint) balances;` declares a state variable labeled `{this}` by default, so every balance is trusted by the contract.

A *dependent map* instead labels each entry by its own key. In `map(address owner, uint{owner}) deposits`, the entry for a key `k` is labeled `{k}`: it is trusted by the account `k` itself. The key used to access a dependent map must be a principal, such as `sender` or a `final address` variable. The [ERC20 tutorial](/Introduction/Your-First-SCIF-Contract) uses a dependent map so that the allowances of each account are trusted by that account.

## Structs

A struct groups named fields. Structs are declared at the top of a contract or interface, and their fields are accessed with `.`:

```scif
contract Orders {
    struct Order {
        address owner;
        uint amount;
    }

    Order latest;

    constructor() { super(); }

    public void record(uint amount) {
        latest.amount = endorse(amount, sender -> this);
    }
}
```

Struct fields carry no labels of their own: a struct value has the label of the variable that holds it. A struct value is built by assigning its fields.

## Closure types

A closure type such as `(fun {this} (uint) -> void) {this}` is the type of a pending call to a closurable function; Closures describes its form and labels.

## Labeled types

A label can follow the type in every declaration: state variables, local variables, parameters, return types, and exceptions in a `throws` clause. An omitted label takes the default described in [Information Flow Control](/LanguageBasics/information-flow-control) and Functions.

## Zero values

A variable that is not explicitly initialized holds the zero value of its type: `false`, `0`, the zero address, or an empty array, `bytes` or `string`. The entries of a map and the fields of a struct start out zero in the same way.
