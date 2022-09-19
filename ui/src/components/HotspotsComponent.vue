<template>
  <div class="container">
    <div class="row">
      <div class="col-3">
        <div class="card">
          <div class="card-header">
            Controls
          </div>
          <div class="card-body">
            <div v-if="currentHotspot()">
              <div class="input-group mb-3 form-floating" v-if="currentHotspot().enabled">
                <input v-model="reportInterval" type="number" class="form-control" id="reportInterval" aria-describedby="btnReportInterval">
                <label for="reportInterval">Report interval ({{ currentHotspot().name }})</label>
                <button @click="setReportInterval" class="btn btn-outline-secondary" type="button" id="btnReportInterval">Apply</button>
              </div>
            </div>
            <div v-else>
              <p>please select a hotspot</p>
            </div>
            <div class="input-group mb-3 form-floating">
              <input v-model="reportIntervalAll" type="number" class="form-control" id="reportIntervalAll" aria-describedby="btnReportIntervalAll">
              <label for="reportIntervalAll">Report interval (all)</label>
              <button @click="setReportIntervalAll" class="btn btn-outline-secondary" type="button" id="btnReportIntervalAll">Apply</button>
            </div>
            <div class="input-group mb-3 form-floating">
              <input v-model="minBeacons" type="number" class="form-control" id="minBeacons" aria-describedby="btnMinBeacons">
              <label for="minBeacons">Minimum required beacons</label>
              <button @click="setMinBeacons" class="btn btn-outline-secondary" type="button" id="btnMinBeacons">Apply</button>
            </div>
            <div class="input-group mb-3 form-floating">
              <input v-model="minWitness" type="number" class="form-control" id="minWitness" aria-describedby="btnMinWitness">
              <label for="minWitness">Minimum required witnesses</label>
              <button @click="setMinWitness" class="btn btn-outline-secondary" type="button" id="btnMinWitness">Apply</button>
            </div>
            <div class="input-group mb-3 form-floating">
              <input v-model="epochSeconds" type="number" class="form-control" id="epochSeconds" aria-describedby="btnEpochSeconds">
              <label for="minWitness">Epoch (seconds)</label>
              <button @click="setEpochSeconds" class="btn btn-outline-secondary" type="button" id="btnEpochSeconds">Apply</button>
            </div>
          </div>
        </div>
      </div>
      <div class="col-3">
        <div class="card">
          <div class="card-header">
            Hotspots
          </div>
          <div class="card-body">
            <form>
              <div class="input-group input-group-sm mb-3">
                <input type="text" class="form-control" v-model="name" placeholder="Name" aria-label="Name">
              </div>
              <div class="input-group input-group-sm mb-3">
                <input type="password" class="form-control" v-model="key" placeholder="Key" aria-label="Key">
              </div>
              <button class="btn btn-outline-success me-2" type="button" @click="hotspotsPlus">Add</button>
            </form>
          </div>
          <ol class="list-group border-0">
            <li class="list-group-item d-flex justify-content-between align-items-start border-0"
                v-for="hotspot in hotspots"
                v-bind:key="hotspot.id"
                @click="setHotspotId(hotspot.id)"
            >
              <div class="ms-2 me-auto">
                <div class="fw-bold">
                  <a href="" v-on:click.prevent="setHotspotId(hotspot.id)">{{ hotspot.name }}</a>
                </div>
                {{ hotspot.accountId }}
              </div>
              <button v-if="hotspot.enabled" type="button" class="btn btn-outline-success" @click="hostspotDisable(hotspot.id)">on</button>
              <button v-else type="button" class="btn btn-outline-danger" @click="hostspotEnable(hotspot.id)">off</button>
            </li>
          </ol>
        </div>
      </div>
      <div class="col-6">
        <div class="card">
          <BeaconChartComponent :hotspotId="currentHotspotId"/>
        </div>
        <div class="row">
          <div class="col">
            <div v-if="currentHotspot()" class="card">
              <h5 class="card-title">{{ currentHotspot().name }}</h5>
              <p class="card-text">
                Paid accounts: {{ currentHotspot().paidAccounts }}
              </p>
              <div v-if="currentHotspot().runData">
                <p class="card-text">
                  Started: {{ secondsToDate(currentHotspot().runData.startSeconds) }}
                  <br>
                  Running for: {{ currentHotspot().runData.runSeconds }}s
                  <br>
                  Beaconed: {{ currentHotspot().runData.reportCount }} times
                  <br>
                  Reporting every: {{ currentHotspot().runData.intervalMs }}ms
                </p>
              </div>
              <div v-else>
                Hotspot not running
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="row" v-if="currentHotspot()">
      <div class="col">
      <div class="card">
        <div class="card-header">
          On ledger Transactions for {{ currentHotspot().name }} (beacons and witness reports)
        </div>
        <div class="card-body">
          <table class="table table-striped table-hover border-1">
            <thead>
            <tr>
              <th scope="col">Id</th>
              <th scope="col">Timestamp</th>
              <th scope="col">Node</th>
              <th scope="col">Status</th>
            </tr>
            </thead>
            <tbody>
            <tr
                v-for="transaction in transactions"
                v-bind:key="transaction.transaction_id"
                >
              <td>{{ transaction.transaction_id}}</td>
              <td>{{ dateFromTimestamp(transaction.consensus_timestamp) }}</td>
              <td>{{ transaction.node }}</td>
              <td>{{ transaction.result }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
      </div>
    </div>
    <div class="row" v-if="currentHotspot()">
      <div class="col">
        <div class="card">
          <div class="card-header">
            Token receipts for {{ currentHotspot().name }}
          </div>
          <div class="card-body">
            <table class="table table-striped table-hover border-1">
              <thead>
              <tr>
                <th scope="col">Id</th>
                <th scope="col">Timestamp</th>
                <th scope="col">Status</th>
                <th scope="col">Token Id</th>
                <th scope="col">Amount</th>
              </tr>
              </thead>
              <tbody>
              <tr
                  v-for="transaction in transfers"
                  v-bind:key="transaction.transaction_id"
              >
                <td>{{ transaction.transaction_id}}</td>
                <td>{{ transaction.consensus_timestamp }}</td>
                <td>{{ transaction.result }}</td>
                <td> {{ transaction.token_id }}</td>
                <td> {{ transaction.amount }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import BeaconChartComponent from "@/components/BeaconChartComponent";
import {
  apiAddHotspot,
  apiSetReportInterval,
  apiSetReportIntervalAll,
  apiDisableHotspot,
  apiEnableHotspot, apiSetEpochSeconds, apiSetMinWitness, apiSetMinBeacons
} from "@/service/hotspots";
import {mirrorGetReports, tokenTransfers} from "@/service/mirror";

export default {
  name: 'HotspotsComponent',
  components: {
    BeaconChartComponent
  },
  props: [
    'hotspots'
  ],
  data() {
    return {
      beaconReports: {},
      currentHotspotId: 0,
      name: '',
      key: '',
      reportInterval : 10000,
      reportIntervalAll: 10000,
      minBeacons: 2,
      minWitness: 1,
      epochSeconds: 10,
      transactions: [],
      interval: null,
      transfers: []
    };
  },
  async mounted() {
    this.interval = setInterval(() => {
      this.getTransactions();
    }, 2000);
  },
  beforeUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },
  methods: {
    dateFromTimestamp(timestamp) {
      const timestampParts = timestamp.split(".");
      return new Date(timestampParts[0] * 1000).toLocaleString();
    },
    async getTransactions() {
      const data = await mirrorGetReports(this.currentHotspot().accountId);
      this.transactions = data.transactions;

      const transferResponse = await tokenTransfers(this.currentHotspot().accountId);
      const reducedResponse = [];
      transferResponse.transactions.forEach(transaction => {
        const oneTransfer = {};
        oneTransfer.consensus_timestamp = this.dateFromTimestamp(transaction.consensus_timestamp);
        oneTransfer.transaction_id = transaction.transaction_id;
        oneTransfer.result = transaction.result;
        const tokenTransfers = transaction.token_transfers;
        tokenTransfers.forEach(transfer => {
          if (transfer.account == this.currentHotspot().accountId) {
            oneTransfer.token_id = transfer.token_id;
            oneTransfer.amount = transfer.amount;
          }
        });
        reducedResponse.push(oneTransfer);
      });

      this.transfers = reducedResponse;
    },
    setHotspotId(id) {
      console.log(`Setting id to ${id}`);
      this.currentHotspotId = id;
      this.transactions = [];
    },
    hostspotEnable(id) {
      apiEnableHotspot(id);
    },
    hostspotDisable(id) {
      apiDisableHotspot(id);
    },
    hotspotsPlus() {
      apiAddHotspot(this.name, this.key);
    },
    secondsToDate(seconds) {
      return new Date(seconds * 1000).toLocaleString();
    },
    currentHotspot() {
      // return this.hotspots.at(this.currentHotspotId);
      const object = this.hotspots.find(el => el.id === this.currentHotspotId);
      return object;
    },
    async setReportInterval() {
      await apiSetReportInterval(this.currentHotspotId, this.reportInterval);
    },
    async setReportIntervalAll() {
      await apiSetReportIntervalAll(this.reportIntervalAll);
    },
    async setMinBeacons() {
      await apiSetMinBeacons(this.beaconReports);
    },
    async setMinWitness() {
      await apiSetMinWitness(this.minWitness);
    },
    async setEpochSeconds() {
      await apiSetEpochSeconds(this.epochSeconds);
    },
    concatAccountAndtoken(tokenTransfer) {
      return `${tokenTransfer.token_id}-${tokenTransfer.account}`;
    }

  },
  computed: {
  },
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
</style>
