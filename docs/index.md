# SCIF Reference Manual

SCIF (**S**mart **C**ontract with **I**nformation **F**low) is a secure object-oriented high-level language for implementing smart contracts. It enforces improved security properties with hybrid information flow tracking and lock mechanism. The primary goal is to prevent integrity failures such as reentrancy vulnerabilities and access control failures.

Comparing to a regular smart contract language, such as Solidity, SCIF supports expressing integrity policies by annotating code with *labels*. The programmer is able to specify which data and code are important and should be protected from adversarial parties. If a SCIF program typechecks, the type system ensures that no integrity failures could ever happen.

### Getting Started

### Layout of a SCIF source file

### SCIF by Examples

## Language Basics

### Information flow control

* Principals
* Integrity Policy

### Types

* Primitive types
* Arrays
* Maps
* Classes
* Labels

### Contracts

* Structure of a contract
* State variables
* Exceptions
* Methods
  * Labels
* Inheritance
* Interfaces

### Expressions and Statements

* Contract creation
* Unary expressions
* Binary expressions
* Method calls
* Assignments
* Control structures
* Scoping and declarations
* Exceptions

### Built-in methods and variables

## Security Mechanisms

### Information flow control

### Label model

* Variable labels
* Method labels

### Label Checking

* Variable access
* Method declarations
* Method calls
* Dynamic trust and label checking

### Reentrancy Protection

* Reentrancy Security
* Locks enforcing reentrancy
* Static locks
* Dynamic locks

### Confused Deputy Protection

### Runtime System

* Dynamic trust management
* Dynamic lock management
* Customizable management policies

## Compiling to Solidity

## Deploying to Ethereum

### contract creation

### interacting with contracts

* SCIF contract
* legacy contract

## Section 1

An example of internal link: [Getting Started](/Introduction/getting-started.md)

```scif
public class Example {
    private int x;
    public int x() { return x; }
}
```

## Section 2

!!! Note
    This is an admonition

!!! Info
    This is another admonition. For more styles, go to [Admonitions](https://squidfunk.github.io/mkdocs-material/reference/admonitions).
