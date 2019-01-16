#!/usr/bin/env sh
set -euo pipefail
source env.sh
main(){
    clear
    new_account
    init_genesis
    init_blockchain
    echo "Init env datadir: '$datadir'; etherbase: '$init_account' successfully"
}

main
