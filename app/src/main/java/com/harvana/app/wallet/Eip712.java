package com.harvana.app.wallet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** EIP-712 typed data (eth_signTypedData_v4): the 32-byte digest a wallet signs. */
public final class Eip712 {
    private Eip712() {}

    /** keccak256(0x19 0x01 ‖ domainSeparator ‖ hashStruct(primaryType, message)). */
    public static byte[] digest(JSONObject typed) {
        JSONObject types = typed.optJSONObject("types");
        if (types == null) throw new IllegalArgumentException("BAD_TYPED_DATA");
        String primary = typed.optString("primaryType");
        JSONObject domain = typed.optJSONObject("domain");
        if (domain == null) domain = new JSONObject();
        byte[] domainSep = hashStruct("EIP712Domain", domain, types);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x19);
        out.write(0x01);
        out.write(domainSep, 0, 32);
        if (!"EIP712Domain".equals(primary)) {
            JSONObject message = typed.optJSONObject("message");
            if (message == null) throw new IllegalArgumentException("BAD_TYPED_DATA");
            out.write(hashStruct(primary, message, types), 0, 32);
        }
        return EvmTx.keccak(out.toByteArray());
    }

    static byte[] hashStruct(String type, JSONObject data, JSONObject types) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] th = EvmTx.keccak(encodeType(type, types).getBytes(StandardCharsets.UTF_8));
        out.write(th, 0, 32);
        JSONArray fields = types.optJSONArray(type);
        if (fields == null) throw new IllegalArgumentException("UNKNOWN_TYPE");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject f = fields.optJSONObject(i);
            byte[] enc = encodeValue(f.optString("type"), data.opt(f.optString("name")), types);
            out.write(enc, 0, 32);
        }
        return EvmTx.keccak(out.toByteArray());
    }

    static String encodeType(String primary, JSONObject types) {
        Set<String> deps = new LinkedHashSet<>();
        collect(primary, types, deps);
        deps.remove(primary);
        List<String> sorted = new ArrayList<>(deps);
        Collections.sort(sorted);
        sorted.add(0, primary);
        StringBuilder sb = new StringBuilder();
        for (String t : sorted) {
            JSONArray fields = types.optJSONArray(t);
            sb.append(t).append('(');
            for (int i = 0; fields != null && i < fields.length(); i++) {
                JSONObject f = fields.optJSONObject(i);
                if (i > 0) sb.append(',');
                sb.append(f.optString("type")).append(' ').append(f.optString("name"));
            }
            sb.append(')');
        }
        return sb.toString();
    }

    private static void collect(String type, JSONObject types, Set<String> seen) {
        String base = baseType(type);
        if (!types.has(base) || seen.contains(base)) return;
        seen.add(base);
        JSONArray fields = types.optJSONArray(base);
        for (int i = 0; fields != null && i < fields.length(); i++) collect(fields.optJSONObject(i).optString("type"), types, seen);
    }

    private static String baseType(String t) {
        int b = t.indexOf('[');
        return b < 0 ? t : t.substring(0, b);
    }

    static byte[] encodeValue(String type, Object value, JSONObject types) {
        if (type.endsWith("]")) {
            String inner = type.substring(0, type.lastIndexOf('['));
            JSONArray arr = value instanceof JSONArray ? (JSONArray) value : new JSONArray();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 0; i < arr.length(); i++) out.write(encodeValue(inner, arr.opt(i), types), 0, 32);
            return EvmTx.keccak(out.toByteArray());
        }
        if (types.has(type)) {
            return hashStruct(type, value instanceof JSONObject ? (JSONObject) value : new JSONObject(), types);
        }
        String v = value == null || value == JSONObject.NULL ? "" : String.valueOf(value);
        switch (type) {
            case "string": return EvmTx.keccak(v.getBytes(StandardCharsets.UTF_8));
            case "bytes": return EvmTx.keccak(bytes(v));
            case "bool": return word(BigInteger.valueOf("true".equalsIgnoreCase(v) || "1".equals(v) ? 1 : 0));
            case "address": return word(new BigInteger(1, bytes(v)));
            default:
                if (type.startsWith("uint") || type.startsWith("int")) return word(number(v));
                if (type.startsWith("bytes")) {
                    byte[] b = bytes(v), out = new byte[32];
                    System.arraycopy(b, 0, out, 0, Math.min(32, b.length));
                    return out;
                }
                throw new IllegalArgumentException("UNSUPPORTED_TYPE " + type);
        }
    }

    private static BigInteger number(String v) {
        String t = v.trim();
        if (t.isEmpty()) return BigInteger.ZERO;
        if (t.startsWith("0x") || t.startsWith("0X")) return new BigInteger(t.substring(2).isEmpty() ? "0" : t.substring(2), 16);
        if (t.contains(".") || t.contains("e") || t.contains("E")) return new java.math.BigDecimal(t).toBigIntegerExact();
        return new BigInteger(t);
    }

    /** 32-byte big-endian two's complement. */
    private static byte[] word(BigInteger v) {
        byte[] out = new byte[32];
        if (v.signum() < 0) java.util.Arrays.fill(out, (byte) 0xff);
        byte[] b = v.toByteArray();
        int n = Math.min(32, b.length);
        System.arraycopy(b, b.length - n, out, 32 - n, n);
        return out;
    }

    private static byte[] bytes(String hex) {
        String h = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
        if (h.length() % 2 == 1) h = "0" + h;
        return EvmTx.unhex(h);
    }
}
