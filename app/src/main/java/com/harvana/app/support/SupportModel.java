package com.harvana.app.support;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** On-device classifier. The weights are trained from approved questions. The reply text is not generated. */
public final class SupportModel {
    private static final Set<String> STOP = new HashSet<>();
    static {
        String[] stop = {"a", "an", "the", "to", "my", "is", "it", "of", "for", "and", "or", "i", "me", "in", "on", "do", "how", "can", "what", "why", "please"};
        for (String s : stop) STOP.add(s);
    }

    private static boolean ready;
    private static String[] vocab;
    private static final Map<String, Integer> index = new HashMap<>();
    private static String[] category;
    private static String[] answer;
    private static boolean[] person;
    private static double[][] weights;
    private static double[] bias;
    private static double margin = 0.08;
    private static int unknown = -1;

    private SupportModel() {}

    public static HelpArticles.Answer classify(Context c, String text) {
        if (!load(c) || text == null) return null;
        double[] scores = new double[bias.length];
        System.arraycopy(bias, 0, scores, 0, bias.length);
        boolean known = false;
        for (String token : tokens(text)) {
            Integer at = index.get(token);
            if (at == null) continue;
            known = true;
            for (int row = 0; row < scores.length; row++) scores[row] += weights[row][at];
        }
        int choice = unknown;
        if (known) {
            int top = 0;
            int second = scores.length > 1 ? 1 : 0;
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > scores[top]) { second = top; top = i; }
                else if (i != top && scores[i] > scores[second]) second = i;
            }
            if (gap(scores, top, second) >= margin) choice = top;
        }
        if (choice < 0) choice = unknown;
        if (choice < 0) return null;
        return new HelpArticles.Answer(answer[choice], category[choice], person[choice]);
    }

    private static double gap(double[] scores, int top, int second) {
        double max = Math.max(scores[top], scores[second]);
        double a = Math.exp(scores[top] - max);
        double b = Math.exp(scores[second] - max);
        double rest = 0;
        for (double s : scores) rest += Math.exp(s - max);
        return rest == 0 ? 0 : (a - b) / rest;
    }

    private static String[] tokens(String text) {
        String lower = text.toLowerCase();
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i <= lower.length(); i++) {
            char ch = i == lower.length() ? ' ' : lower.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') word.append(ch);
            else if (word.length() > 1) {
                String token = word.toString();
                if (!STOP.contains(token)) out.add(token);
                word.setLength(0);
            } else word.setLength(0);
        }
        return out.toArray(new String[0]);
    }

    private static boolean load(Context c) {
        if (ready) return unknown >= 0;
        try {
            java.io.InputStream in = c.getAssets().open("harvana_support_model.json");
            byte[] raw = new byte[in.available()];
            int n = 0;
            while (n < raw.length) {
                int r = in.read(raw, n, raw.length - n);
                if (r < 0) break;
                n += r;
            }
            in.close();
            JSONObject model = new JSONObject(new String(raw, 0, n, StandardCharsets.UTF_8));
            JSONArray words = model.getJSONArray("vocab");
            vocab = new String[words.length()];
            for (int i = 0; i < words.length(); i++) {
                vocab[i] = words.getString(i);
                index.put(vocab[i], i);
            }
            JSONArray labels = model.getJSONArray("labels");
            category = new String[labels.length()];
            answer = new String[labels.length()];
            person = new boolean[labels.length()];
            for (int i = 0; i < labels.length(); i++) {
                JSONObject row = labels.getJSONObject(i);
                category[i] = row.getString("category");
                answer[i] = row.getString("answer");
                person[i] = row.getBoolean("person");
                if ("unknown".equals(row.getString("id"))) unknown = i;
            }
            JSONArray w = model.getJSONArray("W");
            weights = new double[w.length()][];
            for (int r = 0; r < w.length(); r++) {
                JSONArray row = w.getJSONArray(r);
                weights[r] = new double[row.length()];
                for (int col = 0; col < row.length(); col++) weights[r][col] = row.getDouble(col);
            }
            JSONArray offsets = model.getJSONArray("b");
            bias = new double[offsets.length()];
            for (int i = 0; i < offsets.length(); i++) bias[i] = offsets.getDouble(i);
            if (model.has("threshold")) margin = model.getDouble("threshold");
            ready = true;
            return unknown >= 0;
        } catch (Exception e) {
            ready = true;
            return false;
        }
    }
}
