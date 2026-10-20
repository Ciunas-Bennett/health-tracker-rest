package ie.setu.repository

import ie.setu.domain.Activity
import ie.setu.domain.db.Activities
import ie.setu.domain.db.Users
import ie.setu.domain.repository.ActivityDAO
import ie.setu.domain.repository.UserDAO
import ie.setu.helpers.activities
import ie.setu.helpers.users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

val activity1 = activities[0]
val activity2 = activities[1]
val activity3 = activities[2]

class ActivityDAOTest {

    companion object {
        @BeforeAll
        @JvmStatic
        internal fun setupInMemoryDatabaseConnection() {
            Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver", user = "root", password = "")
        }
    }

    internal fun populateUserTable(): UserDAO {
        SchemaUtils.create(Users)
        val userDAO = UserDAO()
        userDAO.save(users[0])
        userDAO.save(users[1])
        userDAO.save(users[2])
        return userDAO
    }

    internal fun populateActivityTable(): ActivityDAO {
        SchemaUtils.create(Activities)
        val activityDAO = ActivityDAO()
        activityDAO.save(activity1)
        activityDAO.save(activity2)
        activityDAO.save(activity3)
        return activityDAO
    }

    @Nested
    inner class CreateActivities {

        @Test
        fun `multiple activities added to table can be retrieved successfully`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()

                assertEquals(3, activityDAO.getAll().size)
                assertEquals(activity1.description, activityDAO.findByActivityId(activity1.id)?.description)
                assertEquals(activity2.description, activityDAO.findByActivityId(activity2.id)?.description)
                assertEquals(activity3.description, activityDAO.findByActivityId(activity3.id)?.description)
            }
        }
    }

    @Nested
    inner class ReadActivities {

        @Test
        fun `get all activities from a populated table returns all rows`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(3, activityDAO.getAll().size)
            }
        }

        @Test
        fun `get all activities over empty table returns none`() {
            transaction {
                populateUserTable()
                SchemaUtils.create(Activities)
                val activityDAO = ActivityDAO()
                assertEquals(0, activityDAO.getAll().size)
            }
        }

        @Test
        fun `get activity by id that exists returns correct activity`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(activity1.description, activityDAO.findByActivityId(activity1.id)?.description)
            }
        }

        @Test
        fun `get activity by id that doesn't exist returns null`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(null, activityDAO.findByActivityId(99))
            }
        }

        @Test
        fun `get activities by user id that exists returns correct activities`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                // user 1 has activity1 and activity2
                assertEquals(2, activityDAO.findByUserId(1).size)
            }
        }

        @Test
        fun `get activities by user id that doesn't exist returns empty list`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(0, activityDAO.findByUserId(99).size)
            }
        }
    }

    @Nested
    inner class DeleteActivities {

        @Test
        fun `deleting an existing activity by id results in record being deleted`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(3, activityDAO.getAll().size)
                activityDAO.delete(activity1.id)
                assertEquals(2, activityDAO.getAll().size)
            }
        }

        @Test
        fun `deleting a non-existing activity by id results in no deletion`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(3, activityDAO.getAll().size)
                activityDAO.delete(99)
                assertEquals(3, activityDAO.getAll().size)
            }
        }

        @Test
        fun `deleting activities by user id removes all activities for that user`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(3, activityDAO.getAll().size)
                activityDAO.deleteByUserId(1)
                assertEquals(1, activityDAO.getAll().size)
            }
        }

        @Test
        fun `deleting activities by non-existing user id results in no deletion`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                assertEquals(3, activityDAO.getAll().size)
                activityDAO.deleteByUserId(99)
                assertEquals(3, activityDAO.getAll().size)
            }
        }
    }

    @Nested
    inner class UpdateActivities {

        @Test
        fun `updating an existing activity results in successful update`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                val updatedActivity = Activity(
                    id = activity1.id,
                    description = "Swimming",
                    duration = 30.0,
                    calories = 300,
                    started = DateTime.now(),
                    userId = 1
                )
                activityDAO.update(activity1.id, updatedActivity)
                assertEquals("Swimming", activityDAO.findByActivityId(activity1.id)?.description)
                assertEquals(300, activityDAO.findByActivityId(activity1.id)?.calories)
            }
        }

        @Test
        fun `updating a non-existing activity results in no updates`() {
            transaction {
                populateUserTable()
                val activityDAO = populateActivityTable()
                val updatedActivity = Activity(
                    id = 99,
                    description = "Swimming",
                    duration = 30.0,
                    calories = 300,
                    started = DateTime.now(),
                    userId = 1
                )
                activityDAO.update(99, updatedActivity)
                assertEquals(null, activityDAO.findByActivityId(99))
                assertEquals(3, activityDAO.getAll().size)
            }
        }
    }
}
