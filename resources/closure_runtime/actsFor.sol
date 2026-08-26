function actsFor(address[] memory a, address[] memory b) internal returns (bool) {
  if (a.length == 0) return true;
  if (b.length == 0) return false;
  for (uint256 i = 0; i < a.length; i++) {
    if (a[i] == address(this)) continue;
    bool success = false;
    for (uint256 j = 0; j < b.length; j++) {
      if (b[j] == address(0) || primitiveActsFor(a[i], b[j])) {
        success = true;
        break;
      }
    }
    if (!success) return false;
  }
  return true;
}
