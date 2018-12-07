package org.starcoin.contract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.starcoin.core.Hash;
import org.starcoin.liquidity.BalanceUpdateChallenge;
import org.starcoin.liquidity.BalanceUpdateChallengeStatus;
import org.starcoin.liquidity.OffchainTransaction;
import org.starcoin.liquidity.TransferDeliveryChallenge;

/** Created by dqm on 2018/10/5. */
public class ChallengeSet {

  private Map<Hash, TransferDeliveryChallenge> transferDeliveryChallenges;
  private BalanceUpdateChallengeStatus challengeStatus;
  private GlobalBalance.Balance balance;

  private Logger logger = Logger.getLogger(ChallengeSet.class.getName());

  public ChallengeSet(GlobalBalance.Balance balance) {
    this.balance = balance;
  }

  public boolean openBalanceUpdateChallenge(BalanceUpdateChallenge challenge) {
    synchronized (this) {
      if (this.challengeStatus == null) {
        this.challengeStatus = new BalanceUpdateChallengeStatus(challenge);
        logger.info("openBalanceUpdateChallenge succ");
        return true;
      }

      logger.warning("openBalanceUpdateChallenge fail");
      return false;
    }
  }

  public BalanceUpdateChallengeStatus getBalanceUpdateChallengeStatus() {
    return this.challengeStatus;
  }

  public boolean closeBalanceUpdateChallenge() {
    return (this.challengeStatus != null && this.challengeStatus.closeChallenge());
  }

  public boolean openTransferDeliveryChallenge(TransferDeliveryChallenge challenge) {
    synchronized (this) {
      if (this.transferDeliveryChallenges == null)
        this.transferDeliveryChallenges = new ConcurrentHashMap<>();
      Hash hash = challenge.getTransaction().hash();
      if (!this.transferDeliveryChallenges.containsKey(hash)) {
        boolean flag = challenge.openChallenge();
        if (flag) {
          this.transferDeliveryChallenges.put(hash, challenge);
          logger.info("openTransferDeliveryChallenge succ");
          return true;
        }
      }

      logger.warning("openTransferDeliveryChallenge fail");
      return false;
    }
  }

  public boolean closeTransferDeliveryChallenge(OffchainTransaction transaction) {
    synchronized (this) {
      Hash hash = transaction.hash();
      return transferDeliveryChallenges.get(hash).closeChallenge();
    }
  }

  public Map<Hash, TransferDeliveryChallenge> getTransferDeliveryChallenges() {
    return transferDeliveryChallenges;
  }
}
