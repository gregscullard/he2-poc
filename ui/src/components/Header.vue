<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light" >
    <div class="container-fluid">
      <a class="navbar-brand" href="https://www.hedera.com">
        <img src="../assets/logo.svg" alt="Hedera" width="30" height="24">
        He2 Proof Of Concept
      </a>
    </div>
    <div class="container-fluid justify-content-end">
      <button class="btn btn-outline-success me-2" type="button" @click="getHotspotsList">refresh</button>
    </div>
  </nav>
  <div class="container">
    <HotspotsComponent v-bind:hotspots="hotspots"/>
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<script>
import HotspotsComponent from "./HotspotsComponent"
import {apiGetHotspots} from "../service/hotspots"

export default {
  name: "HeaderComponent",
  components: {
    HotspotsComponent
  },
  data: function() {
    return {
      hotspots: [],
      interval: null
    };
  },
  async created() {
    const newHotspots = await apiGetHotspots();
    this.hotspots = newHotspots.hotspots;

    this.interval = setInterval(async () => {
      const newHotspots = await apiGetHotspots();
      this.hotspots = newHotspots.hotspots;
    }, 1000);
  },
  beforeUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },
  methods: {
    async getHotspotsList() {
      const newHotspots = await apiGetHotspots();
      this.hotspots = newHotspots.hotspots;
    },
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
