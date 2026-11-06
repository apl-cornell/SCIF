# SCIF by Example

This section presents several small example contracts to illustrate core SCIF language features and common design patterns. The source code of every example is available in [`test/contracts/docs`](https://github.com/apl-cornell/SCIF/tree/master/test/contracts/docs).

## Layout of a SCIF source file with `SimpleStorage`

A SCIF source file can contain multiple SCIF contract and interface definitions. This section serves as a quick overview of the source file structure.

Below is an example of a contract named `SimpleStorage`. It has an integer (`uint`) field named `storedData`, representing data trusted by the contract itself, as indicated by the label annotation `{this}`. The contract also defines two functions on the field: a setter `set` and a getter `get`. Notice that `set` is labeled `{this}` just like `storedData`, so only code at least as trusted as the contract can call it and modify the field.

```scif
contract SimpleStorage {
  uint{this} storedData;

  exception X();
  exception Y(uint aug1, bool aug2);

  constructor() {
    super();
  }

  void set{this}(uint x) {
    storedData = x;
  }

  public uint get() {
    return storedData;
  }
}
```

### Importing from other source files

SCIF supports import statements. By using an import statement like the following, all global symbols such as contracts and interfaces are imported from the specified file. Import statements come before the first contract or interface in the file.

```scif
import "path-to-file";
```

### Structure of a contract

The structure of SCIF contracts is similar to that of Solidity contracts and of classes in object-oriented languages such as Java. Each contract declares state variables, exceptions, a constructor and functions. A contract can also declare structs, as well as events that it emits to signal important state changes, just like events in Solidity. The order of these members is fixed: structs, state variables, exceptions, events, the constructor, and then the functions.

#### Contract name, interface implementation, and inheritance

Each contract starts with a name and optional inheritance declarations.

```scif
contract SimpleStorage implements IStorage extends BaseContract {
```

The contract declaration above specifies that `SimpleStorage` implements the `IStorage` interface and inherits from `BaseContract`. The `implements` clause must come before `extends`.

#### State variables

State variables are variables that are stored persistently and used globally in the contract.

```scif
contract SimpleStorage {
    uint{this} storedData;
```

The above code declares an integer state variable trusted by the contract. The label `{this}` is also the default for state variables, so omitting it here leaves the meaning unchanged.

#### Exceptions

Exceptions can be used to indicate special behaviors and scenarios during contract executions.

```scif
    exception X();
    exception Y(uint aug1, bool aug2);
```

The above code declares two exceptions `X` and `Y`. Notice that exceptions can carry information as parameters.

#### Constructor

Every contract declares a constructor. It is executed exactly once, right when the contract is created. Calling `super()` is required before any other function call.

```scif
    constructor() {
        super();
    }
```

A constructor carries arbitrary parameters and can manipulate state variables; it is the natural place to establish the contract's invariants.

#### Functions

Users can define public or private functions as they wish. In the example code, we define two functions `set` and `get`. Their declarations resemble those in Java and Solidity, with additional information flow labels. Functions without the `public` modifier, such as `set`, can only be called from inside the contract.

See the chapter on language basics for more information.

## Multi-User Wallet

The following contract demonstrates a straightforward implementation of a multi-user wallet, which keeps track of each user's balance in the `balances` map and allows any user to securely deposit and withdraw funds.

```scif
contract Wallet {
    map(address, uint) balances;

    constructor() {
        super();
    }

    public void withdraw(uint amount) {
        endorse(amount, any -> this) when (balances[sender] >= amount) {
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

Both the `withdraw` and `deposit` functions are declared `public`, which designates them as *entry points*. As a result, these functions can be invoked by anyone, including untrusted parties, to access the wallet's services. When a caller invokes either function, the built-in variables `sender` and `value`, the counterparts of Solidity's `msg.sender` and `msg.value`, are set to the caller's address and the amount of ether sent with the call.

The `withdraw` function is particularly noteworthy. The sender initiates a withdrawal request for a specified amount of funds. Because `withdraw` is public, its parameter `amount` carries only the integrity of the caller: it can be any value the caller chooses, so the contract cannot trust it unconditionally. To prevent this untrusted `amount` from corrupting the sensitive `balances` data, we employ a *conditional endorsement*: `endorse(amount, any -> this) when (...)`. This endorsement permits `amount` to be trusted only when the sender possesses sufficient funds.

Within the `when` branch, the funds are transferred and the `balances` are updated. Performing these operations without additional security measures could introduce a reentrancy vulnerability, enabling the sender to maliciously call the `withdraw` function again before the `balances` have been updated. To counter this, a *dynamic lock* is explicitly applied to `this`, which denotes the integrity level of the current contract instance. Consequently, SCIF guarantees that no reentrancy can occur for any function with integrity level `this`, including the `withdraw` function. If the `lock(this)` block were mistakenly omitted, the compiler would reject the contract.

SCIF supports *exceptions* to enable more sophisticated control flows. The following contract showcases a multi-user wallet implementation that incorporates exceptions.

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
                } rescue * {
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

In this example, two custom exceptions have been defined: `balanceNotEnough` and `transferFailure`. These exceptions represent scenarios where the sender has insufficient funds and scenarios where the call to `send` fails, respectively. Any exception a function may throw without catching it is listed in its `throws` clause.

It is important to note the use of the `atomic`/`rescue` pattern for invoking `send`. An `atomic` block takes effect either as a whole or not at all: if anything inside it fails (a revert in the recipient's code, an out-of-gas error, etc.), all effects of the block are rolled back and the `rescue` handler runs, where `rescue *` handles any kind of failure. This pattern ensures that the control flow outcome of operations inside the `atomic` block does not influence the control flow outside the block, thus alleviating the burden on developers when reasoning about control flow. Here, a failed `send` is turned into the `transferFailure` exception, and the balance is only reduced if the transfer actually succeeded.

## ERC20 Token

ERC20 is the standard interface for fungible tokens on Ethereum. The [ERC20 tutorial](/Introduction/Your-First-SCIF-Contract) implements it in SCIF with an `IERC20` interface and an `ERC20` contract, and explains how their labels protect token balances and allowances: moving tokens requires the integrity of their owner, and each allowance is trusted by the account that granted it. It also covers compiling and deploying the token, and extending it with events, minting and burning. The next example reuses the tutorial's `IERC20` interface.

## A Token Swap Aggregator with Multiple Contracts

The following example shows how SCIF programs span multiple files and contracts. `Dexible` is a simplified token-swap aggregator: a user asks it to swap tokens through a router of the user's choosing. Both the tokens and the router are contracts the aggregator does not trust, which makes this a textbook setting for *confused deputy* attacks, in which an attacker tricks a trusted contract into misusing its authority. Besides the `IERC20` interface, the example uses the following interface for routers:

```scif
import "./IERC20.scif";

interface IRouter {
    public void swap{user -> this}(
        final address user,
        IERC20{user} tokenIn, IERC20{user} tokenOut, uint{user} amount
        );
}
```

The router's `swap` function requires its caller to be as trusted as `user`, the account whose tokens are being swapped, and its parameters are labeled `{user}` as well.

```scif
import "./IERC20.scif";
import "./IRouter.scif";

contract Dexible {
    exception SwapFailed();

    constructor() { super(); }

    public void swap{sender}(IERC20 tokenIn, IERC20 tokenOut, IRouter router, uint amount)
        throws (SwapFailed) {
        atomic {
            tokenIn.approveFrom(sender, address(router), amount);
            router.swap(sender, tokenIn, tokenOut, amount);
        } rescue * {
            throw SwapFailed();
        }
    }
}
```

The `swap` function of `Dexible` first calls `approveFrom` on `tokenIn`, which allows the router to spend `amount` of the caller's tokens, and then forwards the request to the router's `swap` function. Both calls are wrapped in an `atomic` block, so a failing router cannot leave a dangling approval behind; the failure is reported to the caller as the `SwapFailed` exception instead.

The `swap` function is labeled `{sender}`, which abbreviates `{sender -> sender; sender}`: it runs entirely with the integrity of its caller and never endorses anything. As a result, `Dexible` can only do on the caller's behalf what the caller could have done directly, which keeps it from being used as a confused deputy. If `swap` tried to approve another user's tokens instead of its caller's, the compiler would reject the contract. The full mechanism is described in [Confused Deputy Protection](/SecurityMechanisms/confused-deputy).
