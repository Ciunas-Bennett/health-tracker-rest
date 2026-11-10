<template id="activity-detail">
  <app-layout>
    <div v-if="activity" class="card">
      <h5 class="card-header">Activity {{activity.id}}</h5>
      <div class="card-body">
        <dl class="row mb-0">
          <dt class="col-sm-3">Description</dt>
          <dd class="col-sm-9">{{activity.description}}</dd>
          <dt class="col-sm-3">Duration</dt>
          <dd class="col-sm-9">{{activity.duration}} minutes</dd>
          <dt class="col-sm-3">Calories</dt>
          <dd class="col-sm-9">{{activity.calories}}</dd>
          <dt class="col-sm-3">Started</dt>
          <dd class="col-sm-9">{{activity.started}}</dd>
          <dt class="col-sm-3">User</dt>
          <dd class="col-sm-9"><a :href="`/users/${activity.userId}`">User {{activity.userId}}</a></dd>
        </dl>
      </div>
    </div>
    <p v-if="notFound">Activity not found.</p>
  </app-layout>
</template>

<script>
app.component("activity-detail", {
  template: "#activity-detail",
  data: () => ({
    activity: null,
    notFound: false
  }),
  created() {
    const activityId = this.$javalin.pathParams["activity-id"];
    axios.get(`/api/activities/${activityId}`)
        .then(res => this.activity = res.data)
        .catch(error => {
          if (error.response && error.response.status === 404) {
            this.notFound = true;
          } else {
            alert("Error while fetching activity " + activityId);
          }
        });
  }
});
</script>
