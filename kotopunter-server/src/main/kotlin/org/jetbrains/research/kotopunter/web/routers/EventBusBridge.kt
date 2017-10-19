package org.jetbrains.research.kotopunter.web.routers

import io.vertx.ext.web.handler.sockjs.PermittedOptions
import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import org.jetbrains.research.kotopunter.util.routing.EnableSessions
import org.jetbrains.research.kotopunter.util.routing.HandlerFactoryFor
import org.jetbrains.research.kotopunter.util.routing.JsonResponse
import org.jetbrains.research.kotopunter.util.routing.RoutingConfig
import org.jetbrains.research.kotopunter.web.UrlPattern
import org.jetbrains.research.kotopunter.web.eventbus.BridgeGuardian
import org.jetbrains.research.kotopunter.web.eventbus.EventBusBridge
import org.jetbrains.research.kotopunter.web.eventbus.guardian.KotoedFilter
import org.jetbrains.research.kotopunter.web.eventbus.guardian.WithRequestUUIDPatcher

@HandlerFactoryFor(UrlPattern.EventBus)
@EnableSessions
@JsonResponse
fun eventBusHandlerFactory(cfg: RoutingConfig) = with(cfg) {
    val filter = KotoedFilter(vertx)

    val bo = io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions().apply {
        for (po in filter.makePermittedOptions())
            addInboundPermitted(po)

        // FIXME belyaev: make this a nice method
        addOutboundPermitted(io.vertx.ext.web.handler.sockjs.PermittedOptions().apply { addressRegex = ".*" })

    }
    EventBusBridge(vertx, bo, BridgeGuardian(vertx, filter, WithRequestUUIDPatcher))
}
