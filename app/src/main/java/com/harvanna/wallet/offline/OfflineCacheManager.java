package com.harvanna.wallet.offline;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypted, general-purpose "last known good data" store — the offline-
 * first foundation the rest of the app's screens read through.
 *
 * Backed by a real local SQLite database (OfflineDatabaseHelper), the same
 * architectural pattern WhatsApp's Android client uses for its own offline
 * store: an on-device embedded database that screens read from directly
 * when they open, before any network call resolves, rather than a cache
 * that's only consulted after a fetch fails. This used to be backed by a
 * single flat SharedPreferences file — that worked, but a real database
 * is what "fill the screen from local storage, sync when connectivity
 * returns" actually calls for: it scales past what SharedPreferences is
 * built for, supports pruning stale entries by age, and matches the
 * standard mobile offline-first pattern rather than approximating it.
 *
 * Every value is still AES-256-GCM encrypted via Android Keystore before
 * it's written to a row — moving to SQLite changed nothing about the
 * encryption, only where the encrypted bytes live.
 *
 * The public API is unchanged from the SharedPreferences-backed version on
 * purpose: every screen that already calls save()/load()/saveList()/
 * loadList() across the app needed zero changes for this swap.
 *
 * Usage pattern for a screen going offline-first:
 *   1. On screen open, synchronously call load()/loadList() and render
 *      whatever's there immediately (WhatsApp-style: show last known state
 *      first, don't block on network).
 *   2. Kick off the real fetch in the background as normal.
 *   3. On fetch success, call save()/saveList() with the fresh data AND
 *      re-render — this keeps the local database warm for the next cold
 *      start.
 *   4. On fetch failure, check NetworkStatus.isConnectivityError(error) —
 *      if true, leave the already-rendered cached view as-is (don't clear
 *      it, don't show an error); if false (a real server-side error, e.g.
 *      HTTP 404/500), handle it as an actual error since that's not a
 *      connectivity problem caching can paper over.
 */
public final class OfflineCacheManager {

    private static final String TAG = "OfflineCacheManager";

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ajoo_offline_cache_key_v1";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /** Default staleness window for pruneStale() — a week of no successful
     *  refresh means the group/screen is very likely gone or abandoned. */
    private static final long DEFAULT_PRUNE_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    private OfflineCacheManager() {}

    // ── Keystore / crypto — unchanged from the SharedPreferences version ──

    private static SecretKey getOrCreateMasterKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(true);
        }

        keyGenerator.init(builder.build());
        return keyGenerator.generateKey();
    }

    private static String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            SecretKey key = getOrCreateMasterKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    private static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return null;
        try {
            byte[] combined = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            if (combined.length < GCM_IV_LENGTH + 16) return null;

            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            SecretKey key = getOrCreateMasterKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return null;
        }
    }

    // ── Public API: single document ─────────────────────────────────────

    public static void save(Context context, String key, Map<String, Object> doc) {
        if (context == null || key == null || doc == null) return;
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Object> entry : doc.entrySet()) {
                json.put(entry.getKey(), toJson(entry.getValue()));
            }
            json.put("_cachedAtMs", System.currentTimeMillis());
            String encrypted = encrypt(json.toString());
            if (encrypted != null) {
                OfflineDatabaseHelper.getInstance(context).putRaw(key, encrypted, false);
            }
        } catch (Exception e) {
            // Cache write failure is non-fatal.
        }
    }

    public static Map<String, Object> load(Context context, String key) {
        if (context == null || key == null) return null;
        Object[] row = OfflineDatabaseHelper.getInstance(context).getRaw(key);
        if (row == null) return null;
        String raw = decrypt((String) row[0]);
        if (raw == null) return null;
        try {
            JSONObject json = new JSONObject(raw);
            Map<String, Object> result = new HashMap<>();
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                result.put(k, fromJson(json.get(k)));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Public API: list of documents ───────────────────────────────────

    public static void saveList(Context context, String key, List<? extends Map<String, Object>> list) {
        if (context == null || key == null || list == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (Map<String, Object> item : list) {
                JSONObject obj = new JSONObject();
                for (Map.Entry<String, Object> entry : item.entrySet()) {
                    obj.put(entry.getKey(), toJson(entry.getValue()));
                }
                arr.put(obj);
            }
            JSONObject wrapper = new JSONObject();
            wrapper.put("_list", arr);
            wrapper.put("_cachedAtMs", System.currentTimeMillis());
            String encrypted = encrypt(wrapper.toString());
            if (encrypted != null) {
                OfflineDatabaseHelper.getInstance(context).putRaw(key, encrypted, true);
            }
        } catch (Exception e) {
            // Non-fatal
        }
    }

    public static List<HashMap<String, Object>> loadList(Context context, String key) {
        if (context == null || key == null) return null;
        Object[] row = OfflineDatabaseHelper.getInstance(context).getRaw(key);
        if (row == null) return null;
        String raw = decrypt((String) row[0]);
        if (raw == null) return null;
        try {
            JSONObject wrapper = new JSONObject(raw);
            JSONArray arr = wrapper.getJSONArray("_list");
            List<HashMap<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                HashMap<String, Object> item = new HashMap<>();
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    item.put(k, fromJson(obj.get(k)));
                }
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /** Whether *anything* is cached at this key — lets a screen tell "never
     *  fetched before" apart from "fetched before, now offline". */
    public static boolean has(Context context, String key) {
        return context != null && key != null
                && OfflineDatabaseHelper.getInstance(context).contains(key);
    }

    public static long ageMs(Context context, String key) {
        if (context == null || key == null) return -1L;
        Object[] row = OfflineDatabaseHelper.getInstance(context).getRaw(key);
        if (row == null) return -1L;
        long cachedAt = (Long) row[1];
        return System.currentTimeMillis() - cachedAt;
    }

    public static void remove(Context context, String key) {
        if (context == null || key == null) return;
        OfflineDatabaseHelper.getInstance(context).remove(key);
    }

    public static void clearAll(Context context) {
        if (context == null) return;
        OfflineDatabaseHelper.getInstance(context).clearAll();
    }

    /** Drops entries the app hasn't refreshed in over a week (default) —
     *  call occasionally (e.g. MainActivity.onResume, alongside the offline
     *  write-queue flush) so the local database doesn't grow unbounded with
     *  data from groups the user has since left or old discover snapshots. */
    public static void pruneStale(Context context) {
        pruneStale(context, DEFAULT_PRUNE_AGE_MS);
    }

    public static void pruneStale(Context context, long maxAgeMs) {
        if (context == null) return;
        OfflineDatabaseHelper.getInstance(context).pruneOlderThan(maxAgeMs);
    }

    // ── JSON <-> Java conversion, identical approach to GroupDataCache ──

    private static Object toJson(Object value) throws Exception {
        if (value instanceof List) {
            JSONArray arr = new JSONArray();
            for (Object item : (List<?>) value) arr.put(toJson(item));
            return arr;
        }
        if (value instanceof Map) {
            JSONObject obj = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                obj.put(String.valueOf(entry.getKey()), toJson(entry.getValue()));
            }
            return obj;
        }
        return value != null ? value : JSONObject.NULL;
    }

    private static Object fromJson(Object value) throws Exception {
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) list.add(fromJson(arr.get(i)));
            return list;
        }
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            Map<String, Object> map = new HashMap<>();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                map.put(k, fromJson(obj.get(k)));
            }
            return map;
        }
        if (value == JSONObject.NULL) return null;
        return value;
    }
}