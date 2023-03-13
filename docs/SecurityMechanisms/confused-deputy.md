# Confused Deputy Protection

*The confused deputy attack* is a type of attack where a trusted principal (the confused deputy) is tricked into misusing its integrity by the attacker, and causing integrity failures as a result.

[The Poly Network Attack](https://rekt.news/polynetwork-rekt/) is an example of the confused deputy attack. And it is the second biggest cryptocurrency hack ($611M stolen) to date. Poly Network provided a service in their contract `EthCrossChainManager` where users could ask the service to call other contracts for them. However, Poly Network didn't notice that `EthCrossChainManager` was actually calling other contracts on its own behalf, and the user tricked it to call another important contract: `EthCrossChainData`, which maintained high-integrity information and was supposed to be called only by highly trusted parties. Because `EthCrossChainData` trusted `EthCrossChainManager`, any user could manipulate data in `EthCrossChainData` by confusing `EthCrossChainManager`. 

In the lens of information flow control, the confused deputy attack can be defined as: 
A confused deputy attack occurs when the pc label of the calling context does not act for the external begin label of the called method, yet the method call accepts the call and executes anyway.

In the lens of information flow control, the confused deputy attack involves three parties: the attacker, the deputy, and the victim.
The attacker is not trusted by the deputy nor the victim, and the deputy is trusted by the victim. The deputy opens some entry point that allows the attacker to call and ask the deputy to execute legitimate tasks on the attacker's behalf. If the attacker asks the deputy to call the victim, the victim should reject this call since the deputy is acting on the attacker's behalf and the victim doesn't trust the attacker. *The confused deputy attack happens when this call is accepted somehow*.

SCIF helps prevent this attack using both static and dynamic information flow control: statically, SCIF type system ensures that all non-first-order method calls cause no integrity failures.