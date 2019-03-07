# Sirius Hub

## run hub

1. Start eth local node

```bash
./sirius-chainconnector/scripts/docker.sh run --dev.period 2
```
        
2. Start hub node

```bash
./gradlew sirius-hub:run
```
       
       
## Note

1. Default dataDir is ~/.sirius/hub
