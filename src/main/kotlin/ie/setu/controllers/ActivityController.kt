package ie.setu.controllers

import ie.setu.domain.Activity
import ie.setu.domain.repository.ActivityDAO
import ie.setu.domain.repository.UserDAO
import ie.setu.utils.jsonToObject
import io.javalin.http.Context

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
    fun updateActivity(ctx: Context) {
        val activity = jsonToObject<Activity>(ctx.body())
        if (activityDAO.update(ctx.pathParam("activity-id").toInt(), activity) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }
}
