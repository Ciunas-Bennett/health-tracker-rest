<template id="activity-overview">
  <app-layout>
    <h3>Activities list </h3>
    <ul class="activity-overview-list">
      <li v-for="activity in activities">
        <a :href="`/activities/${activity.id}`">
          {{activity.id}}: {{activity.description}} for {{activity.duration}} minutes
        </a>
        (user {{activity.userId}})
      </li>
    </ul>
  </app-layout>
</template>

<script>
app.component("activity-overview", {
  template: "#activity-overview",
  data: () => ({
    activities: [],
  }),
  created() {
    axios.get("/api/activities")
        .then(res => this.activities = res.data)
        .catch(error => {
          // The API returns 404 when there are no activities, which is not a failure for this page
          if (!error.response || error.response.status !== 404) {
            alert("Error while fetching activities");
          }
        });
  }
});
</script>
