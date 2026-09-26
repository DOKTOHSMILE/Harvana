package com.ajoo.app.offline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Local database for offline-first screen data — group docs, payments,
 * loans, notices, members, profile, wallet balance, the discover feed.
 *
 * This is the same architectural pattern WhatsApp's Android client uses:
 * a real embedded SQLite database lives on the device and IS the thing
 * screens read from when they open, before any network call resolves —
 * not a side cache that's consulted only after a fetch fails. Every value
 * stored here is still AES-256-GCM encrypted (OfflineCacheManager owns
 * that, this class only owns the table), so moving off SharedPreferences
 * to a real database doesn't change the security story, only how much
 * data can live here comfortably and how it's organized.
 *
 * One table, keyed by an arbitrary string cache key (the same keys every
 * screen already uses, e.g. "profile_" + uid, "payments_" + groupId) —
 * intentionally still a key-value shape rather than one table per data
 * type, so every existing OfflineCacheManager.save()/load() call site
 * across the app keeps working completely unchanged; only the storage
 * engine underneath moved from flat prefs to SQLite.
 */
public final class OfflineDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ajoo_offline_store.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "offline_cache";
    public static final String COL_KEY = "cache_key";
    public static final String COL_VALUE = "encrypted_value";
    public static final String COL_CACHED_AT = "cached_at_ms";
    public static final String COL_IS_LIST = "is_list";

    private static volatile OfflineDatabaseHelper instance;

    public static OfflineDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (OfflineDatabaseHelper.class) {
                if (instance == null) {
                    instance = new OfflineDatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private OfflineDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_KEY + " TEXT PRIMARY KEY, "
                + COL_VALUE + " TEXT NOT NULL, "
                + COL_CACHED_AT + " INTEGER NOT NULL, "
                + COL_IS_LIST + " INTEGER NOT NULL DEFAULT 0"
                + ")");
        db.execSQL("CREATE INDEX idx_offline_cache_cached_at ON " + TABLE + "(" + COL_CACHED_AT + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No prior versions yet — nothing to migrate.
    }

    // ── Row-level operations — OfflineCacheManager encrypts/decrypts the
    //    value before it ever reaches here, so everything below just moves
    //    already-encrypted bytes in and out of the table. ──────────────

    void putRaw(String key, String encryptedValue, boolean isList) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_KEY, key);
        values.put(COL_VALUE, encryptedValue);
        values.put(COL_CACHED_AT, System.currentTimeMillis());
        values.put(COL_IS_LIST, isList ? 1 : 0);
        db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Returns {encryptedValue, cachedAtMs} or null if the key doesn't exist. */
    Object[] getRaw(String key) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE,
                new String[]{COL_VALUE, COL_CACHED_AT},
                COL_KEY + " = ?", new String[]{key},
                null, null, null)) {
            if (c.moveToFirst()) {
                return new Object[]{c.getString(0), c.getLong(1)};
            }
        }
        return null;
    }

    boolean contains(String key) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE, new String[]{COL_KEY},
                COL_KEY + " = ?", new String[]{key}, null, null, null)) {
            return c.getCount() > 0;
        }
    }

    void remove(String key) {
        getWritableDatabase().delete(TABLE, COL_KEY + " = ?", new String[]{key});
    }

    void clearAll() {
        getWritableDatabase().delete(TABLE, null, null);
    }

    /** Drops any entry older than maxAgeMs — call periodically so the local
     *  store doesn't grow unbounded with stale keys from groups the user
     *  has since left, old discover snapshots, etc. */
    void pruneOlderThan(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        getWritableDatabase().delete(TABLE, COL_CACHED_AT + " < ?",
                new String[]{String.valueOf(cutoff)});
    }
}
