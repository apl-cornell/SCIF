var LockOracle = artifacts.require("LockOracle");
var TrustOracle = artifacts.require("TrustOracle");

module.exports = function (deployer) {
    deployer.deploy(LockOracle);
    deployer.deploy(TrustOracle);
};
