/* @notice                  Dynamically invoke the targeting contract, and trigger executation of cross chain tx on Ethereum side
    *  @param _toContract       The targeting contract that will be invoked by the Ethereum Cross Chain Manager contract
    *  @param _method           At which method will be invoked within the targeting contract
    *  @param _args             The parameter that will be passed into the targeting contract
    *  @param _fromContractAddr From chain smart contract address
    *  @param _fromChainId      Indicate from which chain current cross chain tx comes
    *  @return                  true or false
    */
    function _executeCrossChainTx(address _toContract, bytes memory _method, bytes memory _args, bytes memory _fromContractAddr, uint64 _fromChainId) internal returns (bool){
        // Ensure the targeting contract gonna be invoked is indeed a contract rather than a normal account address
        require(Utils.isContract(_toContract), "The passed in address is not a contract!");
        bytes memory returnData;
        bool success;

        // The returnData will be bytes32, the last byte must be 01;
        (success, returnData) =
            _toContract.call(
                abi.encodePacked(
                    bytes4(keccak256(abi.encodePacked(_method, "(bytes,bytes,uint64)"))),
                    abi.encode(_args, _fromContractAddr, _fromChainId)
                )
            );
        // keccak256('putCurEpochConPubKeyBytes(bytes)').slice(0, 10) == '0x41973cd9'
        // keccak256('f1121318093(bytes,bytes,uint64)').slice(0, 10) == '0x41973cd9'

        // Ensure the executation is successful
        require(success == true, "EthCrossChain call business contract failed");

        // Ensure the returned value is true
        require(returnData.length != 0, "No return value from business contract!");
        (bool res,) = ZeroCopySource.NextBool(returnData, 31);
        require(res == true, "EthCrossChain call business contract return is not true");

        return true;
    }