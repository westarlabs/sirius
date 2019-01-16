#!/usr/bin/env bash
set -euo pipefail

datadir=./geth_data
init_account_passwd="starcoinmakeworldbetter"


new_account(){
    echo -e "$init_account_passwd"> ./pass
    geth --verbosity 0 account new --datadir $datadir --password pass 1>/dev/null
    rm -f ./pass
}

get_account(){
    echo $(geth --verbosity 0  account list --datadir $datadir|awk -F'[{}]' '{print $2}'|head -n 1)
}

clear(){
    rm -rf $datadir
}

init_genesis(){
    init_account=$(get_account)
    sed  "s/INIT_ACCOUNT/${init_account}/" genesis.json.template > genesis.json
}

init_blockchain(){
    geth --datadir $datadir init genesis.json
}

init_account=$(get_account)

echo "env datadir: '$datadir'; etherbase: '$init_account'"
