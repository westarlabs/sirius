/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.starcoin.core;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.protobuf.UnsafeByteOperations;
import org.apache.commons.lang3.RandomUtils;
import org.starcoin.io.BlockOutputStream;
import org.starcoin.proto.Starcoin.ProtoChainHash;
import org.starcoin.util.HashUtil;
import org.starcoin.util.Utils;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A ChainHash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map. It also checks that the length is correct and provides a bit more type
 * safety. It is a final unmodifiable object.
 */
public final class Hash
        implements Serializable, Comparable<Hash>, ChainEncoder, Hashable {

    public static final int LENGTH = 32; // bytes
    public static final int CHECKSUM_LENGTH = 4; // bytes

    public static final Hash ZERO_HASH = wrap(new byte[LENGTH]);

    private ByteBuffer bytes;

    private Hash(byte[] rawHashBytes) {
        checkArgument(rawHashBytes.length == LENGTH, "unexpected hash length:" + rawHashBytes.length);
        this.bytes = ByteBuffer.allocate(rawHashBytes.length).put(rawHashBytes);
        this.bytes.flip();
    }

    public Hash() {
    }

    /**
     * Creates a new instance that wraps the given hash value. Keep private for unmodifiable object.
     *
     * @param rawHashBytes the raw hash bytes to readFrom
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly 32
     */
    private static Hash wrap(byte[] rawHashBytes) {
        return new Hash(rawHashBytes);
    }

    public static Hash wrap(ProtoChainHash protoChainHash) {
        if (protoChainHash == null) {
            return null;
        }
        return new Hash(protoChainHash.getHash().toByteArray());
    }

    /**
     * Creates a new instance that wraps the given hash value (represented as a hex string).
     *
     * @param hexString a hash value represented as a hex string
     * @return a new instance
     * @throws IllegalArgumentException if the given string is not a valid hex string, or if it does
     *                                  not represent exactly 32 bytes
     */
    public static Hash wrap(String hexString) {
        return wrap(Utils.HEX.decode(hexString));
    }

    /**
     * combine tow hash to one.
     */
    public static Hash combine(Hash left, Hash right) {
        Preconditions.checkArgument(left != null || right != null, "left and right both null.");
        int leftLength = left == null ? 0 : left.bytes.capacity();
        int rightLength = right == null ? 0 : right.bytes.capacity();
        ByteBuffer bb = ByteBuffer.allocate(leftLength + rightLength);
        // ByteBuffer.put(buf) affect the argument buf's position, so use asReadOnlyBuffer
        if (left != null) {
            bb.put(left.bytes.asReadOnlyBuffer());
        }
        if (right != null) {
            bb.put(right.bytes.asReadOnlyBuffer());
        }
        return of(bb);
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the given bytes.
     *
     * @param contents the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     */
    public static Hash of(byte[] contents) {
        return wrap(hash(contents));
    }

    public static Hash of(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return wrap(hash(buffer.array()));
        } else {
            buffer.mark();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            buffer.reset();
            return wrap(hash(bytes));
        }
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the given file's contents.
     *
     * <p>The file contents are read fully into memory, so this method should only be used with small
     * files.
     *
     * @param file the file on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     * @throws IOException if an error occurs while reading the file
     */
    public static Hash of(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            return of(ByteStreams.toByteArray(in));
        } finally {
            in.close();
        }
    }

    /**
     * Returns a new SHA-256 MessageDigest instance.
     *
     * <p>This is a convenience method which wraps the checked exception that can never occur with a
     * RuntimeException.
     *
     * @return a new SHA-256 MessageDigest instance
     */
    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Can't happen.
        }
    }

    /**
     * Calculates the SHA-256 hash of the given bytes.
     *
     * @param input the bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input) {
        return hash(input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range.
     *
     * @param input  the array containing the bytes to hash
     * @param offset the offset within the array of the bytes to hash
     * @param length the number of bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest();
    }

    public static byte[] hash(ByteBuffer input) {
        MessageDigest digest = newDigest();
        digest.update(input);
        return digest.digest();
    }

    /**
     * @see #checksum(byte[])
     */
    public static byte[] checksum(ByteBuffer input) {
        return Arrays.copyOfRange(hash(input), 0, 4);
    }

    /**
     * Calculates checksum, SHA-256 hash of the given bytes and return first 4 byte.
     */
    public static byte[] checksum(byte[] input) {
        return Arrays.copyOfRange(hash(input), 0, CHECKSUM_LENGTH);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hash)) {
            return false;
        }
        Hash chainHash = (Hash) o;
        // TODO ensure
        return this.compareTo(chainHash) == 0;
    }

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable
     * hash code even for blocks, where the goal is to try and get the first bytes to be zeros (i.e.
     * the value as a big integer lower than the target value).
     */
    @Override
    public int hashCode() {
        // Use the last 4 bytes, not the first 4 which are often zeros.
        return Ints.fromBytes(
                bytes.get(LENGTH - 4), bytes.get(LENGTH - 3), bytes.get(LENGTH - 2), bytes.get(LENGTH - 1));
    }

    @Override
    public String toString() {
        return Utils.HEX.encode(bytes.array());
    }

    public String toMD5Hex() {
        return HashUtil.md5Hex(bytes.array());
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, bytes.array());
    }

    /**
     * @return a ReadOnlyBuffer
     */
    public ByteBuffer gotBytes() {
        return bytes.asReadOnlyBuffer();
    }

    @Override
    public int compareTo(final Hash other) {
        for (int i = LENGTH - 1; i >= 0; i--) {
            final int thisByte = this.bytes.get(i) & 0xff;
            final int otherByte = other.bytes.get(i) & 0xff;
            if (thisByte > otherByte) {
                return 1;
            }
            if (thisByte < otherByte) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * Write hash bytes to out
     */
    @Override
    public void writeTo(BlockOutputStream out) throws IOException {
        out.write(this.bytes.array());
    }

    @Override
    public int getMarshalSize() {
        return Hash.LENGTH;
    }

    /**
     * Read bytes and create ChainHash object.
     */
    public static Hash readFrom(InputStream in) throws IOException {
        byte[] bytes = new byte[Hash.LENGTH];
        int len = in.read(bytes);
        if (len != LENGTH) {
            throw new EOFException("unexpected enf of stream to parse Sha256Hash");
        }
        return Hash.wrap(bytes);
    }

    public static Hash random() {
        return Hash.of(RandomUtils.nextBytes(100));
    }

    public ProtoChainHash toProto() {
        return ProtoChainHash.newBuilder()
                .setHash(UnsafeByteOperations.unsafeWrap(this.bytes.asReadOnlyBuffer()))
                .build();
    }

    @Override
    public Hash hash() {
        return this;
    }
}
