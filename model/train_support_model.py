#!/usr/bin/env python3
"""Train the on-device Harvana support model.

The model classifies a member's question. The answer text is still an approved
article. It does not generate new policy, and it never moves funds.
"""
import json
import math
import random
import re
from pathlib import Path

import numpy as np

STOP = {"a", "an", "the", "to", "my", "is", "it", "of", "for", "and", "or", "i", "me", "in", "on", "do", "how", "can", "what", "why", "please"}
TOKEN = re.compile(r"[a-z0-9]+")

# label, category, person, approved answer, training lines
ARTICLES = [
    ("account_create", "Account", False,
     "A wallet is created on this phone. The recovery phrase stays on the device. Harvanna staff will never ask for it, and support cannot recreate it if it is lost.",
     ["how do i create a wallet", "make a new wallet", "start a wallet on this phone", "where is my recovery phrase kept", "can support recreate my wallet", "i want to open a new account wallet", "make a wallet", "new wallet setup"]),
    ("account_login", "Account", False,
     "Sign-in is the phone or email already on the account. Support cannot bypass that check or hand the account to someone else.",
     ["i cannot log in", "otp not arriving", "sign in with email", "how do i login", "change the phone number on my account", "support should log me in", "otp code never came", "login code not delivered"]),
    ("wallet_receive", "Wallet", False,
     "Open the asset and use Receive. Senders must use the address this wallet shows. A screenshot of someone else's address is not yours.",
     ["how do i receive harv", "show my receive address", "where is my qr code", "someone sent to the wrong address", "how do i get my wallet address", "copy my receiving address", "where is the receive qr"]),
    ("transfer_status", "Transfers", False,
     "Submitted means the phone sent it. Confirmed means the node accepted it into a block. Pending is not the same as confirmed, and support cannot call a transfer successful without the node saying so.",
     ["my transaction is pending", "transfer not confirmed", "how long does a send take", "is pending the same as confirmed", "transaction stuck", "sent harv and it has not arrived", "transfer still pending a long time"]),
    ("transfer_history", "Transfers", False,
     "Transaction history is on the asset and in the activity list. If a hash is missing there, the node has no such transfer on this account.",
     ["where is my transaction history", "i cannot find a transfer", "show past sends", "missing transaction hash", "activity list is empty", "see older transactions", "where is the transaction list"]),
    ("p2p", "P2P", False,
     "A P2P order stays in escrow until the trade finishes. Support can read the order. Support cannot release or return the escrow from this chat. That stays a guardian action.",
     ["how does p2p trading work", "where is my p2p order", "what is escrow on a trade", "how do i buy on the marketplace", "explain a p2p sell", "marketplace order stays in escrow", "how marketplace orders work"]),
    ("escrow_dispute", "P2P", True,
     "I can gather this, not decide it. A payment picture is not proof of payment. Open a ticket with the order id and the desk will compare the order, the escrow, and the chain. I will not release the escrow.",
     ["the seller did not pay", "i have a payment screenshot", "open a dispute", "buyer is cheating", "release the escrow", "return my escrow", "p2p payment dispute", "seller will not release i paid"]),
    ("groups", "Groups", False,
     "Groups are opened from Discover or from an invite. Contributions and payouts are in HARV. A slot and a payout follow the group rules, not a promise in chat.",
     ["how do i join a group", "create an osusu", "when is my payout slot", "group contribution in harv", "discover a savings group", "ajo group rules", "join osusu from discover"]),
    ("staking", "Staking", False,
     "Staking rewards follow the lock and the published drip. Support can explain that record. It cannot invent a reward or pay one from this chat.",
     ["how does staking work", "where is my staking reward", "unbond my stake", "reward drip looks wrong", "stake harv", "claim rewards", "stake reward was not what i expected"]),
    ("security", "Security", True,
     "This needs a person. I cannot freeze an account, reverse a transfer, or release escrow. Use Report a security issue so the desk gets a case without those secrets.",
     ["my account was hacked", "someone stole my coins", "unauthorized transfer", "i clicked a phishing link", "this is fraud", "freeze my wallet", "i think the account is compromised"]),
    ("app", "App", False,
     "If a screen fails, force-close the app and open it again. Tell support the screen name and what you tapped. Do not send a seed phrase or a private key with the report.",
     ["the app crashed", "screen is blank", "theme toggle does nothing", "button does not work", "app error on home", "dark mode is broken", "home screen keeps crashing"]),
    ("unknown", "Other", True,
     "I don't have an approved answer for that. I won't guess. A person on support can take the case.",
     ["what is the price of gold tomorrow", "book me a flight", "who won the match", "write me a poem", "translate this contract", "recommend a restaurant", "what is the weather"]),
]

HELD_OUT = [
    ("help me set up a fresh wallet", "account_create"),
    ("the sign in code did not arrive", "account_login"),
    ("i need the address people send harv to", "wallet_receive"),
    ("this send has been pending for hours", "transfer_status"),
    ("where can i look up an old transfer", "transfer_history"),
    ("tell me how a marketplace trade is held", "p2p"),
    ("i paid the seller and the coins are still locked", "escrow_dispute"),
    ("where do i find a savings group to join", "groups"),
    ("the drip on my stake looks short", "staking"),
    ("someone else moved coins out of my account", "security"),
    ("the home page closes by itself", "app"),
    ("sing me a song about lagos", "unknown"),
]


def tokens(text):
    return [t for t in TOKEN.findall(text.lower()) if t not in STOP and len(t) > 1]


def main():
    random.seed(7)
    docs = []
    for label, _cat, _person, _answer, lines in ARTICLES:
        for line in lines:
            docs.append((tokens(line), label))
    vocab = sorted({t for toks, _ in docs for t in toks})
    index = {t: i for i, t in enumerate(vocab)}
    labels = [row[0] for row in ARTICLES]
    label_index = {name: i for i, name in enumerate(labels)}
    x = np.zeros((len(docs), len(vocab)), dtype=np.float64)
    y = np.zeros(len(docs), dtype=np.int64)
    for n, (toks, label) in enumerate(docs):
        y[n] = label_index[label]
        for t in toks:
            x[n, index[t]] = 1.0
    k = len(labels)
    v = len(vocab)
    w = np.zeros((k, v))
    b = np.zeros(k)
    lr = 0.35
    reg = 0.02
    for _ in range(400):
        scores = x @ w.T + b
        scores -= scores.max(axis=1, keepdims=True)
        exp = np.exp(scores)
        prob = exp / exp.sum(axis=1, keepdims=True)
        yhot = np.zeros_like(prob)
        yhot[np.arange(len(y)), y] = 1.0
        grad = (prob - yhot) / len(y)
        w -= lr * (grad.T @ x + reg * w)
        b -= lr * grad.sum(axis=0)
    def predict(text):
        known = [t for t in tokens(text) if t in index]
        if not known:
            return "unknown", 1.0
        vec = np.zeros(v)
        for t in known:
            vec[index[t]] = 1.0
        scores = w @ vec + b
        order = np.argsort(scores)
        top, second = int(order[-1]), int(order[-2])
        scores -= scores.max()
        exp = np.exp(scores)
        prob = exp / exp.sum()
        if float(prob[top] - prob[second]) < 0.08:
            return "unknown", float(prob[top])
        return labels[top], float(prob[top])
    train_ok = sum(predict(" ".join(toks))[0] == label for toks, label in docs)
    hold_ok = 0
    print("held-out")
    for text, expected in HELD_OUT:
        got, conf = predict(text)
        mark = "ok" if got == expected else "MISS"
        if got == expected:
            hold_ok += 1
        print(f"  {mark} {conf:.2f} {got} <= {text}")
    print(f"train {train_ok}/{len(docs)} held-out {hold_ok}/{len(HELD_OUT)}")
    model = {
        "name": "harvana-support",
        "version": 1,
        "vocab": vocab,
        "labels": [
            {"id": row[0], "category": row[1], "person": row[2], "answer": row[3]}
            for row in ARTICLES
        ],
        "threshold": 0.08,
        "W": [[round(float(n), 5) for n in row] for row in w],
        "b": [round(float(n), 5) for n in b],
    }
    raw = json.dumps(model, separators=(",", ":"))
    root = Path(__file__).resolve().parents[1]
    targets = [
        root / "model" / "harvana_support_model.json",
        root / "app" / "src" / "main" / "assets" / "harvana_support_model.json",
    ]
    for path in targets:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(raw)
        print("wrote", path, "bytes", path.stat().st_size)


if __name__ == "__main__":
    main()
