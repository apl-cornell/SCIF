function bindClosure(
  Closure memory c,
  uint256[] memory ks,
  bytes[] memory vals
) internal pure returns (Closure memory) {
  // Partial application of a closure value: fill some of its open
  // slots and hand back the narrower closure, leaving `c` intact.
  Closure memory f = _copyClosure(c);
  _bindSlots(f, ks, vals);
  return f;
}
