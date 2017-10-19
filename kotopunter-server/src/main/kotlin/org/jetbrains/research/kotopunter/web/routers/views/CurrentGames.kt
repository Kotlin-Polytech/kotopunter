package org.jetbrains.research.kotopunter.web.routers.views

import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotopunter.util.redirect
import org.jetbrains.research.kotopunter.util.routing.HandlerFor
import org.jetbrains.research.kotopunter.util.routing.JsBundle
import org.jetbrains.research.kotopunter.util.routing.Templatize
import org.jetbrains.research.kotopunter.web.UrlPattern

@HandlerFor(UrlPattern.CurrentGames)
@Templatize("dispatcher.jade")
@JsBundle("dispatcher")
suspend fun handleDispatcher(context: RoutingContext) {}

@HandlerFor(UrlPattern.Index)
suspend fun handleSubmissionResultsById(context: RoutingContext) {
    context.response().redirect(UrlPattern.CurrentGames)
}
