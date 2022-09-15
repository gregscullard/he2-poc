<template>
  <Bar
      :chart-options="chartOptions"
      :chart-data="chartData"
      :chart-id="chartId"
      :dataset-id-key="datasetIdKey"
      :plugins="plugins"
      :css-classes="cssClasses"
      :styles="styles"
      :width="width"
      :height="height"
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
      default: 400
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
      chartData: {
        labels: [ 'January', 'February', 'March' ],
        datasets: [ { data: [40, 20, 12] } ]
      },
      chartOptions: {
        responsive: true
      }
    }
  },
  async mounted() {
    await this.hotspotBeaconReports();
  },
  methods: {
    async hotspotBeaconReports() {
      const newData = await getHotspotBeaconReports(this.hotspotId);

      let ordered = Object.keys(newData).sort().reduce(
          (obj, key) => {
            obj[key] = newData[key];
            return obj;
          },
          {}
      );

      let labels = Object.keys(ordered);
      let data = Object.values(ordered);
      if (labels.length > 50) {
        labels = labels.slice(-50);
        data = data.slice(-50);
      }

      for (let i=0; i < labels.length; i++) {
        labels[i] = new Date(labels[i] * 1000).toLocaleTimeString();
      }

      this.chartData = {
        labels: labels,
        datasets: [{label: '# of Beacon Reports', data: data}]
      };

      console.log(this.chartData);
    }
  }
}
</script>

<style scoped>

</style>