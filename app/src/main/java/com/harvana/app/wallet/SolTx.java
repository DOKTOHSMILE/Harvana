package com.harvana.app.wallet;

import com.harvana.app.crypto.Ed25519;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** A Solana transfer of native SOL. The signature is the same Ed25519 the rest of the app uses. */
final class SolTx {
    private SolTx() {}

    static byte[] transfer(byte[] seed, byte[] to, long lamports, byte[] blockhash) throws Exception {
        if (to.length != 32 || blockhash.length != 32) throw new IllegalArgumentException("Bad Solana address.");
        byte[] from = Ed25519.publicKeyFromSeed(seed);
        ByteBuffer msg = ByteBuffer.allocate(160);
        msg.put((byte) 1).put((byte) 0).put((byte) 1);
        msg.put((byte) 3);
        msg.put(from).put(to).put(new byte[32]);
        msg.put(blockhash);
        msg.put((byte) 1);
        msg.put((byte) 2);
        msg.put((byte) 2).put((byte) 0).put((byte) 1);
        msg.put((byte) 12);
        msg.putInt(Integer.reverseBytes(2));
        msg.putLong(Long.reverseBytes(lamports));
        byte[] message = Arrays.copyOf(msg.array(), msg.position());
        byte[] sig = Ed25519.sign(seed, message);
        ByteBuffer tx = ByteBuffer.allocate(1 + sig.length + message.length);
        tx.put((byte) 1).put(sig).put(message);
        return Arrays.copyOf(tx.array(), tx.position());
    }
}
