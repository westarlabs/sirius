# Getting started

## Required
+ Java version >= "1.8.0"
+ Gradle version >= "4.10.x"
+ Docker version >= 18 for testing enviroment of ethereum

## Run ethereum node
```
>./scripts/docker.sh run --dev.period 10
```

## Prepare ethereum accounts

Create three new accounts without password for testing. 
The first one for hub, the second one for alice, the last one for bob.
```
>touch /tmp/geth_data/pass; 
>for i in {1..3};do ./scripts/docker.sh geth account new --password /tmp/geth_data/pass;done
```
Then transfer some testing eth from coinbase to these new accounts
```
>./scripts/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[1],value:web3.toWei(5000,"ether")})' attach
>./scripts/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[2],value:web3.toWei(5000,"ether")})' attach
>./scripts/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[3],value:web3.toWei(5000,"ether")})' attach
```

## Run hub service
1. Prepare the hub configure
    ```
    mkdir -p ~/.sirius/hub;touch ~/.sirius/hub/hub.conf
    ```
2. Edit ~/.sirius/hub/hub.conf

    **NOTE: Replace those values being commented**

    ```
    accountIDOrAddress= // THE_HUB_ACCOUNT_CREATED_BEFORE
    blocksPerEon=32
    ownerKeystorePassword=
    connector=chain\:ethereum\:ws\://127.0.0.1\:8546
    rpcBind=0.0.0.0\:7985
    ownerKeystore=/tmp/geth_data/keystore/
    network=DEV
    ```
3. Start hub service
    ```
    ./gradlew sirius-hub:run
    ```

## Run wallet

1. Compile wallet
    ```
    >./gradlew fatjar
    ```

2. Create configure for Alice
    ```
    >mkdir -p ~/.sirius/alice; cp sirius-wallet/src/main/resources/conf.properties ~/.sirius/alice
    ```
3. Replace the value of "contract_addr" in ~/.sirius/alice/conf.properties, which can be found in  ~/.sirius/hub/hub.conf
4. Change keystore file to alice, which under the directory /tmp/geth_data/keystore
5. Start the REPL wallet for "alice"
    ```
    >java -jar sirius-wallet/build/libs/sirius-wallet-all.jar alice
    >alice
    ```
6. Register users
    ```
	alice>wallet reg
    ```
	Register bob repeatedly by step 2 to 5
	
7. Deposit to hub
    Deposit 1000 coin
	```
	>wallet deposit value=1000
	```
	The response display
	```
	alice>Mar 13, 2019 12:04:22 PM org.starcoin.sirius.wallet.core.blockchain.BlockChain handleTransaction
	INFO: Deposit:{"address":"0x5b65ad1e0ef2911bee87a27f8d7badf89c0683ea","amount":"0x03e8"}
	```
	After several blocks, check local balance
	```	
	hub balance is 1000
	withdrawal coin is 0
	chain balance is 4999999999999987386487
	```
	
8. transfer money in hub 
    transfer 100 coin from Alice to Bob:
    ```
	alice>wallet new_transfer addr=${ADDRESS_BOB} value=100
	```
	**NOTE: Get the binary addr from `wallet account` command**

	In the REPL of bob will display:
	**NOTE: Do not need to run**
	```
	bob>INFO: recieve new transfer from 0xf2606e81b3ad0c28c3dddb504a9533e150a3ff3csucc: true
	```

More operation please see [sirius-wallet-doc](../sirius-wallet/README.md)
