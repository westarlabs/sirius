#!/usr/bin/env sh
set -euo pipefail
source env.sh
main(){
    clear
    init_genesis
    init_blockchain
    alice_account=$(new_account alice_passwd)
    bob_account=$(new_account bob_passwd)
    echo "Init env datadir: '$datadir'; etherbase: '$etherbase'; alice: '$alice_account'; bob: '$bob_account' successfully"
}

main
