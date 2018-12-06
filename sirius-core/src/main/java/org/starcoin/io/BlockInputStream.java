package org.starcoin.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BlockInputStream extends DataInputStream {


    public BlockInputStream(ByteBuffer byteBuffer) {
        super(new ByteBufferInputStream(byteBuffer));
    }

    public BlockInputStream(InputStream in) {
        super(in);
    }

    public String readString() throws IOException {
        int len = this.readInt();
        // TODO limit max size.
        byte[] s = new byte[len];
        this.read(s);
        return new String(s, StandardCharsets.UTF_8);
    }

    public long readVarInt() throws IOException {
        //TODO implement
        return this.readLong();
    }

    public long readUInt32() throws IOException {
        //TODO implement
        return this.readLong();
    }

}
