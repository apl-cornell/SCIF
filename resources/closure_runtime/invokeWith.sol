function invokeWith(
  Closure memory c,
  uint256[] memory ks,
  bytes[] memory vals,
  address[] memory lExtBef
) internal returns (bool success, bytes memory result)
{
  // Invocation does not consume the closure: the arguments supplied at
  // the invoke site are bound into a copy, so the same closure value
  // can be invoked again, with different arguments.
  Closure memory f = _copyClosure(c);
  _bindSlots(f, ks, vals);
  return invoke(f, lExtBef);
}
