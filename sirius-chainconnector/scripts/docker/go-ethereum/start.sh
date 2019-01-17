#!/usr/bin/env sh
set -euo pipefail
source env.sh
if [[ -z $init_account ]];then
    echo "Run init.sh firstly"
    exit
fi
geth --rpcapi personal,web3,eth,miner,txpool --rpc  --rpcaddr 0.0.0.0 --mine --minerthreads=1  --cache=256 --datadir ./geth_data --etherbase $init_account 
