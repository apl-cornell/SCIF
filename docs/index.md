# SCIF Reference Manual

SCIF (**S**mart **C**ontract with **I**nformation **F**low) is a secure object-oriented high-level language for implementing smart contracts. It enforces improved security properties with hybrid information flow tracking and lock mechanism. The primary goal is to prevent integrity failures such as reentrancy vulnerabilities and access control failures.

Comparing to a regular smart contract language, such as Solidity, SCIF supports expressing integrity policies by annotating code with *labels*. The programmer is able to specify which data and code are important and should be protected from adversarial parties. If a SCIF program typechecks, the type system ensures that no integrity failures could ever happen.


