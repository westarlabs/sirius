package org.starcoin.sirius.lang

import org.junit.Assert
import org.junit.Test

class ResetableLazyData {
    var changedData = 0
    val delegate = resetableLazy { changedData }
    val readOnlyData by delegate
}

class ResetableLazyTest {

    @Test
    fun testResetableLazy() {
        val data = ResetableLazyData()
        data.changedData = 1
        Assert.assertEquals(data.changedData, data.readOnlyData)
        data.changedData = 2
        Assert.assertNotEquals(data.changedData, data.readOnlyData)
        data.delegate.reset()
        Assert.assertEquals(data.changedData, data.readOnlyData)
        data.changedData = 3
        Assert.assertNotEquals(data.changedData, data.readOnlyData)
    }
}
