#!/usr/bin/env sh
set -euo pipefail
source env.sh
main(){
    clear
    init_genesis
    init_blockchain
    echo "Init env datadir: '$datadir'; etherbase: '$etherbase' successfully"
}

main
