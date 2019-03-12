#!/usr/bin/env bash
usage(){
    echo "Usage $(basename $0) [clean, build, run, start, stop, logs, attach, geth]"
}


if [ $# -lt 1 ]; then
    usage;
fi

SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
source $SCRIPTPATH/docker/go-ethereum/env.sh
case $"$1" in
    clean)
	containers=$(docker ps -a |grep go-ethereum|awk '{print $1}')
	if [[ -n $containers ]];then
	    docker rm -f $containers
	fi
	if [[ -d $datadir ]] ;then
	    rm -rf $datadir
	fi
        ;;
    build)
	docker build  -f $(dirname $0)/docker/go-ethereum/Dockerfile -t starcoin/sirius:ethereum $(dirname $0)/docker/go-ethereum/
	;;
    rebuild)
	docker build --no-cache -f $(dirname $0)/docker/go-ethereum/Dockerfile -t starcoin/sirius:ethereum $(dirname $0)/docker/go-ethereum/
	;;
    run)
	shift 1
	docker run -d --name go-ethereum -p 127.0.0.1:8545:8545 -p 30303:30303 -p 8546:8546  -v /tmp/geth_data:/tmp/geth_data starcoin/sirius:ethereum $@
	while true; do
	    if [[ -d $datadir ]]; then
		break
	    fi
	    sleep 1
	    continue
	done
	;;
    logs)
	docker logs $(docker ps -a |grep go-ethereum|awk '{print $1}') -f
	;;
    stop)
	docker stop $(docker ps -a |grep go-ethereum|awk '{print $1}')
	;;
    start)
	docker start $(docker ps -a |grep go-ethereum|awk '{print $1}')
	;;
    attach)
	shift 1
	docker exec -it $(docker ps -a |grep go-ethereum|awk '{print $1}') $@
	;;
    geth)
	shift 1
	docker exec -it $(docker ps -a |grep go-ethereum|awk '{print $1}') \
	geth --datadir $datadir $@
esac
