package com.harvana.app.learn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Screen for Learn Hub articles, run on the phone before anything is published. There is no approval step: an article that passes
 * goes live straight away. This only stops the obvious: guaranteed-returns promises, requests for keys/seed phrases/codes,
 * "send money to me" pitches, impersonation of the team, and shortened or chat-invite links. Members can report a post, and
 * the author can delete their own.
 */
public final class LearnContentScreen {
    private LearnContentScreen() {}

    public enum Verdict { OK, BLOCK }

    public static final class Result {
        public final Verdict verdict;
        public final List<String> reasons;
        Result(Verdict v, List<String> r) { verdict = v; reasons = r; }
        public boolean blocked() { return verdict == Verdict.BLOCK; }
        public String message() {
            StringBuilder sb = new StringBuilder("This can't be posted:");
            for (String r : reasons) sb.append("\n• ").append(r);
            return sb.toString();
        }
    }

    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?://|www\\.)[^\\s]+");
    private static final String[] SHORTENERS = {"bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "rb.gy", "is.gd", "ow.ly", "shorturl.at"};
    private static final String[] CHAT_INVITES = {"t.me/", "telegram.me/", "wa.me/", "chat.whatsapp.com", "discord.gg", "discord.com/invite"};

    // {reason, regex} - matched case-insensitively against title + description.
    private static final String[][] RULES = {
        {"It promises guaranteed or risk-free returns.", "guarantee[ds]?\\s+(?:\\w+\\s+){0,3}(?:profit|return|income|payout|earning)|risk[- ]?free\\s+(?:profit|return|invest)|100\\s*%\\s*(?:profit|return|safe|guarantee)|no\\s+risk"},
        {"It promises to multiply or double money.", "(?:double|triple|multiply|10x|100x)\\s+(?:your\\s+)?(?:money|coins?|harv|investment|deposit)|get\\s+rich\\s+quick|earn\\s+[\\d,]+\\s*(?:%|percent)\\s+(?:daily|weekly|per\\s+day|per\\s+week)"},
        {"It asks for a seed phrase, secret key, PIN, password or one-time code.", "(?:seed|recovery|secret)\\s+phrase|private\\s+key|secret\\s+key|(?:send|share|give|enter|tell)\\s+(?:me\\s+|us\\s+)?(?:your\\s+)?(?:pin|password|otp|passphrase|one[- ]time\\s+code)"},
        {"It asks people to send money or coins to a person or address.", "(?:send|transfer|deposit|pay)\\s+(?:\\w+\\s+){0,3}(?:to\\s+(?:my|this)\\s+(?:wallet|account|address)|to\\s+hv[0-9a-z]{6,})|dm\\s+me\\s+to\\s+invest|invest\\s+with\\s+me"},
        {"It pretends to be the Harvana team or support.", "(?:official|verified)\\s+harvana\\s+(?:support|team|admin|agent)|harvana\\s+(?:support|admin)\\s+(?:here|will\\s+contact)|i\\s+am\\s+(?:from|with)\\s+harvana"},
        {"It uses pressure or urgency to make people act now.", "(?:act|invest|join|send)\\s+(?:now|immediately|today)\\s+or\\s+(?:lose|miss)|last\\s+chance\\s+to\\s+(?:invest|join)|limited\\s+slots?\\s+(?:left|remaining)"},
    };

    public static Result check(String title, String description, String imageUrl) {
        List<String> reasons = new ArrayList<>();
        String text = ((title == null ? "" : title) + "\n" + (description == null ? "" : description));
        String lower = text.toLowerCase(Locale.US);

        for (String[] rule : RULES) {
            if (Pattern.compile(rule[1], Pattern.CASE_INSENSITIVE).matcher(lower).find()) reasons.add(rule[0]);
        }

        Matcher m = URL.matcher(text);
        while (m.find()) {
            String u = m.group().toLowerCase(Locale.US);
            String host = u.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
            int slash = host.indexOf('/');
            if (slash >= 0) host = host.substring(0, slash);
            for (String s : SHORTENERS) {
                if ((host.equals(s) || host.endsWith("." + s)) && !reasons.contains("It contains a shortened link. Use the full web address.")) {
                    reasons.add("It contains a shortened link. Use the full web address.");
                }
            }
            for (String s : CHAT_INVITES) if (u.contains(s) && !reasons.contains("It contains a chat-group invite link.")) reasons.add("It contains a chat-group invite link.");
        }

        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.toLowerCase(Locale.US).startsWith("https://")) {
            reasons.add("The cover image must use a secure (https) address.");
        }
        if (letters(text) > 40 && upperShare(text) > 0.6) reasons.add("Too much of it is in capital letters.");

        return new Result(reasons.isEmpty() ? Verdict.OK : Verdict.BLOCK, reasons);
    }

    private static int letters(String s) { int n = 0; for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) n++; return n; }
    private static double upperShare(String s) {
        int l = 0, u = 0;
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (Character.isLetter(c)) { l++; if (Character.isUpperCase(c)) u++; } }
        return l == 0 ? 0 : (double) u / l;
    }
}
