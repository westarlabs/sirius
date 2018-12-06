package org.starcoin.sirius.core;

import org.starcoin.core.MerklePath;

import java.util.logging.Logger;

public class TransferDeliveryChallenge {

    private Update update;

    private ChallengeStatus status;

    private OffchainTransaction transaction;

    private MerklePath<OffchainTransaction> provePath;

    private Logger logger = Logger.getLogger(TransferDeliveryChallenge.class.getName());

    public TransferDeliveryChallenge(Update update, OffchainTransaction trans, MerklePath path) {
        this.update = update;
        this.transaction = trans;
        this.provePath = path;
    }

    public Update getUpdate() {
        return this.update;
    }

    public void setUpdate(Update update) {
        this.update = update;
    }

    public ChallengeStatus getStatus() {
        return this.status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public OffchainTransaction getTransaction() {
        return this.transaction;
    }

    public void setTransaction(OffchainTransaction transaction) {
        this.transaction = transaction;
    }

    public MerklePath getProvePath() {
        return this.provePath;
    }

    public void setProvePath(MerklePath provePath) {
        this.provePath = provePath;
    }

    public boolean openChallenge() {
        synchronized (this) {
            if (this.status == null) {
                this.status = ChallengeStatus.OPEN;
                logger.warning("openChallenge succ");
                return true;
            }

            logger.warning(
                    "openChallenge err status : " + ((this.status == null) ? "null" : this.status));
            return false;
        }
    }

    public boolean closeChallenge() {
        synchronized (this) {
            if (this.status != null && this.status == ChallengeStatus.OPEN) {
                this.status = ChallengeStatus.CLOSE;
                logger.warning("closeChallenge succ");
                return true;
            }

            logger.warning(
                    "openChallenge err status : " + ((this.status == null) ? "null" : this.status));
            return false;
        }
    }

    public boolean isClosed() {
        synchronized (this) {
            return (this.status != null && this.status == ChallengeStatus.CLOSE);
        }
    }
}
