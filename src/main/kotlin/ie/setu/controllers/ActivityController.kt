package ie.setu.controllers

import ie.setu.domain.Activity
import ie.setu.domain.repository.ActivityDAO
import ie.setu.domain.repository.UserDAO
import ie.setu.utils.jsonObjectMapper
import ie.setu.utils.jsonToObject
import io.javalin.http.Context

object ActivityController {

    private val activityDAO = ActivityDAO()
    private val userDAO = UserDAO()

    fun getAllActivities(ctx: Context) {
        ctx.json(jsonObjectMapper().writeValueAsString(activityDAO.getAll()))
    }

    fun getActivitiesByUserId(ctx: Context) {
        if (userDAO.findById(ctx.pathParam("user-id").toInt()) != null) {
            val activities = activityDAO.findByUserId(ctx.pathParam("user-id").toInt())
            if (activities.isNotEmpty()) {
                ctx.json(jsonObjectMapper().writeValueAsString(activities))
            }
        }
    }

    fun getActivitiesByActivityId(ctx: Context) {
        val activity = activityDAO.findByActivityId(ctx.pathParam("activity-id").toInt())
        if (activity != null) {
            ctx.json(jsonObjectMapper().writeValueAsString(activity))
        }
    }

    fun addActivity(ctx: Context) {
        val activity = jsonToObject<Activity>(ctx.body())
        activityDAO.save(activity)
        ctx.json(activity)
    }

    fun deleteActivityByActivityId(ctx: Context) {
        if (activityDAO.delete(ctx.pathParam("activity-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    fun deleteActivitiesByUserId(ctx: Context) {
        if (activityDAO.deleteByUserId(ctx.pathParam("user-id").toInt()) != 0)
            ctx.status(204)
        else
            ctx.status(404)
    }

    fun updateActivity(ctx: Context) {
        val activity = jsonToObject<Activity>(ctx.body())
        activityDAO.update(ctx.pathParam("activity-id").toInt(), activity)
        ctx.status(204)
    }
}
