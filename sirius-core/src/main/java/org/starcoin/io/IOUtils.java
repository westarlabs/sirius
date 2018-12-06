package org.starcoin.io;

import com.google.common.io.ByteStreams;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class IOUtils {

    public static ByteBuffer toByteBuffer(InputStream in) throws IOException {
        return ByteBuffer.wrap(ByteStreams.toByteArray(in));
    }

    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            // just ignore
        }
    }
}
