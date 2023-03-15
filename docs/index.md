# SCIF Reference Manual

SCIF (**S**mart **C**ontract **I**nformation **F**low) is a secure high-level language for implementing smart contracts. It enforces improved security properties with information flow tracking to prevent untrusted users and contracts from subverting the intended behavior of contracts. Beyond classic information flow control, SCIF includes mechanisms for preventing vulnerabilities arising from reentrancy or confused deputies.

Unlike other smart contract languages such as Solidity, SCIF allows annotating code with *integrity labels* that express security requirements and trust. The programmer specifies which data and code are important and should be protected from adversarial parties. A SCIF program that type-checks is protected against integrity failures.


