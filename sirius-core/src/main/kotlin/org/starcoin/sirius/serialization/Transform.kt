package org.starcoin.sirius.serialization

import com.google.protobuf.GeneratedMessageV3
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.staticFunctions

object Transform {

    inline fun <reified P : GeneratedMessageV3, T : Any> transform(t: T): P {
        var fromKclass = t.javaClass.kotlin
        var toKclass = P::class.java.kotlin
        //toKclass.java.getDeclaredMethod()

        var builder: Any =
            toKclass.staticFunctions.stream().filter { it.name == "newBuilder" && it.parameters.isEmpty() }.findAny()
                .map { it.call()!! }.orElseThrow {
                throw IllegalArgumentException(
                    "Illegal type:${toKclass}"
                )
            }
        var buildKclass = builder.javaClass.kotlin
        var toClassProperties = buildKclass.declaredMemberProperties
        //TODO
        fromKclass.declaredMemberProperties.forEach { fp ->
            toClassProperties.forEach { tp ->
                if (fp.name == tp.name) {
                    if (tp is KMutableProperty<*>) {
                        tp.setter.call(fp.get(t), "value")
                    }
                }
            }
        }
        return buildKclass.declaredMemberFunctions.stream().filter { it.name == "build" }.findAny()
            .map { it.call(builder) as P }.orElseThrow {
                throw IllegalArgumentException(
                    "Illegal type:${toKclass}"
                )
            }
    }
}