#!/usr/bin/env sh
set -euo pipefail
source init.sh
geth --rpcapi admin,personal,web3,eth,miner,txpool --rpc --rpcaddr 0.0.0.0 --dev --cache=256 --datadir $datadir --networkid 42  --targetgaslimit 100000000 --dev.period 1
