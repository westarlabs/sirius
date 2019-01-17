# Build Geth in a stock Go builder container
FROM golang:1.11-alpine as builder
RUN apk add --no-cache make gcc musl-dev linux-headers
ARG DISTRO_NAME=go-ethereum
ARG VERSION=1.8.21

RUN set -x \
    && cd / \
    && wget https://github.com/ethereum/go-ethereum/archive/v$VERSION.tar.gz \
    && tar -xzvf /v$VERSION.tar.gz \
    && cd /$DISTRO_NAME-$VERSION && make geth 

# Pull Geth into a second stage deploy alpine container
FROM alpine:latest
ARG DISTRO_NAME=go-ethereum
ARG VERSION=1.8.21
RUN apk add --no-cache ca-certificates
COPY --from=builder /$DISTRO_NAME-$VERSION/build/bin/geth /usr/local/bin/
RUN mkdir /ethereum
#COPY env.sh init.sh start.sh genesis.json.template /ethereum/

EXPOSE 8545 8546 30303 30303/udp
WORKDIR /ethereum
CMD ./start.sh
