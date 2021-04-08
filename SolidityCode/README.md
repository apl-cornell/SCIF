# Barebone contracts for SCIF
Here contains multiple contracts required by the dynamic system of `SCIF`, including:

1. Base contract classes that provide APIs to support dynamic trust relationship management and lock management;
2. An implementation of smart contracts for a brute-force centralized dynamic system.

## Run
`truffle`: `npm install -g truffle`
Run `truffle test` to run all test cases.

## Test Cases

### Trust relationship management
1. Adding `A => B`, query that if A trusts B, covering `ifTrust(address, address)`, `ifDtrust(address)`, `getDTrustList()`, `setTrust(address)`;
2. Adding `A => B` and `B => C`, query that if A trusts C;
3. Adding a trust chain from A to B, revoke one in the middle, query if A trusts B;
