package views

interface Jsonable

data class Player(val name: String?): Jsonable
data class Status(val port: Int, val players: MutableList<Player>): Jsonable

data class NewPlayer(val game: Int, val name: String): Jsonable
data class GameCreated(val game: Int): Jsonable
data class GameStarted(val game: Int): Jsonable
data class GameFinished(val game: Int): Jsonable

data class Update(val type: String, val payload: Jsonable): Jsonable
