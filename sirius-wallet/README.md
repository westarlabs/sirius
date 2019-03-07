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
   
