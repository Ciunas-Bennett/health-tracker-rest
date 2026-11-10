<template id="user-overview">
  <app-layout>
    <div>
      <form class="input-group mb-3" @submit.prevent="searchByEmail">
        <input type="email" class="form-control" v-model="email" placeholder="Search by email" required />
        <button type="submit" class="btn btn-primary">Search</button>
      </form>
      <div v-if="searched">
        <p v-if="found">
          Found: <a :href="`/users/${found.id}`">{{found.name}} ({{found.email}})</a>
        </p>
        <p v-else class="text-muted">No user found with that email.</p>
      </div>
      <ul class="user-overview-list">
        <li v-for="user in users">
          <a :href="`/users/${user.id}`">{{user.name}} ({{user.email}})</a>
        </li>
      </ul>
    </div>
  </app-layout>
</template>

<script>
app.component("user-overview", {
  template: "#user-overview",
  data: () => ({
    users: [],
    email: "",
    found: null,
    searched: false,
  }),
  created() {
    this.fetchUsers();
  },
  methods: {
    fetchUsers: function () {
      axios.get("/api/users")
          .then(res => this.users = res.data)
          .catch(() => alert("Error while fetching users"));
    },
    searchByEmail: function () {
      axios.get(`/api/users/email/${encodeURIComponent(this.email)}`)
          .then(res => {
            this.found = res.data;
            this.searched = true;
          })
          .catch(err => {
            if (err.response && err.response.status === 404) {
              this.found = null;
              this.searched = true;
            } else {
              alert("Error while searching for user");
            }
          });
    }
  }
});
</script>
