package io.github.shivam61.mlinference.domain;

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
     * High-performance implementation using a 64-bit hash on a canonical representation.
     * Optimized for request-scope deduplication.
     */
    class CanonicalHasher implements FeatureHasher {
        @Override
        public String hash(Map<String, Object> features) {
            if (features == null || features.isEmpty()) return "0";
            
            // 1. Stable sorting for canonical form
            TreeMap<String, Object> sorted = new TreeMap<>(features);
            
            // 2. Fast 64-bit mixing hash
            long h = 1125899906842597L; // prime
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                h = 31 * h + entry.getKey().hashCode();
                Object v = entry.getValue();
                h = 31 * h + (v == null ? 0 : v.toString().hashCode());
            }
            return Long.toHexString(h);
        }
    }
}
