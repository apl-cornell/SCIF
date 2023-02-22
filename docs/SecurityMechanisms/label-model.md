# Label Model

SCIF allows programmers to specify security policies for integrity using the *decentralized label model*.
A variable or method, when declared, can be annotated with labels which specify the security policy attached to this variable or method.

## Variable labels

A variable can be associated with a label when it is declared. For example, the following code declares a variable `trustedCounter` which is trusted by the principal `owner` in terms of its integrity level.

```scif
final address owner;
uint{owner} trustedCounter;
```

The label of a variable can be ommited.
By default, a state variable is labeled as `this`, meaning that it is trusted by the current contract;
when a local variable is unlabeled, the compiler will infer its label according to specified security policies and report an error when there exists no label that can be assciated to this local variable securely.

## Method labels

A method carries multiple labels in its signature.
The follwing code shows a fully labeled method signature:

```scif
bool{l_r} f{l_ex -> l_in; l_lk}(uint{l_i} i);
```

`l_ex` represents the integrity level required of the environments that are allowed to call this method;
`l_in` represents the integrity level of the environment at the beginning of the method body;
`l_lk` represents the integrity level of the lock this method respects;
`l_i` represents the integrity level of the argument `i`;
`l_r` represents the integrity level of the returned value.

SCIF allows the programmer to ommit these labels in the following ways:

```scif
bool f(uint i) ==> bool f{sender->this}(uint i
bool f{l_ex}(uint i) ==> bool f{l_ex->l_ex}(uint i))
bool f{l_ex->l_in}(uint i) ==> bool{this join l_ex} f{l_ex->l_in;l_in}(uint{l_ex} i)
```