package org.starcoin.core;

import com.google.protobuf.ByteString;
import org.starcoin.proto.Starcoin.ProtoSignature;
import org.starcoin.util.KeyPairUtil;
import org.starcoin.util.Utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

public final class Signature {

    public static final Signature COINBASE_SIGNATURE = Signature.wrap(new byte[]{0});

    private byte[] sign;

    private Signature(byte[] sign) {
        this.sign = sign;
    }

    public Signature() {
    }

    public static Signature wrap(byte[] sign) {
        return new Signature(sign);
    }

    public static Signature wrap(ProtoSignature protoSignature) {
        return new Signature(protoSignature.getSign().toByteArray());
    }

    public static Signature of(PrivateKey privateKey, byte[] data) {
        return new Signature(KeyPairUtil.signData(data, privateKey));
    }

    public boolean verify(PublicKey publicKey, byte[] data) {
        return KeyPairUtil.verifySig(data, publicKey, this.sign);
    }

    public int marshalSize() {
        return 1 + sign.length;
    }

    public ProtoSignature toProto() {
        return ProtoSignature.newBuilder().setSign(ByteString.copyFrom(sign)).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Signature)) {
            return false;
        }
        Signature signature = (Signature) o;
        return Arrays.equals(sign, signature.sign);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sign);
    }

    @Override
    public String toString() {
        return Utils.HEX.encode(this.sign);
    }

    /**
     * Write sign bytes to out
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(this.sign.length);
        out.write(this.sign);
    }

    /**
     * Read bytes and create Signature object.
     */
    public static Signature readFrom(InputStream in) throws IOException {
        int len = in.read();
        byte[] bytes = new byte[len];
        int rLen = in.read(bytes);
        if (len != rLen) {
            throw new EOFException("unexpected enf of stream to parse Signature");
        }
        return Signature.wrap(bytes);
    }


}
