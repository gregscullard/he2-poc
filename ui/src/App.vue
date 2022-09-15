<template>
  <DashboardComponent />
</template>

<script>

import DashboardComponent from "./components/Dashboard";

export default {
  name: 'App',
  components: {
    DashboardComponent
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin: 10px;
}
</style>
