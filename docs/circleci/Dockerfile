FROM circleci/openjdk:8-jdk

USER root

RUN curl -L https://github.com/ethereum/solidity/releases/download/v0.5.5/solc-static-linux -o /usr/bin/solc \
 && chmod +x /usr/bin/solc

USER circleci