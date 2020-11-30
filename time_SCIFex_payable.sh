#!/bin/sh

time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/DistributedBankEx.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/DistributedBankExA0.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/DistributedBankExW0.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Holder.scif -i=SCIFex/sendImp/TokenEx.scif -i=SCIFex/sendImp/UniswapEx.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Holder.scif -i=SCIFex/sendImp/TokenExA0.scif -i=SCIFex/sendImp/UniswapEx.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Holder.scif -i=SCIFex/sendImp/TokenExW0.scif -i=SCIFex/sendImp/UniswapEx.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/TownCrierEx.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/TownCrierExW0.scif
time ./stc -t -i=SCIFex/sendImp/BaseContract.scif -i=SCIFex/sendImp/Callback.scif -i=SCIFex/sendImp/TownCrierExW1.scif
