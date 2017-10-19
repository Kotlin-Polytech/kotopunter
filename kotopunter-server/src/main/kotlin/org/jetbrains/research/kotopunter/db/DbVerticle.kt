package org.jetbrains.research.kotopunter.db

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.research.kotopunter.config.Config
import org.jetbrains.research.kotopunter.data.db.BatchUpdateMsg
import org.jetbrains.research.kotopunter.data.db.CountResponse
import org.jetbrains.research.kotopunter.data.db.ReadPageMsg
import org.jetbrains.research.kotopunter.database.Tables
import org.jetbrains.research.kotopunter.database.tables.records.DenizenRecord
import org.jetbrains.research.kotopunter.database.tables.records.GameRecord
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.*
import org.jetbrains.research.kotopunter.util.database.*
import org.jooq.*
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KClass

abstract class DatabaseVerticle<R : TableRecord<R>>(
        val table: Table<R>,
        val entityName: String = table.name.toLowerCase()
) : AbstractKotopunterVerticle() {

    companion object {
        val DBPool =
                newFixedThreadPoolContext(Config.Debug.Database.PoolSize, "dbVerticles.dispatcher")
    }

    val dataSource get() = vertx.getSharedDataSource()

    val pk: Field<Any>
        get() = table.primaryKey?.fields?.first()?.uncheckedCast<Field<Any>>()
                ?: table.field("id").uncheckedCast<Field<Any>>()

    val recordClass: KClass<R> = table.recordType.kotlin.uncheckedCast()

    protected suspend fun <T> db(body: DSLContext.() -> T) =
            kotlinx.coroutines.experimental.run(DBPool) { jooq(dataSource).use(body) }

    protected suspend fun <T> dbAsync(body: suspend DSLContext.() -> T) =
            kotlinx.coroutines.experimental.run(DBPool) { jooq(dataSource).use { it.body() } }

    protected fun DataAccessException.unwrapRollbackException() =
            (if ("Rollback caused" == message) cause else this) ?: this

    protected fun <T> DSLContext.withTransaction(
            body: DSLContext.() -> T): T =
            try {
                transactionResult { conf -> DSL.using(conf).body() }
            } catch (ex: DataAccessException) {
                throw ex.unwrapRollbackException()
            }

    protected suspend fun <T> DSLContext.withTransactionAsync(
            body: suspend DSLContext.() -> T): T =
            suspendCoroutine { cont ->
                transactionResultAsync { conf -> runBlocking(DBPool) { DSL.using(conf).body() } }
                        .whenComplete { res, ex ->
                            when (ex) {
                                null -> cont.resume(res)
                                is DataAccessException -> cont.resumeWithException(ex.unwrapRollbackException())
                                else -> cont.resumeWithException(ex)
                            }
                        }
            }

    protected fun <T> DSLContext.sqlStateAware(body: DSLContext.() -> T): T =
            try {
                body()
            } catch (ex: DataAccessException) {
                val ex_ = ex.unwrapRollbackException()
                        as? DataAccessException
                        ?: throw ex
                when (ex_.sqlState()) {
                    "23505" -> throw Conflict(ex_.message ?: "Oops")
                    else -> throw ex_
                }
            }

    protected suspend fun <T> DSLContext.sqlStateAwareAsync(body: suspend DSLContext.() -> T): T =
            try {
                body()
            } catch (ex: DataAccessException) {
                val ex_ = ex.unwrapRollbackException()
                        as? DataAccessException
                        ?: throw ex
                when (ex_.sqlState()) {
                    "23505" -> throw Conflict(ex_.message ?: "Oops")
                    else -> throw ex_
                }
            }

    protected suspend fun <T> dbWithTransaction(body: DSLContext.() -> T) =
            db { withTransaction { body() } }

    protected suspend fun <T> dbWithTransactionAsync(body: suspend DSLContext.() -> T) =
            dbAsync { withTransactionAsync { body() } }

    protected fun DSLContext.selectById(id: Any) =
            select().from(table).where(pk.eq(id)).fetch().into(recordClass).firstOrNull()

}

abstract class CrudDatabaseVerticle<R : TableRecord<R>>(
        table: Table<R>,
        entityName: String = table.name.toLowerCase()
) : DatabaseVerticle<R>(table, entityName) {

    val createAddress = Address.DB.create(entityName)
    val updateAddress = Address.DB.update(entityName)
    val readAddress = Address.DB.read(entityName)
    val findAddress = Address.DB.find(entityName)
    val deleteAddress = Address.DB.delete(entityName)

    protected fun R.toWhere(): List<Condition> {
        val queryFields = table
                .fields()
                .asSequence()
                .filter { this[it] != null }
                .map { it.uncheckedCast<Field<Any>>() }
        return queryFields.map { it.eq(this.get(it)) }.toList()
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "deleteAddress")
    suspend fun handleDeleteWrapper(message: JsonObject) =
            handleDelete(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleDelete(message: R): R {
        val id = message.getValue(pk.name)
        log.trace("Delete requested for id = $id in table ${table.name}")

        return db {
            delete(table)
                    .where(pk.eq(id))
                    .returning()
                    .fetch()
                    .into(recordClass)
                    .firstOrNull()
                    ?: throw NotFound("Cannot find ${table.name} entry for id $id")
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "readAddress")
    suspend fun handleReadWrapper(message: JsonObject) =
            handleRead(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleRead(message: R): R {
        val id = message.getValue(pk.name)
        log.trace("Read requested for id = $id in table ${table.name}")
        return dbWithTransaction { selectById(id) } ?: throw NotFound("Cannot find ${table.name} entry for id $id")
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "findAddress")
    suspend fun handleFindWrapper(message: JsonObject) =
            handleFind(message.toRecord(recordClass))

    protected open suspend fun handleFind(message: R): JsonArray {
        val query = message
        log.trace("Find requested in table ${table.name}:\n$query")

        val resp = db {
            selectFrom(table)
                    .where(message.toWhere())
                    .fetch()
                    .into(JsonObject::class.java)
                    .let{ io.vertx.core.json.JsonArray(it) }
        }
        log.trace("Found ${resp.size()} records")
        return resp
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "updateAddress")
    suspend fun handleUpdateWrapper(message: JsonObject) =
            handleUpdate(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleUpdate(message: R): R {
        val id = message[pk]
        log.trace("Update requested for id = $id in table ${table.name}:\n$message")
        return db {
            sqlStateAware {
                update(table)
                        .set(message)
                        .where(pk.eq(id))
                        .returning()
                        .fetch()
                        .into(recordClass)
                        .firstOrNull()
                        ?: throw NotFound("Cannot find ${table.name} entry for id $id")
            }
        }
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "createAddress")
    suspend fun handleCreateWrapper(message: JsonObject) =
            handleCreate(message.toRecord(recordClass)).toJson()

    protected open suspend fun handleCreate(message: R): R {
        log.trace("Create requested in table ${table.name}:\n$message")

        for (field in table.primaryKey.fieldsArray) {
            message.reset(field)
        }

        return db {
            sqlStateAware {
                insertInto(table)
                        .set(message)
                        .returning()
                        .fetch()
                        .into(recordClass)
                        .first()
            }
        }
    }

    private fun makeFindCondition(table: Table<*>, record: JsonObject): Condition {
        val queryFields = table
                .fields()
                .asSequence()
                .filter { record[it] != null }
                .map { it.uncheckedCast<Field<Any>>() }
                .toList()

        return DSL.and(queryFields.map { table.field(it).eq(record[it]) })
    }

    private fun jsonb_build_object(args: List<QueryPart>) =
            FunctionCall<Any>("jsonb_build_object", Any::class, args).coerce(PostgresDataTypeEx.JSONB)

    private fun to_jsonb(arg: QueryPart) =
            FunctionCall<Any>("to_jsonb", arg).coerce(PostgresDataTypeEx.JSONB)

    private fun array(arg: QueryPart) =
            FunctionCall<Any>("array", arg)

    private fun convertField(field: Field<*>): Field<*> {
        if (field.dataType.isDateTime) {
            return DSL.field("((EXTRACT(EPOCH FROM ({0}::TIMESTAMP WITH TIME ZONE)) * 1000)::BIGINT)", Long::class.java, field).uncheckedCast()
        }
        return field
    }

}

@AutoDeployable
class DenizenVerticle : CrudDatabaseVerticle<DenizenRecord>(Tables.DENIZEN)

@AutoDeployable
class GameVerticle: CrudDatabaseVerticle<GameRecord>(Tables.GAME) {
    val readPageAddress = Address.DB.readPage(entityName)
    val countAddress = Address.DB.count(entityName)

    @JsonableEventBusConsumerForDynamic(addressProperty = "readPageAddress")
    suspend fun handlePageWrapper(message: JsonObject) =
            handlePage(message.toJsonable())

    private suspend fun handlePage(message: ReadPageMsg): JsonArray {
        val find: GameRecord = message.find.toRecord()

        val query = message
        log.trace("Page requested in table ${table.name}:\n$query")

        val resp = db {
            selectFrom(table)
                    .where(find.toWhere())
                    .orderBy(Tables.GAME.TIME.desc())
                    .limit(message.pageSize)
                    .offset(message.page * message.pageSize)
                    .fetch()
                    .into(JsonObject::class.java)
                    .let{ io.vertx.core.json.JsonArray(it) }
        }
        log.trace("Returning ${resp.size()} records")
        return resp
    }

    @JsonableEventBusConsumerForDynamic(addressProperty = "countAddress")
    suspend fun handleCountWrapper(message: JsonObject) =
            handleCount(message.toRecord())

    private suspend fun handleCount(message: GameRecord): CountResponse {
        log.trace("Count requested in table ${table.name}:\n$message")

        val (size) = db {
            selectCount()
                    .from(table)
                    .where(message.toWhere())
                    .fetchOne()
        }
        log.trace("Returning $size")
        return CountResponse(size)
    }


}
