package org.jetbrains.research.kotopunter.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent

object Permissive : BridgeEventFilter {
    override suspend fun isAllowed(be: BridgeEvent) = true.also { logResult(be, it) }

    override fun toString(): String = "Permissive"
}