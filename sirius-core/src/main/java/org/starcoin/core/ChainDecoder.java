package org.starcoin.core;

import org.starcoin.io.BlockInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ChainDecoder {

    default void unmarshal(ByteBuffer buf) {
        try {
            BlockInputStream in = new BlockInputStream(buf);
            this.readFrom(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void readFrom(BlockInputStream in) throws IOException;
}
