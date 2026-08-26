#!/bin/bash
# Regenerate the Solidity under test from the SCIF sources, then run:
#   ./regen.sh && forge test
cd "$(dirname "$0")/../.."
./scif -c test/contracts/basic/IRouterClosureCompile.scif -o closure/foundry-run/src/full.sol
./scif -c test/contracts/basic/IRouterClosureCompile_IERC20.scif -o closure/foundry-run/src/IRouterClosureCompile_IERC20.sol
./scif -c closure/foundry-run/scif/DirectCall.scif  -o closure/foundry-run/src/direct.sol
./scif -c closure/foundry-run/scif/InvokeArgs.scif  -o closure/foundry-run/src/invokeargs.sol
./scif -c closure/foundry-run/scif/InvokeBinder.scif -o closure/foundry-run/src/invokebinder.sol
./scif -c closure/foundry-run/scif/DynArgs.scif     -o closure/foundry-run/src/dynargs.sol
./scif -c closure/foundry-run/scif/DynEndToEnd.scif -o closure/foundry-run/src/dynend.sol
./scif -c closure/foundry-run/scif/BindReuse.scif   -o closure/foundry-run/src/bindreuse.sol
