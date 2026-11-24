package ie.setu.config

import ie.setu.controllers.ActivityController
import ie.setu.controllers.UserController
import ie.setu.utils.jsonObjectMapper
import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.json.JavalinJackson
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.redoc.ReDocPlugin
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import io.javalin.plugin.bundled.JavalinVuePlugin
import io.javalin.vue.VueComponent

class ServerConfig {
    private fun getRemoteAssignedPort(): Int {
        val remotePort = System.getenv("PORT")
        return if (remotePort != null) {
            Integer.parseInt(remotePort)
        } else 8080
    }

    fun startJavalinService(port: Int = getRemoteAssignedPort()): Javalin {

        val app = Javalin.create { config ->
            config.registerPlugin(OpenApiPlugin { openapi ->
                openapi.withDefinitionConfiguration { _, builder ->
                    builder.info { info ->
                        info.title("Health Tracker API")
                        info.version("1.0.0")
                        info.description("This API provides endpoints for handling CRUD operations on the Health Tracker data.")
                    }
                }
            })
            config.registerPlugin(SwaggerPlugin())
            config.registerPlugin(ReDocPlugin())
            config.jsonMapper(
                JavalinJackson(jsonObjectMapper())
            )
            config.routes.exception(Exception::class.java) { e, ctx ->
                e.printStackTrace()
            }
            config.routes.error(404) { ctx ->
                ctx.json("404 - Not Found")
            }
            registerRoutes(config)

            config.registerPlugin(
                JavalinVuePlugin { vue ->
                    vue.vueInstanceNameInJs = "app"
                }
            )
            config.staticFiles.add("/META-INF/resources")

        }.start(port)
        return app
    }

    private fun registerRoutes(config: JavalinConfig) {
        val userController = UserController()
        val activityController = ActivityController()
        config.routes.get("/api/users", userController::getAllUsers)
        config.routes.get("/api/users/{user-id}", userController::getUserByUserId)
        config.routes.get("/api/users/email/{email}", userController::getUserByEmail)
        config.routes.post("/api/users", userController::addUser)
        config.routes.delete("/api/users/{user-id}", userController::deleteUser)
        config.routes.patch("/api/users/{user-id}", userController::updateUser)
        config.routes.get("/api/users/{user-id}/activities", activityController::getActivitiesByUserId)
        config.routes.delete("/api/users/{user-id}/activities", activityController::deleteActivitiesByUserId)
        config.routes.get("/api/activities", activityController::getAllActivities)
        config.routes.post("/api/activities", activityController::addActivity)
        config.routes.get("/api/activities/{activity-id}", activityController::getActivitiesByActivityId)
        config.routes.delete("/api/activities/{activity-id}", activityController::deleteActivityByActivityId)
        config.routes.patch("/api/activities/{activity-id}", activityController::updateActivity)
        config.routes.get("/", VueComponent("<home-page></home-page>"))
        config.routes.get("/users", VueComponent("<user-overview></user-overview>"))
        config.routes.get("/activities", VueComponent("<activity-overview></activity-overview>"))
        config.routes.get("/activities/{activity-id}", VueComponent("<activity-detail></activity-detail>"))
        config.routes.get("/users/{user-id}", VueComponent("<user-profile></user-profile>"))
        config.routes.get("/users/{user-id}/activities", VueComponent("<user-activity-overview></user-activity-overview>"))

    }
}
