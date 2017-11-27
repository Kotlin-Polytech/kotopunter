import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.funktionale.collections.tail
import org.jetbrains.research.kotopunter.config.Config
import org.jetbrains.research.kotopunter.database.tables.records.GameRecord
import org.jetbrains.research.kotopunter.dispatch.GameFinished
import org.jetbrains.research.kotopunter.dispatch.Update
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

object Competition {

    fun runServer(punters: Int, map: File) = ProcessBuilder(
            "lampunt",
            "--coordinates",
            "--address",
            "0.0.0.0",
            "--port",
            "9000",
            "--punters",
            "$punters",
            "--move-timeout",
            "${Config.Game.Timeout}",
            "--map",
            map.absolutePath
    ).also { println("Running ${it.command().joinToString(" ")}") }
            .let { pb -> pb.redirectErrorStream(true).start()!! }

    fun lineToJson(line: String): JsonObject? {
        val reLine = line.removePrefix("lampunt: main: ")
        if (reLine.startsWith("JSON:")) {
            return JsonObject(reLine.substringAfter("JSON:"))
        }
        return null
    }

    fun getLog(process: Process): JsonObject {
        println("Game created")

        val lines = process.inputStream.bufferedReader().lineSequence()
        val logArray = jsonArrayOf(JsonObject("start" to JsonObject()))
        for (line in lines) {
            lineToJson(line)?.let { logArray += it }
        }

        process.waitFor(5, TimeUnit.SECONDS)
        println("Game finished")
        process.destroy()

        logArray += JsonObject("stop" to JsonObject())
        return JsonObject("game" to logArray)
    }

    fun runClient(dir: File) =
            ProcessBuilder("bash", "${dir.absolutePath}/go.sh")
            .apply { println("Running ${command()}") }
            .redirectErrorStream(true)
            .start()
            .apply {
                Thread {
                    val reader = this.inputStream.bufferedReader()
                    while(this.isAlive) {
                        if(reader.ready()) reader.readLine().let { println("${dir.name}: $it") }
                    }
                }.start()
            }


    val scoring = mutableMapOf<String, Int>()
    val points = mutableMapOf<String, Long>()

    fun runRound(map: File, clients: List<File>): JsonObject {
        val pb = runServer(clients.size, map)
        val clientProcesses = clients.map { Thread.sleep(500); runClient(it) }
        val log = getLog(pb)
        clientProcesses.forEach {
            it.waitFor(1, TimeUnit.SECONDS)
            it.destroyForcibly()
        }
        val roundRes = log
                .getJsonArray("game")
                .filterIsInstance<JsonObject>()
                .find { it.containsKey("scores") }!!
                .getJsonArray("scores")
                .filterIsInstance<JsonObject>()
                .sortedByDescending { it.getInteger("score") }

        var score = roundRes.size
        for(res in roundRes) {
            val team = res.getString("team")
            points[team] = points.getOrDefault(team, 0) + res.getLong("score")
            scoring[team] = scoring.getOrDefault(team, 0) + score
            score--
        }
        return log
    }

    lateinit var baseDir: String
    lateinit var teamDirs: List<File>
    val joe by lazy { File(File(baseDir, "teams"), "\$average_joe") }
    val mack by lazy { File(File(baseDir, "teams"), "\$nasty_mack") }

    fun sanityCheckRound() {
        val scores = mutableMapOf<File, Int>()
        teamDirs.forEach { team -> scores[team] = 0 }

        teamDirs.forEach { team ->
            val res = runRound(File("maps/default.json"), listOf(team))
            File("sanity", "${team.name}.json").apply { parentFile.mkdirs() }.writeText(res.encodePrettily())
            scores[team] = (scores[team] ?: 0) +
                    res
                            .getJsonArray("game")
                            .filterIsInstance<JsonObject>()
                            .find { it.containsKey("scores") }!!
                            .getJsonArray("scores")
                            .filterIsInstance<JsonObject>()
                            .first()
                            .getInteger("score")
        }

        teamDirs.forEach { team ->
            val res = runRound(File("maps/lambda.json"), listOf(team))
            File("sanity2", "${team.name}.json").apply { parentFile.mkdirs() }.writeText(res.encodePrettily())
            scores[team] = (scores[team] ?: 0) +
                    res
                            .getJsonArray("game")
                            .filterIsInstance<JsonObject>()
                            .find { it.containsKey("scores") }!!
                            .getJsonArray("scores")
                            .filterIsInstance<JsonObject>()
                            .first()
                            .getInteger("score")
        }

        for ((team, score) in scores) if(score == 0) teamDirs -= team
    }

    fun firstRound() {
        val maps = listOf("vancouver", "boston", "triangle", "star")

        maps.forEach { map ->
            val mapFile = File("maps/$map.json")
            teamDirs.forEachIndexed { i, team ->
                val res = runRound(mapFile, listOf(team, joe))
                File("round1", "$map-$i.json").apply { parentFile.mkdirs() }.writeText(res.encodePrettily())
            }
        }

    }

    fun secondRound() {
        val firsts = ArrayList(teamDirs)
        Collections.shuffle(firsts, ThreadLocalRandom())
        val seconds = ArrayList(firsts).also { Collections.rotate(it, 1) }
        val thirds = ArrayList(firsts).also { Collections.rotate(it, -1) }

        val groups = (firsts zip seconds zip thirds).map { (a, b) -> Triple(a.first, a.second, b) }

        val maps = listOf("vancouver", "boston", "triangle", "star", "oxford-10000", "icfp-coauthors-pj")

        maps.forEach { map ->
            val mapFile = File("maps/$map.json")
            groups.forEachIndexed { i, group ->
                val res = runRound(mapFile, group.toList())
                File("round2", "$map-$i.json").apply { parentFile.mkdirs() }.writeText(res.encodePrettily())
            }
        }

    }

    fun thirdRound() {
        val maps = listOf("vancouver", "boston", "triangle", "star", "gothenburg")

        maps.forEach { map ->
            val mapFile = File("maps/$map.json")
            teamDirs.forEachIndexed { i, team ->
                val res = runRound(mapFile, listOf(team, mack))
                File("round3", "$map-$i.json").apply { parentFile.mkdirs() }.writeText(res.encodePrettily())
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {

        baseDir = args.firstOrNull() ?: System.getProperty("user.dir")
        teamDirs = File(baseDir, "teams").listFiles().filter { it.isDirectory && !it.name.startsWith("$") }

        sanityCheckRound()
        println("Current scores: \n${scoring.asIterable().joinToString("\n")}")
        firstRound()
        println("Current scores: \n${scoring.asIterable().joinToString("\n")}")
        secondRound()
        println("Current scores: \n${scoring.asIterable().joinToString("\n")}")
        thirdRound()
        println("Final scores: \n${scoring.asIterable().joinToString("\n")}")
        println("Final points: \n${points.asIterable().joinToString("\n")}")
    }

}
