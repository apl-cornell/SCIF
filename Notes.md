# Dynamic Code Generation
* Dynamic Locks `L`
* When entering a function `f{pc_f -> pc_t, l}(args)`
  * check if `for any l' from L, pc_f => l' join pc_t`
    * statically check if `pc_f => pc_t`, then no need to check
  * no need to lock `l` ?

# Normal Typecheck


 * only supports non-reference type

## TODO
1. support `struct`.

# Translate to Solidity

## Built-in Library

* `send(address, value)` -> `address.send(value)` 
* [global variables](https://solidity.readthedocs.io/en/v0.5.3/miscellaneous.html#global-variables)
    * block
        * coinbase - address payable
        * difficulty, gaslimit, number, timestamp - uint
    * msg
        * data - bytes
        * sender - address payable
        * value - uint


