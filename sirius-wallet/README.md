# Sirius wallet
Starcoin offical wallet

## Required
	+ Java version >= "1.8.0"
	+ Gradle >= "4.10.x"
	+ Start sirius-hub
	  See: docs in sirius-hub/README.md
	
## Setting up wallet
	1. Compile wallet
	
	   ```bash
	   >./gradlew fatjar
	   ```
	   
	2. Edit configure: sirius-wallet/conf.properties
	   Note: Information of hub see ~/.sirius/hub/hub.conf

	3. Run wallet for "alice"
	
	   ```bash
	   >java -jar build/libs/sirius-wallet-all.jar alice
	   >alice
	   ```

## Begin to use
	Assume Bob and Alice need to do some mirco pay for each other.
	
	1. Regisger account in hub
	
	   ```bash
	   bob>wallet reg
	   ```
	
	2. Display your hub account
	
	   ```bash
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
	   e1bed6be7a42b46e8a0ff3998612af5b4f0cbca1  # Note: encoded address for transfer
       ```

	3. Deposit some coin to hub and check it 
		Deposit 1000 coin:
		
		```bash
		>wallet deposit value=1000 
		succ: true
		```
		
		Check local balance:
		
		```bash
		bob>wallet lb
			hub balance is 1000
			withdrawal coin is 0
			chain balance is 0
		```
		
	4. Transfer coin 
	
		transfer 100 coin from Bob to Alice:
		
		```bash
		bob>wallet new_transfer addr=36709418f1cbec9774e60f51d8b6c368160755eb value=100
		transaction hash is :8230aaa7a2f267f791aa335bd3a8428a transaction will be used in sec 7
		```
		
		Note: get the binary addr from `wallet account` command

        In the REPL of alice will display:
		
	   
	    ```bash
        # Note: do not need to run
	   
	    alice>recieve new transfer from e1bed6be7a42b46e8a0ff3998612af5b4f0cbca1succ: true
        get hub sign
        start change eon
        finish change eon
        start change eon
        finish change eon
	    ```
		
	5. Withdraw coin
	   withdraw 100 coin:
	   
	   ```bash
	   bob>wallet wd value=100
	   succ: true
	   ```
	   
	   check balance again:
	   
	   
	   ```bash
	   bob>wallet lb
       hub balance is 800
       withdrawal coin is 0
       chain balance is 100
	   ```
	   
	6. Malicious mode and challenge
		After deposit
		wallet cheat flag =0|1|2|3
		flag=0 means steal deposit
		flag=1 means steal withdrawal
		flag=2 means steal transfer
		flag=3 means steal iou
		
		then transfer to others
		after steal (0|1|2), wallet will open balance challenge automatically.
		
		after steal (3), you need open transfer delivery challenge.
		
		```bash	
		bob>wallet otdc txh=09b59d6526e7a77728d3d9f5e9216350
		```
		
		txh is offline transaction hash, when you open a offline transaction
		successfully, wallet will print transaction hash in terminal.
		
