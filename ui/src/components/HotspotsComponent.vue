<template>
  <ul class="nav nav-tabs" id="myTab" role="tablist">
    <li class="nav-item" role="presentation"
        v-for="hotspot in hotspots"
        v-bind:key="hotspot.id"
    >
      <button :class=navLinkActive(hotspot.id)
              :id=tabId(hotspot.id)
              data-bs-toggle="tab"
              :data-bs-target=targetWithHash(hotspot.id)
              type="button"
              role="tab"
              :aria-controls=targetNoHash(hotspot.id)
              :aria-selected=trueIfZero(hotspot.id)
      >{{ hotspot.region }}</button>
    </li>
  </ul>
  <div class="tab-content" id="myTabContent">
    <div
        v-for="hotspot in hotspots"
        v-bind:key="hotspot.id"
        :class=tabContentClass(hotspot.id)
        :id=targetNoHash(hotspot.id)
        role="tabpanel"
        :aria-labelledby=tabId(hotspot.id)
    >
      <div v-if="hotspot.runData">
        Started {{ secondsToDate(hotspot.runData.startSeconds) }},
        Running for {{ hotspot.runData.runSeconds }}s,
        Beaconed {{ hotspot.runData.reportCount }} times,
        Reporting every {{ hotspot.runData.intervalMs }}ms
      </div>
    </div>
  </div>
  <BeaconChartComponent />
</template>

<script>

import BeaconChartComponent from "@/components/BeaconChartComponent";

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
      beaconReports: {}
    };
  },
  methods: {
    secondsToDate(seconds) {
      return new Date(seconds * 1000).toLocaleString();
    },
    tabId(id) {
      return "id" + id;
    },
    tabContentClass(id) {
      let result = "tab-pane fade";
      if (id === 0) {
        result += " active show";
      }
      return result;
    },
    navLinkActive(id) {
      let result = "nav-link";
      if (id === 0) {
        result += " active";
      }
      if ( ! this.hotspots.at(id).enabled) {
        result += " disabled";
      }
      return result;
    },
    trueIfZero(id) {
      return (id === 0);
    },
    targetWithHash(id) {
      return "#hotspot" + id;
    },
    targetNoHash(id) {
      return "hotspot" + id;
    },
    // onSlideStart() {
    //   this.sliding = true
    // },
    // onSlideEnd() {
    //   this.sliding = false
    // }
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
