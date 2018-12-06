package org.starcoin.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BlockOutputStream extends DataOutputStream {

    public BlockOutputStream(ByteBuffer wrappedBuffer) {
        super(new ByteBufferOutputStream(wrappedBuffer));
    }

    public BlockOutputStream(ByteBuffer wrappedBuffer, boolean autoEnlarge) {
        super(new ByteBufferOutputStream(wrappedBuffer, autoEnlarge));
    }

    public BlockOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Write string length and contents.
     *
     * @see BlockInputStream#readString()
     */
    public void writeString(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        this.writeInt(bytes.length);
        this.write(bytes);
    }

    private static final byte[] blanks = new byte[1000];

    public void writeWithPadding(byte[] b, int length) throws IOException {
        if (b.length <= length) {
            this.write(b);
            if (b.length < length) {
                this.write(blanks, 0, length - b.length);
            }
        } else {
            this.write(b, 0, length);
        }
    }

    public void writeVarInt(long v) throws IOException {
        //TODO
        this.writeLong(v);
    }

    public void writeUInt32(long v) throws IOException {
        //TODO
        this.writeLong(v);
    }
}