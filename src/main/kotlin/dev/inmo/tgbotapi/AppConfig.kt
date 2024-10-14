package dev.inmo.tgbotapi

object AppConfig {
    private var appName: String? = null

    fun init(name: String) {
        if (appName != null) {
            throw IllegalStateException("already initialized!")
        }
        appName = name
    }

    fun appName() = appName ?: throw IllegalStateException("appName is null")
    fun botToken() = System.getenv("BOT_TOKEN")

}