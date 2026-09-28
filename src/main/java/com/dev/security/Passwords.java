package com.dev.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with PBKDF2-HMAC-SHA256. Plaintext passwords never touch the
 * database: only a salted, stretched hash is stored, in the self-describing
 * form {@code iterations:salt:hash} (both parts Base64).
 */
public final class Passwords {

  private static final int ITERATIONS = 120_000;
  private static final int KEY_LENGTH_BITS = 256;
  private static final int SALT_BYTES = 16;

  private static final SecureRandom RANDOM = new SecureRandom();

  private Passwords() {
  }

  public static String hash(String rawPassword) {
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);

    byte[] digest = derive(rawPassword.toCharArray(), salt, ITERATIONS);

    return ITERATIONS + ":" + encode(salt) + ":" + encode(digest);
  }

  public static boolean verify(String rawPassword, String stored) {
    if (stored == null || stored.isBlank()) {
      return false;
    }

    String[] parts = stored.split(":");

    if (parts.length != 3) {
      return false;
    }

    try {
      int iterations = Integer.parseInt(parts[0]);
      byte[] salt = Base64.getDecoder().decode(parts[1]);
      byte[] expected = Base64.getDecoder().decode(parts[2]);
      byte[] actual = derive(rawPassword.toCharArray(), salt, iterations);

      return MessageDigest.isEqual(expected, actual);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private static byte[] derive(char[] password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);

    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
          .getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
      throw new IllegalStateException("PBKDF2 unavailable", exception);
    } finally {
      spec.clearPassword();
    }
  }

  private static String encode(byte[] value) {
    return Base64.getEncoder().encodeToString(value);
  }
}
