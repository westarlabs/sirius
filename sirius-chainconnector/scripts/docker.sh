#!/usr/bin/env bash

usage(){
    echo "Usage $(basename $0) [clean, build, run, start, stop, logs, attach, geth]"
}


if [ $# -lt 1 ]; then
    usage;
fi

case $"$1" in
    clean)
	docker rm -f $(docker ps -a |grep go-ethereum|awk '{print $1}')
        ;;
    build)
	docker build  -f $(dirname $0)/docker/go-ethereum/Dockerfile -t starcoin/go-etherum $(dirname $0)/docker/go-ethereum/
	;;
    run)
	docker run -d --name go-ethereum -p 127.0.0.1:8545:8545 -p 30303:30303 starcoin/go-etherum 
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
	geth --datadir /ethereum/geth_data $@
esac
