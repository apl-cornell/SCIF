# SCIF by example

<!-- 
    Here gives some famous smart contract examples:
        audition
        wallet
        town crier
 -->

## Multi-User Wallet

The following contract demonstrates a straightforward implementation of a multi-user wallet, which allows any user to deposit and withdraw funds from the wallet in a secure manner.

```scif
contract Wallet {
    map(address, uint) balances;
    
    constructor() {
    	super();
    }

    public void withdraw(uint amount) {
        endorse(amount, any -> this) if (balances[sender] >= amount) {
            lock(this) {
                send(sender, amount);
                balances[sender] -= amount;
            }
        } else {
            revert "insufficient funds";
        }
    }

    public payable void deposit() {
        balances[sender] += value;
    }
}
```

Both the `withdraw` and `deposit` methods are decorated as `public`, which designates them as *entry points* by default.
As a result, these methods can be invoked by anyone, including untrusted parties, to access the wallet's services.

The `withdraw` method is particularly noteworthy.
The sender initiates a withdrawal request for a specified amount of funds.
By default, the `amount` variable is labeled as `any` (indicating that it is untrusted).
However, to manipulate the sensitive `balances` data according to the untrusted `amount`, we need to employ a *conditioned endorsement*. 
This endorsement permits the endorsement of `amount` only when the sender possesses sufficient funds.

Within the `if` branch, the funds are transferred and the `balances` are updated.
Performing these operations without additional security measures could introduce a reentrancy vulnerability,
enabling the sender to call the `withdraw` method again before the `balances` have been updated.
To counter this, a dynamic lock is explicitly applied to the integrity level `this` (referring to the integrity level of the current contract instance). Consequently, SCIF ensures that no reentrancy can occur for any methods with integrity level `this`, including the `withdraw` method.

SCIF supports *exceptions* to enable more sophisticated control flows.
The following contract showcases a multi-user wallet implementation that incorporates exceptions.

```scif
contract Wallet {
    map(address, uint) balances;
    exception balanceNotEnough();
    exception transferFailure();
    
    constructor() {
    		super();
    }

    public void withdraw(uint amount) throws (balanceNotEnough, transferFailure) {
        endorse(amount, any -> this) when (balances[sender] >= amount) {
            lock(this) {
                atomic {
                    send(sender, amount);
                } rescue (error e) {
                    throw transferFailure();
                }
                balances[sender] -= amount;
            }
        } else {
            throw balanceNotEnough();
        }
    }

		public payable void deposit() {
        balances[sender] += value;
    }
}
```

In this example, two custom exceptions have been defined: `balanceNotEnough` and `transferFailure`.
These exceptions represent scenarios where the sender has insufficient funds and scenarios where the call to `send` fails, respectively.
It is important to note the use of the `atomic`/`rescue` pattern for invoking the `send` method and handling any exceptions or errors resulting from the call.
This pattern ensures that the control flow outcome of operations inside the `atomic` block does not influence the control flow outside the block, thus alleviating the burden on developers when reasoning about control flow.

## ERC20 token

