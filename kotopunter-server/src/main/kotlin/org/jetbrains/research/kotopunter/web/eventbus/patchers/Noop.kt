package org.jetbrains.research.kotopunter.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent

object Noop : BridgeEventPatcher {
    override suspend fun patch(be: BridgeEvent) { logPatch(be) }

    override fun toString(): String {
        return "Noop"
    }
}