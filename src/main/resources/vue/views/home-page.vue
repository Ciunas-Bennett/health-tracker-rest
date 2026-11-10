<!-- the "home-page" element is passed as a parameter to VueComponent in the ServerConfig file -->
<template id="home-page">
  <app-layout>
    <h1> Welcome to Health Tracker App</h1>
  </app-layout>
</template>

<script>
app.component('home-page',
    {
      template: "#home-page",
    });
</script>
