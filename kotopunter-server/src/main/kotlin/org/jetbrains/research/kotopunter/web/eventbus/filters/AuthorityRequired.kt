package org.jetbrains.research.kotopunter.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotopunter.util.isAuthorisedAsync

class AuthorityRequired(val authority: String) : BridgeEventFilter {
    suspend override fun isAllowed(be: BridgeEvent): Boolean =
            LoginRequired.isAllowed(be) && be.socket().webUser()?.isAuthorisedAsync(authority) ?: false

    override fun toString(): String {
        return "AuthorityRequired(authority='$authority')"
    }


}