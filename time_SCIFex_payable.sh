#!/bin/sh

echo -n "Multi-DAO 1: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/DistributedBankEx.scif

echo -n "Multi-DAO 2: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/DistributedBankExA0.scif

echo -n "Multi-DAO*: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/DistributedBankExW0.scif

echo -n "Uniswap 1: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Holder.scif -i=SCIFex/sendImp/TokenEx.scif -i=SCIFex/sendImp/UniswapEx.scif

echo -n "Uniswap 2: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Holder.scif -i=SCIFex/sendImp/TokenExA0.scif -i=SCIFex/sendImp/UniswapEx.scif

echo -n "Uniswap*: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Holder.scif -i=SCIFex/sendImp/TokenExW0.scif -i=SCIFex/sendImp/UniswapEx.scif

echo -n "Town Crier: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/TownCrierEx.scif

echo -n "Town Crier 1*: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/TownCrierExW0.scif

echo -n "Town Crier 2*: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/TownCrierExW1.scif

echo -n "KV Store: " >&2;
time -f %U ./stc -t -i=SCIFex/map/Array.scif -i=SCIFex/map/MappingFunction.scif -i=SCIFex/map/Map.scif

echo -n "KV Store*: " >&2;
time -f %U ./stc -t -i=SCIFex/map/Array.scif -i=SCIFex/map/MappingFunction.scif -i=SCIFex/map/MapW0.scif

echo -n "Town Crier FullVer 1: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/Crypto.scif -i=./SCIFex/sendImp/TownCrierExFull.scif

echo -n "Town Crier FullVer 2*: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/Crypto.scif -i=./SCIFex/sendImp/TownCrierExFullW0.scif

echo -n "Town Crier FullVer 3*: " >&2;
time -f %U ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/Crypto.scif -i=./SCIFex/sendImp/TownCrierExFullW1.scif
