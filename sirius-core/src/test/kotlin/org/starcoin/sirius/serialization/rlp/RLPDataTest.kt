package org.starcoin.sirius.serialization.rlp

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.util.WithLogging
import java.math.BigInteger

class RLPDataTest {

    companion object : WithLogging()

    lateinit var testJson: JSONObject

    @Before
    fun setup() {
        this.testJson =
            JSONParser().parse(this.javaClass.getResourceAsStream("rlptest.json").readBytes().toString(Charsets.UTF_8)) as JSONObject
    }

    @Test
    fun test() {
        for (kv in testJson) {
            val name = kv.key as String
            val value = kv.value as Map<String, *>
            LOG.info("RLP test $name $value")
            var input = value.getValue("in")!!
            if (input is String && input.startsWith("#")) {
                input = BigInteger(input.substring(1))
            }
            val output = value.getValue("out")!! as String
            val rlp = input.toRLP()
            val decodeRLP = output.hexToByteArray().decodeRLP()
            Assert.assertEquals(rlp.encode().toHEXString(), output)
            Assert.assertEquals(rlp, decodeRLP)
        }
    }

}
