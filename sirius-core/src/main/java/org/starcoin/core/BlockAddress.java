package org.starcoin.core;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.RandomUtils;
import org.starcoin.io.BlockOutputStream;
import org.starcoin.proto.Starcoin.ProtoBlockAddress;
import org.starcoin.util.HashUtil;
import org.starcoin.util.Utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * BlockMsg Address:
 *
 * <p>RIPEMD160(sha256(public key))
 *
 * <p>pubkey is 65 bytes ecdsa public key result is 20 bytes hash
 *
 * @author Tim
 */
public final class BlockAddress implements Protobufable<ProtoBlockAddress>, ChainEncoder, Hashable {

    public static final int LENGTH = 20;
    public static final BlockAddress DEFAULT_ADDRESS =
            new BlockAddress(
                    new byte[]{
                            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                            11, 12, 13, 14, 15, 16, 17, 18, 19, 20
                    });

    // TODO add network flag to address
    private BlockAddress(byte[] addr) {
        Preconditions.checkArgument(addr.length == LENGTH, "expect address length:" + LENGTH);
        this.address = addr;
    }

    public BlockAddress() {
    }

    private byte[] address;

    public byte[] toBytes() {
        // not changeable
        return address.clone();
    }

    @Override
    public int getMarshalSize() {
        return LENGTH;
    }

    @Override
    public void writeTo(BlockOutputStream out) throws IOException {
        out.write(address);
    }

    public static BlockAddress readFrom(InputStream in) throws IOException {
        byte[] ad = new byte[LENGTH];
        int len = in.read(ad);
        Preconditions.checkArgument(
                len == LENGTH, new EOFException(BlockAddress.class.getName() + " expect more data"));
        return new BlockAddress(ad);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlockAddress that = (BlockAddress) o;
        return Arrays.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    @Override
    public String toString() {
        return Utils.HEX.encode(this.address);
    }

    public ProtoBlockAddress toProto() {
        return ProtoBlockAddress.newBuilder().setAddress(ByteString.copyFrom(address)).build();
    }

    @Override
    public Hash hash() {
        return Hash.of(this.address);
    }

    public static BlockAddress valueOf(ProtoBlockAddress address) {
        return new BlockAddress(address.getAddress().toByteArray());
    }

    public static BlockAddress valueOf(String addressHex) {
        Preconditions.checkNotNull(addressHex, "addressHex");
        return new BlockAddress(Utils.HEX.decode(addressHex));
    }

    public static BlockAddress random() {
        return new BlockAddress(RandomUtils.nextBytes(LENGTH));
    }

    public static BlockAddress genBlockAddressFromPublicKey(PublicKey publicKey) {
        return new BlockAddress(HashUtil.hash160(HashUtil.sha256(publicKey.getEncoded())));
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }
}
