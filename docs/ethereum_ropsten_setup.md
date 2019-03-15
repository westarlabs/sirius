1. Regist a project in https://infura.io which will genarate a endpoint for ropsten
    network https://ropsten.infura.io/v3/${project_id}, then we can get the securiety
    websocket endpoint(wss) wss://ropsten.infura.io/v3/${project_id}
    
    You can use this ws endpoint we generated before for testing
    ```
    wss://ropsten.infura.io/ws/v3/f1b03c0078284d7197c7f53f68accdee
    ```
    
2. Prepare the hub configuration
    ```
    mkdir -p ~/.sirius/hub;touch ~/.sirius/hub/hub.conf
    echo "ownerKeystore=${keystore_dir}" >~/.sirius/hub/hub.conf
    echo "connector=chain:ethereum:${wss_endpoint}" >>~/.sirius/hub/hub.conf
    echo "accountIDOrAddress=${owner_addr}" >>~/.sirius/hub/hub.conf
    echo "ownerKeystorePassword=${owner_pwd}" >>~/.sirius/hub/hub.conf
    ```
