var userContract = artifacts.require("BaseContractCentralized");
var trustOracle = artifacts.require("TrustOracle");
var lockOracle = artifacts.require("LockOracle");

module.exports = function (deployer) {
    deployer.deploy(userContract, trustOracle.address, lockOracle.address);
};
