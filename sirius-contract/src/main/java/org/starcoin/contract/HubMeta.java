package org.starcoin.contract;

import org.starcoin.core.BlockAddress;
import org.starcoin.liquidity.Constants;

import java.security.PublicKey;

/** Created by dqm on 2018/9/25. */
public class HubMeta {
  private BlockAddress hubAddr;
  private PublicKey pubKey;

  public HubMeta(PublicKey pubKey) {
    this.hubAddr = Constants.CONTRACT_ADDRESS;
    this.pubKey = pubKey;
  }

  public BlockAddress getBlockAddress() {
    return this.hubAddr;
  }

  public PublicKey getPubKey() {
    return this.pubKey;
  }
}
