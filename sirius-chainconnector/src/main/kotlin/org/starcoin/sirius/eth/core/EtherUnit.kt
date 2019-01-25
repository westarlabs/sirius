package org.starcoin.sirius.eth.core

import org.starcoin.sirius.lang.toBigInteger
import java.math.BigInteger

interface EtherUnit {
    val radio: BigInteger
    fun <OtherUnit : EtherUnit> conversionRate(otherEtherUnit: OtherUnit): BigInteger {
        return radio / otherEtherUnit.radio
    }
}

class EtherNumber<out T : EtherUnit>(value: Number, factory: () -> T) {
    companion object {
        inline operator fun <reified K : EtherUnit> invoke(value: Number) = EtherNumber(value) {
            K::class.java.newInstance()
        }
    }

    val unit: T = factory()

    val value = value.toLong().toBigInteger()

    val inWei: EtherNumber<Wei>
        get() = converted()

    val inGWei: EtherNumber<Gwei>
        get() = converted()

    val inSzabo: EtherNumber<Szabo>
        get() = converted()

    val inFinney: EtherNumber<Finney>
        get() = converted()

    val inEther: EtherNumber<Ether>
        get() = converted()

    inline fun <reified OtherUnit : EtherUnit> converted(): EtherNumber<OtherUnit> {
        val otherInstance = OtherUnit::class.java.newInstance()
        return EtherNumber(value * unit.conversionRate(otherInstance))
    }

    operator fun plus(other: EtherNumber<EtherUnit>): EtherNumber<T> {
        val newValue = value + other.value * other.unit.conversionRate(unit)
        return EtherNumber(newValue) { unit }
    }

    operator fun minus(other: EtherNumber<EtherUnit>): EtherNumber<T> {
        val newValue = value - other.value * other.unit.conversionRate(unit)
        return EtherNumber(newValue) { unit }
    }

    operator fun times(other: Number): EtherNumber<T> {
        return EtherNumber(value * other.toBigInteger()) { unit }
    }

    operator fun div(other: Number): EtherNumber<T> {
        return EtherNumber(value / other.toBigInteger()) { unit }
    }

    operator fun inc() = EtherNumber(value + BigInteger.ONE) { unit }

    operator fun dec() = EtherNumber(value - BigInteger.ONE) { unit }

    operator fun compareTo(other: EtherNumber<EtherUnit>) = inWei.value.compareTo(other.inWei.value)

    operator fun contains(other: EtherNumber<EtherUnit>) = inWei.value >= other.inWei.value

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EtherNumber<EtherUnit>) return false
        return compareTo(other) == 0
    }

    override fun hashCode() = inWei.value.hashCode()

    override fun toString(): String {
        val unitString = unit::class.java.simpleName.toLowerCase()
        return value.toString()
            .plus(" ")
            .plus(unitString)
    }
}

class Wei : EtherUnit {
    override val radio: BigInteger = BigInteger.ONE
}

class Gwei : EtherUnit {
    override val radio: BigInteger = 1_000_000_000.toBigInteger()
}

class Szabo : EtherUnit {
    override val radio: BigInteger = 1_000_000_000_000L.toBigInteger()
}

class Finney : EtherUnit {
    override val radio: BigInteger = 1_000_000_000_000_000L.toBigInteger()
}

class Ether : EtherUnit {
    override val radio: BigInteger = 1_000_000_000_000_000_000L.toBigInteger()
}

val Number.wei: EtherNumber<Wei>
    get() = EtherNumber(this)

val Number.gwei: EtherNumber<Gwei>
    get() = EtherNumber(this)

val Number.szabo: EtherNumber<Szabo>
    get() = EtherNumber(this)

val Number.finney: EtherNumber<Finney>
    get() = EtherNumber(this)

val Number.ether: EtherNumber<Ether>
    get() = EtherNumber(this)
