package org.jetbrains.research.kotopunter.util

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.jetbrains.research.kotopunter.util.StatusCodes.BAD_REQUEST
import org.jetbrains.research.kotopunter.util.StatusCodes.CONFLICT
import org.jetbrains.research.kotopunter.util.StatusCodes.FORBIDDEN
import org.jetbrains.research.kotopunter.util.StatusCodes.NOT_FOUND
import org.jetbrains.research.kotopunter.util.StatusCodes.UNAUTHORIZED

object StatusCodes {
    const val FORBIDDEN = 403
    const val INTERNAL_ERROR = 500
    const val NOT_FOUND = 404
    const val TIMED_OUT = 503
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val CONFLICT = 409
}

data class KotoedException(val code: Int, override val message: String) : Exception(message)

data class WrappedException(val inner: Throwable?) : Exception(inner)

fun NotFound(message: String) = KotoedException(code = NOT_FOUND, message = message)
fun Forbidden(message: String) = KotoedException(code = FORBIDDEN, message = message)
fun Unauthorized(message: String) = KotoedException(code = UNAUTHORIZED, message = message)
fun Conflict(message: String) = KotoedException(code = CONFLICT, message = message)
fun BadRequest(message: String) = KotoedException(code = BAD_REQUEST, message = message)

val Throwable.unwrapped
    get() =
        when (this) {
            is org.jetbrains.research.kotopunter.util.WrappedException -> inner ?: this
            is java.lang.reflect.InvocationTargetException -> cause ?: this
            else -> this
        }

fun codeFor(ex: Throwable): Int =
        when (ex) {
            is WrappedException -> ex.inner?.let(::codeFor) ?: StatusCodes.INTERNAL_ERROR
            is java.lang.reflect.InvocationTargetException -> ex.cause?.let(::codeFor) ?: StatusCodes.INTERNAL_ERROR
            is ReplyException -> ex.failureCode()
            is KotoedException -> ex.code
            is IllegalArgumentException, is IllegalStateException ->
                StatusCodes.BAD_REQUEST
            else ->
                StatusCodes.INTERNAL_ERROR
        }

inline fun Loggable.handleException(handler: (Throwable) -> Unit, t: Throwable) {
    val exception = t.unwrapped
    log.error("Exception caught", exception)
    log.error("Code: ${codeFor(exception)}")
    handler(exception)
}

inline fun <T> Loggable.withExceptions(handler: (Throwable) -> Unit, body: () -> T) =
        try {
            body()
        } catch (t: Throwable) {
            handleException(handler, t)
        }

fun <U> Loggable.handleException(msg: Message<U>, t: Throwable) {
    val exception = t.unwrapped
    log.error("Exception caught while handling message:\n" +
            "${msg.body()} sent to ${msg.address()}", exception)
    log.error("Code: ${codeFor(exception)}")
    msg.fail(
            codeFor(exception),
            exception.message
    )
}

fun <T> Loggable.withExceptions(handler: Handler<AsyncResult<T>>, body: () -> T) =
        try {
            body()
        } catch (t: Throwable) {
            handleException(handler, t)
        }

fun <T> Loggable.handleException(handler: Handler<AsyncResult<T>>, t: Throwable) {
    val exception = t.unwrapped
    log.error("Exception caught while handling async result", exception)
    log.error("Code: ${codeFor(exception)}")
    handler.handle(Future.failedFuture(t))
}

fun <T, U> Loggable.withExceptions(msg: Message<U>, body: () -> T) =
        try {
            body()
        } catch (t: Throwable) {
            handleException(msg, t)
        }

fun Loggable.handleException(ctx: RoutingContext, t: Throwable) {
    val exception = t.unwrapped
    log.error("Exception caught while handling request to ${ctx.request().uri()}", exception)
    log.error("Code: ${codeFor(exception)}")
    ctx.fail(exception)
}

fun <T> Loggable.withExceptions(ctx: RoutingContext, body: () -> T) =
        try {
            body()
        } catch (t: Throwable) {
            handleException(ctx, t)
        }

class CleanedUpMessageWrapper<T>(
        val msg: Message<T>,
        private val cleanupJsonFields: Array<String>) : Message<T> by msg {
    override fun body(): T = msg.body().cleanupJsonFields(cleanupJsonFields)
}

private fun <T> T.cleanupJsonFields(cleanupJsonFields: Array<String>): T {
    if (cleanupJsonFields.isEmpty()) return this

    return when (this) {
        is JsonObject -> JsonObject(
                this.copy()
                        .removeFields(*cleanupJsonFields)
                        .map
                        .mapValues { it.value.cleanupJsonFields(cleanupJsonFields) }
        ).uncheckedCast()
        is JsonArray -> JsonArray(
                this.copy()
                        .map { it.cleanupJsonFields(cleanupJsonFields) }
        ).uncheckedCast()
        else -> this
    }
}
