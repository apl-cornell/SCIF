# Syntax for SHErrLoc constraints conf

Basically three parts: *declarations*, *assumptions* and *equations*. Separated by a line `%%` when assumptions exist.


## Declarations
A declaration can declare a *constructor*, a *function* or a *variable*.

* constructor
* function
* variable
```
CONSTRUCTOR <id> <arity> (level <level>)
FUNCTION <id> <arity>
VARIABLE <id> (level <level>)

// ex
CONSTRUCTOR ALICE 0
```

## Assumptions
```
AXIOM (<id> (, <id>)* .) ((<inequality>;)+ => ) (<inequality>;)+ ;
// examples to be added
```

## Equations

```
<inequality>         ({(<inequality>;)*}); [(<snippet_string>:) (<int>,<int>-<int>,<int>) (@ <id>) ]
//constraint         hypothesis            position

// exs
alice <= bob {alice == carol}; ["test condition": 11,2-11,3]
```

### Inequalities
```

<inequality> ::= <ele_pos> (== | <= | =>) <ele_pos>
<ele_pos> ::= <element> <position>
<element> ::= <id> | "(" <term> ")"
<term> ::= 
    <term> (-> | <- | , | MEET | JOIN)  <term>
    // precedence right ->, <-, MEET, JOIN;
    // precedent left ,;
    | <ele_pos>+
    
// ex
(f.y ⊓ BOB) <= m..lbl.SubscriptL8C22 
f.y == f.z
f.z >= ALICE
```

<!--
## Polymorphic Func
```python
def sqr[x]{x}(int{x}) -> int{x}

// pc = ALICE
// aliceNo : int{ALICE}
sqr(aliceNo)

// pc = BOB
// bobNo : int{BOB}
sqr(bobNo)

```
-->