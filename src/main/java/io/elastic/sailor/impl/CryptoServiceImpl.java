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
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.security.Key;
import java.security.MessageDigest;
import java.util.UUID;

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

    public String encryptMessage(final Message message) {
        return encrypt(message.toString());
    }

    // converts JSON to string and encrypts
    public String encryptJsonObject(JsonObject message) {
        return encrypt(message.toString());
    }

    // decrypts string and returns JSON object
    public JsonObject decryptMessageContent(final String message) {

        if (message == null || message.isEmpty()) {
            logger.info("Message is null or empty. Returning empty JSON object");
            return Json.createObjectBuilder().build();
        }

        final String decryptedMessage = decrypt(message);
        logger.trace("decryptedMessage: {}", decryptedMessage);
        if (Utils.isJsonObject(decryptedMessage)) {
            logger.info("Parsing message JSON");
            JsonObject parsedObject = JSON.parseObject(decryptedMessage);
            logger.trace("parsedObject: {}", parsedObject.toString());
            return parsedObject;
        }

        throw new RuntimeException("Message is not a JSON object: " + decryptedMessage);
    }

    public String encrypt(String message) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, encryptionIV);

            byte[] a = cipher.doFinal(message.getBytes());

            return new String(Base64.encodeBase64(a));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(final String message) {
        try {

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, encryptionIV);

            byte[] messageBytes = cipher.doFinal(Base64.decodeBase64(message.getBytes()));

            return new String(messageBytes, "UTF-8");

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