package com.github.placeholder.mlinference.domain;

import java.util.Map;
import java.util.TreeMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Interface for deterministic hashing of model features for deduplication.
 */
public interface FeatureHasher {
    String hash(Map<String, Object> features);

    /**
     * Default implementation using SHA-256 on a canonical (sorted) representation of features.
     */
    class CanonicalHasher implements FeatureHasher {
        @Override
        public String hash(Map<String, Object> features) {
            if (features == null || features.isEmpty()) return "empty";
            
            // 1. Sort by key for canonical representation
            TreeMap<String, Object> sorted = new TreeMap<>(features);
            StringBuilder sb = new StringBuilder();
            sorted.forEach((k, v) -> sb.append(k).append(":").append(v).append("|"));

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] encodedHash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(encodedHash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 not available", e);
            }
        }
    }
}
