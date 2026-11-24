package ie.setu.controllers

import ie.setu.domain.repository.UserDAO
import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class UserControllerMockTest {

    @Test
    fun `getting a user by id when it doesn't exist returns 404`() {
        val userDao = mockk<UserDAO>()
        val context = mockk<Context>(relaxed = true)
        val controller = UserController(userDao)

        every { context.pathParam("user-id") } returns "42"
        every { userDao.findById(42) } returns null

        controller.getUserByUserId(context)

        verify { userDao.findById(42) }
        verify { context.status(404) }
    }
}
