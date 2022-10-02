<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light" >
    <div class="container-fluid">
      <a class="navbar-brand" href="https://www.hedera.com">
        <img src="../assets/logo.svg" alt="Hedera" width="30" height="24">
        He2 Proof Of Concept
      </a>
    </div>
    <div class="container-fluid justify-content-end">
      <TpsChartComponent />
    </div>
  </nav>
  <div class="container">
    <HotspotsComponent />
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<script>
import HotspotsComponent from "./HotspotsComponent"
import TpsChartComponent from "@/components/TpsChartComponent";

export default {
  name: "HeaderComponent",
  components: {
    TpsChartComponent,
    HotspotsComponent
  },
  data: function() {
    return {
      // interval: null
    };
  },
  async created() {
    // this.interval = setInterval(async () => {
    //   const newHotspots = await apiGetHotspots();
    //   this.hotspots = newHotspots.hotspots;
    // }, 1000);
  },
  beforeUnmount() {
    // if (this.interval) {
    //   clearInterval(this.interval);
    // }
  },
  methods: {
  },
};
</script>
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
