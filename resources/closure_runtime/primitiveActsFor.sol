function primitiveActsFor(address a, address b) internal returns (bool) {
  // a => b: reflexive; the executing contract is top; any is bottom;
  // otherwise only if b has declared that a acts for it.
  return (a == b) || (a == address(this)) || (b == address(0)) || trusts(b, a);
}
