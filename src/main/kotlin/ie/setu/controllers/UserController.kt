package ie.setu.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ie.setu.domain.User
import ie.setu.domain.repository.UserDAO
import ie.setu.utils.jsonToObject
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse

object UserController {

    private val userDao = UserDAO()

    @OpenApi(
        path = "/api/users",
        methods = [HttpMethod.GET],
        summary = "Get all users",
        operationId = "getAllUsers",
        tags = ["Users"],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = Array<User>::class)],
                description = "A list of the users in the app"
            ),
            OpenApiResponse(status = "404", description = "There are no users in the app")
        ]
    )
    fun getAllUsers(ctx: Context) {
        val users = userDao.getAll()
        if (users.size != 0) {
            ctx.status(200)
        }
        else{
            ctx.status(404)
        }
        ctx.json(users)
    }


    @OpenApi(
        path = "/api/users/{user-id}",
        methods = [HttpMethod.GET],
        summary = "Get a user by ID",
        operationId = "getUserByUserId",
        tags = ["Users"],
        pathParams = [OpenApiParam(name = "user-id", type = Int::class, required = true, description = "The user ID")],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = User::class)],
                description = "The user with the specified ID"
            ),
            OpenApiResponse(status = "404", description = "User not found")
        ]
    )
    fun getUserByUserId(ctx: Context) {
        val user = userDao.findById(ctx.pathParam("user-id").toInt())
        if (user != null) {
            ctx.json(user)
            ctx.status(200)
        }
        else {
            ctx.status(404)
        }
    }

    @OpenApi(
        path = "/api/users/email/{email}",
        methods = [HttpMethod.GET],
        summary = "Get a user by email",
        operationId = "getUserByEmail",
        tags = ["Users"],
        pathParams = [OpenApiParam(name = "email", type = String::class, required = true, description = "The user's email")],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = User::class)],
                description = "The user with the specified email"
            ),
            OpenApiResponse(status = "404", description = "User not found")
        ]
    )
    fun getUserByEmail(ctx: Context) {
        val email = ctx.pathParam("email")
        val user = userDao.findByEmail(email)
        if (user != null) {
            ctx.json(user)
            ctx.status(200)
        }
        else{
            ctx.status(404)
        }
    }


    @OpenApi(
        path = "/api/users",
        methods = [HttpMethod.POST],
        summary = "Add a user",
        operationId = "addUser",
        tags = ["Users"],
        requestBody = OpenApiRequestBody(
            content = [OpenApiContent(from = User::class)],
            required = true,
            description = "The user to add"
        ),
        responses = [
            OpenApiResponse(
                status = "201",
                content = [OpenApiContent(from = User::class)],
                description = "The user has been created"
            )
        ]
    )
    fun addUser(ctx: Context) {
        val mapper = jacksonObjectMapper()
        val user = mapper.readValue<User>(ctx.body())
        val userId = userDao.save(user)
        if (userId != null) {
            user.id = userId
            ctx.json(user)
            ctx.status(201)
        }
    }

    @OpenApi(
        path = "/api/users/{user-id}",
        methods = [HttpMethod.DELETE],
        summary = "Delete a user by ID",
        operationId = "deleteUser",
        tags = ["Users"],
        pathParams = [OpenApiParam(name = "user-id", type = Int::class, required = true, description = "The user ID")],
        responses = [
            OpenApiResponse(status = "204", description = "The user has been deleted"),
            OpenApiResponse(status = "404", description = "User not found")
        ]
    )
    fun deleteUser(ctx: Context) {
        val userId = ctx.pathParam("user-id").toInt()
        if (userDao.delete(userId) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    @OpenApi(
        path = "/api/users/{user-id}",
        methods = [HttpMethod.PATCH],
        summary = "Update a user by ID",
        operationId = "updateUser",
        tags = ["Users"],
        pathParams = [OpenApiParam(name = "user-id", type = Int::class, required = true, description = "The user ID")],
        requestBody = OpenApiRequestBody(
            content = [OpenApiContent(from = User::class)],
            required = true,
            description = "The updated user details"
        ),
        responses = [
            OpenApiResponse(status = "204", description = "The user has been updated"),
            OpenApiResponse(status = "404", description = "User not found")
        ]
    )
    fun updateUser(ctx: Context) {
        val userId = ctx.pathParam("user-id").toInt()
        val mapper = jacksonObjectMapper()
        val userUpdates = mapper.readValue<User>(ctx.body())
        if (userDao.update(userId, userUpdates) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

}
