package com.petbuddy.user_profile_service.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtil {

    private final String secretKey;
    private final String iv;

    public AESUtil(String secretKey, String iv) {
        this.secretKey = secretKey;
        this.iv = iv;
    }

    public String encrypt(String plainText) {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            return Base64.getEncoder()
                .encodeToString(cipher.doFinal(plainText.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error during encryption", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            return new String(cipher.doFinal(Base64.getDecoder()
                .decode(cipherText)));
        } catch (Exception e) {
            throw new RuntimeException("Error during decryption", e);
        }
    }
}
