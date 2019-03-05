package org.starcoin.sirius.util

import kotlinx.io.PrintWriter
import kotlinx.io.StringWriter
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject

// Return logger for Java class, if companion object fix the name
fun <T : Any> logger(forClass: Class<T>): Logger {
    return Logger.getLogger(unwrapCompanionClass(forClass).name)
}

// unwrap companion class to enclosing class given a Java Class
fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}

// unwrap companion class to enclosing class given a Kotlin Class
fun <T : Any> unwrapCompanionClass(ofClass: KClass<T>): KClass<*> {
    return unwrapCompanionClass(ofClass.java).kotlin
}

// Return logger for Kotlin class
fun <T : Any> logger(forClass: KClass<T>): Logger {
    return logger(forClass.java)
}

// return logger from extended class (or the enclosing class)
fun <T : Any> T.logger(): Logger {
    return logger(this.javaClass)
}

// return a lazy logger property delegate for enclosing class
fun <R : Any> R.lazyLogger(): Lazy<Logger> {
    return lazy { logger(this.javaClass) }
}

// return a logger property delegate for enclosing class
fun <R : Any> R.injectLogger(): Lazy<Logger> {
    return lazyOf(logger(this.javaClass))
}

// marker interface and related extension (remove extension for Any.logger() in favour of this)
interface Loggable {}

fun Loggable.logger(): Logger = logger(this.javaClass)

fun Logger.log(level: Level, e: Throwable) {
    this.log(level, "${e.javaClass.name} : ${e.message}")
    this.log(level) {
        val out = StringWriter()
        e.printStackTrace(PrintWriter(out))
        out.toString()
    }
}

fun Logger.error(msg: String) {
    this.severe(msg)
}

fun Logger.error(msgSuply: () -> String) {
    this.severe(msgSuply)
}

fun Logger.error(e: Throwable) {
    this.log(Level.SEVERE, e)
}

fun Logger.warning(e: Throwable) {
    this.log(Level.WARNING, e)
}

fun Logger.info(e: Throwable) {
    this.log(Level.INFO, e)
}

fun Logger.fine(e: Throwable) {
    this.log(Level.FINE, e)
}

// abstract base class to provide logging, intended for companion objects more than classes but works for either
abstract class WithLogging : Loggable {
    val LOG = logger()

    init {
        val rootLogger = LogManager.getLogManager().getLogger("")
        rootLogger.level = Level.ALL
        for (h in rootLogger.handlers) {
            h.level = Level.ALL
        }
    }
}
