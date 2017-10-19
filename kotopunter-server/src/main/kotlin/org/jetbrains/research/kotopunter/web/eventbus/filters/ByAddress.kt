package org.jetbrains.research.kotopunter.web.eventbus.filters

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotopunter.util.get

abstract class ByAddress: BridgeEventFilter {
    suspend abstract fun isAllowed(address: String): Boolean

    suspend override fun isAllowed(be: BridgeEvent): Boolean {
        val address = be.rawMessage?.get("address") as? String
        address ?: return false
        return isAllowed(address).also { logResult(be, it) }
    }

}