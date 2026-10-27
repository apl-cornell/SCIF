# Confused Deputy Protection

*The confused deputy attack* is a type of attack where a trusted principal (the confused deputy) is tricked into misusing its integrity by the attacker, and causing integrity failures as a result.

[The Poly Network Attack](https://rekt.news/polynetwork-rekt/) is an example of the confused deputy attack. And it is the second biggest cryptocurrency hack ($611M stolen) to date. Poly Network provided a service in their contract `EthCrossChainManager` where users could ask the service to call other contracts for them. However, Poly Network didn't notice that `EthCrossChainManager` was actually calling other contracts on its own behalf, and the user tricked it to call another important contract: `EthCrossChainData`, which maintained high-integrity information and was supposed to be called only by highly trusted parties. Because `EthCrossChainData` trusted `EthCrossChainManager`, any user could manipulate data in `EthCrossChainData` by confusing `EthCrossChainManager`.

<!-- In the lens of information flow control, the confused deputy attack can be defined as: 
A confused deputy attack occurs when the pc label of the calling context does not act for the external begin label of the called method, yet the method call accepts the call and executes anyway. -->

In the lens of information flow control, the confused deputy attack involves three parties: the attacker, the deputy, and the victim.
The attacker is not trusted by the deputy nor the victim, and the deputy is trusted by the victim. The deputy opens some entry point that allows the attacker to call and ask the deputy to execute legitimate tasks on the attacker's behalf. If the attacker asks the deputy to call the victim, the victim should reject this call since the deputy is acting on the attacker's behalf and the victim doesn't trust the attacker. *The confused deputy attack happens when this call is accepted somehow*.

External calls in smart contracts are like remote procedure calls: the caller has limited information and control over the environment in which the callee runs, and vice versa. To get rid of confused deputy vulnerabilities, SCIF applied additional checking both statically and dynamically.

## Type confusion prevention

When compiling to Solidity, information flow labels are stripped off from the code. 
And because the way Solidity assigns identities to methods, two methods whose signatures only differ in labels will be assigned the same id. This leads to potential of confused deputy vulnerabilities. 
For example, `uint{this} g{this}(uint{this} amount)` and `uint{any} g{this}(uint{this} amount)` will be assigned the same id while they return values of different integrity level, resulting in potential information flow violation.

To get rid of method type confusion, SCIF embeds labels in method signatures in their names when compiling to Solidity. For example, the name of a method `uint{this} g{this}(uint{this} amount)` will become `"g" + hash(["this", "this", "this"])`, when compiling to a Solidity function.


## Run-time system for confused deputy prevention



```scif
uint{this} f1{sender -> this}(final ContractTarget{sender} target, uint{sender} amount) {
    return target.g(amount);
}

uint{this} f2{any -> this}(ContractTarget{any} target, uint{any} amount) {
    endorse([target, amount], any -> this) {
        return target.g(amount);
    }
}

uint{this} f3{any -> this}(uint{any} amount) {
    return constTarget.g(amount);
}

uint{this} f4{any -> this}() {
    return constTarget.g(constValue);
}

uint{this} g{this}(uint{sender} amount) in ContractTarget
uint{this} g{this}(uint{this} amount) in ContractRealTarget1
uint{any} g{this}(uint{this} amount) in ContractRealTarget2
```

```scif
uint{this} f1{sender -> this}(final ContractTarget{sender} target, uint{sender} amount) {
    assert sender => target;
    assert this => target;

    assert target => this;
    return target.g(amount);
}

uint{this} g{this}(uint{any} amount) {
    checkHash;
    assert sender => this;
}
in ContractTarget

uint{this} g{l -> this}(uint{any} amount) {
    checkHash;
    assert sender => l; // if sender => l, can we trust sender calls in the right pc
}
```

## Support for legacy code

When a SCIF contract calls a method in a legacy contract, its signature will be filled conservatively, promising the lowest level security gurantees. Any entry-point will be treated as a method that requires no integrity level and autoendorse to this, respects no locks and returns low-level values.

When a legacy contract calls a method in a SCIF contract, it is expected that the method pointer points to the method whose legacy method name matches the hash of the method signature.

### SCIF calls legacy

### legacy calls SCIF