package org.jetbrains.research.kotopunter.history

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotopunter.data.db.CountResponse
import org.jetbrains.research.kotopunter.data.db.ReadPageMsg
import org.jetbrains.research.kotopunter.database.tables.records.GameRecord
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.*

@AutoDeployable
class HistoryVerticle: AbstractKotopunterVerticle() {

    fun cleanupLog(log: JsonObject): JsonObject {
        val minifiedGame =
                log.getJsonArray("game").list

        val startIx = minifiedGame.indexOfFirst { it == JsonObject("gameplay" to "start") }
        val endIx = minifiedGame.indexOfLast { it == JsonObject("gameplay" to "stop") }

        if(startIx == -1 || endIx == -1) return log

        minifiedGame.subList(startIx + 1, endIx).clear()

        log["game"] = minifiedGame
        return log
    }

    @JsonableEventBusConsumerFor(Address.History.Page)
    suspend fun handlePage(message: ReadPageMsg): List<GameRecord> {
        val recs: List<GameRecord> = sendJsonableCollectAsync(Address.DB.readPage("game"), message)

        recs.forEach { it.apply { log = cleanupLog(it.log.uncheckedCast()) } }

        return recs
    }

    @JsonableEventBusConsumerFor(Address.History.Count)
    suspend fun handleCount(message: GameRecord): CountResponse =
            sendJsonableAsync(Address.DB.count("game"), message)

}
