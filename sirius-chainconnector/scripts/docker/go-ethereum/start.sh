#!/usr/bin/env sh
set -euo pipefail
source init.sh
geth --rpcapi admin,personal,web3,eth,miner,txpool --rpc --rpcaddr 0.0.0.0 --mine --miner.threads=1 --cache=256 --datadir $datadir --etherbase $etherbase --networkid 42  --targetgaslimit 100000000
