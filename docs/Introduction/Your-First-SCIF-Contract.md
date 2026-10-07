# Your First SCIF Contract: An ERC20 Token

In this tutorial we will cover how to:

- Explore the `IERC20` SCIF interface and its `ERC20` implementation
- Understand the information-flow annotations that make the implementation secure
- Compile the `ERC20` SCIF code and (optionally) deploy it
- Extend the `ERC20` token with additional functionality

If the SCIF compiler is not installed yet, please refer to [Getting Started](./getting-started) first.

## Understanding the ERC20 Contract in SCIF

Before diving into code, let's briefly review how SCIF's information flow control (IFC) works, since its annotations are what enable the security guarantees.

### Basics on Information Flow Control (IFC)

SCIF labels information with security policies and uses compile-time IFC checks to ensure that sensitive data cannot be modified or influenced by untrusted sources.

#### Principals

A *principal* in SCIF represents an integrity source. It is typically a blockchain address or one of the built-in symbols:

- `{this}`: the contract itself, the highest integrity level within the contract
- `{sender}`: the caller of the current function
- `{any}`: the lowest integrity level, trusted by nobody

A `final` parameter or state variable of type `address` can also be used as a principal, as `from` is in the `IERC20` interface below.

#### Variable labels

Each variable's integrity is declared with a label:

```
address{this} owner
```

This means only principals as trusted as `{this}` may influence `owner`.

If a state variable has no label, it is labeled `{this}` by default. Inside a function, you may use `{sender}` as a label to indicate the caller's integrity.

#### Endorsement

Labels are enforced by one rule: information may flow from a more trusted source to a less trusted destination, but never the other way around. Taken on its own, this rule would make a contract useless: an untrusted caller could never influence anything the contract trusts, so a token could not even update a balance when a user asks for a transfer.

*Endorsement* is the deliberate exception to the rule. To endorse a value is to decide that it may be treated as more trustworthy than its label indicates, usually after checking that it is acceptable. In SCIF, this decision is never implicit: the only way for untrusted information to reach trusted state is through an endorsement written in the code. A reader can therefore find every place where a contract chooses to trust its inputs by looking for `endorse`.

The basic form is an expression. `endorse(e, l1 -> l2)` takes a value `e` trusted at level `l1` and yields the same value trusted at level `l2`. For example, with a state variable `uint{this} trusted` and a function that anyone may call:

```scif
uint{sender} low = trusted;             // allowed: trusted data may flow to a less trusted variable
trusted = low;                          // rejected: low is less trusted than trusted
trusted = endorse(low, sender -> this); // allowed: low is explicitly endorsed to {this}
```

Endorsement appears in three forms in SCIF, all of which are used in the ERC20 contract below:

- The `endorse(e, l1 -> l2)` expression, as in the constructor, which endorses the token name and symbol supplied by whoever deploys the contract.
- The *conditional endorsement* statement, `endorse([x, y], l1 -> l2) when (cond) { ... } else { ... }`. The listed variables are endorsed, and the first block is executed, only if the condition holds; otherwise the `else` block runs with nothing endorsed. This form is preferred over a bare `endorse` expression: an endorsement is an assumption about untrusted input, and `when` keeps that assumption next to the check that justifies it, for example endorsing a requested amount only after checking that the balance covers it.
- *Auto-endorsement* in a function signature, written `{l_ex -> l_in}`, which endorses the control flow itself when the function is entered. It is explained in the next section.

#### Function labels

A function signature can also include optional labels that describe integrity requirements for the caller, the parameters, the return value and the reentrancy lock. The general syntax looks like this:

```
bool{l_r} f{l_ex -> l_in; l_lk}(address{l_addr} addr, uint{l_amt} amt);
```

- `{l_ex}` - **External begin label (caller integrity)**

  This label specifies *who* is allowed to call the function.

  A function may only be invoked when the caller's integrity level is **at least as high as** `{l_ex}`. For instance, if `{l_ex}` is `from`, only a principal as trusted as `from` can invoke this function.

- `{l_in}` - **Internal begin label (auto-endorsement of control flow)**

  When the function body starts executing, SCIF *auto-endorses* the control flow from `l_ex` to `l_in`.

  This mechanism effectively raises the "trust level" of the ongoing computation. Auto-endorsement is what enables high-integrity operations (like modifying state protected by `this`) once the call passes its security check.

- `{l_lk}` - **Reentrancy lock label**

  This label specifies the integrity level of the reentrancy lock the function promises to maintain: while the function runs, it will not hand control to code less trusted than `l_lk` without an explicit dynamic lock.

  To learn more about reentrancy and how SCIF prevents it, see [Reentrancy Protection](../SecurityMechanisms/reentrancy).

- `{l_addr}`, `{l_amt}` - **Parameter labels**

  Each parameter has its own integrity label indicating the minimum trust level required for its value.

  When the caller supplies arguments, those arguments must satisfy these label constraints. Typically, each parameter label is at least as restrictive as the external label `{l_ex}`, since untrusted parameters would violate the function's integrity assumption.

- `{l_r}` - **Return-value label**

  The label attached to the return type indicates the integrity level of the result value the function produces.

  This ensures that results from low-integrity calls cannot later influence high-integrity logic without explicit endorsement.

When labels are omitted, SCIF fills them in as follows. A function that is not `public` is callable only from within the contract, so everything defaults to the contract's own integrity:

```
l_ex    = this
l_in    = l_ex
l_lk    = l_in
l_param = l_ex (for each parameter)
l_r     = l_ex
```

A `public` function is an entry point that anyone may call, so its defaults are instead:

```
l_ex    = sender
l_in    = this
l_lk    = this
l_param = sender (for each parameter)
l_r     = sender
```

That is, a public function accepts any caller, immediately endorses the control flow to the contract's integrity, and promises not to call untrusted code without a dynamic lock. Partial annotations are completed in the same spirit: `f{l_ex}` means `f{l_ex -> l_ex; l_ex}`, and `f{l_ex -> l_in}` means `f{l_ex -> l_in; l_in}`.

### `IERC20.scif` Interface

```scif
interface IERC20 {
    exception ERC20InsufficientBalance(address owner, uint cur, uint needed);
    exception ERC20InsufficientAllowance(address owner, uint cur, uint needed);
    public uint balanceOf(address account);
    public void approve{sender}(address allowed, uint amount);
    public void approveFrom{from}(final address from, address spender, uint val);
    public void transfer{from -> this}(final address from, address to, uint amount) throws (ERC20InsufficientBalance{this});
    public void transferFrom{sender -> from; sender}(final address from, address to, uint amount) throws (ERC20InsufficientAllowance{this}, ERC20InsufficientBalance{this});
}
```

`IERC20.scif` defines the `ERC20` token interface, including two exceptions and five public functions. A few things to note:

- `uint balanceOf(address account)` returns the balance of an account.
- `void approve{sender}(address allowed, uint amount)` allows the caller (`{sender}`) to set an allowance. The label `{sender}` abbreviates `{sender -> sender; sender}`: the function runs with the caller's integrity and does not endorse anything.
- `void approveFrom{from}(final address from, address spender, uint val)` lets a caller that is as trusted as `{from}` set an allowance on behalf of `from`.
- `void transfer{from -> this}(final address from, address to, uint amount)` moves tokens from `from` to `to` when the caller is as trusted as `{from}`, and the control flow is auto-endorsed to the integrity level of `{this}` (the contract itself), enabling high-integrity operations.
- `void transferFrom{sender -> from; sender}(final address from, address to, uint amount)` accepts any caller, but only endorses the control flow to `{from}`, the owner of the tokens. That is enough integrity to adjust the allowance `from` granted to the caller, and to call `transfer`.
- Exceptions declared in a `throws` clause carry labels too: `ERC20InsufficientBalance{this}` says the exception is raised from a context trusted by the contract.

### Implementing the `ERC20` Interface

Now that we've examined the `IERC20` interface, let's look at how it is implemented in SCIF. The full [implementation](https://github.com/apl-cornell/SCIF/blob/master/test/contracts/docs/ERC20.scif) is available. Below we walk through its structure step by step.

#### Imports

```scif
import "./IERC20.scif";
```

The `import` statement includes other SCIF files by relative path. Here, we import the `IERC20` interface that we will implement. All import statements must come before the first contract or interface in a file.

#### Contract header and inheritance

```scif
contract ERC20 implements IERC20 {
	...
}
```

As in Java, the header declares that `ERC20` implements the `IERC20` interface. Inheritance from another contract can be added with `extends`, written after the `implements` clause.

#### State variable declarations

```scif
map(address, uint) _balances;
map(address owner, map(address, uint{owner}){owner}) _allowances;
uint _burnt;
uint _totalSupply;
bytes _name;
bytes _symbol;
```

State variables are persistent storage and are globally visible to all functions in the contract. SCIF supports common types such as `bool`, `uint`, `bytes`, `address`, `string`, dynamic arrays `T[]`, and associative maps `map(keyT, valT)`.

Each type can carry an integrity label with the syntax `T{l}`, meaning the value of type `T` is labeled by `l`. State variables without a label are labeled `{this}`.

The declaration of `_allowances` is a *dependent map*: it names its key `owner`, and uses that name in the label of the value. The allowance `_allowances[owner][spender]` is therefore trusted by `owner`, the account that granted it, which is exactly the integrity `transferFrom` has after endorsing to `{from}`.

#### Exceptions

```scif
exception ERC20InsufficientBalance(address owner, uint cur, uint needed);
exception ERC20InsufficientAllowance(address owner, uint cur, uint needed);
```

Exceptions can be used to indicate special behaviors and scenarios during contract executions. Here, we declare two exceptions for handling insufficient balance and allowance scenarios during `ERC20` execution. Just like state variables and function definitions, exception declarations can optionally be annotated with information labels too.

#### Constructors

```scif
constructor(bytes name_, bytes symbol_) {
    _name = endorse(name_, sender -> this);
    _symbol = endorse(symbol_, sender -> this);
    super();
}
```

Every contract declares exactly one constructor, which is executed once when the contract is created, just like in Solidity. The constructor must call `super()` before making any other function calls. The constructor's arguments come from whoever deploys the contract, so they carry the integrity `{sender}`; storing them in state variables trusted by the contract requires an explicit `endorse`.

The members of a contract appear in a fixed order: state variables, exceptions, events, the constructor, and then the functions.

#### Functions

A typical function definition looks like this:

```scif
public void transfer{from -> this}(final address from, address to, uint val)
		throws (ERC20InsufficientBalance{this})
{
    endorse([from, to, val], from -> this)
    when (_balances[from] >= val) {
        _balances[from] -= val;
        _balances[to] += val;
    } else {
        throw ERC20InsufficientBalance(from, _balances[from], val);
    }
}
```

This `ERC20` function transfers `val` tokens from address `from` to address `to`, potentially throwing the `ERC20InsufficientBalance` exception.

##### Function signature and labels

Function definitions are Java-like, with optional modifiers placed at the start of the header: `public`, `private`, and `payable`. Like interfaces, functions can have optional information labels annotated.

In `ERC20.transfer`, the external label `{l_ex}` is `from`, which is the first parameter passed in by the caller; this means that the caller must have at least the same integrity level as principal `{from}`. The internal label is `{this}`, so the control flow is auto-endorsed to the highest integrity level `{this}`, permitting the function body to perform high-integrity operations.

##### Function body and IFC statements

SCIF supports standard statements, including assignment, contract creation, control flow (`if`, `else`, `while`, `break`, `continue`, `return`), exception handling (`try`, `catch`, `throw`), and function calls. There are also IFC-related statements. The one used here is the conditional endorsement

```
endorse([vars], low_lbl -> high_lbl) when (cond) { body_1 } else { body_2 }
```

which endorses the listed variables from `{low_lbl}` to `{high_lbl}` and executes `body_1` only when `cond` is satisfied; otherwise, no endorsement happens and the program executes `body_2`.

The example `ERC20.transfer` function does the following step by step:

1. **Caller requirement**: the caller and all the parameters are required to be as trusted as principal `{from}`
2. **Auto-endorsement**: the control flow is auto-endorsed to the highest integrity level `{this}`
3. **Balance check**: verify whether address `from` has sufficient balance to send `val` tokens; if not, throw `ERC20InsufficientBalance` and exit the body
4. **Variable endorsement**: endorse all three parameters from `{from}` to the highest `{this}` integrity level
5. **State updates**: move `val` tokens from address `from` to address `to`

##### Why IFC is important

- If you downgrade the internal label in `ERC20.transfer`'s signature to `{from}`, in both the interface and the implementation, the code will not compile: the compiler reports that the integrity of the control flow is not sufficient for the assignment to `_balances[from]`, since `_balances` is trusted by the contract and the function would now run with only `from`'s integrity.
- If you remove the endorsement and keep a plain `if` for the balance check, the code will also not compile: the compiler reports that the argument `val` is not labeled correctly, since a value trusted only by `from` would be flowing into `_balances`.

This example shows how SCIF enforces the requirements and respects the principle that if the code compiles, it has **no reentrancy vulnerabilities, no confused deputy vulnerabilities, and no improper error handling**.

## Compiling and Deploying the Token

The `ERC20` contract imports `IERC20.scif`, so both files need to be compiled, each to its own Solidity file:

```bash
java -ea -jar SCIF.jar -c IERC20.scif
java -ea -jar SCIF.jar -c ERC20.scif
```

This produces `IERC20.sol` and `ERC20.sol` in the current directory; `ERC20.sol` imports `./IERC20.sol`. To deploy with Foundry, create a project as described in [Getting Started](./getting-started#deploying-scif-contracts), copy both files into its `src` folder, and run `forge build`.

The constructor takes the token name and symbol as `bytes`, so the constructor arguments are passed hex-encoded. `cast from-utf8` does the encoding:

```bash
forge create src/ERC20.sol:ERC20 \
  --rpc-url http://127.0.0.1:8545 \
  --private-key <PRIVATE_KEY> \
  --constructor-args $(cast from-utf8 "MyToken") $(cast from-utf8 "MTK") \
  --broadcast
```

## Extending the ERC20 Token

The provided `ERC20.scif` implements the core functionality of the `ERC20` token, but you can easily extend it.

#### Adding events

Like Solidity, SCIF supports event declarations for logging. Event declarations are placed after the exception definitions and before the constructor. For example, we can add the following two events to the `ERC20` contract:

```scif
event Transfer(address from, address to, uint value);
event Approval(address owner, address spender, uint value);
```

You can emit them within functions, for example:

```scif
emit Transfer(from, to, val);
```

#### Minting and burning

Minting and burning are crucial for operating on `ERC20` tokens. Minting is used to create new tokens and add them to the system, and burning is for destroying tokens and removing them from the system. You can try to implement those with proper security annotations, and see if that compiles!

<details> <summary>Example implementation: mint & burn</summary>

```scif
/**
 * Only owners can mint tokens
 */
public void mint{this}(address to, uint val) {
    if (val > 0) {
        _totalSupply += val;
        _balances[to] += val;
    }
}

/**
 * Sender can only burn their own tokens
 */
public void burn{from -> this}(final address from, uint val)
    throws (ERC20InsufficientBalance{this})
{
    endorse([from, val], from -> this)
    when (_balances[from] >= val) {
        _balances[from] -= val;
        _totalSupply -= val;
        _burnt += val;
    } else {
        throw ERC20InsufficientBalance(from, _balances[from], val);
    }
}
```

`mint{this}` can only be invoked from a context as trusted as the contract itself, so no external account can mint. `burn` follows the same pattern as `transfer`. The complete extended contract is available as [`ERC20_extended.scif`](https://github.com/apl-cornell/SCIF/blob/master/test/contracts/docs/ERC20_extended.scif).
</details>
