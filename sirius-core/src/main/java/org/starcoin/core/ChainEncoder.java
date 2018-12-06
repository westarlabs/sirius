package org.starcoin.core;

import org.starcoin.io.BlockOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ChainEncoder {

    /**
     * @return Object marshal size in bytes.
     */
    int getMarshalSize();

    default ByteBuffer marshal() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(this.getMarshalSize());
            BlockOutputStream out = new BlockOutputStream(buffer);
            this.writeTo(out);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void writeTo(BlockOutputStream out) throws IOException;
}
