package org.starcoin.sirius.core


import java.util.logging.Logger

class TransferDeliveryChallenge(
    var update: UpdateData?,
    var transaction: OffchainTransaction?,
    path: MerklePath
) {

    var status: ChallengeStatus? = null

    private var provePath: MerklePath? = null

    private val logger = Logger.getLogger(TransferDeliveryChallenge::class.java.name)

    val isClosed: Boolean
        get() = synchronized(this) {
            return this.status != null && this.status == ChallengeStatus.CLOSE
        }

    init {
        this.provePath = path
    }

    fun getProvePath(): MerklePath? {
        return this.provePath
    }

    fun setProvePath(provePath: MerklePath) {
        this.provePath = provePath
    }

    fun openChallenge(): Boolean {
        synchronized(this) {
            if (this.status == null) {
                this.status = ChallengeStatus.OPEN
                logger.warning("openChallenge succ")
                return true
            }

            logger.warning(
                "openChallenge err status : " + if (this.status == null) "null" else this.status
            )
            return false
        }
    }

    fun closeChallenge(): Boolean {
        synchronized(this) {
            if (this.status != null && this.status == ChallengeStatus.OPEN) {
                this.status = ChallengeStatus.CLOSE
                logger.warning("closeChallenge succ")
                return true
            }

            logger.warning(
                "openChallenge err status : " + if (this.status == null) "null" else this.status
            )
            return false
        }
    }
}
