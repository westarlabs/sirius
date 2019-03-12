# Getting started

## Setup ethereum

We run a local ethereum development node for test.
please ensure docker in your computer , execute such code in sirius dir.

#### run ethereum node
```
cd sirius-chainconnector/scripts
sh docker.sh run --dev.period 10
```

#### create eth account

Find ethereum node docker id by

```
docker ps 
```

Find container id of image with name fikgol/starcoin-goethereum,then create account by 

```
docker exec -it {container_id} geth account new --keystore /tmp/geth_data/keystore

```

You need execute this command three times at least to create three new account.
Then connect to eth in docker by this command:
```
geth attach http://localhost:8545 
```
Then transfer some eth to these account by this command:
```
eth.sendTransaction({from:eth.coinbase, to:eth.accounts[1], value: web3.toWei(5000, "ether")}) 
```
Note: this only transfer to the first account you created.You need execute this command several times to complete transfer.

## Run hub node
1. create home dir ~/.sirius/hub and create hub.conf,copy this configuration to hub.conf
```
accountIDOrAddress=
blocksPerEon=32
ownerKeystorePassword=
connector=chain\:ethereum\:ws\://127.0.0.1\:8546
rpcBind=0.0.0.0\:7985
ownerKeystore=/tmp/geth_data/keystore/
network=DEV
```
Use one of the accounts address which you created in previous step to accountIDOrAddress.Then execute:
```
./gradlew sirius-hub:run
```

then hub will create config dir in ~/.sirius/hub and auto deploy smart-contract, you can find contract addr in hub.conf file.

## run wallet

1. Compile wallet

```
>gradle fatjar
```

2. get contract addr in ~/.sirius/hub/hub.conf
    

3. copy sirius-wallet/src/main/resources/conf.properties to dir ~/.sirius/alice. Change contract_addr to the dir you get in hub.conf.
    

4. change keystore to the eth account keystore ,you could find them in /tmp/geth_data/keystore. at the same time ,change password to the password which you input when create new eth account.Configuration should like:

```
hub_addr=localhost:7985
chain_addr=ws://127.0.0.1:8546
contract_addr=0x2bb5bde59f025535640b3bfd8032626e6d2e7855 
key_store=/tmp/geth_data/keystore/UTC--2019-03-12T07-35-32.014489400Z--8cea30258d425e0fea34ace11afc7e34142f2411
password=abcd
```

5. Run wallet for "alice" by :

```
>java -jar build/libs/sirius-wallet-all.jar alice

>alice
```

6. register user

```
alice> wallet reg
```

then you get response from hub which begin with 

```
Update.....
```

7. transfer money from coinbase to accounts you create. Repeat step 3-6, while keep keystore to use the first keystore and password need be blank. Transfer on chain 
```
wallet chain_transfer addr=Oxf10e422253061c6a2e9fec1219558c862c6b1b71 value=20000000000
```
you need change addr and value.And transfer money to both of accounts which you created.

8. deposit to hub
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

9. transfer money in hub 
transfer 100 coin from Bob to Alice:
```
bob>wallet new_transfer addr=36709418f1cbec9774e60f51d8b6c368160755eb value=100
transaction hash is :8230aaa7a2f267f791aa335bd3a8428a transaction will be used in sec 7
```
Note: get the binary addr from `wallet account` command

In the REPL of alice will display:	   
```
// Note: do not need to run
	   
alice>recieve new transfer from e1bed6be7a42b46e8a0ff3998612af5b4f0cbca1succ: true
get hub sign
start change eon
finish change eon
start change eon
finish change eon
```

