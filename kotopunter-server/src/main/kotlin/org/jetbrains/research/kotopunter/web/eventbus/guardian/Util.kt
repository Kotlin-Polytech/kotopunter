package org.jetbrains.research.kotopunter.web.eventbus.guardian

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotopunter.util.get
import org.jetbrains.research.kotopunter.util.removeFields
import org.jetbrains.research.kotopunter.web.eventbus.filters.BridgeEventFilter
import org.jetbrains.research.kotopunter.web.eventbus.filters.logResult

abstract class LoggingBridgeEventFilter : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean =
            checkIsAllowed(be).also { logResult(be, it) }

    suspend abstract fun checkIsAllowed(be: BridgeEvent): Boolean
}

fun JsonObject?.cleanUpBody(): JsonObject? {
    this ?: return this
    val msg = this.copy()
    val body = msg?.get("body") as? JsonObject ?: return this

    body.removeFields("password", "initiatorPassword", "newPassword")

    return msg
}