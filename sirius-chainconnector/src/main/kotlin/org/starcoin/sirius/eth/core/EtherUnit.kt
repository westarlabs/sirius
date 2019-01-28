package org.starcoin.sirius.eth.core

import org.starcoin.sirius.lang.toBigInteger
import java.math.BigDecimal
import java.math.BigInteger

sealed class EtherUnit {
    abstract val radio: BigInteger
    fun <OtherUnit : EtherUnit> conversionRate(otherEtherUnit: OtherUnit): BigDecimal {
        return radio.toBigDecimal().divide(otherEtherUnit.radio.toBigDecimal())
    }

    operator fun compareTo(other: EtherUnit) = this.radio.compareTo(other.radio)

    object Wei : EtherUnit() {
        override val radio: BigInteger = BigInteger.ONE
    }

    object Gwei : EtherUnit() {
        override val radio: BigInteger = 1_000_000_000.toBigInteger()
    }

    object Szabo : EtherUnit() {
        override val radio: BigInteger = 1_000_000_000_000L.toBigInteger()
    }

    object Finney : EtherUnit() {
        override val radio: BigInteger = 1_000_000_000_000_000L.toBigInteger()
    }

    object Ether : EtherUnit() {
        override val radio: BigInteger = 1_000_000_000_000_000_000L.toBigInteger()
    }

    companion object {
        val units: List<EtherUnit> by lazy {
            EtherUnit::class.sealedSubclasses.map { it.objectInstance!! }.sortedByDescending { it.radio }
        }
    }
}


class EtherNumber<out T : EtherUnit>(value: Number, val unit: T) {

    val value = value.toBigInteger()

    val inWei: EtherNumber<EtherUnit.Wei>
        get() = converted()

    val inGWei: EtherNumber<EtherUnit.Gwei>
        get() = converted()

    val inSzabo: EtherNumber<EtherUnit.Szabo>
        get() = converted()

    val inFinney: EtherNumber<EtherUnit.Finney>
        get() = converted()

    val inEther: EtherNumber<EtherUnit.Ether>
        get() = converted()
    /**
     * largest unit that without loss of precision
     */
    val inBestUnit: EtherNumber<EtherUnit>
        get() {
            //val unit = EtherUnit.bestUnit(this.inWei)
            //EtherNumber(unit.conversionRate(value), unit)
            val weiNumber = this.inWei
            for (unit in EtherUnit.units) {
                if (unit.radio > weiNumber.value) {
                    continue
                }
                if (weiNumber.value % unit.radio != BigInteger.ZERO) {
                    continue
                }
                val newValue = weiNumber.value.toBigDecimal() * EtherUnit.Wei.conversionRate(unit)
                return EtherNumber(newValue, unit)
            }
            return weiNumber
        }

    inline fun <reified OtherUnit : EtherUnit> converted(): EtherNumber<OtherUnit> {
        val otherInstance = OtherUnit::class.objectInstance!!
        return EtherNumber(value.toBigDecimal() * unit.conversionRate(otherInstance), otherInstance)
    }

    operator fun plus(other: EtherNumber<EtherUnit>): EtherNumber<EtherUnit> {
        val newValue =
            this.value.toBigDecimal() * this.unit.conversionRate(EtherUnit.Wei) + other.value.toBigDecimal() * other.unit.conversionRate(
                EtherUnit.Wei
            )
        return EtherNumber(newValue, EtherUnit.Wei).inBestUnit
    }

    operator fun minus(other: EtherNumber<EtherUnit>): EtherNumber<EtherUnit> {
        val newValue =
            this.value.toBigDecimal() * this.unit.conversionRate(EtherUnit.Wei) - other.value.toBigDecimal() * other.unit.conversionRate(
                EtherUnit.Wei
            )
        return EtherNumber(newValue, EtherUnit.Wei).inBestUnit
    }

    operator fun times(other: Number): EtherNumber<T> {
        return EtherNumber(value * other.toBigInteger(), unit)
    }

    operator fun div(other: Number): EtherNumber<EtherUnit> {
        return EtherNumber(this.inWei.value / other.toBigInteger(), EtherUnit.Wei).inBestUnit
    }

    operator fun inc() = EtherNumber(value + BigInteger.ONE, unit)

    operator fun dec() = EtherNumber(value - BigInteger.ONE, unit)

    operator fun compareTo(other: EtherNumber<EtherUnit>) = inWei.value.compareTo(other.inWei.value)

    operator fun contains(other: EtherNumber<EtherUnit>) = inWei.value >= other.inWei.value

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EtherNumber<EtherUnit>) return false
        return compareTo(other) == 0
    }

    fun fuzzyEquals(other: EtherNumber<EtherUnit>, tolerance: EtherUnit): Boolean {
        return (inWei.value - other.inWei.value).abs() <= tolerance.radio
    }

    override fun hashCode() = inWei.value.hashCode()

    override fun toString(): String {
        val unitString = unit::class.java.simpleName.toLowerCase()
        return value.toString()
            .plus(" ")
            .plus(unitString)
    }
}


val Number.wei: EtherNumber<EtherUnit.Wei>
    get() = EtherNumber(this, EtherUnit.Wei)

val Number.gwei: EtherNumber<EtherUnit.Gwei>
    get() = EtherNumber(this, EtherUnit.Gwei)

val Number.szabo: EtherNumber<EtherUnit.Szabo>
    get() = EtherNumber(this, EtherUnit.Szabo)

val Number.finney: EtherNumber<EtherUnit.Finney>
    get() = EtherNumber(this, EtherUnit.Finney)

val Number.ether: EtherNumber<EtherUnit.Ether>
    get() = EtherNumber(this, EtherUnit.Ether)
