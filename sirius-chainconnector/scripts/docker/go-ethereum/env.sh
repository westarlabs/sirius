#!/usr/bin/env sh
set -euo pipefail

datadir=./geth_data
etherbase_passwd="starcoinmakeworldbetter"
new_account(){
    echo  "$1"> ./pass
    account=$(geth --verbosity 0 account new --datadir $datadir --password pass |awk -F'[{}]' '{print $2}'|sed -n "1p")
    rm -f ./pass
    echo $account
}

get_account(){
    echo $(geth --verbosity 0  account list --datadir $datadir|awk -F'[{}]' '{print $2}'|sed -n "$1p")
}

clear(){
    rm -rf $datadir
}

init_genesis(){
    etherbase=$(new_account $etherbase_passwd)
    sed  "s/ETHERBASE/${etherbase}/" genesis.json.template > genesis.json
}

init_blockchain(){
    geth --datadir $datadir init genesis.json
}
