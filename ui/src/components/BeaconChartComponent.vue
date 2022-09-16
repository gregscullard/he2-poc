<template>
    <Bar
        :chart-options="chartOptions"
        :chart-data="chartData"
        :chart-id="chartId"
        :dataset-id-key="datasetIdKey"
        :plugins="plugins"
        :css-classes="cssClasses"
        :styles="styles"
        :height=height
        responsive=true
    />
</template>

<script>
import { Bar } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale } from 'chart.js'
import {getHotspotBeaconReports} from "@/service/hotspots";

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

export default {
  name: "BeaconChartComponent",
  components: { Bar },
  props: {
    hotspotId: {
      type: Number,
      default: 0
    },
    chartId: {
      type: String,
      default: 'bar-chart'
    },
    datasetIdKey: {
      type: String,
      default: 'label'
    },
    width: {
      type: Number,
      default: 400
    },
    height: {
      type: Number,
      default: 200
    },
    cssClasses: {
      default: '',
      type: String
    },
    styles: {
      type: Object,
      default: () => {}
    },
    plugins: {
      type: Object,
      default: () => {}
    }
  },
  data() {
    return {
      interval: null,
      chartData: {
        labels: [],
        datasets: []
      },
      chartOptions: {
        responsive: true,
        scales: {
          x: {
            stacked: true,
            grid: {
              display: false
            },
          },
          y: {
            stacked: true
          }
        }
      }
    }
  },
  async mounted() {
    await this.hotspotBeaconReports();
    this.interval = setInterval(() => {
      this.hotspotBeaconReports();
    }, 5000);
  },
  beforeUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },
  methods: {
    async hotspotBeaconReports() {
      const newData = await getHotspotBeaconReports(this.hotspotId);

      let labels = [];
      let witnesses = [];
      let beacons = [];
      let beaconsPaid = [];
      const keys = Object.keys(newData.reports);
      for (let i = 0; i < keys.length; i++) {
        const report = newData.reports[keys[i]];

        labels.push(new Date(report.epochStartSeconds * 1000).toLocaleTimeString());
        witnesses.push(report.witnessCount);
        if (report.rewardPaid) {
          beacons.push(0);
          beaconsPaid.push(report.beaconCount);
        } else {
          beacons.push(report.beaconCount);
          beaconsPaid.push(0);
        }
      }

      this.chartData = {
        labels: labels,
        datasets: [
          {type: 'bar', label: '# of Beacon Reports', data: beacons, backgroundColor: 'rgba(54, 162, 235, 0.2)'},
          {type: 'bar', label: '# of Paid Beacon Reports', data: beaconsPaid, backgroundColor: 'rgba(140,19,229,0.2)'},
          {type: 'bar', label: '# of Witness Reports', data: witnesses, backgroundColor: 'rgba(16,220,74,0.2)'},
        ]
      };
    }
  },
  watch:{
    async hotspotId() {
      await this.hotspotBeaconReports();
    },
    async refresh() {
      await this.hotspotBeaconReports();
      console.log(`refreshing ${this.refresh}`);
    }
  }

}
</script>

<style scoped>

</style>