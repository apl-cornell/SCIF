# Your First SCIF Contract

In this tutorial we will learn how to:

- Install the SCIF compiler and (optional) the Foundry toolchain
- Explore the `IERC20` SCIF interface and its `ERC20` implementation
- Compile the `ERC20` SCIF code and (optional) deploy it
- Extend the `ERC20` token with more functionality

## Dependencies

Make sure that you have the following installed:

- Java 21
- (optional for deployment) Foundry

## Running SCIF Contracts

#### SCIF Compilation

You can directly download the prebuilt SCIF JAR file from: https://github.com/apl-cornell/SCIF/releases/download/latest/SCIF.jar, and run:

```
java -ea -jar SCIF.jar -c [path_to_SCIF_contract]
```

> [!NOTE]
>
> Alternatively, if you would like to compile from the source code, make sure to install Java 21, JFlex, Ant, and Gradle, and run:
>
> ```bash
> git clone --recurse-submodules https://github.com/apl-cornell/SCIF.git
> cd SCIF
> ./gradlew run --args [path_to_SCIF_contract]
> ```

#### Deploying SCIF Contracts

After compilation, you will get a Solidity file that is equipped with full security guarantees that SCIF promises. One way to deploy it is to use the Foundry toolchain. 

```bash
mkdir scif_erc20_token
cd scif_erc20_token
forge init --no-git
```

Copy the generated Solidity contracts to the `src` folder, then run 

```bash
forge build
```

Now you have built the project. 

```bash
forge create src/ERC20_depmap.sol:ERC20 \
  --rpc-url http://127.0.0.1:8545 \
  --private-key <PRIVATE_KEY> \
  --constructor-args "MyToken" "MTK" \
  --broadcast
```

## Understanding ERC20 Contract in SCIF

Before we dive into the actual SCIF code, let's first do a brief overview on how SCIF's information flow control (IFC) works, because the IFC annotations provide the security guarantees for our contracts.

### Basic Knowledge on Information Flow Control (IFC)

SCIF provides the ability to label information manipulated by programs with security policies. The compiler then enforces the security of the program by leveraging information flow control techniques.

#### Principal

A principal in SCIF represents some authority. It usually is a blockchian address, or some acronym referring to an address, such as the contract itself (`this`), the caller (`sender`), or method parameters. 

#### Variable Labels

A variable's integrity level is expressed by the label annotating to its type. For example, 

```
address{this} owner
```

says that upon the declaration of address `owner`, we require that only principals as trusted as `this` (the contract itself) are allowed to inflence it. 

#### Method Labels

A method signature can include optional labels to describe the integriy of the caller, parameters, return valuesk and reentrancy locks:

```
bool{l_r} f{l_ex -> l_in; l_lk}(address{l_addr} addr, uint{l_amt} amt);
```

- The external label `l_ex` denotes the integrity requirement for the caller. The method can be invoked only when the caller is as trusted as `l_ex`. 
- The internal (auto-endorsement) label `l_in` denotes the integrity requirement for the control flow at the beginning of the method. Upon the execution of the method body, the caller's control flow is auto endorsed to `l_in`, enabling high-integrity operations. When the `l_in` label is not specified, by default we make no auto-endorsement, i.e., let `l_in := l_ex`.
- The reentrancy lock label `l_lk` denotes the integrity requirement for the lock to prevent reentrancy. Only principals that are as trusted as `l_lk` can reenter the method. 
- The parameter labels `l_addr` and `l_amt` denote the integrity requirement for argumument `addr` and `amt`. The caller must supply inputs that are as trusted as `l_addr` and `l_i`, respectively. 
- The return label `l_r` denotes the integrity of the return value. 

[user defined addr params example]

If a label is not specified when declaring a variable, the compiler will either infer a label from the context, or assign a default label to it.

### `IERC20.scif` Interface

```solidity
interface IERC20 {
    exception ERC20InsufficientBalance(address owner, uint cur, uint needed);
    exception ERC20InsufficientAllowance(address owner, uint cur, uint needed);
    @public uint balanceOf(address account);
    @public void approve{sender}(address allowed, uint amount);
    @public void approveFrom{from}(final address from, address spender, uint val);
    @public void transfer{from -> this}(final address from, address to, uint amount) throws (ERC20InsufficientBalance{this});
    @public void transferFrom{sender -> from; sender}(final address from, address to, uint amount) throws (ERC20InsufficientAllowance{this}, ERC20InsufficientBalance{this});
}
```

`IERC20.scif` defines the `ERC20` token interface, including two exceptions and five public methods. A few things to note:

- `uint balanceOf(address account)` returns the balance of an account.

- `void approve{sender}(address allowed, uint amount)` allows the caller (`sender`) to set an allowance. 
- `void approveFrom{from}(final address from, address spender, uint val)` lets a caller that is as trusted as `from` to set an allowance on behalf of `from`.
- `void transfer{from -> this}(final address from, address to, uint amount)` moves tokens from `from` to `to` when the caller is as trusted as `from`, and the control flow will be auto-endorsed to the highest (?) level (`this` means the contract itself), enabling high-integrity operations.

### `ERC20.scif` Implementation Revealed

Based on the IERC20 interface, we are able to build the ERC20 implementation (which can be accessed here) steps by steps and **in order**:

#### imports

```solidity
import "./IERC20.scif";
```

The `import` statement is used to import contracts, methods, and other definitions, by specifing the relative path to another SCIF file. Here, we import `IERC20` (the `ERC20` interface) and implments it. 

#### contract name (and inheritance)

```solidity
contract ERC20 implements IERC20 {
	...
}
```

Similar to Java syntax, the header declares that the contract `ERC20` implements `IERC20` interface. You can also add an optional inheritance; for more details, you can look at ___. 

#### state variable declarations

```solidity
map(address, uint) _balances;
map(address owner, map(address, uint{owner}){owner}) _allowances;
uint _burnt;
uint _totalSupply;
bytes _name;
bytes _symbol;
```

State variables are variables that are stored persistently and used globally in the contract. A few common variable types are supported in SCIF. This includes:

- `bool`: boolean value of `true` or `false`.
- `uint`: Unsigned integers whose possible values are between `0` and `2^256 - 1`.
- `bytes`: Dynamically-sized byte array. 
- `address`: Address of an Ethereum account, represented as `20` bytes.
- `string`: String literal containing printably ASCII and common escape characters. 
- `T[n]`: Array of type `T` and fixed length `n`. 
- `map(keyT, valT)`: Map mapping from type `keyT`  to type `valT` elements. 

Each variable type in SCIF is associated with a label representing its level of integrity. It can be explicitly annotated by`T{l}`, meaning that the type `T` is associated with the label `l`. For example, `map(address owner, map(address, uint{owner}){owner}) _allowances;`...

The above code declares all necessary state variables to implement `ERC20` contract. 

#### exceptions

```solidity
exception ERC20InsufficientBalance(address owner, uint cur, uint needed);
exception ERC20InsufficientAllowance(address owner, uint cur, uint needed);
```

Exceptions can be used to indicate special behaviors and scenarios during contract executions. Here, we declare two exceptions for handling insufficient balance and allowance scenarios during `ERC20` exection. Just like state varaibles, exception declarations can be optionally annotated with information labels too.

#### constructor methods

```solidity
constructor(bytes name_, bytes symbol_) {
    _name = endorse(name_, sender -> this);
    _symbol = endorse(symbol_, sender -> this);
    super();
}
```

Contructor methods are optional, and if exist, the constructor method will be executed once, just like in Solidity, upon the contract is created. It is usually required to call `super()` (why?) at the end of the constructor method. 

#### methods

A typical method declaaration looks like this:

```solidity
@public void transferFrom{sender -> from; sender}(final address from, address to, uint val) throws (ERC20InsufficientAllowance{this}, ERC20InsufficientBalance{this}) {
    endorse([from, to, val], sender -> from)
    if (_allowances[from][sender] >= val) {
        transfer(from, to, val);
        unchecked {
            _allowances[from][sender] -= val;
        }
    } else {
        throw ERC20InsufficientAllowance(to, _allowances[from][sender], val);
    }
}
```

This `ERC20` methods ...

##### header

Method definitions are Java-like, with optional flags placed at the start of the header: `@public`, `@private`, `@payable`, `@override`.  Similar to interface, method can have optional information labels annotated, as detailed in ___. 

##### method body

SCIF supports most common statements, including but not limited to assignment, contract creation, control flow expressions (`if`, `else`, `while`, `for`, `break`, `continute`, `return`), exception handling (`try`, `catch`), and method calls. There are also IFC-related statements, `endorse (var, low_lbl -> high_lbl) when (cond) { body_1 } else { body_2 }`, which will endrose `var`'s integrity from `low_lbl` to `high_lbl` and execute `body1` only when `cond` is satisifed; otherwise, no endorsement happens and the program will execute `body_2`. 

## Extending the ERC20 Token

#### Adding Events

#### Minting and burning

## Futher Reading

inheritence to uniswap

exception handling

reentrancy prevention

CDA prevention