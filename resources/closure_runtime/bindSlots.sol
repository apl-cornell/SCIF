function _bindSlots(
  Closure memory c,
  uint256[] memory ks,
  bytes[] memory vals
) internal pure {
  uint256 unboundCount = 0;
  uint256 ki = 0;
  uint256 arity = c.dynArgs.length;
  bytes memory head = c.head;
  for (uint256 i = 0; i < arity && ki < ks.length; i++) {
    if (c.boundMask & (1 << i) == 0) {
      if (unboundCount == ks[ki]) {
        if (c.dynMask & (1 << i) == 0) {
          bytes memory src = vals[ki];
          assert(src.length == 32);
          assembly {
            mstore(
              add(add(head, 32), add(4, mul(i, 32))),
              mload(add(src, 32))
            )
          }
        } else {
          bytes memory enc = vals[ki];
          assert(enc.length >= 32);
          uint256 len = enc.length - 32;
          bytes memory stripped = new bytes(len);
          assembly {
            let srcPtr := add(enc, 64)
            let dstPtr := add(stripped, 32)
            for { let j := 0 } lt(j, len) { j := add(j, 32) } {
              mstore(add(dstPtr, j), mload(add(srcPtr, j)))
            }
          }
          c.dynArgs[i] = stripped;
        }
        c.boundMask |= (1 << i);
        ki++;
      }
      unboundCount++;
    }
  }
  assert(ki == ks.length);
}
