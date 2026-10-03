package com.harvana.app.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** JSONObject to plain Maps/Lists so the engine classes stay Android-free and unit-testable. */
public final class JsonMaps {
    private JsonMaps() {}

    public static Map<String, Object> toMap(JSONObject o) {
        Map<String, Object> m = new HashMap<>();
        if (o == null) return m;
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            m.put(k, unwrap(o.opt(k)));
        }
        return m;
    }

    private static Object unwrap(Object v) {
        if (v == null || v == JSONObject.NULL) return null;
        if (v instanceof JSONObject) return toMap((JSONObject) v);
        if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            List<Object> l = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) l.add(unwrap(a.opt(i)));
            return l;
        }
        return v;
    }
}
