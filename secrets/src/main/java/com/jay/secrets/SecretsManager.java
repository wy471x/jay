package com.jay.secrets;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Manages sensitive credentials using JDK KeyStore + Bouncy Castle.
 * Replaces Rust's credential vault (~1,400 lines) with JDK's
 * built-in KeyStore abstraction.
 */
public class SecretsManager {

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final KeyStore keyStore;
    private final SecureRandom random;

    public SecretsManager(KeyStore keyStore) {
        this.keyStore = keyStore;
        this.random = new SecureRandom();
    }

    public String getSecret(String alias) throws Exception {
        var entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(null));
        if (entry instanceof KeyStore.SecretKeyEntry secretEntry) {
            return new String(secretEntry.getSecretKey().getEncoded());
        }
        throw new IllegalArgumentException("Secret not found: " + alias);
    }

    public void storeSecret(String alias, String secret) throws Exception {
        var key = new SecretKeySpec(secret.getBytes(), "AES");
        keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(key),
                new KeyStore.PasswordProtection(null));
    }

    public byte[] encrypt(byte[] key, byte[] plaintext) throws Exception {
        var iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        var cipher = Cipher.getInstance(AES_GCM);
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
        var ciphertext = cipher.doFinal(plaintext);
        var result = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH, ciphertext.length);
        return result;
    }

    public byte[] decrypt(byte[] key, byte[] encrypted) throws Exception {
        var iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);
        var ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
        System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
        var cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(ciphertext);
    }
}
