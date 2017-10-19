package org.jetbrains.research.kotopunter.web.routers

import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import org.jetbrains.research.kotopunter.util.redirect

import org.jetbrains.research.kotopunter.util.routing.HandlerFactoryFor
import org.jetbrains.research.kotopunter.util.routing.HandlerFor
import org.jetbrains.research.kotopunter.util.routing.RoutingConfig
import org.jetbrains.research.kotopunter.web.UrlPattern

@HandlerFactoryFor(UrlPattern.Static)
fun staticHandlerFactory(cfg: RoutingConfig) = StaticHandler.create(cfg.staticLocalPath)

@HandlerFactoryFor(UrlPattern.Legacy)
fun staticHandlerFactory2(cfg: RoutingConfig) = StaticHandler.create("webroot/orgs")
