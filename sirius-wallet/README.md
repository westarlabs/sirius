# Sirius wallet
Starcoin offical wallet

## Required
	+ Java version >= "1.8.0"
	+ Gradle >= "4.10.x"
	+ Start sirius-hub
	  See: docs in sirius-hub/README.md
	
## Setting up wallet
	1. Compile wallet
	   ```
	   >gradle fatjar
	   ```
	2. Edit configure: sirius-wallet/conf.properties
	   Note: Information of hub see ~/.sirius/hub/hub.conf

	3. Run wallet for "alice"
	   ```
	   >java -jar build/libs/sirius-wallet-all.jar alice
	   >alice
	   ```

## Begin to use
	Assume Bob and Alice need to do some mirco pay for each other.

	1. Create a REPL for Bob and Alice
	   ```
	   >java -jar $lib_path/starcoind-1.0-SNAPSHOT-all.jar Bob
	   bob>
       ```
	   Same as Alice

	
	2. Regisger account in hub
	   ```
	   bob>wallet reg
	   ```
	
	3. Display your hub account
	   ```
	   bob>wallet account
	   {
	     "address": {
		 "address": "4b7WvnpCtG6KD/OZhhKvW08MvKE="
		 },
		 "update": {
		 "eon": 9
		 },
		 "publicKey": "A66W2Fny0t5oiqFoIab4FX5DZhJW+cg+ulsvuGTTIj4o"
	   }
	   e1bed6be7a42b46e8a0ff3998612af5b4f0cbca1  //note: encoded address for transfer
       ```

	4. Deposit some coin to hub and check it 
		Deposit 1000 coin:
		```
		>wallet deposit value=1000 
		succ: true
		```
		Check local balance:
		```
		bob>wallet lb
			hub balance is 1000
			withdrawal coin is 0
			chain balance is 0
		```
		
	5. Transfer coin 
	
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
		
	6. Withdraw coin
	   withdraw 100 coin:
	   ```
	   bob>wallet wd value=100
	   succ: true
	   ```
	   check balance again:
	   
	   ```
	   bob>wallet lb
       hub balance is 800
       withdrawal coin is 0
       chain balance is 100
	   ```
	   
	7. Malicious mode and challenge
		After deposit
		wallet cheat flag =0|1|2|3
		flag=0 means steal deposit
		flag=1 means steal withdrawal
		flag=2 means steal transfer
		flag=3 means steal iou
		
		then transfer to others
		after steal (0|1|2), wallet will open balance challenge automatically.
		
		after steal (3), you need open transfer delivery challenge.
		```		
		bob>wallet otdc txh=09b59d6526e7a77728d3d9f5e9216350
		```
		txh is offline transaction hash, when you open a offline transaction
		successfully, wallet will print transaction hash in terminal.
		
		
	8. Sync
	   When wallet disconnected from hub, wallet need to sync data from hub by this command
	   This comannd will sync user status from hub, such as balance/update/MPT tree path/withdraw.
		```
		bob>wallet sync
		```
		
	9. Manual mode
		If sometimes wallet lose event notify connections from hub, the wallet 
		won't recieve the offline transaction and hub signed update from hub.
		So you need recieve data through manual mode manually.
		
		Recieve offline transaction:
		```
		bob>wallet rt 
		```
		
		Recieve hub signed update:
		```
        bob>wallet rhs 
		```

