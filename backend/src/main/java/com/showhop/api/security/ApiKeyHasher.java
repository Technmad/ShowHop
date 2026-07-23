package com.showhop.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 of the raw API key, hex-encoded -- shared by {@code ApiKeyServiceImpl}
 * (hashes a freshly generated key before storing it) and
 * {@link ApiKeyAuthenticationFilter} (hashes a presented key to compare
 * against the stored hash). A raw key is never itself persisted.
 */
public final class ApiKeyHasher {

  public static final String KEY_MARKER = "shk_";
  private static final int PREFIX_LENGTH = 8;

  private ApiKeyHasher() {
  }

  public static String hash(String rawKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  /** The indexed lookup column -- the first few characters after the {@code shk_} marker. */
  public static String prefixOf(String rawKey) {
    String withoutMarker = rawKey.startsWith(KEY_MARKER) ? rawKey.substring(KEY_MARKER.length()) : rawKey;
    return withoutMarker.length() <= PREFIX_LENGTH ? withoutMarker : withoutMarker.substring(0, PREFIX_LENGTH);
  }
}
