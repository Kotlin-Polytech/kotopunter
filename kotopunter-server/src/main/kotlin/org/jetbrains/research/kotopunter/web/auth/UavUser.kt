package org.jetbrains.research.kotopunter.web.auth

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import org.jetbrains.research.kotopunter.data.db.HasPermMsg
import org.jetbrains.research.kotopunter.data.db.HasPermReply
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.*

class UavUser(val vertx: Vertx,
              val denizenId: String,
              val id: Int) : User, Loggable {

    override fun isAuthorised(authority: String, handler: Handler<AsyncResult<Boolean>>): User = apply {
        val uuid = newRequestUUID()
        log.trace("Assigning $uuid to authority request for $authority")
        vertx.eventBus().send(
                Address.User.Auth.HasPerm,
                HasPermMsg(denizenId = denizenId, perm = authority).toJson(),
                withRequestUUID(uuid),
                Handler { ar: AsyncResult<Message<JsonObject>> ->
                    handler.handle(ar.map { fromJson<HasPermReply>(it.body()).result})
                })
    }

    override fun clearCache(): User = this  // Cache? What cache?

    override fun setAuthProvider(ap: AuthProvider) {}  // Provider? Which provider?

    override fun principal(): JsonObject = JsonObject(
            "denizenId" to denizenId,
            "id" to id
    )
}
