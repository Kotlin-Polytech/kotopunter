package org.jetbrains.research.kotopunter.web

object UrlPattern {
    const val Star = "/*"
    const val Index = "/"

    const val CurrentGames = "/running"
    const val PastGames = "/finished"
    const val Games = "/games/:game.json"
    const val Static = "/static/*"
    const val Legacy = "/legacy/*"
    const val PuntTV = "/punttv/*"
    const val Maps = "/maps/*"
    const val MapView = "/viewMap/*"
    const val MapViewJSCore = "/viewMap/js-core/*"
    const val EventBus = "/eventbus/*"

    object Auth {
        const val Index = "/auth/login"
    }

    /**
     * Each Any in parameter will be converted to string
     */
    fun reverse(pattern: String, params: Map<String, Any>, star: Any = ""): String {
        var url = pattern
        for ((k, v) in params) {
            url = url.replace(":$k", "$v")
        }

        url = url.replace("*", "$star")

        return url
    }
}
