# He2 Proof of concept

## Prerequisites

* Java 14
* Node v16.17.0
* Hedera testnet (or mainnet/previewnet) account

## Back end architecture

For re-use, the back end is modular in its construction and is made up of the following modules:

* common: A common set of capabilities which include reading configuration files, secrets, etc...
* hotspot: The definition of a hotspot which is capable of generating reports
* oracle: The definition of an oracle which is capable of reading messages on a topic and processing them into token payments
* restapi: A REST API to support the UI (this is dependent on demo running below)
* hotspots-simulator: TBD, but this will just run a large number of hotspots for simulation purposes
* demo: Automatically starts 3 hotspots, an oracle and the rest api for demonstration purposes 

## Install and operate the Backend

The back end consists in hotspot simulators that generate beacon and witness reports, as well as an Oracle that processes the reports and issues tokens to hotspots meeting the beacon and witness criteria.

```shell
cd OracleAndHotspotSimulator
./gradlew build
cp .env.sample .env
cp config.yaml.sample config.yaml 
```

edit `.env`

* OPERATOR_ID= Hedera network account id (0.0.xxxxx)
* OPERATOR_KEY= Private key for the account id
* HEDERA_NETWORK=testnet (or mainnet or previewnet)

edit `.config.yaml`

* Setup api.apiKey to secure the write apis
* Choose a port for the back end server REST API (e.g 8080) 

## Setup data

### one shot, takes care of everything

```shell
cd OracleAndHotspotSimulator
./gradlew :common:initAll 
```

### one by one

```shell
cd OracleAndHotspotSimulator
./gradlew :common:initTopic 
./gradlew :common:initTreasury
./gradlew :common:initToken 
./gradlew :common:initHotspots 
```

## Running the demo

The demo sets up 3 hotspots, one oracle and the rest api

```shell
cd OracleAndHotspotSimulator
./gradlew :demo:run
```

## Install and operate the Front end

__Note: Running the front end depends on running the demo back end__

```shell
cd ui
yarn install
cp .env.sample .env
```

edit `.env`

* VUE_APP_MIRROR_END_POINT="" (leave as is for now)
* VUE_APP_SERVER_URL = "http://localhost:8080/api/v1" specify the host and port of the back end API
* VUE_APP_X_API_KEY = "" copy the API key from the backend setup

## Running the UI

```shell
yarn serve --port 8081
```

navigate to the url printed in your console (e.g.  http://localhost:8081/)
