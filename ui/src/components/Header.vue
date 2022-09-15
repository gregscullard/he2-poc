<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light" >
    <div class="container-fluid">
      <a class="navbar-brand" href="https://www.hedera.com">
        <img src="../assets/logo.svg" alt="Hedera" width="30" height="24">
        He2 Proof Of Concept
      </a>
    </div>
    <div class="container-fluid justify-content-end">
      <button class="btn btn-outline-success me-2" type="button" @click="hotspotsMinus">-</button>
      <button class="btn btn-outline-success me-2" type="button" @click="hotspotsPlus">+</button>
      <button class="btn btn-outline-success me-2" type="button" @click="getHotspotsList">refresh</button>
    </div>
    <div class="container">
    </div>
  </nav>
  <HotspotsComponent v-bind:hotspots="hotspots"/>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<script>
import HotspotsComponent from "./HotspotsComponent"
import {getHotspots, moreOrLessHotspots} from "../service/hotspots"

export default {
  name: "HeaderComponent",
  components: {
    HotspotsComponent
  },
  data: function() {
    return {
      hotspots: []
    };
  },
  async created() {
    const newHotspots = await getHotspots();
    this.hotspots = newHotspots.hotspots;

    // this.interval = setInterval(() => {
    //   this.getWalletIds();
    // }, 1000);
  },
  beforeUnmount() {
    // clearInterval(this.interval);
  },
  methods: {
    async getHotspotsList() {
      const newHotspots = await getHotspots();
      this.hotspots = newHotspots.hotspots;
    },
    hotspotsMinus() {
      moreOrLessHotspots(false);
    },
    hotspotsPlus() {
      moreOrLessHotspots(true);
    }
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
