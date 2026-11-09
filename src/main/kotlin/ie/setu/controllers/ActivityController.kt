package ie.setu.controllers

import ie.setu.domain.Activity
import ie.setu.domain.repository.ActivityDAO
import ie.setu.domain.repository.UserDAO
import ie.setu.utils.jsonToObject
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse

/**
 * Handles the HTTP requests for the activity endpoints.
 *
 * Each function reads its input from the Javalin [Context] and sets the
 * HTTP status code and (where applicable) the JSON body of the response.
 */
object ActivityController {

    private val activityDAO = ActivityDAO()
    private val userDAO = UserDAO()

    /**
     * Returns all activities, regardless of user.
     *
     * Responds with 200 if any activities exist, otherwise 404.
     */
    @OpenApi(
        path = "/api/activities",
        methods = [HttpMethod.GET],
        summary = "Get all activities",
        operationId = "getAllActivities",
        tags = ["Activities"],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = Array<Activity>::class)],
                description = "A list of all activities in the app"
            ),
            OpenApiResponse(status = "404", description = "There are no activities in the app")
        ]
    )
    fun getAllActivities(ctx: Context) {
        val activities = activityDAO.getAll()
        if (activities.size != 0) {
            ctx.status(200)
        }
        else {
            ctx.status(404)
        }
        ctx.json(activities)
    }

    /**
     * Returns all activities for the user identified by the `user-id` path parameter.
     *
     * Responds with 200 if the user has activities, otherwise 404
     * (the user does not exist or has no activities).
     */
    @OpenApi(
        path = "/api/users/{user-id}/activities",
        methods = [HttpMethod.GET],
        summary = "Get all activities for a user",
        operationId = "getActivitiesByUserId",
        tags = ["Activities"],
        pathParams = [OpenApiParam(name = "user-id", type = Int::class, required = true, description = "The user ID")],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = Array<Activity>::class)],
                description = "A list of activities for the specified user"
            ),
            OpenApiResponse(status = "404", description = "User not found, or the user has no activities")
        ]
    )
    fun getActivitiesByUserId(ctx: Context) {
        val userId = ctx.pathParam("user-id").toInt()
        if (userDAO.findById(userId) != null) {
            val activities = activityDAO.findByUserId(userId)
            if (activities.isNotEmpty()) {
                ctx.json(activities)
                ctx.status(200)
            }
            else {
                ctx.status(404)
            }
        }
        else {
            ctx.status(404)
        }
    }

    /**
     * Returns the activity identified by the `activity-id` path parameter.
     *
     * Responds with 200 if the activity exists, otherwise 404.
     */
    @OpenApi(
        path = "/api/activities/{activity-id}",
        methods = [HttpMethod.GET],
        summary = "Get an activity by ID",
        operationId = "getActivitiesByActivityId",
        tags = ["Activities"],
        pathParams = [OpenApiParam(name = "activity-id", type = Int::class, required = true, description = "The activity ID")],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = Activity::class)],
                description = "The activity with the specified ID"
            ),
            OpenApiResponse(status = "404", description = "Activity not found")
        ]
    )
    fun getActivitiesByActivityId(ctx: Context) {
        val activity = activityDAO.findByActivityId(ctx.pathParam("activity-id").toInt())
        if (activity != null) {
            ctx.json(activity)
            ctx.status(200)
        }
        else {
            ctx.status(404)
        }
    }

    /**
     * Adds the activity supplied as JSON in the request body.
     *
     * Responds with 201 and the saved activity (including its generated id).
     */
    @OpenApi(
        path = "/api/activities",
        methods = [HttpMethod.POST],
        summary = "Add an activity",
        operationId = "addActivity",
        tags = ["Activities"],
        requestBody = OpenApiRequestBody(
            content = [OpenApiContent(from = Activity::class)],
            required = true,
            description = "The activity to add"
        ),
        responses = [
            OpenApiResponse(
                status = "201",
                content = [OpenApiContent(from = Activity::class)],
                description = "The activity has been created"
            )
        ]
    )
    fun addActivity(ctx: Context) {
        val activity = jsonToObject<Activity>(ctx.body())
        val activityId = activityDAO.save(activity)
        if (activityId != null) {
            activity.id = activityId
            ctx.json(activity)
            ctx.status(201)
        }
    }

    /**
     * Deletes the activity identified by the `activity-id` path parameter.
     *
     * Responds with 204 if it was deleted, otherwise 404.
     */
    @OpenApi(
        path = "/api/activities/{activity-id}",
        methods = [HttpMethod.DELETE],
        summary = "Delete an activity by ID",
        operationId = "deleteActivityByActivityId",
        tags = ["Activities"],
        pathParams = [OpenApiParam(name = "activity-id", type = Int::class, required = true, description = "The activity ID")],
        responses = [
            OpenApiResponse(status = "204", description = "The activity has been deleted"),
            OpenApiResponse(status = "404", description = "Activity not found")
        ]
    )
    fun deleteActivityByActivityId(ctx: Context) {
        if (activityDAO.delete(ctx.pathParam("activity-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    /**
     * Deletes all activities for the user identified by the `user-id` path parameter.
     *
     * Responds with 204 if any activities were deleted, otherwise 404.
     */
    @OpenApi(
        path = "/api/users/{user-id}/activities",
        methods = [HttpMethod.DELETE],
        summary = "Delete all activities for a user",
        operationId = "deleteActivitiesByUserId",
        tags = ["Activities"],
        pathParams = [OpenApiParam(name = "user-id", type = Int::class, required = true, description = "The user ID")],
        responses = [
            OpenApiResponse(status = "204", description = "All activities for the user have been deleted"),
            OpenApiResponse(status = "404", description = "User not found, or the user has no activities")
        ]
    )
    fun deleteActivitiesByUserId(ctx: Context) {
        if (activityDAO.deleteByUserId(ctx.pathParam("user-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    /**
     * Updates the activity identified by the `activity-id` path parameter
     * with the activity supplied as JSON in the request body.
     *
     * Responds with 204 if it was updated, otherwise 404.
     */
    @OpenApi(
        path = "/api/activities/{activity-id}",
        methods = [HttpMethod.PATCH],
        summary = "Update an activity by ID",
        operationId = "updateActivity",
        tags = ["Activities"],
        pathParams = [OpenApiParam(name = "activity-id", type = Int::class, required = true, description = "The activity ID")],
        requestBody = OpenApiRequestBody(
            content = [OpenApiContent(from = Activity::class)],
            required = true,
            description = "The updated activity details"
        ),
        responses = [
            OpenApiResponse(status = "204", description = "The activity has been updated"),
            OpenApiResponse(status = "404", description = "Activity not found")
        ]
    )

    fun updateActivity(ctx: Context) {
        val activity = jsonToObject<Activity>(ctx.body())
        if (activityDAO.update(ctx.pathParam("activity-id").toInt(), activity) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }
}
