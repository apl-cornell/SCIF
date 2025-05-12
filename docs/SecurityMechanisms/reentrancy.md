# Reentrancy Security

In a reentrancy attack, an attacker unexpectedly causes control flow to
\emph{reenter} an application while it is in an intermediate state. A long
string of reentrancy attacks have resulted in hundreds of millions of dollars
of damage.

The Uniswap token exchange fell victim to a reentrancy vulnerability
in 2020, showing that the combination of multiple contracts—each seemingly secure
in isolation—can be vulnerable.

The following simplified segment of Uniswap code shows how the attack works.
The `sellXForY` function allows users to exchange tokens of
type `X` for those of type `Y`.
Uniswap determines the rate of exchange by holding constant the
product of its balance of `X` and its balance of `Y`.
Both Uniswap and its accompanying token contracts were
originally thought reentrancy-secure because they follow the best-practice paradigm
of checks–effects–interactions, but
their combination unwittingly opens the door to reentrancy attacks.
During the invocation of `transferFrom`,
the client receives a notification, giving it control of execution
and allowing an attacker to opportunistically reenter `sellXForY`.
Because the exchange rate depends on Uniswap's token balances
and one transfer is still pending,
Uniswap computes the exchange rate incorrectly in the reentrant call.
The attacker then receives too favorable a rate, extracting tokens from Uniswap.

```
contract Uniswap {
  Token tX, tY;

  function sellXForY(uint xSold)
      returns uint {
    uint prod = tX.getBal(this) * tY.getBal(this);
    uint yKept = prod / (tX.getBal(this) + xSold);
    uint yBought = tY.getBal(this) - yKept;

    assert tX.transferFrom(msg.sender, this, xSold);
    assert tY.transfer(this, msg.sender, yBought);
    return yBought;
  }
}
```

Reentrancy vulnerabilities arise because in general, smart-contract state
must obey some invariants for the contract to be
correct, but those invariants may be temporarily broken while a method executes.
If an attacker gains control of execution while the contract
is in this inconsistent state (such as through a callback),
they can engineer a reentrant call into a public method.
Though the call comes from attacker integrity, the public method endorses and accepts the call.
Because contract invariants are temporarily broken, the contract might behave improperly.


## Defining reentrancy and reentrancy security

## How SCIF enforses reentrancy security.

SCIF uses an mechanism based on information flow
to prevent reentrancy attacks,
combining static and dynamic _reentrancy locks_
to prevent reentrant endorsement, so that reentrant
calls do not enable new attacks.

SeRIF requires any untrusted call made without dynamic locks to be in tail position,
forbidding any subsequent operations.
This approach prevents dangerous reentrancy, but it also enforces two limiting constraints:
  * Trusted values computed before an untrusted call cannot be returned afterward.
  * In auto-endorse functions, untrusted operations cannot execute after an untrusted call returns,
    even though they inherently cannot create reentrancy concerns.

SCIF maintains the security of SeRIF's reentrancy protection,
while improving precision to allow useful code patterns.
First, methods define their return values by assigning to a special `result` variable.
A method must assign to this variable on every return path.
The usual syntax `return` _e_ is just syntactic
sugar for assigning `result =` _e_ and then returning.
Second, after an untrusted call, the control-flow integrity (the `pc` label)
is modified, restricting future operations to only those that cannot
violate high-integrity invariants.
Neither of these changes can introduce reentrancy concerns, and both simplify programs.
