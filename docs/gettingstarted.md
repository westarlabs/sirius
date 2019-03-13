# Getting started

## Required
+ Java version >= "1.8.0"
+ Gradle version >= "4.10.x"
+ Docker version >= 18 for testing enviroment of ethereum

## Run ethereum node
```
./scripts/docker.sh run --dev.period 10
```

## Prepare ethereum accounts

Create three new accounts without password for testing. 
The first one for hub, the second one for alice, the last one for bob.
```
touch /tmp/geth_data/pass;
for i in {1..3};do ./scripts/docker.sh geth account new --password /tmp/geth_data/pass;done
```
Then transfer some testing eth from coinbase to these new accounts
```
./scripts/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[1],value:web3.toWei(5000,"ether")})' attach
./scripts/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[2],value:web3.toWei(5000,"ether")})' attach
./scripts/docker.sh geth --exec='eth.sendTransaction({from:eth.coinbase,to:eth.accounts[3],value:web3.toWei(5000,"ether")})' attach
```

## Run hub service
1. Prepare the hub configure
    ```
    mkdir -p ~/.sirius/hub;touch ~/.sirius/hub/hub.conf
	echo "accountIDOrAddress=1" >~/.sirius/hub/hub.conf
	echo "ownerKeystore=/tmp/geth_data/keystore/" >>~/.sirius/hub/hub.conf
	```
	
2. Start hub service
    ```
    ./gradlew sirius-hub:run
    ```

## Run wallet

1. Compile wallet
    ```
	./gradlew fatjar
    ```
2. Create configure

	For Alice
    ```
    mkdir -p ~/.sirius/alice;
    echo "hubAddr=localhost:7985\nchainAddr=ws://127.0.0.1:8546\nkeyStore=/tmp/geth_data/keystore/\npassword=\naccountIDOrAddress=2">~/.sirius/alice/conf.properties
    ```
	For Bob
    ```
    mkdir -p ~/.sirius/bob;
    echo "hubAddr=localhost:7985\nchainAddr=ws://127.0.0.1:8546\nkeyStore=/tmp/geth_data/keystore/\npassword=\naccountIDOrAddress=3">~/.sirius/bob/conf.properties
    ```
3. Start the REPL wallet

	For Alice
    ```
    java -jar sirius-wallet/build/libs/sirius-wallet-all.jar alice
    ```
	For Bob
    ```
    java -jar sirius-wallet/build/libs/sirius-wallet-all.jar bob
    ```
4. Register users

	For Alice
    ```
	alice>
	wallet reg
    ```
	
	For Bob
    ```
	bob>
	wallet reg
    ```
5. Deposit to hub

    Deposit 1000 coin by alice
	```
	alice>
	wallet deposit value=1000
	```
	The response display
	```
	alice>
	Mar 13, 2019 12:04:22 PM org.starcoin.sirius.wallet.core.blockchain.BlockChain handleTransaction
	INFO: Deposit:{"address":"0x5b65ad1e0ef2911bee87a27f8d7badf89c0683ea","amount":"0x03e8"}
	```
	After several blocks, check local balance by
	```
	>alice
	wallet lb
	```
	The response display
	```	
	hub balance is 1000
	withdrawal coin is 0
	chain balance is 4999999999999987386487
	```
6. Transfer funds

	Get the address of bob
	```
	./scripts/docker.sh geth --exec='eth.accounts[3]' attach
	```
	Transfer 100 coin from Alice to Bob
	
	**NOTE: Replace ADDRESS_BOB**
    ```
	alice>
	wallet new_transfer addr=${ADDRESS_BOB} value=100
	```
	The reponse display
	```
	bob>
	INFO: recieve new transfer from 0xf2606e81b3ad0c28c3dddb504a9533e150a3ff3csucc: true
	```
	
More operation please see [sirius-wallet-doc](../sirius-wallet/README.md)
