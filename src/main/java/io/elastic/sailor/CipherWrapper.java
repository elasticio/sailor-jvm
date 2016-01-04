package io.elastic.sailor;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.api.Message;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.MessageDigest;

public final class CipherWrapper {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CipherWrapper.class);
    public static final String MESSAGE_PROPERTY_BODY = "body";
    public static final String MESSAGE_PROPERTY_ATTACHMENTS = "attachments";

    private final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private Key encryptionKey;
    private IvParameterSpec encryptionIV;

    @Inject
    public CipherWrapper(
            @Named(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD) String password,
            @Named(Constants.ENV_VAR_MESSAGE_CRYPTO_IV) String initializationVector) {

        Preconditions.checkNotNull(password,
                Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD + " is required");

        Preconditions.checkNotNull(initializationVector,
                Constants.ENV_VAR_MESSAGE_CRYPTO_IV + " is required");

        this.encryptionKey = generateKey(password);
        this.encryptionIV = new IvParameterSpec(initializationVector.getBytes());
    }

    public String encryptMessage(Message message) {
        JsonObject payload = new JsonObject();
        payload.add(MESSAGE_PROPERTY_BODY, message.getBody());
        payload.add(MESSAGE_PROPERTY_ATTACHMENTS, message.getAttachments());
        return encryptMessageContent(payload);
    }

    public Message decryptMessage(String encrypted) {
        final JsonObject payload = decryptMessageContent(encrypted);

        JsonObject body = payload.getAsJsonObject(MESSAGE_PROPERTY_BODY);
        JsonObject attachments = payload.getAsJsonObject(MESSAGE_PROPERTY_ATTACHMENTS);

        if (body == null) {
            body = new JsonObject();
        }

        if (attachments == null) {
            attachments = new JsonObject();
        }

        return new Message.Builder().body(body).attachments(attachments).build();
    }

    // converts JSON to string and encrypts
    public String encryptMessageContent(JsonObject message) {
        return encrypt(message.toString());
    }

    // decrypts string and returns JSON object
    public JsonObject decryptMessageContent(String message) {

        if (message == null || message.isEmpty()) {
            logger.info("Message is null or empty. Returning empty JSON object");
            return new JsonObject();
        }

        final String decryptedMessage = decrypt(message);

        if (Utils.isJsonObject(decryptedMessage)) {
            logger.info("Parsing message JSON");
            return new JsonParser().parse(decryptedMessage).getAsJsonObject();
        }

        throw new RuntimeException("Message is not a JSON object: " + decryptedMessage);
    }

    private String encrypt(String message) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, encryptionIV);

            byte[] a = cipher.doFinal(message.getBytes());

            return new String(Base64.encodeBase64(a));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(String message) {
        try {

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, encryptionIV);

            byte[] a = cipher.doFinal(Base64.decodeBase64(message.getBytes()));

            return new String(a, "UTF-8");
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