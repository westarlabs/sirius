package org.starcoin.contract;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.starcoin.core.BlockAddress;
import org.starcoin.core.Hash;
import org.starcoin.liquidity.*;
import org.starcoin.liquidity.AugmentedMerkleTree.AugmentedMerkleTreeNode;

/** Created by dqm on 2018/10/3. */
public class GlobalBalance {

  private Logger logger = Logger.getLogger(GlobalBalance.class.getName());

  public static final int INIT_EON = -1;
  public static final int BALANCE_LEN = 3;
  private Balance LATEST;
  private final Set<WithdrawalStatus> UNDO = new CopyOnWriteArraySet<>();
  public HubMeta HUB;
  public final Set<BlockAddress> RECOVERY = new CopyOnWriteArraySet<>();

  public volatile AtomicBoolean RECOVERY_MODE_FLAG = new AtomicBoolean(false);

  public Map<Hash, AtomicLong> DEPOSITS = new ConcurrentHashMap<>(); // check allotment

  public GlobalBalance(PublicKey pubKey) {
    this.LATEST = new Balance(INIT_EON, this);
    this.HUB = new HubMeta(pubKey);
  }

  public PublicKey getHubPublicKey() {
    return this.HUB.getPubKey();
  }

  public BlockAddress getBlockAddress() {
    return this.HUB.getBlockAddress();
  }

  public HubRoot getRoot() {
    return this.LATEST.getRoot();
  }

  public Set<BlockAddress> getRecovery() {
    return this.RECOVERY;
  }

  public HubRoot getRootByIndex(int index) {
    Balance tmp = getBalance(index);

    return (tmp != null) ? tmp.getRoot() : null;
  }

  public HubRoot getRootByEon(int eon) {
    Balance tmp = this.LATEST;
    while (tmp != null) {
      if (tmp.getEon() == eon) {
        return tmp.getRoot();
      } else {
        tmp = tmp.getPre();
      }
    }

    return null;
  }

  public Long getDepositByIndex(int index, Hash addr) {
    Balance tmp = getBalance(index);

    return (tmp != null) ? tmp.getDepositByAddr(addr) : null;
  }

  public WithdrawalStatus getWithdrawalByIndex(int index, Hash addr) {
    Balance tmp = getBalance(index);

    return (tmp != null) ? tmp.getWithdrawal().get(addr) : null;
  }

  private Balance getBalance(int index) {
    Balance tmp = this.LATEST;
    int count = 0;
    while (count < index && tmp.getPre() != null) {
      count++;
      tmp = tmp.getPre();
    }

    return tmp;
  }

  public WithdrawalStatus queryWithdrawalStatus(BlockAddress addr, int index) {
    Balance tmp = getBalance(index);

    return (tmp != null && tmp.getWithdrawal() != null)
        ? tmp.getWithdrawal().get(addr.hash())
        : null;
  }

  public WithdrawalStatus queryWithdrawalStatus(BlockAddress addr) {
    Balance tmp = this.LATEST;
    while (tmp != null) {
      if (tmp.getWithdrawal() != null) {
        WithdrawalStatus withdrawalStatus = tmp.getWithdrawal().get(addr.hash());
        if (withdrawalStatus != null) {
          return withdrawalStatus;
        } else {
          tmp = tmp.getPre();
        }
      }
    }

    return null;
  }

  public WithdrawalStatus latestWithdrawal3Eon(BlockAddress addr) {
    WithdrawalStatus withdrawalStatus = null;
    Hash hashAddr = addr.hash();
    if (this.LATEST.getPre() != null) {
      if (this.LATEST.getPre().getPre() != null) {
        withdrawalStatus = this.LATEST.getPre().getPre().getWithdrawal().get(hashAddr);
      }

      if (withdrawalStatus == null || withdrawalStatus.finish()) {
        withdrawalStatus = this.LATEST.getPre().getWithdrawal().get(hashAddr);
      }
    }

    if (withdrawalStatus == null || withdrawalStatus.finish()) {
      withdrawalStatus = this.LATEST.getWithdrawal().get(hashAddr);
    }

    return withdrawalStatus;
  }

  public WithdrawalStatus latestWithdrawal(BlockAddress addr) {
    Hash hashAddr = addr.hash();
    return this.LATEST.getWithdrawal().get(hashAddr);
  }

  public synchronized void withdrawal(BlockAddress addr, WithdrawalStatus withdrawal) {
    this.LATEST.doWithdrawal(addr, withdrawal);
  }

  public synchronized boolean cancelWithdrawal(BlockAddress addr) {
    return this.LATEST.doCancel(addr);
  }

  public boolean recoveryMode() {
    return this.RECOVERY_MODE_FLAG.get();
  }

  public Set<WithdrawalStatus> getUndo() {
    return this.UNDO;
  }

  public int getCurrentEon() {
    return this.LATEST.getEon();
  }

  public int getEonByIndex(int index) {
    Balance tmp = this.LATEST;
    int count = 0;
    while (count < index && tmp.getPre() != null) {
      count++;
      tmp = tmp.getPre();
    }

    return tmp.getEon();
  }

  public synchronized long computAllotment() {
    return (this.LATEST.getEon() == INIT_EON)
        ? (this.LATEST.getDepositAmount() - this.LATEST.getWithdrawalAmount())
        : (this.LATEST.getRoot().getAllotment()
            + this.LATEST.getDepositAmount()
            - this.LATEST.getWithdrawalAmount());
  }

  public synchronized boolean commit(HubRoot root) throws RuntimeException {
    long allotment = computAllotment();
    if (root.getAllotment() == allotment) {
      Balance balance = new Balance(root.getEon(), this);
      balance.setRoot(root);
      balance.setPre(this.LATEST);

      this.LATEST = balance;
      return true;
    } else {
      logger.severe(
          "expect allotment:"
              + allotment
              + ", but get:"
              + root.toJson()
              + ", latest:"
              + this.LATEST.toString());
    }
    return false;
  }

  public void clearBalance() {
    Balance tmp = this.LATEST;
    int count = 0;
    while (count < BALANCE_LEN && tmp.getPre() != null) {
      count++;
      tmp = tmp.getPre();
    }

    if (count == BALANCE_LEN && tmp != null) {
      tmp.setPre(null);
    }
  }

  public void deposit(Deposit deposit) {
    this.LATEST.doDeposit(deposit);
  }

  public synchronized boolean openBalanceChallenge(BalanceUpdateChallenge challenge) {
    return this.LATEST.doOpenBalanceChallenge(challenge);
  }

  public synchronized boolean closeBalanceChallenge(Hash addr) {
    return this.LATEST.doCloseBalanceChallenge(addr);
  }

  public synchronized BalanceUpdateChallengeStatus getBalanceChallenge(Hash addr) {
    return (this.LATEST.getChallengeByAddr(addr) != null)
        ? this.LATEST.getChallengeByAddr(addr).getBalanceUpdateChallengeStatus()
        : null;
  }

  public synchronized List<TransferDeliveryChallenge> queryTransferChallenges(Hash addr) {
    if (this.LATEST.getChallengeByAddr(addr) != null
        && this.LATEST.getChallengeByAddr(addr).getTransferDeliveryChallenges() != null) {
      Map<Hash, TransferDeliveryChallenge> challenges =
          this.LATEST.getChallengeByAddr(addr).getTransferDeliveryChallenges();
      List<TransferDeliveryChallenge> list = new ArrayList(challenges.size());
      list.addAll(challenges.values());
      return list;
    }

    return null;
  }

  public synchronized TransferDeliveryChallenge getTransferChallenge(Hash addr, Hash transfer) {
    return (this.LATEST.getChallengeByAddr(addr) != null
            && this.LATEST.getChallengeByAddr(addr).getTransferDeliveryChallenges() != null)
        ? this.LATEST.getChallengeByAddr(addr).getTransferDeliveryChallenges().get(transfer)
        : null;
  }

  public synchronized boolean doOpenTransferDeliveryChallenge(TransferDeliveryChallenge challenge) {
    return this.LATEST.doOpenTransferDeliveryChallenge(challenge);
  }

  public synchronized boolean doCloseTransferDeliveryChallenge(OffchainTransaction transaction) {
    return this.LATEST.doCloseTransferDeliveryChallenge(transaction);
  }

  public synchronized void doRecovery() {
    boolean flag = false;
    Map<Hash, ChallengeSet> challenges = this.LATEST.getChallenges();

    if (challenges != null && challenges.size() > 0) {
      for (ChallengeSet cs : challenges.values()) {
        if (flag) break;
        BalanceUpdateChallengeStatus bc = cs.getBalanceUpdateChallengeStatus();
        if (bc != null && !bc.isClosed()) {
          flag = true;
          break;
        }

        Map<Hash, TransferDeliveryChallenge> tc = cs.getTransferDeliveryChallenges();
        if (tc != null && tc.size() > 0) {
          for (TransferDeliveryChallenge t : tc.values()) {
            if (!t.isClosed()) {
              flag = true;
              break;
            }
          }
        }
      }
    }
    if (flag) this.RECOVERY_MODE_FLAG.set(true);
  }

  public synchronized long getRecoverFunds(AugmentedMerkleTreeNode leaf, BlockAddress addr) {
    Balance oldBlance = this.LATEST.getPre();
    return leaf.getAllotment()
        + oldBlance.getDepositByAddr(addr)
        + this.LATEST.getDepositByAddr(addr);
  }

  public boolean zeroAllotment(BlockAddress addr) {
    Hash hash = addr.hash();
    if (!this.DEPOSITS.containsKey(hash) || this.DEPOSITS.get(hash).longValue() == 0) {
      return true;
    }

    long leastDeposit = this.LATEST.getDepositByAddr(addr);
    long secondDeposit = this.LATEST.getPre().getDepositByAddr(addr);
    if (this.DEPOSITS.get(hash).longValue() == (leastDeposit + secondDeposit)) {
      return true;
    }
    return false;
  }

  public static class Balance {

    private Balance pre;
    private GlobalBalance globalBalance;

    private Logger logger2 = Logger.getLogger(Balance.class.getName());

    public GlobalBalance getGlobalBalance() {
      return this.globalBalance;
    }

    public Balance(int eon, GlobalBalance globalBalance) {
      this.eon = eon;
      this.globalBalance = globalBalance;
    }

    private int eon;

    private HubRoot root;
    private volatile AtomicLong depositAmount = new AtomicLong(0);
    private volatile AtomicLong withdrawalAmount = new AtomicLong(0);
    private Map<Hash, AtomicLong> deposit = new ConcurrentHashMap<>();
    private Map<Hash, WithdrawalStatus> withdrawal = new ConcurrentHashMap<>();
    private Map<Hash, ChallengeSet> challenges = new ConcurrentHashMap<>();

    public Balance getPre() {
      return this.pre;
    }

    public int getEon() {
      return this.eon;
    }

    public HubRoot getRoot() {
      return this.root;
    }

    public long getDepositAmount() {
      return this.depositAmount.longValue();
    }

    public long getWithdrawalAmount() {
      return this.withdrawalAmount.longValue();
    }

    public Map<Hash, ChallengeSet> getChallenges() {
      return this.challenges;
    }

    public ChallengeSet getChallengeByAddr(Hash addr) {
      return this.challenges.get(addr);
    }

    public Map<Hash, AtomicLong> getDeposit() {
      return this.deposit;
    }

    public Map<Hash, WithdrawalStatus> getWithdrawal() {
      return this.withdrawal;
    }

    public void setRoot(HubRoot root) {
      this.root = root;
    }

    public void setPre(Balance pre) {
      this.pre = pre;
    }

    private void doDeposit(Deposit d) {
      synchronized (this) {
        Hash hashAddr = d.getAddress().hash();
        if (!this.deposit.containsKey(hashAddr)) {
          AtomicLong first = new AtomicLong(d.getAmount());
          this.deposit.put(hashAddr, first);
        } else {
          this.deposit.get(hashAddr).addAndGet(d.getAmount());
        }

        this.depositAmount.addAndGet(d.getAmount());

        if (!globalBalance.DEPOSITS.containsKey(hashAddr)) {
          globalBalance.DEPOSITS.put(hashAddr, new AtomicLong(d.getAmount()));
        } else {
          globalBalance.DEPOSITS.get(hashAddr).addAndGet(d.getAmount());
        }

        logger2.info("doDeposit succ");
      }
    }

    private void doWithdrawal(BlockAddress addr, WithdrawalStatus w) {
      synchronized (this) {
        this.withdrawal.put(addr.hash(), w);
        this.globalBalance.UNDO.add(w);
        this.withdrawalAmount.addAndGet(w.getWithdrawalAmount());
      }
    }

    private boolean doCancel(BlockAddress addr) {
      synchronized (this) {
        WithdrawalStatus withdrawal = this.withdrawal.get(addr.hash());

        if (withdrawal != null) {
          boolean tmp = withdrawal.cancel();
          if (tmp) {
            this.withdrawalAmount.addAndGet(-withdrawal.getWithdrawalAmount());
            this.globalBalance.UNDO.remove(withdrawal);

            logger2.info("doCancel succ");
            return true;
          }
        }
      }

      logger2.info("doCancel fail");
      return false;
    }

    private boolean doOpenBalanceChallenge(BalanceUpdateChallenge challenge) {
      synchronized (this) {
        Hash addr = BlockAddress.genBlockAddressFromPublicKey(challenge.getPublicKey()).hash();

        if (!this.challenges.containsKey(addr)) {
          ChallengeSet challengeSet = new ChallengeSet(this);
          this.challenges.put(addr, challengeSet);
        }

        return this.challenges.get(addr).openBalanceUpdateChallenge(challenge);
      }
    }

    private boolean doCloseBalanceChallenge(Hash addr) {
      synchronized (this) {
        if (this.challenges.containsKey(addr))
          return this.challenges.get(addr).closeBalanceUpdateChallenge();

        logger2.warning("doCloseBalanceChallenge : balance challenge not exist");
        return false;
      }
    }

    private boolean doOpenTransferDeliveryChallenge(TransferDeliveryChallenge challenge) {
      synchronized (this) {
        Hash addr = challenge.getTransaction().getTo().hash();

        if (!this.challenges.containsKey(addr)) {
          ChallengeSet challengeSet = new ChallengeSet(this);
          this.challenges.put(addr, challengeSet);
        }

        return this.challenges.get(addr).openTransferDeliveryChallenge(challenge);
      }
    }

    private boolean doCloseTransferDeliveryChallenge(OffchainTransaction transaction) {
      synchronized (this) {
        Hash addr = transaction.getTo().hash();
        return this.challenges.get(addr).closeTransferDeliveryChallenge(transaction);
      }
    }

    public long getDepositByAddr(BlockAddress address) {
      return getDepositByAddr(address.hash());
    }

    public long getDepositByAddr(Hash address) {
      synchronized (this) {
        long amount = 0;
        if (this.deposit.containsKey(address)) {
          amount = this.deposit.get(address).longValue();
        }

        return amount;
      }
    }

    @Override
    public String toString() {
      return "Balance{"
          + "eon="
          + eon
          + ", root="
          + root
          + ", depositAmount="
          + depositAmount
          + ", withdrawalAmount="
          + withdrawalAmount
          + ", deposit="
          + deposit
          + ", withdrawal="
          + withdrawal
          + ", challenges="
          + challenges
          + '}';
    }
  }
}
