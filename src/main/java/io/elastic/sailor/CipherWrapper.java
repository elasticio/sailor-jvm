package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.security.MessageDigest;
import io.elastic.api.Message;

public final class CipherWrapper {
    private final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private Key ENCRYPTION_KEY;
    private IvParameterSpec ENCRYPTION_IV;

    public CipherWrapper() {
        ENCRYPTION_KEY = null;
        ENCRYPTION_IV = null;
    }

    public CipherWrapper(String password, String initializationVector) {
        if (password != null) {
            ENCRYPTION_KEY = generateKey(password);
        }
        if (password != null) {
            ENCRYPTION_IV = new IvParameterSpec(initializationVector.getBytes());
        }
    }

    public String encryptMessage(Message message) {
        JsonObject payload = new JsonObject();
        payload.add("body", message.getBody());
        payload.add("attachments", message.getAttachments());
        return encryptMessageContent(payload);
    }

    public Message decryptMessage(String encrypted) {
        JsonObject payload = decryptMessageContent(encrypted);
        try {
            JsonObject body = payload.getAsJsonObject("body");
            JsonObject attachments = payload.getAsJsonObject("attachments");
            if (body == null) {
                body = new JsonObject();
            }
            if (attachments == null) {
                attachments = new JsonObject();
            }
            return new Message.Builder().body(body).attachments(attachments).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message: "  + e.getMessage());
        }
    }

    // converts JSON to string and encrypts
    public String encryptMessageContent(JsonObject message) {
        return encrypt(message.toString());
    }

    // decrypts string and returns JSON object
    public JsonObject decryptMessageContent(String message) {
        if (message == null || message.length() == 0) {
            return null;
        }
        try {
            message = decrypt(message);
            if (Utils.isJsonObject(message)) {
                return new JsonParser().parse(message).getAsJsonObject();
            } else {
                throw new RuntimeException("Message '" + message + "' is not Json");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message: " + e);
        }
    }

    private String encrypt(String message){
        try {
            if (ENCRYPTION_KEY == null) return URLEncoder.encode(message, "UTF-8");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, ENCRYPTION_KEY,ENCRYPTION_IV);

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
            cipher.init(Cipher.DECRYPT_MODE, ENCRYPTION_KEY, ENCRYPTION_IV);

            byte[] a = cipher.doFinal(Base64.decodeBase64(message.getBytes()));

            return new String(a);
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