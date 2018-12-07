package org.starcoin.contract;

import java.security.PublicKey;
import java.util.List;

import org.starcoin.core.BlockAddress;
import org.starcoin.core.MerklePath;
import org.starcoin.liquidity.*;

/** Created by dqm on 2018/9/27. */
public interface LiquidityContractService {

  boolean deposit(Deposit deposit);

  boolean initiateWithdrawal(Withdrawal withdrawal);

  boolean cancelWithdrawal(Participant participant, Update update, AugmentedMerklePath merklePath);

  boolean openBalanceUpdateChallenge(BalanceUpdateChallenge challenge);

  boolean closeBalanceUpdateChallenge(BalanceUpdateProof proof);

  BalanceUpdateChallengeStatus queryCurrentBalanceChallenge(BlockAddress addr);

  boolean commit(HubRoot root) throws RuntimeException;

  HubRoot getLatestRoot();

  HubRoot queryRootByEon(int eon);

  boolean openTransferDeliveryChallenge(
      OffchainTransaction transaction, Update update, MerklePath<OffchainTransaction> path);

  List<TransferDeliveryChallenge> queryCurrentTransferDeliveryChallenges(BlockAddress addr);

  boolean closeTransferDeliveryChallenge(
      AugmentedMerklePath merklePath,
      Update update,
      MerklePath<OffchainTransaction> path,
      PublicKey fromPublicKey);

  boolean recoverFunds(AugmentedMerklePath path, BlockAddress addr);

  WithdrawalStatus withdrawalStat(BlockAddress addr, int eon);
}
