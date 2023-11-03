contract Dexible {
    function swap(uint amount, address tokenIn,
        address router, bytes routerData) external {
        if (IERC(tokenIn).
            transferFrom(msg.sender, address(this), amount)) {
            IERC(tokenIn).safeApprove(msg.sender, amount);
            (bool succ, ) = router.call(routerData);
            if (!succ) {
                revert("Failed to swap");
            }
        } else {
            revert("Insufficient balance");
        }
    }
}

