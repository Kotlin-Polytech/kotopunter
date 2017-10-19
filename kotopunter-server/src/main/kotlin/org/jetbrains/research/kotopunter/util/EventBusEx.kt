package org.jetbrains.research.kotopunter.util

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.*
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.Warnings.DEPRECATION
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotopunter.eventbus.Address
import org.jetbrains.research.kotopunter.util.database.toJson
import org.jetbrains.research.kotopunter.util.database.toRecord
import org.jooq.Record
import org.jooq.Table
import org.jooq.TableRecord
import org.kohsuke.randname.RandomNameGenerator
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

/******************************************************************************/

const val KOTOED_REQUEST_UUID = "\$kotopunterrequestuuid\$"

val REQUEST_UUID_GEN = RandomNameGenerator()

fun newRequestUUID(): String = REQUEST_UUID_GEN.next()

fun <T> Message<T>.requestUUID() = headers()[KOTOED_REQUEST_UUID] ?: "UNKNOWN"

fun withRequestUUID(uuid: String = newRequestUUID()): DeliveryOptions =
        DeliveryOptions().addHeader(KOTOED_REQUEST_UUID, uuid)

/******************************************************************************/

suspend fun <ReturnType> EventBus.sendAsync(address: String, message: Any, deliveryOptions: DeliveryOptions = DeliveryOptions()): Message<ReturnType> {
    if (true != deliveryOptions.headers?.contains(KOTOED_REQUEST_UUID)) {
        deliveryOptions.addHeader(KOTOED_REQUEST_UUID,
                currentCoroutineName().name)
    }
    return vxa { send(address, message, deliveryOptions, it) }
}

@JvmName("sendJsonAsync")
suspend fun EventBus.sendAsync(address: String, message: Any, deliveryOptions: DeliveryOptions = DeliveryOptions()): Message<JsonObject> =
        sendAsync<JsonObject>(address, message, deliveryOptions)

@JvmName("trySendJsonAsync")
suspend fun EventBus.trySendAsync(address: String, message: Any, deliveryOptions: DeliveryOptions = DeliveryOptions()): Message<JsonObject>? =
        try {
            sendAsync<JsonObject>(address, message, deliveryOptions)
        } catch (ex: ReplyException) {
            null
        }

@Deprecated("Forgot to call .toJson()?",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("sendAsync(address, message.toJson())"))
@Suppress("UNUSED_PARAMETER")
suspend fun EventBus.sendAsync(address: String, message: Jsonable): Unit = Unit

@Deprecated("Forgot to call .toJson()?",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("reply(message.toJson())"))
@Suppress("UNUSED_PARAMETER", "EXTENSION_SHADOWED_BY_MEMBER")
suspend fun <T> Message<T>.reply(message: Jsonable): Unit = Unit

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any> EventBus.sendJsonable(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>
) {

    val toJson = getToJsonConverter(argClass.starProjectedType)
    val currentName = currentCoroutineName()
    send(address, toJson(value), DeliveryOptions().addHeader(KOTOED_REQUEST_UUID, currentName.name))
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any> EventBus.publishJsonable(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>
) {

    val toJson = getToJsonConverter(argClass.starProjectedType)
    val currentName = currentCoroutineName()
    publish(address, toJson(value), DeliveryOptions().addHeader(KOTOED_REQUEST_UUID, currentName.name))
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableAsync(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>,
        resultClass: KClass<out Result>
): Result {
    val toJson = getToJsonConverter(argClass.starProjectedType)
    val fromJson = getFromJsonConverter(resultClass.starProjectedType)
    return sendAsync(address, toJson(value)).body().let(fromJson).uncheckedCast<Result>()
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableCollectAsync(
        address: String,
        value: Argument,
        argClass: KClass<out Argument>,
        resultClass: KClass<out Result>
): List<Result> {
    val toJson = getToJsonConverter(argClass.starProjectedType)
    val fromJson = getFromJsonConverter(resultClass.starProjectedType)
    return sendAsync<JsonArray>(address, toJson(value))
            .body()
            .asSequence()
            .filterIsInstance<JsonObject>()
            .map(fromJson)
            .map { it.uncheckedCast<Result>() }
            .toList()
}

@PublishedApi
@Deprecated("Do not call directly")
internal suspend fun <Argument : Any, Result : Any> EventBus.sendJsonableCollectAsync(
        address: String,
        value: List<Argument>,
        argClass: KClass<out Argument>,
        resultClass: KClass<out Result>
): List<Result> {
    val toJson = getToJsonConverter(
            List::class.createType(
                    listOf(KTypeProjection.invariant(argClass.starProjectedType))))
    val fromJson = getFromJsonConverter(resultClass.starProjectedType)
    return sendAsync<JsonArray>(address, toJson(value))
            .body()
            .asSequence()
            .filterIsInstance<JsonObject>()
            .map(fromJson)
            .map { it.uncheckedCast<Result>() }
            .toList()
}

inline suspend fun <
        reified Argument : Any
        > EventBus.sendJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return sendJsonable(address, value, Argument::class)
}

inline suspend fun <
        reified Argument : Any
        > EventBus.publishJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return publishJsonable(address, value, Argument::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > EventBus.sendJsonableAsync(address: String, value: Argument): Result {
    @Suppress(DEPRECATION)
    return sendJsonableAsync(address, value, Argument::class, Result::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > EventBus.sendJsonableCollectAsync(address: String, value: Argument): List<Result> {
    @Suppress(DEPRECATION)
    return sendJsonableCollectAsync(address, value, Argument::class, Result::class)
}

@Target(AnnotationTarget.FUNCTION)
annotation class EventBusConsumerFor(val address: String)

@Target(AnnotationTarget.FUNCTION)
annotation class JsonableEventBusConsumerFor(val address: String)


@Target(AnnotationTarget.FUNCTION)
annotation class EventBusConsumerForDynamic(val addressProperty: String)

@Target(AnnotationTarget.FUNCTION)
annotation class JsonableEventBusConsumerForDynamic(val addressProperty: String)


@Target(AnnotationTarget.CLASS)
annotation class CleanupJsonFields(val fields: Array<String>)


private fun getToJsonConverter(type: KType): (value: Any) -> Any {
    val klazz = type.jvmErasure
    return when {
        klazz == JsonObject::class -> {
            { it.expectingIs<JsonObject>() }
        }
        klazz.isSubclassOf(Jsonable::class) -> {
            { it.expectingIs<Jsonable>().toJson() }
        }
        klazz.isSubclassOf(Record::class) -> {
            { it.expectingIs<Record>().toJson() }
        }
        klazz == Unit::class -> {
            { JsonObject() }
        }

    // collections
        klazz == JsonArray::class -> {
            { it }
        }
        klazz.isSubclassOf(Collection::class) -> {
            val elementMapper = getToJsonConverter(type.arguments.first().type!!);
            {
                (it as Collection<*>)
                        .asSequence()
                        .filterNotNull()
                        .map(elementMapper)
                        .toList()
                        .let(::JsonArray)
            }
        }

        else -> throw IllegalArgumentException("Non-jsonable class: $klazz")
    }
}

private fun getFromJsonConverter(type: KType): (value: Any) -> Any {
    val klazz = type.jvmErasure
    return when {
        klazz == JsonObject::class -> {
            { it.expectingIs<JsonObject>() }
        }
        klazz.isSubclassOf(Jsonable::class) -> {
            { fromJson(it.expectingIs<JsonObject>(), klazz) }
        }
        klazz.isSubclassOf(Record::class) -> {
            { it.expectingIs<JsonObject>().toRecord(klazz.uncheckedCast<KClass<out Record>>()) }
        }
        klazz == Unit::class -> {
            {}
        }

    // collections
        klazz == JsonArray::class -> {
            { it.expectingIs<JsonArray>() }
        }
        klazz.isSubclassOf(List::class) -> {
            val elementMapper = getFromJsonConverter(type.arguments.first().type!!);
            { it.expectingIs<JsonArray>().map(elementMapper) }
        }
        klazz.isSubclassOf(Set::class) -> {
            val elementMapper = getFromJsonConverter(type.arguments.first().type!!);
            { it.expectingIs<JsonArray>().map(elementMapper).toSet() }
        }

        else -> throw IllegalArgumentException("Non-jsonable class: $klazz")
    }
}

object ConsumerAutoRegister : Loggable

fun AbstractKotopunterVerticle.registerAllConsumers() {
    val klass = this::class

    val cleanupJsonFields = klass.annotations
            .filterIsInstance<CleanupJsonFields>()
            .firstOrNull()
            ?.fields ?: emptyArray()

    for (function in klass.memberFunctions) {
        for (annotation in function.annotations) {
            when (annotation) {
                is EventBusConsumerFor ->
                    registerRawConsumer(function, annotation.address, cleanupJsonFields)
                is JsonableEventBusConsumerFor ->
                    registerJsonableConsumer(function, annotation.address, cleanupJsonFields)
                is EventBusConsumerForDynamic -> {
                    val address = klass
                            .memberProperties
                            .find { it.name == annotation.addressProperty }
                            ?.call(this)
                            as? String
                            ?: throw IllegalStateException("Property ${annotation.addressProperty} not found in class $klass")
                    registerRawConsumer(function, address, cleanupJsonFields)
                }
                is JsonableEventBusConsumerForDynamic -> {
                    val address = klass
                            .memberProperties
                            .find { it.name == annotation.addressProperty }
                            ?.call(this)
                            as? String
                            ?: throw IllegalStateException("Property ${annotation.addressProperty} not found in class $klass")
                    registerJsonableConsumer(function, address, cleanupJsonFields)
                }
            }
        }
    }
}

private fun AbstractVerticle.registerRawConsumer(
        function: KFunction<*>,
        address: String,
        cleanupJsonFields: Array<String>
) {
    val klass = this::class
    val eb = vertx.eventBus()
    ConsumerAutoRegister.log.info(
            "Auto-registering raw consumer for address $address \n" +
                    "using function $function"
    )

    if (function.isSuspend) {
        eb.consumer<JsonObject>(address) { msg ->
            launch(DelegateLoggable(klass.java).WithExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields))
                    + VertxContext(vertx)
                    + CoroutineName(msg.requestUUID())) {
                function.callAsync(this@registerRawConsumer, msg)
            }
        }
    } else {
        eb.consumer<JsonObject>(address) { msg ->
            val oldName = Thread.currentThread().name
            try {
                Thread.currentThread().name = msg.requestUUID()
                DelegateLoggable(klass.java).withExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields)) {
                    function.call(this@registerRawConsumer, msg)
                }
            } finally {
                Thread.currentThread().name = oldName
            }
        }
    }
}

private fun AbstractVerticle.registerJsonableConsumer(
        function: KFunction<*>,
        address: String,
        cleanupJsonFields: Array<String>
) {
    val klass = this::class
    val eb = vertx.eventBus()

    ConsumerAutoRegister.log.info(
            "Auto-registering json-based consumer for address $address \n" +
                    "using function $function"
    )

    // TODO handle parameterless consumers
    // first parameter is the receiver, we need the second one
    val argNumber = function.parameters.size
    val parameterType = function.parameters.drop(1).firstOrNull()?.type ?: typeOf{ Unit }

    val resultType = function.returnType
    val toJson = getToJsonConverter(resultType)
    val fromJson = getFromJsonConverter(parameterType)

    if (function.isSuspend) {
        eb.consumer<JsonObject>(address) { msg ->
            launch(DelegateLoggable(klass.java).WithExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields))
                    + VertxContext(vertx)
                    + CoroutineName(msg.requestUUID())) {
                val argument = fromJson(msg.body())
                val res = expectNotNull(function.callAsync(this@registerJsonableConsumer, argument))
                msg.reply(toJson(res))
            }
        }
    } else {
        eb.consumer<JsonObject>(address) { msg ->
            val oldName = Thread.currentThread().name
            try {
                Thread.currentThread().name = msg.requestUUID()
                DelegateLoggable(klass.java).withExceptions(CleanedUpMessageWrapper(msg, cleanupJsonFields)) {
                    val argument = fromJson(msg.body())
                    val res = expectNotNull(function.call(*listOf(this@registerJsonableConsumer, argument).take(argNumber).toTypedArray()))
                    msg.reply(toJson(res))
                }
            } finally {
                Thread.currentThread().name = oldName
            }
        }
    }
}

object DebugInterceptor : Handler<SendContext<*>>, Loggable {
    override fun handle(event: SendContext<*>) {
        val message = event.message()
        log.trace("Message to ${message.address()}[${message.replyAddress() ?: ""}]")
        event.next()
    }
}

open class AbstractKotopunterVerticle : AbstractVerticle(), Loggable {
    override fun start(startFuture: Future<Void>) {
        registerAllConsumers()
        super.start(startFuture)
    }

    suspend fun<R> async(dispatcher: CoroutineContext = VertxContext(vertx), body: suspend () -> R) =
            kotlinx.coroutines.experimental.async(LogExceptions() + dispatcher + currentCoroutineName()) {
                body()
            }

    fun<R> spawn(dispatcher: CoroutineContext = VertxContext(vertx), body: suspend () -> R) {
        var context = dispatcher
        if(context[CoroutineName.Key] == null) {
            val name = newRequestUUID()
            log.trace("Assigning $name to spawned call of $body")
            context += CoroutineName(name)
        }

        launch(LogExceptions() + context) { body() }
    }

}

inline suspend fun <
        reified Argument : Any
        > AbstractKotopunterVerticle.sendJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonable(address, value)
}

inline suspend fun <
        reified Argument : Any
        > AbstractKotopunterVerticle.publishJsonable(address: String, value: Argument) {
    @Suppress(DEPRECATION)
    return vertx.eventBus().publishJsonable(address, value)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > AbstractKotopunterVerticle.sendJsonableAsync(address: String, value: Argument): Result {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonableAsync(address, value, Argument::class, Result::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > AbstractKotopunterVerticle.sendJsonableCollectAsync(address: String, value: Argument): List<Result> {
    @Suppress(DEPRECATION)
    return vertx.eventBus().sendJsonableCollectAsync(address, value, Argument::class, Result::class)
}

inline suspend fun <
        reified Result : Any,
        reified Argument : Any
        > AbstractKotopunterVerticle.trySendJsonableAsync(address: String, value: Argument): Result? {
    return try {
        sendJsonableAsync(address, value)
    } catch (ex: ReplyException) {
        if (ex.failureType() == ReplyFailure.NO_HANDLERS) null
        else throw ex
    }
}
