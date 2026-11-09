package ie.setu.controllers

import ie.setu.controllers.UserController.updateUser
import ie.setu.domain.Activity
import ie.setu.domain.User
import ie.setu.domain.db.Activities
import ie.setu.domain.db.Users
import ie.setu.helpers.ServerContainer
import ie.setu.helpers.activities
import ie.setu.helpers.nonExistingEmail
import ie.setu.helpers.users
import ie.setu.helpers.validEmail
import ie.setu.helpers.validName
import ie.setu.domain.repository.UserDAO
import ie.setu.utils.jsonObjectMapper
import ie.setu.utils.jsonToObject
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import kong.unirest.core.Unirest
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.joda.time.DateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthTrackerTest {

    private val app = ServerContainer.instance
    private val origin = "http://localhost:" + app.port()

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupInMemoryDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
            transaction {
                SchemaUtils.create(Users, Activities)
            }
        }
    }
    @Nested
    inner class ReadUsers {
        @Test
        fun `get all users from the database returns 200 or 404 response`() {
            val response = Unirest.get(origin + "/api/users/").asString()
            if (response.status == 200) {
                val retrievedUsers: ArrayList<User> = jsonToObject(response.body.toString())
                assertNotEquals(0, retrievedUsers.size)
            }
            else {
                assertEquals(404, response.status)
            }
        }


        @Test
        fun `get user by id when user does not exist returns 404 response`() {

            //Arrange - test data for user id
            val id = Integer.MIN_VALUE

            // Act - attempt to retrieve the non-existent user from the database
            val retrieveResponse = Unirest.get(origin + "/api/users/${id}").asString()

            // Assert -  verify return code
            assertEquals(404, retrieveResponse.status)
        }

        @Test
        fun `get user by email when user does not exist returns 404 response`() {
            // Arrange & Act - attempt to retrieve the non-existent user from the database
            val retrieveResponse = Unirest.get(origin + "/api/users/email/${nonExistingEmail}").asString()
            // Assert -  verify return code
            assertEquals(404, retrieveResponse.status)
        }
        @Test
        fun `getting a user by id when id exists, returns a 200 response`() {

            //Arrange - add the user
            val addResponse = addUser(validName, validEmail)
            val addedUser : User = jsonToObject(addResponse.body.toString())

            //Assert - retrieve the added user from the database and verify return code
            val retrieveResponse = retrieveUserById(addedUser.id)
            assertEquals(200, retrieveResponse.status)

            //After - restore the db to previous state by deleting the added user
            deleteUser(addedUser.id)
        }

        @Test
        fun `getting a user by email when email exists, returns a 200 response`() {

            //Arrange - add the user
            addUser(validName, validEmail)

            //Assert - retrieve the added user from the database and verify return code
            val retrieveResponse = retrieveUserByEmail(validEmail)
            assertEquals(200, retrieveResponse.status)

            //After - restore the db to previous state by deleting the added user
            val retrievedUser : User = jsonToObject(retrieveResponse.body.toString())
            deleteUser(retrievedUser.id)
        }

    }

    @Nested
    inner class CreateUsers {
        @Test
        fun `add a user with correct details returns a 201 response`() {

            //Arrange & Act & Assert
            //    add the user and verify return code (using fixture data)
            val addResponse = addUser(validName, validEmail)
            assertEquals(201, addResponse.status)

            //Assert - retrieve the added user from the database and verify return code
            val retrieveResponse= retrieveUserByEmail(validEmail)
            assertEquals(200, retrieveResponse.status)

            //Assert - verify the contents of the retrieved user
            val retrievedUser : User = jsonToObject(addResponse.body.toString())
            assertEquals(validEmail, retrievedUser.email)
            assertEquals(validName, retrievedUser.name)

            //After - restore the db to previous state by deleting the added user
            val deleteResponse = deleteUser(retrievedUser.id)
            assertEquals(204, deleteResponse.status)
        }
        @Test
        fun `multiple users added to table can be retrieved successfully`() {
            transaction {
                val userDAO = populateUserTable()

                assertEquals(3, userDAO.getAll().size)
                assertEquals(users[0].name, userDAO.findByEmail(users[0].email)?.name)
                assertEquals(users[1].name, userDAO.findByEmail(users[1].email)?.name)
                assertEquals(users[2].name, userDAO.findByEmail(users[2].email)?.name)
            }
        }

    }

    @Nested
    inner class UpdateUsers {
        @Test
        fun `updating a user when it exists, returns a 204 response`() {

            //Arrange - add the user that we plan to do an update on
            val updatedName = "Updated Name"
            val updatedEmail = "Updated Email"
            val addedResponse = addUser(validName, validEmail)
            val addedUser : User = jsonToObject(addedResponse.body.toString())

            //Act & Assert - update the email and name of the retrieved user and assert 204 is returned
            assertEquals(204, updateUser(addedUser.id, updatedName, updatedEmail).status)

            //Act & Assert - retrieve updated user and assert details are correct
            val updatedUserResponse = retrieveUserById(addedUser.id)
            val updatedUser : User = jsonToObject(updatedUserResponse.body.toString())
            assertEquals(updatedName, updatedUser.name)
            assertEquals(updatedEmail, updatedUser.email)

            //After - restore the db to previous state by deleting the added user
            deleteUser(addedUser.id)
        }

        @Test
        fun `updating a user when it doesn't exist, returns a 404 response`() {

            //Arrange - creating some text fixture data
            val updatedName = "Updated Name"
            val updatedEmail = "Updated Email"

            //Act & Assert - attempt to update the email and name of user that doesn't exist
            assertEquals(404, updateUser(-1, updatedName, updatedEmail).status)
        }

    }

    @Nested
    inner class DeleteUsers {

        @Test
        fun `deleting a user when it doesn't exist, returns a 404 response`() {
            //Act & Assert - attempt to delete a user that doesn't exist
            assertEquals(404, deleteUser(-1).status)
        }

        @Test
        fun `deleting a user when it exists, returns a 204 response`() {

            //Arrange - add the user that we plan to do a delete on
            val addedResponse = addUser(validName, validEmail)
            val addedUser : User = jsonToObject(addedResponse.body.toString())

            //Act & Assert - delete the added user and assert a 204 is returned
            assertEquals(204, deleteUser(addedUser.id).status)

            //Act & Assert - attempt to retrieve the deleted user --> 404 response
            assertEquals(404, retrieveUserById(addedUser.id).status)
        }

    }
    @Nested
    inner class CreateActivities {
        //   post(  "/api/activities", ActivityController::addActivity)

        @Test
        fun `add an activity when a user exists for it, returns a 201 response`() {

            //Arrange - add a user to attach the activity to
            val addedUser = addTestUser()

            //Act & Assert - add the activity and verify return code (using fixture data)
            val addResponse = addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            )
            assertEquals(201, addResponse.status)

            //Assert - verify the contents of the returned activity
            val addedActivity: Activity = jsonToObject(addResponse.body.toString())
            assertEquals(activities[0].description, addedActivity.description)
            assertEquals(activities[0].duration, addedActivity.duration)
            assertEquals(activities[0].calories, addedActivity.calories)
            assertEquals(addedUser.id, addedActivity.userId)

            //Assert - retrieve the added activity from the database and verify return code
            assertEquals(200, retrieveActivityById(addedActivity.id).status)

            //After - restore the db to previous state by deleting the added activity and user
            assertEquals(204, deleteActivityById(addedActivity.id).status)
            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class ReadActivities {
        //   get(   "/api/users/{user-id}/activities", ActivityController::getActivitiesByUserId)
        //   get(   "/api/activities", ActivityController::getAllActivities)
        //   get(   "/api/activities/{activity-id}", ActivityController::getActivitiesByActivityId)

        @Test
        fun `get all activities from the database returns 200 or 404 response`() {
            val response = retrieveAllActivities()
            if (response.status == 200) {
                val retrievedActivities: ArrayList<Activity> = jsonToObject(response.body.toString())
                assertNotEquals(0, retrievedActivities.size)
            }
            else {
                assertEquals(404, response.status)
            }
        }

        @Test
        fun `get all activities when activities exist, returns a 200 response`() {

            //Arrange - add a user and an activity
            val addedUser = addTestUser()
            val addedActivity: Activity = jsonToObject(addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            ).body.toString())

            //Act & Assert - retrieve all activities and verify return code
            assertEquals(200, retrieveAllActivities().status)

            //After - restore the db to previous state
            deleteActivityById(addedActivity.id)
            deleteUser(addedUser.id)
        }

        @Test
        fun `get activity by activity id when activity does not exist returns 404 response`() {
            assertEquals(404, retrieveActivityById(Integer.MIN_VALUE).status)
        }

        @Test
        fun `get activity by activity id when activity exists returns a 200 response`() {

            //Arrange - add a user and an activity
            val addedUser = addTestUser()
            val addedActivity: Activity = jsonToObject(addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            ).body.toString())

            //Act & Assert - retrieve the activity and verify return code and contents
            val retrieveResponse = retrieveActivityById(addedActivity.id)
            assertEquals(200, retrieveResponse.status)
            val retrievedActivity: Activity = jsonToObject(retrieveResponse.body.toString())
            assertEquals(addedActivity.id, retrievedActivity.id)
            assertEquals(activities[0].description, retrievedActivity.description)

            //After - restore the db to previous state
            deleteActivityById(addedActivity.id)
            deleteUser(addedUser.id)
        }

        @Test
        fun `get activities by user id when user does not exist returns 404 response`() {
            assertEquals(404, retrieveActivitiesByUserId(Integer.MIN_VALUE).status)
        }

        @Test
        fun `get activities by user id when user exists but has no activities returns 404 response`() {

            //Arrange - add a user with no activities
            val addedUser = addTestUser()

            //Act & Assert - retrieve the activities of the user
            assertEquals(404, retrieveActivitiesByUserId(addedUser.id).status)

            //After - restore the db to previous state
            deleteUser(addedUser.id)
        }

        @Test
        fun `get activities by user id when user has activities returns a 200 response`() {

            //Arrange - add a user and two activities
            val addedUser = addTestUser()
            val activity1: Activity = jsonToObject(addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            ).body.toString())
            val activity2: Activity = jsonToObject(addActivity(
                activities[1].description, activities[1].duration,
                activities[1].calories, activities[1].started, addedUser.id
            ).body.toString())

            //Act & Assert - retrieve the activities of the user and verify them
            val retrieveResponse = retrieveActivitiesByUserId(addedUser.id)
            assertEquals(200, retrieveResponse.status)
            val retrievedActivities: ArrayList<Activity> = jsonToObject(retrieveResponse.body.toString())
            assertEquals(2, retrievedActivities.size)

            //After - restore the db to previous state
            deleteActivityById(activity1.id)
            deleteActivityById(activity2.id)
            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class UpdateActivities {
        //  patch( "/api/activities/{activity-id}", ActivityController::updateActivity)

        @Test
        fun `updating an activity when it exists, returns a 204 response`() {

            //Arrange - add a user and the activity that we plan to do an update on
            val addedUser = addTestUser()
            val addedActivity: Activity = jsonToObject(addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            ).body.toString())

            //Act & Assert - update the activity and assert 204 is returned
            val updatedDescription = "Updated Description"
            val updatedDuration = 99.0
            val updatedCalories = 999
            assertEquals(204, updateActivity(
                addedActivity.id, updatedDescription, updatedDuration,
                updatedCalories, activities[0].started, addedUser.id
            ).status)

            //Act & Assert - retrieve updated activity and assert details are correct
            val updatedActivity: Activity = jsonToObject(retrieveActivityById(addedActivity.id).body.toString())
            assertEquals(updatedDescription, updatedActivity.description)
            assertEquals(updatedDuration, updatedActivity.duration)
            assertEquals(updatedCalories, updatedActivity.calories)

            //After - restore the db to previous state
            deleteActivityById(addedActivity.id)
            deleteUser(addedUser.id)
        }

        @Test
        fun `updating an activity when it doesn't exist, returns a 404 response`() {

            //Arrange - add a user to attach the (non-existent) activity to
            val addedUser = addTestUser()

            //Act & Assert - attempt to update an activity that doesn't exist
            assertEquals(404, updateActivity(
                -1, "Updated Description", 99.0, 999, activities[0].started, addedUser.id
            ).status)

            //After - restore the db to previous state
            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class DeleteActivities {
        //   delete("/api/activities/{activity-id}", ActivityController::deleteActivityByActivityId)
        //   delete("/api/users/{user-id}/activities", ActivityController::deleteActivitiesByUserId)

        @Test
        fun `deleting an activity by activity id when it doesn't exist, returns a 404 response`() {
            assertEquals(404, deleteActivityById(-1).status)
        }

        @Test
        fun `deleting an activity by activity id when it exists, returns a 204 response`() {

            //Arrange - add a user and the activity that we plan to do a delete on
            val addedUser = addTestUser()
            val addedActivity: Activity = jsonToObject(addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            ).body.toString())

            //Act & Assert - delete the added activity and assert a 204 is returned
            assertEquals(204, deleteActivityById(addedActivity.id).status)

            //Act & Assert - attempt to retrieve the deleted activity --> 404 response
            assertEquals(404, retrieveActivityById(addedActivity.id).status)

            //After - restore the db to previous state
            deleteUser(addedUser.id)
        }

        @Test
        fun `deleting activities by user id when user doesn't exist, returns a 404 response`() {
            assertEquals(404, deleteActivitiesByUserId(-1).status)
        }

        @Test
        fun `deleting activities by user id when activities exist, returns a 204 response`() {

            //Arrange - add a user and two activities that we plan to do a delete on
            val addedUser = addTestUser()
            addActivity(
                activities[0].description, activities[0].duration,
                activities[0].calories, activities[0].started, addedUser.id
            )
            addActivity(
                activities[1].description, activities[1].duration,
                activities[1].calories, activities[1].started, addedUser.id
            )

            //Act & Assert - delete the activities of the user and assert a 204 is returned
            assertEquals(204, deleteActivitiesByUserId(addedUser.id).status)

            //Act & Assert - attempt to retrieve the deleted activities --> 404 response
            assertEquals(404, retrieveActivitiesByUserId(addedUser.id).status)

            //After - restore the db to previous state
            deleteUser(addedUser.id)
        }
    }



    //helper function to add a test user to the database
    private fun addUser (name: String, email: String): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/users")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
            .asJson()
    }
    //helper function to delete a test user from the database
    private fun deleteUser (id: Int): HttpResponse<String> {
        return Unirest.delete(origin + "/api/users/$id").asString()
    }

    //helper function to retrieve a test user from the database by email
    private fun retrieveUserByEmail(email : String) : HttpResponse<String> {
        return Unirest.get(origin + "/api/users/email/${email}").asString()
    }

    //helper function to retrieve a test user from the database by id
    private fun retrieveUserById(id: Int) : HttpResponse<String> {
        return Unirest.get(origin + "/api/users/${id}").asString()
    }

    private fun populateUserTable(): UserDAO {
        SchemaUtils.create(Users)
        val userDAO = UserDAO()
        userDAO.save(users[0])
        userDAO.save(users[1])
        userDAO.save(users[2])
        return userDAO
    }

    //helper function to add a test user to the database
    private fun updateUser (id: Int, name: String, email: String): HttpResponse<JsonNode> {
        return Unirest.patch(origin + "/api/users/$id")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
            .asJson()
    }

    //--------------------------------------------------------------
    // Activity helper functions
    //--------------------------------------------------------------

    //helper function to add a test activity to the database
    private fun addActivity(description: String, duration: Double, calories: Int,
                            started: DateTime, userId: Int): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/activities")
            .body(activityJson(description, duration, calories, started, userId))
            .asJson()
    }

    //helper function to update a test activity in the database
    private fun updateActivity(id: Int, description: String, duration: Double, calories: Int,
                               started: DateTime, userId: Int): HttpResponse<JsonNode> {
        return Unirest.patch(origin + "/api/activities/$id")
            .body(activityJson(description, duration, calories, started, userId))
            .asJson()
    }

    //helper function to retrieve all activities from the database
    private fun retrieveAllActivities(): HttpResponse<String> {
        return Unirest.get(origin + "/api/activities").asString()
    }

    //helper function to retrieve a test activity from the database by activity id
    private fun retrieveActivityById(id: Int): HttpResponse<String> {
        return Unirest.get(origin + "/api/activities/$id").asString()
    }

    //helper function to retrieve the test activities of a user from the database by user id
    private fun retrieveActivitiesByUserId(userId: Int): HttpResponse<String> {
        return Unirest.get(origin + "/api/users/$userId/activities").asString()
    }

    //helper function to delete a test activity from the database by activity id
    private fun deleteActivityById(id: Int): HttpResponse<String> {
        return Unirest.delete(origin + "/api/activities/$id").asString()
    }

    //helper function to delete all the test activities of a user from the database by user id
    private fun deleteActivitiesByUserId(userId: Int): HttpResponse<String> {
        return Unirest.delete(origin + "/api/users/$userId/activities").asString()
    }

    //helper function to build the json body of an activity
    private fun activityJson(description: String, duration: Double, calories: Int,
                             started: DateTime, userId: Int): String {
        return jsonObjectMapper().writeValueAsString(
            Activity(0, description, duration, calories, started, userId)
        )
    }

    //helper function to add a test user and return it
    private fun addTestUser(): User {
        return jsonToObject(addUser(validName, validEmail).body.toString())
    }

}
