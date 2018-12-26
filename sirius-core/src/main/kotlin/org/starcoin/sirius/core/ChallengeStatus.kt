package org.starcoin.sirius.core

import org.starcoin.proto.Starcoin

enum class ChallengeStatus constructor(private val stat: Starcoin.ProtoChallengeStatus) :
    ProtoEnum<Starcoin.ProtoChallengeStatus> {
    OPEN(Starcoin.ProtoChallengeStatus.OPEN), CLOSE(Starcoin.ProtoChallengeStatus.CLOSE);

    override fun toProto(): Starcoin.ProtoChallengeStatus {
        return stat
    }

    companion object {

        fun valueOf(stat: Int): ChallengeStatus {
            for (s in ChallengeStatus.values()) {
                if (s.number == stat) {
                    return s
                }
            }
            throw IllegalArgumentException("Unsupported status type:$stat")
        }
    }
}
