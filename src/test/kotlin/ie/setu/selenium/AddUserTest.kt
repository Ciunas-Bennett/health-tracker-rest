package ie.setu.selenium

import ie.setu.config.ServerConfig
import ie.setu.helpers.TestDB
import ie.setu.helpers.populateActivityTable
import ie.setu.helpers.populateUserTable
import io.javalin.Javalin
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

class AddUserTest {
    private lateinit var driver: WebDriver
    private lateinit var app: Javalin // Javalin app instance

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupInMemoryDatabase() {
            TestDB.connect()
        }
    }

    @BeforeEach
    fun setUp() {
        // Reset the h2 database and give it starting users and activities
        TestDB.reset()
        seedTestData()

        // Headless chrome, which also works on GitHub Actions
        val options = ChromeOptions()
            .addArguments("--headless=new")
            .addArguments("--no-sandbox")
            .addArguments("--disable-dev-shm-usage")
        driver = ChromeDriver(options)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60))
    }

    @Test
    fun testAddUserSuccessfullyAddsToDatabase() {
        // port 0 lets the OS pick a free port, so this doesn't clash with other tests' servers
        app = ServerConfig().startJavalinService(0)
        driver.get("http://localhost:${app.port()}/")
        driver.findElement(By.id("users-more-details")).click()
        driver.findElement(By.id("add-button")).click()
        driver.findElement(By.name("name")).sendKeys("Lisa Simpson")
        driver.findElement(By.name("email")).sendKeys("lisa@simpson.com")
        driver.findElement(By.id("add-user-button")).click()
        driver.findElement(By.linkText("Lisa Simpson (lisa@simpson.com)")).click()
    }

    @AfterEach
    fun tearDown() {
        if (::driver.isInitialized) {
            driver.quit()
        }
        if (::app.isInitialized) {
            app.stop()
        }
    }

    private fun seedTestData() {
        transaction {
            populateUserTable()
            populateActivityTable()
        }
    }
}
