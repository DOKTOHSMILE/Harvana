package com.harvana.app.wallet;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/** Bitcoin-family transactions. Native SegWit (BTC, LTC) and legacy P2PKH (DOGE). */
final class BtcTx {
    private BtcTx() {}

    static final class In {
        final byte[] txidLe;
        final int vout;
        final long value;
        In(byte[] txidLe, int vout, long value) { this.txidLe = txidLe; this.vout = vout; this.value = value; }
    }

    static final class Out {
        final long value;
        final byte[] script;
        Out(long value, byte[] script) { this.value = value; this.script = script; }
    }

    /** BIP-143 sighash for one P2WPKH input. The hash is double SHA-256 of the preimage. */
    static byte[] witnessSighash(int version, List<In> ins, int index, byte[] scriptCode, List<Out> outs, int locktime) throws Exception {
        byte[] prev = hashOutpoints(ins);
        byte[] seq = hashSequence(ins);
        byte[] oh = hashOutputs(outs);
        In in = ins.get(index);
        ByteBuffer b = ByteBuffer.allocate(4 + 32 + 32 + 36 + scriptCode.length + 8 + 4 + 32 + 4 + 4);
        b.putInt(Integer.reverseBytes(version));
        b.put(prev).put(seq);
        b.put(in.txidLe).putInt(Integer.reverseBytes(in.vout));
        b.put(scriptCode);
        b.putLong(Long.reverseBytes(in.value));
        b.putInt(Integer.reverseBytes(0xffffffff));
        b.put(oh);
        b.putInt(Integer.reverseBytes(locktime));
        b.putInt(Integer.reverseBytes(1));
        return dsha(b.array());
    }

    static String p2wpkh(byte[] priv, List<In> ins, List<Out> outs) throws Exception {
        byte[] pub = ChainKeys.pub(priv);
        byte[] program = ChainKeys.hash160(pub);
        byte[] scriptCode = new byte[1 + 25];
        scriptCode[0] = 0x19;
        scriptCode[1] = 0x76; scriptCode[2] = (byte) 0xa9; scriptCode[3] = 0x14;
        System.arraycopy(program, 0, scriptCode, 4, 20);
        scriptCode[24] = (byte) 0x88; scriptCode[25] = (byte) 0xac;
        ByteBuffer raw = ByteBuffer.allocate(4096);
        raw.putInt(Integer.reverseBytes(2));
        raw.put((byte) 0).put((byte) 1);
        raw.put(varInt(ins.size()));
        for (In in : ins) {
            raw.put(in.txidLe);
            raw.putInt(Integer.reverseBytes(in.vout));
            raw.put((byte) 0);
            raw.putInt(Integer.reverseBytes(0xffffffff));
        }
        raw.put(varInt(outs.size()));
        for (Out o : outs) {
            raw.putLong(Long.reverseBytes(o.value));
            raw.put(varInt(o.script.length));
            raw.put(o.script);
        }
        for (int i = 0; i < ins.size(); i++) {
            byte[] hash = witnessSighash(2, ins, i, scriptCode, outs, 0);
            BigInteger[] rs = EvmTx.sign(priv, hash);
            byte[] der = EvmTx.der(rs[0], rs[1]);
            byte[] sig = Arrays.copyOf(der, der.length + 1);
            sig[der.length] = 1;
            raw.put((byte) 2);
            raw.put(varInt(sig.length)).put(sig);
            raw.put(varInt(pub.length)).put(pub);
        }
        raw.putInt(0);
        return EvmTx.hex(Arrays.copyOf(raw.array(), raw.position()));
    }

    /** Legacy P2PKH (Dogecoin). Each input spends a standard pay-to-pubkey-hash output of this key. */
    static String p2pkh(byte[] priv, List<In> ins, List<Out> outs) throws Exception {
        byte[] pub = ChainKeys.pub(priv);
        byte[] program = ChainKeys.hash160(pub);
        byte[] scriptPub = p2pkhScript(program);
        ByteBuffer raw = ByteBuffer.allocate(4096);
        raw.putInt(Integer.reverseBytes(1));
        raw.put(varInt(ins.size()));
        for (int i = 0; i < ins.size(); i++) {
            In in = ins.get(i);
            raw.put(in.txidLe);
            raw.putInt(Integer.reverseBytes(in.vout));
            byte[] sig = legacySig(priv, ins, i, scriptPub, outs);
            byte[] script = new byte[1 + sig.length + 1 + pub.length];
            script[0] = (byte) sig.length;
            System.arraycopy(sig, 0, script, 1, sig.length);
            script[1 + sig.length] = (byte) pub.length;
            System.arraycopy(pub, 0, script, 2 + sig.length, pub.length);
            raw.put(varInt(script.length)).put(script);
            raw.putInt(Integer.reverseBytes(0xffffffff));
        }
        raw.put(varInt(outs.size()));
        for (Out o : outs) {
            raw.putLong(Long.reverseBytes(o.value));
            raw.put(varInt(o.script.length));
            raw.put(o.script);
        }
        raw.putInt(0);
        return EvmTx.hex(Arrays.copyOf(raw.array(), raw.position()));
    }

    static byte[] p2wpkhScript(byte[] program20) {
        byte[] s = new byte[22];
        s[0] = 0x00; s[1] = 0x14;
        System.arraycopy(program20, 0, s, 2, 20);
        return s;
    }

    static byte[] p2pkhScript(byte[] program20) {
        byte[] s = new byte[25];
        s[0] = 0x76; s[1] = (byte) 0xa9; s[2] = 0x14;
        System.arraycopy(program20, 0, s, 3, 20);
        s[23] = (byte) 0x88; s[24] = (byte) 0xac;
        return s;
    }

    private static byte[] legacySig(byte[] priv, List<In> ins, int index, byte[] scriptPub, List<Out> outs) throws Exception {
        ByteBuffer b = ByteBuffer.allocate(4096);
        b.putInt(Integer.reverseBytes(1));
        b.put(varInt(ins.size()));
        for (int i = 0; i < ins.size(); i++) {
            In in = ins.get(i);
            b.put(in.txidLe).putInt(Integer.reverseBytes(in.vout));
            if (i == index) { b.put(varInt(scriptPub.length)); b.put(scriptPub); }
            else b.put((byte) 0);
            b.putInt(Integer.reverseBytes(0xffffffff));
        }
        b.put(varInt(outs.size()));
        for (Out o : outs) { b.putLong(Long.reverseBytes(o.value)); b.put(varInt(o.script.length)); b.put(o.script); }
        b.putInt(0);
        b.putInt(Integer.reverseBytes(1));
        byte[] hash = dsha(Arrays.copyOf(b.array(), b.position()));
        BigInteger[] rs = EvmTx.sign(priv, hash);
        byte[] der = EvmTx.der(rs[0], rs[1]);
        byte[] sig = Arrays.copyOf(der, der.length + 1);
        sig[der.length] = 1;
        return sig;
    }

    private static byte[] hashOutpoints(List<In> ins) throws Exception {
        ByteBuffer b = ByteBuffer.allocate(ins.size() * 36);
        for (In in : ins) b.put(in.txidLe).putInt(Integer.reverseBytes(in.vout));
        return dsha(b.array());
    }

    private static byte[] hashSequence(List<In> ins) throws Exception {
        ByteBuffer b = ByteBuffer.allocate(ins.size() * 4);
        for (int i = 0; i < ins.size(); i++) b.putInt(Integer.reverseBytes(0xffffffff));
        return dsha(b.array());
    }

    private static byte[] hashOutputs(List<Out> outs) throws Exception {
        ByteBuffer b = ByteBuffer.allocate(outs.size() * 64);
        for (Out o : outs) { b.putLong(Long.reverseBytes(o.value)); b.put(varInt(o.script.length)); b.put(o.script); }
        return dsha(Arrays.copyOf(b.array(), b.position()));
    }

    private static byte[] varInt(int n) {
        if (n < 0xfd) return new byte[]{(byte) n};
        return new byte[]{(byte) 0xfd, (byte) n, (byte) (n >> 8)};
    }

    static byte[] dsha(byte[] in) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        return sha.digest(sha.digest(in));
    }

    static byte[] reverse(byte[] in) {
        byte[] o = new byte[in.length];
        for (int i = 0; i < in.length; i++) o[i] = in[in.length - 1 - i];
        return o;
    }
}
