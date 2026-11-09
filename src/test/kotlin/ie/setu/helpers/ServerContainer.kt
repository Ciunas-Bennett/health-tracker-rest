package ie.setu.helpers

import ie.setu.config.ServerConfig

object ServerContainer {

    val instance by lazy {
        startServerContainer()
    }

    private fun startServerContainer() = ServerConfig().startJavalinService()

}
