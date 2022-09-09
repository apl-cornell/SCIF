pragma solidity >=0.8.3;
import "./BaseContractCentralized.sol";
import "./Holder.sol";
contract Token is BaseContractCentralized {
    mapping(address => uint) balances;
    mapping(address => bool) isAdmin;
    constructor (address tO, address lO)
        BaseContractCentralized(tO, lO)
    {
    }
    function transfer(address frm, address to, uint amount)
        public
        returns (bool)
    {
        assert(!false);
        assert(ifTrust(address(this), msg.sender));
        address sender = msg.sender;
        if (((frm != sender) && (isAdmin[frm] != true))) {
            return false;
        }
        if ((balances[frm] < amount)) {
            return false;
        }
        balances[frm] = (balances[frm] - amount);
        balances[to] = (balances[to] + amount);
        Holder _frm = Holder(frm);
        Holder _to = Holder(to);
        assert(lock(address(this)));
        _frm.alertSend(to, amount);
        _to.alertReceive(frm, amount);
        assert(unlock(address(this)));
        return true;
    }
    function getBal(address user)
        public
        returns (uint)
    {
        assert(!ifLocked(address(this)));
        assert(true);
        return balances[user];
    }
}
