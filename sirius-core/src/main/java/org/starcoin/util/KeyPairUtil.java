package org.starcoin.util;

import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class KeyPairUtil {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    //just for junit test
    public static final KeyPair TEST_KEYPAIR = generateKeyPair();

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
            keyGen.initialize(ecSpec, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] signData(byte[] data, PrivateKey key) {
        try {
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(key);
            signer.update(data);
            return signer.sign();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean verifySig(byte[] data, PublicKey key, byte[] sig) {
        try {
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initVerify(key);
            signer.update(data);
            return (signer.verify(sig));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    public static PublicKey recoverPublicKey(byte[] encoded) {
        try {
            ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
            KeyFactory fact = KeyFactory.getInstance("ECDSA", "BC");
            ECCurve curve = params.getCurve();
            java.security.spec.EllipticCurve ellipticCurve =
                    EC5Util.convertCurve(curve, params.getSeed());
            java.security.spec.ECPoint point = ECPointUtil.decodePoint(ellipticCurve, encoded);
            java.security.spec.ECParameterSpec params2 = EC5Util.convertSpec(ellipticCurve, params);
            java.security.spec.ECPublicKeySpec keySpec =
                    new java.security.spec.ECPublicKeySpec(point, params2);
            return (ECPublicKey) fact.generatePublic(keySpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static PrivateKey recoverPrivateKey(byte[] bytes) {
        KeyFactory keyGen;
        try {
            keyGen = KeyFactory.getInstance("ECDSA");
            return keyGen.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encodePrivateKey(PrivateKey privateKey) {
        return privateKey.getEncoded();
    }

    public static byte[] encodePublicKey(PublicKey pubkey, boolean compress) {
        org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey pk =
                (org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey) pubkey;
        byte[] pks = pk.getQ().getEncoded(compress);
        return pks;
    }

    public static byte[] encodePublicKey(PublicKey pubkey) {
        return encodePublicKey(pubkey, true);
    }

    // Ref 1: https://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file
    // Ref 2: https://www.txedo.com/blog/java-generate-rsa-keys-write-pem-file/
    public static void savePemPrivateKey(PrivateKey privateKey, String filename) {
        savePemPrivateKey(privateKey, new File(filename));
    }

    public static void savePemPrivateKey(PrivateKey privateKey, File file) {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);

        try {
            // PemObjectGenerator x = null;
            PemObject x = new PemObject("EC PRIVATE KEY", privateKey.getEncoded());
            pemWriter.writeObject(x);
            pemWriter.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                pemWriter.close();
            } catch (Exception ex2) {
            }
        }

        FileUtil.writeFile(file, stringWriter.toString().getBytes());
    }

    public static void savePemPublicKey(PublicKey publicKey, String filename) {
        savePemPublicKey(publicKey, new File(filename));
    }

    public static void savePemPublicKey(PublicKey publicKey, File file) {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        byte[] data1 = encodePublicKey(publicKey, false);

        try {
            // PemObjectGenerator x = null;
            PemObject x = new PemObject("EC PUBLIC KEY", data1);
            pemWriter.writeObject(x);
            pemWriter.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                pemWriter.close();
            } catch (Exception ex2) {
            }
        }

        FileUtil.writeFile(file, stringWriter.toString().getBytes());
    }

    /**
     * Reads a Private Key from a pem base64 encoded file.
     *
     * @param file name of the file to read.
     * @return returns the privatekey which is read from the file;
     * @throws Exception
     */
    public static PrivateKey loadPemPrivateKey(File file) {
        try {
            byte[] keyBytes = FileUtil.readFile(file);
            Reader rdr = new StringReader(new String(keyBytes));
            PemReader pr = new PemReader(rdr);
            PemObject po = pr.readPemObject();
            PrivateKey privateKey = KeyPairUtil.recoverPrivateKey(po.getContent());
            pr.close();
            return privateKey;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey loadPemPublicKey(File file) {
        try {
            byte[] keyBytes = FileUtil.readFile(file);
            Reader rdr = new StringReader(new String(keyBytes));
            PemReader pr = new PemReader(rdr);
            PemObject po = pr.readPemObject();
            PublicKey publicKey = recoverPublicKey(po.getContent());
            pr.close();
            return publicKey;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
