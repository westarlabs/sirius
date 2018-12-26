package org.starcoin.sirius.core

import kotlinx.serialization.Serializable

@Serializable
class TestObject() : SiriusObject() {
    var name: String = ""
    var age: Int = 0
        set(value) {
            field = value
            resetHash()
        }

    constructor(name: String, age: Int) : this() {
        this.name = name
        this.age = age
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestObject) return false

        if (name != other.name) return false
        if (age != other.age) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + age
        return result
    }

}
