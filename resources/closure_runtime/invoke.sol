function invoke(Closure memory c, address[] memory lExtBef)
  internal returns (bool success, bytes memory result)
{
  uint256 arity = c.dynArgs.length;
  assert(c.boundMask == (1 << arity) - 1);

  uint256 headSize = (arity + 1) * 32;
  uint256 tailOffset = headSize;
  bytes memory head = c.head;

  for (uint256 i = 0; i < arity; i++) {
    if (c.dynMask & (1 << i) != 0) {
      assembly {
        mstore(add(add(head, 32), add(4, mul(i, 32))), tailOffset)
      }
      tailOffset += c.dynArgs[i].length;
    }
  }

  assembly {
    mstore(add(add(head, 32), add(4, mul(arity, 32))), tailOffset)
  }

  bytes memory tail = new bytes(tailOffset - headSize);
  uint256 tailPos = 0;
  for (uint256 i = 0; i < arity; i++) {
    if (c.dynMask & (1 << i) != 0) {
      bytes memory src = c.dynArgs[i];
      uint256 len = src.length;
      assembly {
        let srcPtr := add(src, 32)
        let dstPtr := add(add(tail, 32), tailPos)
        for { let j := 0 } lt(j, len) { j := add(j, 32) } {
          mstore(add(dstPtr, j), mload(add(srcPtr, j)))
        }
      }
      tailPos += len;
    }
  }

  bytes memory lExtBefTail = new bytes(32 + lExtBef.length * 32);
  assembly { mstore(add(lExtBefTail, 32), mload(lExtBef)) }
  for (uint256 i = 0; i < lExtBef.length; i++) {
    assembly {
      mstore(
        add(add(lExtBefTail, 64), mul(i, 32)),
        mload(add(add(lExtBef, 32), mul(i, 32)))
      )
    }
  }

  (success, result) = c.addr.call(bytes.concat(head, tail, lExtBefTail));
}
