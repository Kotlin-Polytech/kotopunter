package org.jetbrains.research.kotopunter.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotopunter.util.get

class PerAddressPatcher(vararg filters: Pair<String, BridgeEventPatcher>) : BridgeEventPatcher {

    private val patchersByAddress = mapOf(*filters)

    override suspend fun patch(be: BridgeEvent) {
        be.rawMessage?.get("address")?.let {
            patchersByAddress[it]?.patch(be)?.also {
                logPatch(be)
            }
        }
    }

    override fun toString(): String {
        return "PerAddressPatcher(patchersByAddress=$patchersByAddress)"
    }


}