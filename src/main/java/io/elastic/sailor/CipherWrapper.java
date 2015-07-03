package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class CipherWrapper {
    private final String ENCRYPTION_KEY;
    private final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private final byte[] ENCRYPTION_IV = new SecureRandom().generateSeed(16);

    public CipherWrapper() {
        this.ENCRYPTION_KEY = System.getenv("MESSAGE_CRYPTO_PASSWORD");
    }

    public CipherWrapper(String PASSWORD) {
        this.ENCRYPTION_KEY = PASSWORD;
    }

    public String encryptMessageContent(JsonObject message) throws IOException {
        return encrypt(message.toString());
    }

    public String encryptMessageContent(String message) throws IOException {
        return encrypt(message);
    }

    public String decryptMessageContent(String message) throws IOException {
        if (message == null || message.length() == 0) {
            return null;
        }
        try {
            message = decrypt(message);
            if (Utils.isJsonObject(message))
                message = new JsonParser().parse(message).toString();
            return message;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message: " + e);
        }
    }

    private String encrypt(String message) throws IOException {
        try {
            if (ENCRYPTION_KEY == null) return URLEncoder.encode(message, "UTF-8");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, generateKey(), new IvParameterSpec(ENCRYPTION_IV));
            byte[] a = cipher.doFinal(message.getBytes());

            return new String(Base64.encodeBase64(a));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(String message) {
        try {
            if (ENCRYPTION_KEY == null) return URLDecoder.decode(message, "UTF-8");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, generateKey(), new IvParameterSpec(ENCRYPTION_IV));
            byte[] a = cipher.doFinal(Base64.decodeBase64(message));

            return new String(a);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Key generateKey() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] key = md.digest(ENCRYPTION_KEY.getBytes("UTF-8"));
        return new SecretKeySpec(key, "AES");
    }
}
