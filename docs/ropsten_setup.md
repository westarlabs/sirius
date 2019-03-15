1、regist a project from https://infura.io/， will genarate a url like ropsten.infura.io/v3/xxx, and the wss url like wss://ropsten.infura.io/ws/v3/xxx. for example wss://ropsten.infura.io/ws/v3/f1b03c0078284d7197c7f53f68accdee

2、Prepare the hub configure

​```
mkdir -p ~/.sirius/hub;touch ~/.sirius/hub/hub.conf
echo "ownerKeystore=${keystore_dir}" >~/.sirius/hub/hub.conf
echo "connector=chain:ethereum:${wss_path}" >>~/.sirius/hub/hub.conf
echo "accountIDOrAddress=${owner_addr}" >>~/.sirius/hub/hub.conf
echo "ownerKeystorePassword=${owner_pwd}" >>~/.sirius/hub/hub.conf
​```