package io.elastic.sailor.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.api.JSON;
import io.elastic.api.Message;
import io.elastic.sailor.Constants;
import io.elastic.sailor.Utils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.security.Key;
import java.security.MessageDigest;

public final class CryptoServiceImpl {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);

    private final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private Key encryptionKey;
    private IvParameterSpec encryptionIV;

    @Inject
    public CryptoServiceImpl(
            @Named(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD) String password,
            @Named(Constants.ENV_VAR_MESSAGE_CRYPTO_IV) String initializationVector) {

        Preconditions.checkNotNull(password,
                Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD + " is required");

        Preconditions.checkNotNull(initializationVector,
                Constants.ENV_VAR_MESSAGE_CRYPTO_IV + " is required");

        this.encryptionKey = generateKey(password);
        this.encryptionIV = new IvParameterSpec(initializationVector.getBytes());
    }

    public byte[] encryptMessage(final Message message, final MessageEncoding encoding) {
        return encrypt(message.toString(), encoding);
    }

    // converts JSON to string and encrypts
    public byte[] encryptJsonObject(JsonObject message, final MessageEncoding encoding) {
        return encrypt(message.toString(), encoding);
    }

    // decrypts string and returns JSON object
    public JsonObject decryptMessageContent(final byte[] bytes, final MessageEncoding encoding) {

        if (bytes == null || bytes.length == 0) {
            logger.info("Message is null or empty. Returning empty JSON object");
            return Json.createObjectBuilder().build();
        }

        final String decryptedMessage = decrypt(bytes, encoding);

        if (Utils.isJsonObject(decryptedMessage)) {
            logger.info("Parsing message JSON");
            return JSON.parseObject(decryptedMessage);
        }

        throw new RuntimeException("Message is not a JSON object: " + decryptedMessage);
    }

    public byte[] encrypt(final String message, final MessageEncoding encoding) {
        logger.info("Encrypting message: {}", message);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, encryptionIV);

            final byte[] bytes = message.getBytes("UTF-8");

            byte[] a = cipher.doFinal(bytes);

            if (encoding == MessageEncoding.BASE64) {
                return Base64.encodeBase64(a);
            }

            return a;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(byte[] bytes, final MessageEncoding encoding) {
        try {

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, encryptionIV);

            if (encoding == MessageEncoding.BASE64) {
                bytes = Base64.decodeBase64(bytes);
            }

            byte[] messageBytes = cipher.doFinal(bytes);

            return new String(messageBytes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Key generateKey(String encryptionKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] key = md.digest(encryptionKey.getBytes("UTF-8"));
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
