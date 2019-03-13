# Sirius - a blockchain layer 2 protocol implementation

[![CircleCI](https://circleci.com/gh/starcoinorg/sirius.svg?style=svg)](https://circleci.com/gh/starcoinorg/sirius)

Welcome to the Sirius source code repository! 

## About

Sirius is a blockchain layer 2 protocol implementation, currently supported Ethereum. Project is under heavy development, not production ready.

Inspiration of the name "Sirius" comes from the brightest star in the night sky.

## Overview

Sirius implement a 2nd-layer financial intermediary protocol secure against double-spending that guarantees users control of funds through leveraging a smart contract enabled (currently on Ethereum) decentralized blockchain ledger as a means of dispute resolution.

Two-party payment channels networks have been proposed for trust-free payments that do not exhaust the resources of the blockchain; however, they bear multiple fundamental limita-tions. Sirius is designed for secure N-party payment hubs with improved transaction utility and cheaper operational costs.

A hub is the operator server which can operate a verifier smart contract for handling the interactive between the blockchain and client.

Clients hold their private keys that control their identities and use them to sign off-chain transactions while communicating with hub. They also use the private keys to control their on-chain wallets.

The hub will periodically commits the off-chain ledger state as a proof to the blockchain. once a untrusted or malicious operation happen, the client can issue a challenge to hub and withdraw their funds.

## Features 

TODO 

## Architecture

```text
                             
                    +-------------------+
                    |       Chain       |
                    | +---------------+ |
            +-------> |Sirius Contract| <-------+
            |       | +---------------+ |       |
            |       +-------------------+       |
            |                                   |
            |                                   |
          Commit                                |
            |                                Challenge
            |                                   |
            |                                   |
+-----------+-------------+                     |
|      Sirius Hub         |            +--------+--------+
| +---------------------+ |            |      Alice      |
| | AugmentedMerkleTree | | Tx Witness |  +------------+ |
| +---------------------+ +<-----------+  |   Wallet   | |
+-------------------------+            |  |AMTree Proof| |
                    ^                  |  +------------+ |
                    |                  +-----------------+
                    |
               +----+-----+
               |   Bob    |
               |          |
               +----------+

```

### Prerequisites

1. Java(1.8)
2. solc (0.5.1) [installing-solidity](https://solidity.readthedocs.io/en/v0.5.1/installing-solidity.html)
2. Docker(for run and test)

#### Build

Go to project dir:

```bash
./gradlew build -x test
```

Build by Docker(Java and solc in docker image)

```bash
./gradleDocker build -x test
```

#### Test

Run unit tests

```bash
./gradlew unitTest
```

Run unit tests by Docker

```bash
./gradleDocker unitTest
```

Run integration tests(need Docker environment)

```bash
./gradlew integrationTest
```

#### Development Environment

TODO 

<a name="gettingstarted"></a>
## Getting Started
Instructions detailing the process of running and using the software can be found in [Getting Started](./docs/gettingstarted.md).
