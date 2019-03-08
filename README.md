# Sirius - a blockchain layer 2 protocol implementation

Welcome to the Sirius source code repository! 

## About

Sirius is a blockchain layer 2 protocol implementation, currently supported Ethereum. Project is under heavy development, not production ready.

Inspiration of the name "Sirius" comes from the brightest star in the night sky.
## Features 

TODO 

### Prerequisites

1. Java(1.8)
2. solc (npm install -g solc)
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
