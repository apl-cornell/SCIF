function _copyClosure(Closure memory c) internal pure returns (Closure memory) {
  // Neither binding nor invoking may consume a closure value, and
  // _bindSlots writes through the reference it is given, so both go
  // through a copy. `head` is written into, hence a fresh array;
  // dynArgs entries are replaced rather than mutated, so copying the
  // array itself is enough.
  bytes[] memory dyn = new bytes[](c.dynArgs.length);
  for (uint256 i = 0; i < c.dynArgs.length; i++) {
    dyn[i] = c.dynArgs[i];
  }
  return Closure(c.addr, c.dynMask, c.boundMask, bytes.concat(c.head), dyn);
}
