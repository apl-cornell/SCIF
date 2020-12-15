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


