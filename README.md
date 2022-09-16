# He2 Proof of concept

## Prerequisites

* Java 14
* Node v16.17.0

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
./gradlew initAll 
```

### one by one

```shell
cd OracleAndHotspotSimulator
./gradlew initTopic 
./gradlew initTreasury
./gradlew initToken 
./gradlew initHotspots 
```

## Running the back end 

```shell
cd OracleAndHotspotSimulator
./gradlew run 
```

## Install and operate the Front end

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
