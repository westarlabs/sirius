package org.starcoin.sirius.core

interface Mockable {

    /**
     * Support mock random data, just for test.
     */
    fun mock(context: MockContext)

}
