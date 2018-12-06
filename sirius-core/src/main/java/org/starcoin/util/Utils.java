package org.starcoin.util;

import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.RandomUtils;

import java.util.UUID;

public class Utils {

    public static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

    public static long timestamp() {
        return System.currentTimeMillis() / 1000;
    }

    public static long newNonce() {
        return RandomUtils.nextLong();
    }

    public static String newReuqestID() {
        // TODO
        return UUID.randomUUID().toString();
    }
}
