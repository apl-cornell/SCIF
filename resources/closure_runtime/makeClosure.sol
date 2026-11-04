function makeClosure(
  address addr,
  bytes4 selector,
  uint256 arity,
  uint256 dynMask,
  uint256[] memory ks,
  bytes[] memory vals
) internal pure returns (Closure memory) {
  assert(ks.length == vals.length);
  bytes memory head = new bytes(4 + (arity + 1) * 32);
  head[0] = selector[0];
  head[1] = selector[1];
  head[2] = selector[2];
  head[3] = selector[3];
  Closure memory c = Closure(addr, dynMask, 0, head, new bytes[](arity));
  _bindSlots(c, ks, vals);
  return c;
}
