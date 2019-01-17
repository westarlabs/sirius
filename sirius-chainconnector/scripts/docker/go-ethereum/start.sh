#!/usr/bin/env sh
set -euo pipefail
source init.sh
geth --rpcapi personal,web3,eth,miner,txpool --rpc  --rpcaddr 0.0.0.0   --cache=256 --datadir ./geth_data --etherbase $etherbase
