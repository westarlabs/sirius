# Getting started

## Required
+ Java version >= "1.8.0"
+ Gradle version >= "4.10.x"
+ Docker version >= 18 for testing enviroment of ethereum

## Run ethereum node
```
./scripts/docker.sh run --dev.period 10
```

## Prepare the configures
For quick testing, you can run the script to generate all the configures of the test enviroment.
```
./scripts/preview_demo_env_init.sh
```
	
## Run hub service
```
./gradlew sirius-hub:run
```

## Run wallet
	Start the REPL wallet

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
