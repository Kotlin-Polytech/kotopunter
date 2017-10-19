package org.jetbrains.research.kotopunter.web.routers.views

import io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotopunter.database.tables.records.GameRecord
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.*
import org.jetbrains.research.kotopunter.util.routing.HandlerFor
import org.jetbrains.research.kotopunter.web.UrlPattern
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

val RFC2616 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)

@HandlerFor(UrlPattern.Games)
suspend fun handleGameLog(context: RoutingContext) {
    val game by context.request()

    val records: List<GameRecord> = context
            .vertx()
            .eventBus()
            .sendJsonableCollectAsync(Address.DB.find("game"), GameRecord().apply { id = game?.toInt() })
    val record = records.firstOrNull() ?: throw NotFound("")
    val resp = context.jsonResponse()
    val headers = resp.headers()

    headers.set("cache-control", "public, max-age=${60 * 60 * 24}")
    headers.set("last-modified", RFC2616.format(record.time.atZoneSameInstant(ZoneOffset.UTC)))

    //resp.statusCode = NOT_MODIFIED.code()

    resp.end(record.log.uncheckedCast<JsonObject>())
}
