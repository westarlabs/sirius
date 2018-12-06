package org.starcoin.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

/**
 * Download Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files see
 * https://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters
 */
public class EncryptUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] encrypt(PublicKey publicKey, byte[] data) {
        try {
            // 参阅:
            // 1. http://www.flexiprovider.de/examples/ExampleECIES.html
            // 2. https://crypto.stackexchange.com/questions/12823/ecdsa-vs-ecies-vs-ecdh
            // 3. https://crypto.stackexchange.com/questions/24516/ecdsa-for-encryption

            Cipher cipher = Cipher.getInstance("ECIES", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | NoSuchPaddingException
                | BadPaddingException
                | NoSuchProviderException
                | IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(PrivateKey privateKey, byte[] encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("ECIES", "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return cipher.doFinal(encrypted);
        } catch (NoSuchProviderException
                | NoSuchAlgorithmException
                | InvalidKeyException
                | NoSuchPaddingException
                | BadPaddingException
                | IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }
}
