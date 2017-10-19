package org.jetbrains.research.kotopunter.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import java.util.*

class ApplyAll(private vararg val patchers: BridgeEventPatcher) : BridgeEventPatcher {
    override suspend fun patch(be: BridgeEvent) {
        for (patcher in patchers)
            patcher.patch(be)
    }

    override fun toString(): String {
        return "ApplyAll(${Arrays.toString(patchers)})"
    }
}