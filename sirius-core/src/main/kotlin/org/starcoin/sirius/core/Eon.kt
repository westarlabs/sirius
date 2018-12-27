package org.starcoin.sirius.core

class Eon(val id: Int, val epoch: Epoch) {

    constructor(id: Long, epoch: Epoch) : this(id.toInt(), epoch)

    enum class Epoch {
        FIRST,
        SECOND,
        THIRD,
        LAST
    }

    companion object {

        fun calculateEon(blockHeight: Long, blocksPerEon: Int): Eon {
            return Eon(
                blockHeight / blocksPerEon.toLong(),
                Epoch.values()[(blockHeight % blocksPerEon / (blocksPerEon / 4)).toInt()]
            )
        }
    }
}
