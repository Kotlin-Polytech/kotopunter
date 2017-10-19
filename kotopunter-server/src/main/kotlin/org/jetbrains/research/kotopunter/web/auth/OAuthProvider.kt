package org.jetbrains.research.kotopunter.web.auth

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import org.jetbrains.research.kotopunter.data.db.OAuthLoginMsg
import org.jetbrains.research.kotopunter.database.tables.records.DenizenUnsafeRecord
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.AsyncAuthProvider
import org.jetbrains.research.kotopunter.util.Unauthorized
import org.jetbrains.research.kotopunter.util.fromJson
import org.jetbrains.research.kotopunter.util.sendJsonableAsync

class OAuthProvider(vertx: Vertx) : AsyncAuthProvider(vertx) {
    override suspend fun doAuthenticateAsync(authInfo: JsonObject): User {
        val msg = fromJson<OAuthLoginMsg>(authInfo)

        val record: DenizenUnsafeRecord = try {
            vertx.eventBus().sendJsonableAsync(Address.User.OAuth.Login, msg)
        } catch (e: ReplyException) {
            if (e.failureCode() == HttpResponseStatus.NOT_FOUND.code()) {
                throw Unauthorized(e.message ?: "Unauthorized")
            } else {
                throw e
            }
        }

        return UavUser(vertx, record.denizenId, record.id)
    }
}