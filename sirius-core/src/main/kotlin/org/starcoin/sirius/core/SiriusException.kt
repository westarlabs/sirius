package org.starcoin.sirius.core

import io.grpc.StatusRuntimeException
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.util.error

typealias Status = io.grpc.Status

open class SiriusException(val status: Status, message: String?, throwable: Throwable?) :
    RuntimeException(message, throwable) {

    constructor(message: String) : this(Status.UNKNOWN, message, null)

    constructor(status: Status, message: String?) : this(status, message, null)

    constructor(exception: Throwable, message: String?) : this(
        when (exception) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT
            is IllegalStateException -> Status.INTERNAL
            else -> Status.UNKNOWN
        }, message, exception
    )

    fun toGrpcException(): StatusRuntimeException {
        return StatusRuntimeException(this.status)
    }

    companion object : WithLogging()
}

fun fail(status: Status = Status.INTERNAL, lazyMessage: () -> String): Nothing {
    SiriusException.LOG.error("${status.code.name} ${lazyMessage()}")
    throw SiriusException(status, lazyMessage())
}

fun fail(exception: Throwable, lazyMessage: () -> String? = { exception.message }): Nothing {
    SiriusException.LOG.error("${exception.javaClass.name} ${lazyMessage()}")
    when (exception) {
        is SiriusException -> throw exception
        else -> throw SiriusException(exception, lazyMessage())
    }
}