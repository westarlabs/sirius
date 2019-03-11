# Getting started

## Setup ethereum

We run a local ethereum development node for test.
please ensure docker in your computer ,execute such code in sirius dir.

#### run ethereum node
```
./scripts/docker.sh run --dev.period 10
```

#### create eth account

```
>./scripts/docker.sh geth account new

```

You need execute this command twice to create two new account.

## Run hub node

```
./gradlew sirius-hub:run
```

Then hub will create config dir in ~/.sirius/hub and auto deploy smart-contract, you can find contract addr in hub.conf file.

## Run wallet

1. Compile wallet

	```
	>./gradle fatjar
	```
2. Copy sirius-wallet/src/main/resources/conf.properties to dir ~/.starcoin/liq/alice. Change contract_addr to the dir you get in hub.conf.
3. Edit the configure
   + Get contract addr in ~/.sirius/hub/hub.conf
   + Change keystore file to the eth account keystore, you could find it in /tmp/geth_data/keystore/
   + Change the password which you inputed when created new eth account
4. Run wallet for "alice"

	```
	>java -jar build/libs/sirius-wallet-all.jar alice
	>alice
	```
5. Register user

	```
	alice> wallet reg
	```
	Then you get response from hub which begin with 

	```
	Update.....
	```
6. Transfer money from coinbase to the account you created. Repeat step 3-6, while keeping keystore to use the first keystore and password need be blank. Transfer on chain 
   ```
   wallet chain_transfer addr=Oxf10e422253061c6a2e9fec1219558c862c6b1b71 value=20000000000
   ```
   You need to change the addr and value. And transfer money to all of the accounts which you created.
7. Deposit to hub
   Deposit 1000 coin:
   ```
   >wallet deposit value=1000 
   succ: true
   ```
   After several blocks ,Check local balance:
   ```
   bob>wallet lb
   hub balance is 1000
   withdrawal coin is 0
   chain balance is 0
   ```
8. transfer money in hub 
   transfer 100 coin from Bob to Alice:
   ```
   bob>wallet new_transfer addr=36709418f1cbec9774e60f51d8b6c368160755eb value=100
   transaction hash is :8230aaa7a2f267f791aa335bd3a8428a transaction will be used in sec 7
   ```
   Note: get the binary addr from `wallet account` command
   In the REPL of alice will display:
   ```
   # Note: do not need to run
   alice>recieve new transfer from e1bed6be7a42b46e8a0ff3998612af5b4f0cbca1succ: true
   get hub sign
   start change eon
   finish change eon
   start change eon
   finish change eon
   ```
