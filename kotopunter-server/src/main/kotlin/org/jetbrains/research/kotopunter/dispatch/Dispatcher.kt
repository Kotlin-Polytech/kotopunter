package org.jetbrains.research.kotopunter.dispatch

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.jetbrains.research.kotopunter.config.Config
import org.jetbrains.research.kotopunter.database.tables.records.GameRecord
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.*
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

enum class Phase { CREATED, RUNNING, ENDED }

data class Player(val name: String?) : Jsonable
data class GameState(
        var port: Int,
        var map: String,
        val players: MutableList<Player>,
        var phase: Phase
) : Jsonable

data class NewPlayer(val game: Int, val name: String) : Jsonable
data class GameCreated(val game: Int) : Jsonable
data class GameStarted(val game: Int) : Jsonable
data class GameFinished(val game: Int) : Jsonable

data class Update(val type: String, val payload: Jsonable) : Jsonable

@AutoDeployable
class Dispatcher : AbstractKotopunterVerticle() {
    private val ports = with(Config.Dispatcher.PortRange) { From..To }

    private val infTimeoutPorts = with(Config.Dispatcher.InfTimeoutPortRange) { From..To }

    fun getNumPlayers() = ThreadLocalRandom()
            .nextInt(Config.Dispatcher.MinPlayers, Config.Dispatcher.MaxPlayers + 1)

    val maps by lazy {
        val dir = File(Config.Game.MapDirectory)
        if (dir.isAbsolute) dir.listFiles { _, name -> name.endsWith(".json") }
        else File(System.getProperty("user.dir"), dir.path).listFiles { _, name -> name.endsWith(".json") }
    }

    val randomMap get() = maps[ThreadLocalRandom.current().nextInt(0, maps.size)]

    val jobs = (ports + infTimeoutPorts).map {
        newSingleThreadContext("kotopunter.dispatcher.$it") +
                WithExceptions { log.error("", it) }
    }

    fun GameState(port: Int) =
            GameState(port, "unknown", MutableList(getNumPlayers()) { Player(null) }, Phase.CREATED)

    val statusTable = (ports + infTimeoutPorts).mapTo(mutableListOf()) { GameState(it) }

    fun lineToJson(line: String): JsonObject? {
        val reLine = line.removePrefix("lampunt: main: ")
        if (reLine.startsWith("JSON:")) {
            return JsonObject(reLine.substringAfter("JSON:"))
        }
        return null
    }

    suspend fun parseLine(gameIx: Int, line: String) {
        val json = lineToJson(line)

        when {
            json == JsonObject("gameplay" to "start") -> {
                log.info("Game $gameIx: starting")
                statusTable[gameIx].phase = Phase.RUNNING
                publishJsonable(Address.Dispatcher.Update, Update("game_started", GameStarted(gameIx)))
            }

            json == JsonObject("gameplay" to "stop") -> {
                log.info("Game $gameIx: stopping")
                statusTable[gameIx].phase = Phase.ENDED
                publishJsonable(Address.Dispatcher.Update, Update("game_started", GameStarted(gameIx)))
            }

            "map" in json -> {
                val map = json?.getString("map") ?: return
                statusTable[gameIx].map = map
                publishJsonable(Address.Dispatcher.Update, Update("game_created", GameCreated(gameIx)))
            }

            "team" in json && "punter" in json -> {
                val team = json?.getString("team") ?: return
                val punter = json?.getInteger("punter") ?: return

                statusTable[gameIx].players[punter] = Player(team)

                log.info("Punter $punter connected: team $team")
                publishJsonable(Address.Dispatcher.Update, Update("new_player", NewPlayer(punter, team)))
            }

        }
    }

    override fun start(startFuture: Future<Void>) {

        for ((ix, portData) in (ports.map { it to "${Config.Game.Timeout}" } +
                infTimeoutPorts.map { it to "inf" }).withIndex()) {
            launch(jobs[ix]) {
                while (true) {
                    val (port, timeout) = portData

                    val punters = statusTable[ix].players.size

                    val pb = ProcessBuilder(
                            "lampunt",
                            "--coordinates",
                            "--address",
                            "0.0.0.0",
                            "--port",
                            "$port",
                            "--punters",
                            "$punters",
                            "--move-timeout",
                            timeout,
                            "--map",
                            randomMap.absolutePath
                    )
                    log.info("Running ${pb.command().joinToString(" ")}")
                    val process = pb.redirectErrorStream(true).start()!!

                    log.info("Game $ix: created")

                    val lines = process.inputStream.bufferedReader().lineSequence()
                    val logArray = jsonArrayOf(JsonObject("start" to JsonObject()))
                    for (line in lines) {
                        parseLine(ix, line)
                        lineToJson(line)?.let { logArray += it }
                    }

                    process.waitFor(5, TimeUnit.SECONDS)
                    log.info("Game $ix: finished")

                    logArray += JsonObject("stop" to JsonObject())
                    val objectLog = JsonObject("game" to logArray)

                    statusTable[ix] = GameState(port)
                    publishJsonable(Address.Dispatcher.Update, Update("game_finished", GameFinished(ix)))

                    if (logArray.asSequence().filterIsInstance<JsonObject>().any { it.containsKey("gameplay") }) {
                        val dbGame: GameRecord =
                                sendJsonableAsync(Address.DB.create("game"), GameRecord().apply { log = objectLog })
                        use(dbGame)
                    }
                }
            }
        }

        super.start(startFuture)
    }

    @JsonableEventBusConsumerFor(Address.Dispatcher.Status)
    fun handleStatus(): List<GameState> = statusTable

}
