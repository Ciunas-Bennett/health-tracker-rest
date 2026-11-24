package ie.setu.helpers

import ie.setu.domain.db.Activities
import ie.setu.domain.db.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * An in-memory H2 database used by the browser (Selenium) tests.
 */
object TestDB {

    private var database: Database? = null

    fun connect() {
        val existing = database
        if (existing == null) {
            database = Database.connect(
                url = "jdbc:h2:mem:test-db;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
            transaction {
                SchemaUtils.create(Users, Activities)
            }
        } else {
            // another test class may have connected to a different database since
            TransactionManager.defaultDatabase = existing
        }
    }

    fun reset() {
        transaction {
            SchemaUtils.drop(Activities, Users)
            SchemaUtils.create(Users, Activities)
        }
    }
}
