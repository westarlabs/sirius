#!/usr/bin/env bash
set -euo pipefail
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"

start_ethereum(){
    $SCRIPTPATH/docker.sh run --dev.period 10
}

generate_etherum_test_accounts(){
    touch /tmp/geth_data/pass;
    for i in {1..3};do
	$SCRIPTPATH/docker.sh geth account new --password /tmp/geth_data/pass;
    done
    $SCRIPTPATH/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[1],value:web3.toWei(5000,"ether")})' attach
    $SCRIPTPATH/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[2],value:web3.toWei(5000,"ether")})' attach
    $SCRIPTPATH/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[3],value:web3.toWei(5000,"ether")})' attach

}
clean(){
    $SCRIPTPATH/docker.sh clean
    rm -rf ~/.sirius
}

generate_hub_conf(){
    mkdir -p ~/.sirius/hub
    touch ~/.sirius/hub/hub.conf
    echo "accountIDOrAddress=1" >~/.sirius/hub/hub.conf
    echo "ownerKeystore=/tmp/geth_data/keystore/" >>~/.sirius/hub/hub.conf
}

generate_wallet_accounts(){
    # Alice
    mkdir -p ~/.sirius/alice;
    echo -e "hubAddr=localhost:7985\nchainAddr=ws://127.0.0.1:8546\nkeyStore=/tmp/geth_data/keystore/\npassword=\naccountIDOrAddress=2">~/.sirius/alice/conf.properties

    # Bob
    mkdir -p ~/.sirius/bob;
    echo -e "hubAddr=localhost:7985\nchainAddr=ws://127.0.0.1:8546\nkeyStore=/tmp/geth_data/keystore/\npassword=\naccountIDOrAddress=3">~/.sirius/bob/conf.properties    
    
}

build_wallet(){
    $SCRIPTPATH/../gradlew fatjar
}

main(){
    clean
    start_ethereum
    generate_etherum_test_accounts
    generate_hub_conf
    generate_wallet_accounts
    build_wallet
}

main
