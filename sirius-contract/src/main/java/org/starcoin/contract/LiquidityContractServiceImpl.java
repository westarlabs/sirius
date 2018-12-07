package org.starcoin.contract;

import com.google.common.base.Preconditions;
import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.starcoin.chain.LiquidityChainService;
import org.starcoin.core.BlockAddress;
import org.starcoin.core.Hash;
import org.starcoin.core.MerklePath;
import org.starcoin.core.MerkleTree;
import org.starcoin.liquidity.*;
import org.starcoin.liquidity.AugmentedMerkleTree.AugmentedMerkleTreeNode;

/** Created by dqm on 2018/9/27. */
public class LiquidityContractServiceImpl implements LiquidityContractService {

  public GlobalBalance globalBalance;

  private LiquidityChainService chainService;
  private static final String RECOVERY_MODE_ERR = "recovery mode.";
  private static final String INVALID_MERKLE_PATH_ERR = "invalid merkle path.";
  private static final String INVALID_SIGN_ERR = "invalid sign.";
  private static final String INVALID_EON_ERR = "invalid eon.";
  private static final String INVALID_ROOT_ERR = "invalid root.";
  private static final String INVALID_ALLOTMENT_ERR = "invalid allotment.";
  private static final String INVALID_UPDATE_ERR = "invalid update.";
  private static final String UPDATE_VERSION_ERR = "invalid update version.";
  private static final String UPDATE_NOT_MATCH_ERR = "update not equals.";
  private static final String INVALID_HUB_SIGN_ERR = "invalid hub sign.";
  private static final String INVALID_AMOUNT_ERR = "invalid amount.";

  private Logger logger = Logger.getLogger(LiquidityContractServiceImpl.class.getName());

  public LiquidityContractServiceImpl(PublicKey publicKey, LiquidityChainService chainService) {
    this.chainService = chainService;
    this.globalBalance = new GlobalBalance(publicKey);
  }

  public boolean deposit(Deposit deposit) {
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);

    Preconditions.checkArgument(
        (deposit != null && deposit.getAddress() != null && deposit.getAmount() > 0));

    globalBalance.deposit(deposit);
    return true;
  }

  public boolean initiateWithdrawal(Withdrawal withdrawal) {
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);

    Preconditions.checkArgument(
        withdrawal != null
            && withdrawal.getAmount() > 0
            && withdrawal.getPath() != null
            && withdrawal.getPath().getLeaf() != null
            && withdrawal.getPath().getLeaf().getAccount() != null
            && withdrawal.getPath().getLeaf().getAccount().getAddress() != null
            && withdrawal.getAddress() != null);

    Preconditions.checkState(
        withdrawal
            .getPath()
            .getLeaf()
            .getAccount()
            .getAddress()
            .equals(withdrawal.getAddress().hash()),
        "err block address.");

    AugmentedMerkleTreeNode leaf = withdrawal.getPath().getLeaf();
    AccountInformation accountInformation = leaf.getAccount();
    BlockAddress addr = withdrawal.getAddress();
    WithdrawalStatus latestWithdrawal = globalBalance.latestWithdrawal3Eon(addr);

    Preconditions.checkState(
        (latestWithdrawal == null || latestWithdrawal.finish()),
        "last withdrawal has not been processed"); // issued

    HubRoot root = globalBalance.getRoot();
    Preconditions.checkState(
        AugmentedMerkleTree.verifyMembershipProof(
            root.hubRoot2AugmentedMerkleTreeNode(), withdrawal.getPath()),
        INVALID_MERKLE_PATH_ERR);
    Preconditions.checkState(
        (accountInformation.getAllotment() >= withdrawal.getAmount()), INVALID_AMOUNT_ERR);
    WithdrawalStatus withdrawalCoin = new WithdrawalStatus(withdrawal);
    globalBalance.withdrawal(addr, withdrawalCoin);
    logger.info("initiateWithdrawal succ");
    return true;
  }

  public boolean cancelWithdrawal(
      Participant participant, Update update, AugmentedMerklePath merklePath) {
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);

    Preconditions.checkArgument(
        participant != null
            && participant.getPublicKey() != null
            && participant.getAddress() != null
            && update != null
            && merklePath != null
            && merklePath.getLeaf() != null
            && merklePath.getLeaf().getAccount() != null);

    boolean cancelFlag = false;
    PublicKey publicKey = participant.getPublicKey();
    Preconditions.checkState(update.verifySig(publicKey), INVALID_SIGN_ERR);

    HubRoot root = globalBalance.getRoot();
    Preconditions.checkState(
        AugmentedMerkleTree.verifyMembershipProof(
            root.hubRoot2AugmentedMerkleTreeNode(), merklePath),
        INVALID_MERKLE_PATH_ERR);

    Preconditions.checkState(root.getEon() == update.getEon(), INVALID_UPDATE_ERR);

    AugmentedMerkleTreeNode leaf = merklePath.getLeaf();
    AccountInformation accountInformation = leaf.getAccount();

    WithdrawalStatus latestWithdrawal = globalBalance.latestWithdrawal(participant.getAddress());

    Preconditions.checkState(
        (latestWithdrawal != null && latestWithdrawal.isInit()), "invalid withdrawal");
    if (latestWithdrawal.getWithdrawalAmount()
        > (accountInformation.getAllotment()
            + update.getReceiveAmount()
            - update.getSendAmount())) {
      cancelFlag = globalBalance.cancelWithdrawal(participant.getAddress());
    } else {
      // nothing to do
    }

    return cancelFlag;
  }

  /** 确认取钱 */
  public void confirmWithdrawal() {
    logger.info("do confirmWithdrawal");
    Set<WithdrawalStatus> withdrawals = globalBalance.getUndo();
    if (withdrawals != null && withdrawals.size() > 0) {
      int secondEon = globalBalance.getEonByIndex(2);
      withdrawals
          .parallelStream()
          .filter(w -> secondEon >= w.getEon() && w.isInit())
          .forEach(
              w -> {
                boolean flag = w.confirm();
                if (flag) {
                  globalBalance.getUndo().remove(w);
                  chainService.contractTransfer(w.getAddress(), w.getWithdrawalAmount());
                }
              });
    }
  }

  public boolean openBalanceUpdateChallenge(BalanceUpdateChallenge challenge) {
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);
    Preconditions.checkArgument(challenge != null);
    BalanceUpdateProof proof = challenge.getProof();
    Preconditions.checkState(
        (proof != null && (proof.getProvePath() != null || proof.getUpdate() != null)),
        "parameters cannot be null");
    PublicKey pk = challenge.getPublicKey();
    Preconditions.checkArgument(pk != null, "challenge publicKey cannot be null.");

    int leastEon = globalBalance.getEonByIndex(1);

    if (proof.getProvePath() != null) {
      int pathEon = proof.getProvePath().getEon();
      logger.info("hub root : " + leastEon + " challenge : " + pathEon);
      Preconditions.checkState(
          ((leastEon == GlobalBalance.INIT_EON && pathEon == 0) || leastEon == pathEon),
          INVALID_EON_ERR);

      HubRoot root = globalBalance.getRootByIndex(1);
      Preconditions.checkState(
          AugmentedMerkleTree.verifyMembershipProof(
              root.hubRoot2AugmentedMerkleTreeNode(), proof.getProvePath()),
          INVALID_MERKLE_PATH_ERR);
    } else {
      boolean flag = globalBalance.zeroAllotment(BlockAddress.genBlockAddressFromPublicKey(pk));

      Preconditions.checkArgument(flag, "challenge allotment is not zero.");
    }

    if (proof.getUpdate() != null) {
      Update tmp = proof.getUpdate();

      Preconditions.checkState(
          ((leastEon == GlobalBalance.INIT_EON && tmp.getEon() == 0) || leastEon == tmp.getEon()),
          INVALID_EON_ERR);

      if (tmp.getSendAmount() != 0 || tmp.getReceiveAmount() != 0) {
        Preconditions.checkState(
            tmp.verifyHubSig(globalBalance.getHubPublicKey()), INVALID_SIGN_ERR);
      } else {
        // nothing to do
      }
    }

    return globalBalance.openBalanceChallenge(challenge);
  }

  public boolean closeBalanceUpdateChallenge(BalanceUpdateProof proof) {
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);
    Preconditions.checkArgument(proof != null);

    AugmentedMerklePath merklePath = proof.getProvePath();
    Update update = proof.getUpdate();
    Preconditions.checkArgument(
        update != null
            && merklePath != null
            && merklePath.getLeaf() != null
            && merklePath.getLeaf().getAccount() != null);

    HubRoot root = globalBalance.getRoot();
    Preconditions.checkState(
        AugmentedMerkleTree.verifyMembershipProof(
            root.hubRoot2AugmentedMerkleTreeNode(), merklePath),
        INVALID_MERKLE_PATH_ERR);

    AugmentedMerkleTreeNode leaf = merklePath.getLeaf();
    AccountInformation accountInformation = leaf.getAccount();
    Hash addr = accountInformation.getAddress();

    BalanceUpdateChallengeStatus challengeStatus = globalBalance.getBalanceChallenge(addr);
    Preconditions.checkState((challengeStatus != null), "challenge not exist");

    BalanceUpdateChallenge challenge = challengeStatus.getChallenge();
    Long deposit = globalBalance.getDepositByIndex(1, addr);
    Preconditions.checkState((deposit != null), INVALID_EON_ERR);
    if (update.getSendAmount() == 0 && update.getReceiveAmount() == 0) {
      if (deposit > 0)
        Preconditions.checkState(
            deposit <= accountInformation.getAllotment(), INVALID_ALLOTMENT_ERR);
    } else {
      PublicKey hubPublicKey = globalBalance.getHubPublicKey();
      Preconditions.checkState(update.verifySig(challenge.getPublicKey()), INVALID_SIGN_ERR);
      Preconditions.checkState(update.verifyHubSig(hubPublicKey), INVALID_HUB_SIGN_ERR);
      Preconditions.checkState(
          (update.getVersion() >= challenge.getProof().getUpdate().getVersion()),
          UPDATE_VERSION_ERR);
    }

    WithdrawalStatus withdrawalStatus = globalBalance.getWithdrawalByIndex(1, addr);

    long send = update.getSendAmount();
    long receive = update.getReceiveAmount();
    long preAllotment =
        (challenge.getProof().getProvePath() != null)
            ? challenge.getProof().getProvePath().getLeaf().getAccount().getAllotment()
            : 0;
    long allotment =
        receive
            - send
            + preAllotment
            + deposit
            - ((withdrawalStatus != null) ? withdrawalStatus.getWithdrawalAmount() : 0);

    Preconditions.checkState((allotment == leaf.getAllotment()), UPDATE_NOT_MATCH_ERR);

    Preconditions.checkState(
        update.equals(merklePath.getLeaf().getAccount().getUpdate()), UPDATE_NOT_MATCH_ERR);

    return globalBalance.closeBalanceChallenge(addr);
  }

  public BalanceUpdateChallengeStatus queryCurrentBalanceChallenge(BlockAddress addr) {
    return globalBalance.getBalanceChallenge(addr.hash());
  }

  public boolean commit(HubRoot root) {
    checkChallengeStat();
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);

    Preconditions.checkArgument(
        root != null
            && root.getNode() != null
            && root.getOffset() == 0
            && root.getAllotment() >= 0
            && root.getEon() > GlobalBalance.INIT_EON);

    int eon = globalBalance.getCurrentEon();
    Preconditions.checkState(root.getEon() > eon, INVALID_EON_ERR);

    long allotment = globalBalance.computAllotment();
    Preconditions.checkState(root.getAllotment() == allotment, INVALID_ROOT_ERR);
    boolean flag = globalBalance.commit(root);
    if (flag) {
      confirmWithdrawal();
      globalBalance.clearBalance();
    }

    return flag;
  }

  public HubRoot getLatestRoot() {
    return globalBalance.getRoot();
  }

  public HubRoot queryRootByEon(int eon) {
    int currentEon = globalBalance.getCurrentEon();
    Preconditions.checkArgument(eon <= currentEon, INVALID_EON_ERR);

    HubRoot root = globalBalance.getRootByEon(eon);
    Preconditions.checkState(root != null, "root null");
    return root;
  }

  public boolean openTransferDeliveryChallenge(
      OffchainTransaction transaction, Update update, MerklePath<OffchainTransaction> path) {
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);

    Preconditions.checkArgument(
        transaction != null
            && update != null
            && update.getRoot() != null
            && path != null
            && path.getLeafNode() != null
            && path.getLeafNode().getNode() != null
            && path.getLeafNode().getNode().getData() != null);

    Preconditions.checkState(
        update.verifyHubSig(this.globalBalance.getHubPublicKey()), INVALID_HUB_SIGN_ERR);
    Preconditions.checkState(
        MerkleTree.verifyMembershipProof(update.getRoot(), path), INVALID_MERKLE_PATH_ERR);
    Preconditions.checkState(
        transaction.equals(path.getLeafNode().getNode().getData()), "transaction equals err.");

    int leastEon = this.globalBalance.getEonByIndex(1);

    Preconditions.checkState(
        (leastEon == GlobalBalance.INIT_EON && update.getEon() == 0) || leastEon == update.getEon(),
        INVALID_EON_ERR);

    TransferDeliveryChallenge challenge = new TransferDeliveryChallenge(update, transaction, path);
    return globalBalance.doOpenTransferDeliveryChallenge(challenge);
  }

  public List<TransferDeliveryChallenge> queryCurrentTransferDeliveryChallenges(BlockAddress addr) {
    Preconditions.checkArgument(addr != null);
    return this.globalBalance.queryTransferChallenges(addr.hash());
  }

  public boolean closeTransferDeliveryChallenge(
      AugmentedMerklePath augmentedPath,
      Update update,
      MerklePath<OffchainTransaction> path,
      PublicKey toUserPublicKey) {
    Preconditions.checkState((!globalBalance.recoveryMode()), RECOVERY_MODE_ERR);

    Preconditions.checkArgument(
        augmentedPath != null
            && augmentedPath.getLeaf() != null
            && augmentedPath.getLeaf().getAccount() != null
            && augmentedPath.getLeaf().getAccount().getUpdate() != null
            && augmentedPath.getLeaf().getAccount().getAddress() != null
            && update != null
            && update.getRoot() != null
            && path != null
            && path.getLeafNode() != null
            && path.getLeafNode().getNode() != null
            && path.getLeafNode().getNode().getData() != null
            && toUserPublicKey != null);

    Preconditions.checkState(
        AugmentedMerkleTree.verifyMembershipProof(
            globalBalance.getRoot().hubRoot2AugmentedMerkleTreeNode(), augmentedPath),
        INVALID_MERKLE_PATH_ERR);

    TransferDeliveryChallenge challenge =
        globalBalance.getTransferChallenge(
            augmentedPath.getLeaf().getAccount().getAddress(), path.getLeafNode().getNode().hash());
    Preconditions.checkState((challenge != null), "invalid transaction");

    Preconditions.checkState(update.verifySig(toUserPublicKey), INVALID_UPDATE_ERR);
    Preconditions.checkState(
        augmentedPath.getLeaf().getAccount().getUpdate().equals(update), UPDATE_NOT_MATCH_ERR);
    Preconditions.checkState(
        MerkleTree.verifyMembershipProof(update.getRoot(), path),
        "invalid transaction merkle path");

    OffchainTransaction transaction = (OffchainTransaction) path.getLeafNode().getNode().getData();
    return globalBalance.doCloseTransferDeliveryChallenge(transaction);
  }

  public boolean recoverFunds(AugmentedMerklePath path, BlockAddress addr) {
    Preconditions.checkState((globalBalance.recoveryMode()), "not recovery mode.");
    Preconditions.checkArgument(
        path != null
            && path.getLeaf() != null
            && path.getLeaf().getAccount() != null
            && path.getLeaf().getAccount().getAddress() != null
            && addr != null);

    AugmentedMerkleTreeNode leaf = path.getLeaf();
    Preconditions.checkState(
        leaf.getAccount().getAddress().equals(addr.hash()), "invalid address.");

    int preEon = globalBalance.getEonByIndex(1);
    Preconditions.checkState((path.getEon() == preEon), INVALID_EON_ERR);
    HubRoot root = globalBalance.getRootByIndex(1);
    Preconditions.checkState(
        AugmentedMerkleTree.verifyMembershipProof(root.hubRoot2AugmentedMerkleTreeNode(), path),
        INVALID_MERKLE_PATH_ERR);

    long amount = globalBalance.getRecoverFunds(leaf, addr);
    Preconditions.checkState(amount > 0, INVALID_AMOUNT_ERR);

    Preconditions.checkState(!globalBalance.getRecovery().contains(addr), "already refund.");
    globalBalance.getRecovery().add(addr);
    chainService.contractTransfer(addr, amount);
    logger.info("recoverFunds succ");
    return true;
  }

  public void checkChallengeStat() {
    logger.info("do checkChallengeStat");
    globalBalance.doRecovery();
  }

  public WithdrawalStatus withdrawalStat(BlockAddress addr, int eon) {
    if (eon >= 0) {
      return globalBalance.queryWithdrawalStatus(addr);
    }

    return null;
  }
}
