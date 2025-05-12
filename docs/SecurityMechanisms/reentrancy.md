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
    uint prod = tX.getBal(this) *
                tY.getBal(this);
    uint yKept = prod /
          (tX.getBal(this) + xSold);
    uint yBought = tY.getBal(this) - yKept;

    assert tX.transferFrom(msg.sender,
                           this, xSold);(*\label{lst:li:uniswap-sol-trans-allowed}*)
    assert tY.transfer(this, msg.sender,
                       yBought);(*\label{lst:li:uniswap-sol-trans-disallowed}*)
    return yBought;
  }
}
```

* More formally define reentrancy and reentrancy security.
* How SCIF enforses enentrancy security.
