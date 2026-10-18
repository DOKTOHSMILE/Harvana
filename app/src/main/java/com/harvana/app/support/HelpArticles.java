package com.harvana.app.support;

/** Approved answers. The assistant may only say what is written here. */
public final class HelpArticles {
    private HelpArticles() {}

    public static final class Answer {
        public final String text;
        public final String category;
        public final boolean person;
        public Answer(String text, String category, boolean person) {
            this.text = text;
            this.category = category;
            this.person = person;
        }
    }

    private static final String[][] ARTICLES = {
            {"Account", "wallet create", "A wallet is created on this phone. The recovery phrase stays on the device. Harvanna staff will never ask for it, and support cannot recreate it if it is lost."},
            {"Account", "login sign otp email", "Sign-in is the phone or email already on the account. Support cannot bypass that check or hand the account to someone else."},
            {"Wallet", "receive address qr", "Open the asset and use Receive. Senders must use the address this wallet shows. A screenshot of someone else's address is not yours."},
            {"Transfers", "pending confirm transaction", "Submitted means the phone sent it. Confirmed means the node accepted it into a block. Pending is not the same as confirmed, and support cannot call a transfer successful without the node saying so."},
            {"Transfers", "history transaction", "Transaction history is on the asset and in the activity list. If a hash is missing there, the node has no such transfer on this account."},
            {"P2P", "trade order buy sell", "A P2P order stays in escrow until the trade finishes. Support can read the order. Support cannot release or return the escrow from this chat. That stays a guardian action."},
            {"Escrow", "dispute payment proof", "A payment screenshot is a claim, not proof. Report the dispute on the order so the desk can compare the order, the escrow state, and the chain. Chat alone does not move the funds."},
            {"Groups", "join group contribute osusu", "Groups are opened from Discover or from an invite. Contributions and payouts are in HARV. A slot and a payout follow the group rules, not a promise in chat."},
            {"Staking", "stake reward drip unbond", "Staking rewards follow the lock and the published drip. Support can explain that record. It cannot invent a reward or pay one from this chat."},
            {"Security", "hack stolen unauthorized", "If you think the account was used without you, stop using the compromised device and open a security report. The assistant cannot freeze the wallet or reverse a confirmed transfer."},
            {"App", "theme crash error", "If a screen fails, force-close the app and open it again. Tell support the screen name and what you tapped. Do not send a seed phrase or a private key with the report."}
    };

    public static Answer answer(String raw) {
        String q = raw == null ? "" : raw.toLowerCase();
        if (secret(q)) {
            return new Answer("Stop. Do not send a seed phrase, a private key, or a password. Delete it from this chat. I cannot recover an account from a secret, and a person on the desk must not be given one either.", "Security", true);
        }
        if (contains(q, "hack", "stolen", "unauthorized", "compromise", "phishing", "fraud")) {
            return new Answer("This needs a person. I cannot freeze an account, reverse a transfer, or release escrow. Use Report a security issue so the desk gets a case without those secrets.", "Security", true);
        }
        if (contains(q, "dispute", "escrow", "seller", "buyer", "not paid", "payment proof")) {
            return new Answer("I can gather this, not decide it. A payment picture is not proof of payment. Open a ticket with the order id and the desk will compare the order, the escrow, and the chain. I will not release the escrow.", "P2P", true);
        }
        String bestCat = "Other";
        String best = "";
        int bestScore = 0;
        for (String[] article : ARTICLES) {
            int score = 0;
            for (String word : article[1].split(" ")) if (q.contains(word)) score++;
            if (score > bestScore) {
                bestScore = score;
                best = article[2];
                bestCat = article[0];
            }
        }
        if (bestScore == 0) {
            return new Answer("I don't have an approved answer for that. I won't guess. A person on support can take the case.", "Other", true);
        }
        if (contains(q, "pending", "unconfirmed", "not arrived")) {
            return new Answer(best + " If it is still pending, tell a person the hash. I will not mark it confirmed.", bestCat, false);
        }
        return new Answer(best, bestCat, false);
    }

    public static String search(String raw) {
        String q = raw == null ? "" : raw.toLowerCase().trim();
        if (q.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String[] article : ARTICLES) {
            if (article[0].toLowerCase().contains(q) || article[1].contains(q) || article[2].toLowerCase().contains(q)) {
                out.append(article[0]).append("\n").append(article[2]).append("\n\n");
            }
        }
        return out.toString().trim();
    }

    private static boolean contains(String q, String... words) {
        for (String w : words) if (q.contains(w)) return true;
        return false;
    }

    private static boolean secret(String q) {
        String[] parts = q.trim().split("\\s+");
        if (parts.length >= 12 && parts.length <= 24) {
            int shortWords = 0;
            for (String p : parts) if (p.length() >= 3 && p.length() <= 12) shortWords++;
            if (shortWords >= 12) return true;
        }
        return q.contains("seed phrase") || q.contains("private key") || q.contains("mnemonic");
    }
}
