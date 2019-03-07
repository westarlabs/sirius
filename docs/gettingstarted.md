## setup eth
### setup eth by docker
please check docker in your computer , excecute such code in sirius dir.
#### setup eth
```
cd sirius-chainconnector/scripts
sh docker.sh run --dev.period 10
```
#### create eth account
find eth docker id,by
```
docker ps 
```
find container id of iamge with name fikgol/starcoin-goethereum,then create acount by 
```
docker exec -it {container_id} geth account new --keystore /tmp/geth_data/keystore

```
you need excecute this command twice to create two new account.

#### setup hub
you need run this command to setup hub
```
./gradlew sirius-hub:run
```
then hub will create config dir in ~/.sirius/hub ,you could change find contract addr in hub.conf file.

#### run wallet
	1. Compile wallet
	   ```
	   >gradle fatjar
	   ```
	2. get contract addr in ~/.sirius/hub/hub.conf
    
    3. copy sirius-wallet/src/main/resources/conf.properties to dir ~/.starcoin/liq/alice. Change contract_addr to the dir you get in hub.conf.
    
    4. change keystore to the eth account keystore ,you could find them in /tmp/geth_data/keystore. at the same time ,change password to the password which you input when create new eth account.
  
	5. Run wallet for "alice"
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
    9.transfer money in hub 
	
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

