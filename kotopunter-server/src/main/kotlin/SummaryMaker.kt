import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotopunter.util.JsonObject
import java.io.File
import java.io.FileFilter
import java.util.SortedSet

fun main(args: Array<String>) {

    val summary = JsonObject()
    val teams = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()

    val dir = args.getOrNull(0)?.let(::File) ?: return System.exit(1)

    dir.listFiles(File::isDirectory).forEach { dir ->
        dir
            .listFiles { f -> f.extension == "json" }
            .forEach { f ->
            val obj = JsonObject(f.readText())
            for(turn in obj.getJsonArray("game", JsonArray())) {
                turn as? JsonObject ?: continue
                val teamName = turn.getString("team") ?: continue
                teams.getOrPut(teamName) { mutableMapOf() }.getOrPut(dir.name) { java.util.TreeSet() } +=
                        "games/competition1/${dir.name}/${f.name}"
            }
        }
    }

    val games = teams.map { (k, v) ->
        val j = JsonObject(v as Map<String, Any?>)
        j.put("team", k)
        j
    }

    summary.put("games", games)

    File("summary.json").writeText(summary.encodePrettily())
}