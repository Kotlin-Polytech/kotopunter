package org.jetbrains.research.kotopunter.web.eventbus.guardian

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.KOTOED_REQUEST_UUID
import org.jetbrains.research.kotopunter.util.currentCoroutineName
import org.jetbrains.research.kotopunter.util.get
import org.jetbrains.research.kotopunter.util.set
import org.jetbrains.research.kotopunter.web.auth.Authority
import org.jetbrains.research.kotopunter.web.eventbus.filters.*
import org.jetbrains.research.kotopunter.web.eventbus.patchers.BridgeEventPatcher
import org.jetbrains.research.kotopunter.web.eventbus.patchers.BridgeEventPatcher.Companion.all
import org.jetbrains.research.kotopunter.web.eventbus.patchers.PerAddressPatcher

val HarmlessTypes =
        ByTypes(BridgeEventType.RECEIVE,
                BridgeEventType.SOCKET_IDLE,
                BridgeEventType.SOCKET_PING,
                BridgeEventType.SOCKET_CLOSED,
                BridgeEventType.SOCKET_CREATED)

val Send = ByType(BridgeEventType.SEND)

val ClientHandlerTypes =
        ByTypes(BridgeEventType.REGISTER,
                BridgeEventType.UNREGISTER,
                BridgeEventType.RECEIVE)


fun kotopunterPerAddressFilter(vertx: Vertx): PerAddress {
    return PerAddress(
            Address.Dispatcher.Status to Permissive
    )
}

val KotoedPerAddressAnonymousFilter = PerAddress(
        Address.Dispatcher.Status to Permissive
)

object ClientPushFilter : ByAddress() {
    suspend override fun isAllowed(address: String): Boolean {
        return address == Address.Dispatcher.Update
    }

    override fun toString() = "ClientPushFilter"
}

class KotoedFilter(vertx: Vertx) : BridgeEventFilter {
    private val perAddress = kotopunterPerAddressFilter(vertx)
    private val perAddressAnonymous = KotoedPerAddressAnonymousFilter
    private val underlying = AnyOf(
            HarmlessTypes,
            Send and perAddressAnonymous,
            LoginRequired and Send and perAddress,
            ClientHandlerTypes and ClientPushFilter
    )

    suspend override fun isAllowed(be: BridgeEvent): Boolean = underlying.isAllowed(be)

    fun makePermittedOptions() = perAddress.makePermittedOptions() + perAddressAnonymous.makePermittedOptions()
}

fun kotopunterPerAddressPatcher(vertx: Vertx) = PerAddressPatcher()

object WithRequestUUIDPatcher : BridgeEventPatcher {
    suspend override fun patch(be: BridgeEvent) {
        val rawMessage = be.rawMessage ?: return

        if (BridgeEventType.SOCKET_PING == be.type()) return

        rawMessage["headers", KOTOED_REQUEST_UUID] = currentCoroutineName().name

        be.rawMessage = rawMessage
    }

    override fun toString(): String {
        return "WithRequestUUIDPatcher"
    }
}

fun kotopunterPatcher(vertx: Vertx) = all(kotopunterPerAddressPatcher(vertx), WithRequestUUIDPatcher)
