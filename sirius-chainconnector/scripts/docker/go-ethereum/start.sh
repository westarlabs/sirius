#!/usr/bin/env sh
set -euo pipefail
source init.sh
geth --ws --rpcapi net,admin,personal,web3,eth,miner,txpool,debug --rpc --rpcaddr 0.0.0.0 --rpccorsdomain "https://remix.ethereum.org" --dev --dev.period 2 --cache=256 --datadir $datadir --networkid 42  --targetgaslimit 100000000 --vmdebug --verbosity 6 --debug --pprof --wsapi net,admin,personal,web3,eth,miner,txpool,debug --wsorigins="*" --wsaddr 0.0.0.0

