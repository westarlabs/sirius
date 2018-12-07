package org.starcoin.sirius.core

class Eon(val id: Int, val epoch: Epoch) {

    enum class Epoch {
        FIRST,
        SECOND,
        THIRD,
        LAST
    }

    companion object {

        fun calculateEon(blockHeight: Int, blocksPerEon: Int): Eon {
            return Eon(
                blockHeight / blocksPerEon,
                Epoch.values()[blockHeight % blocksPerEon / (blocksPerEon / 4)]
            )
        }
    }
}
