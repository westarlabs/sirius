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

        fun calculateEon(startBlockNumber: Long, currentBlockNumber: Long, blocksPerEon: Int): Eon {
            val blocks = (currentBlockNumber - startBlockNumber).toInt()
            return Eon(
                blocks / blocksPerEon,
                Epoch.values()[(blocks % blocksPerEon / (blocksPerEon / 4))]
            )
        }

        fun waitToEon(
            startBlockNumber: Long,
            currentBlockNumber: Long,
            blocksPerEon: Int,
            expectEon: Int
        ): Int {
            return blocksPerEon * expectEon - (currentBlockNumber - startBlockNumber).toInt()
        }

        fun waitBlockNumber(
            startBlockNumber: Long,
            blocksPerEon: Int,
            expectEon: Int
        ): Long {
            return startBlockNumber + blocksPerEon * expectEon
        }
    }
}
