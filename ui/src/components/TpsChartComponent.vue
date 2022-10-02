<template>
    <Line
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
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  CategoryScale,
} from 'chart.js'
import {apiGetTpsReport} from "@/service/hotspots";

ChartJS.register(
    Title,
    Tooltip,
    Legend,
    LineElement,
    LinearScale,
    PointElement,
    CategoryScale
);

export default {
  name: "TpsChartComponent",
  components: { Line },
  props: {
    chartId: {
      type: String,
      default: 'tps-chart'
    },
    datasetIdKey: {
      type: String,
      default: 'label'
    },
    width: {
      type: Number,
      default: 100
    },
    height: {
      type: Number,
      default: 100
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
      default: () => {
      }
    }
  },
  data() {
    return {
      labels: [],
      interval: null,
      chartData: {
        labels: [],
        datasets: []
      },
      chartOptions: {
        responsive: true,
        scales: {
          x: {
            grid: {
              display: false
            },
            ticks: false,
          },
          y: {
          }
        },
        layout: {
          padding: 0
        },
        plugins: {
          legend: {
            display: true,
            labels: {
              boxWidth: 20,
            }
          },
        }
      }
    }
  },
  async mounted() {
    for (let i=60; i > 0; i--) {
      this.labels.push(i);
    }
    await this.tpsReport();
    this.interval = setInterval(() => {
      this.tpsReport();
    }, 1000);
  },
  beforeUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },
  methods: {
    async tpsReport() {
      const newData = await apiGetTpsReport();

      let witnesses = newData.witnessPerSecond;
      let reports = newData.reportsPerSecond;
      let payments = newData.paymentsPerSecond;

      this.chartData = {
        labels: this.labels,
        datasets: [
          { label: 'Reports/s', data: reports, borderColor: 'rgb(54,78,235)', backgroundColor: 'rgb(54,78,235)',tension: 0.5, pointRadius: 0, borderWidth: 1},
          { label: 'Witnesses/s', data: witnesses, borderColor: 'rgba(58,229,19,0.82)', backgroundColor: 'rgba(58,229,19,0.82)', tension: 0.5, pointRadius: 0, borderWidth: 1},
          { label: 'Payments/s', data: payments, borderColor: 'rgba(229,19,19,0.82)', backgroundColor: 'rgba(229,19,19,0.82)', tension: 0.5, pointRadius: 0, borderWidth: 1},
        ]
      };
    }
  }
}
</script>

<style scoped>

</style>