package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin
import org.starcoin.sirius.util.MockUtils

//TODO move other enum
enum class Direction constructor(private val protoType: Starcoin.PathDirection) :
    ProtoEnum<Starcoin.PathDirection> {
    ROOT(Starcoin.PathDirection.DIRECTION_ROOT),
    LEFT(Starcoin.PathDirection.DIRECTION_LEFT),
    RIGHT(Starcoin.PathDirection.DIRECTION_RIGTH);

    override fun toProto(): Starcoin.PathDirection {
        return protoType
    }

    companion object {

        fun valueOf(number: Int): Direction {
            for (direction in Direction.values()) {
                if (direction.number == number) {
                    return direction
                }
            }
            return Direction.ROOT
        }

        fun random(): Direction {
            return Direction.values()[MockUtils.nextInt(
                0,
                values().size
            )]
        }
    }
}
