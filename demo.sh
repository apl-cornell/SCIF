#!/bin/bash

case $1 in
    1)
        ./scif -c -i=demo/Wallet_insecure.scif
        ;;
    2)
        ./scif -c -i=demo/Wallet_lock.scif
        ;;
    3)
        ./scif -c -i=demo/Wallet_reorder.scif
        ;;
    *)
        echo "Wrong argument"
        ;;
esac
