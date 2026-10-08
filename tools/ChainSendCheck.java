package com.harvana.app.wallet;

import com.harvana.app.crypto.Ed25519;

import java.math.BigInteger;
import java.util.Arrays;

/** Checks the signers. A mismatch prints FAIL and exits 1. No network. */
public class ChainSendCheck {
    public static void main(String[] args) throws Exception {
        byte[] empty = EvmTx.keccak(new byte[0]);
        check(EvmTx.hex(empty), "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", "keccak");

        String preimage = "0100000096b827c8483d4e9b96712b6713a7b68d6e8003a781feba36c31143470b4efd3752b0a642eea2fb7ae638c36f6252b6750293dbe574a806984b8e4d8548339a3bef51e1b804cc89d182d279655c3aa89e815b1b309fe287d9b2b55d57b90ec68a010000001976a9141d0f172a0ecb48aee1be1f2687d2963ae33f71a188ac0046c32300000000ffffffff863ef3e1a92afbfdb97f31ad0fc7683ee943e9abcf2501590ff8f6551f47e5e51100000001000000";
        byte[] hash = BtcTx.dsha(EvmTx.unhex(preimage));
        check(EvmTx.hex(hash), "c37af31116d1b27caf68aae9e3ac82f1477929014d5b917657d0eb49478cb670", "bip143 hash");

        byte[] key = EvmTx.unhex("619c335025c7f4012e556c2a58b2506e30b8511b53ade95ea316fd8c3286feb9");
        BigInteger[] rs = EvmTx.sign(key, hash);
        String der = EvmTx.hex(EvmTx.der(rs[0], rs[1]));
        check(der, "304402203609e17b84f6a7d30c80bfa610b5b4542f32a8a0d5447a12fb1366d7f01cc44a0220573a954c4518331561406f90300e8f3358f51928d43c212a8caed02de67eebee", "bip143 signature");

        String raw = EvmTx.raw(key, 0, BigInteger.valueOf(1), 21000, new byte[20], BigInteger.ONE, 1);
        if (raw.length() < 100 || EvmTx.recoverId(hash, rs[0], rs[1], key) < 0) fail("evm raw");
        else System.out.println("evm raw ok " + raw.substring(0, 12));

        String m = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        byte[] seed = ChainKeys.privateKey(m, ChainKeys.Chain.SOL);
        byte[] to = Ed25519.publicKeyFromSeed(seed);
        byte[] bh = new byte[32];
        bh[0] = 1;
        byte[] tx = SolTx.transfer(seed, to, 1, bh);
        byte[] message = Arrays.copyOfRange(tx, 65, tx.length);
        byte[] sig = Arrays.copyOfRange(tx, 1, 65);
        if (!Ed25519.verify(Ed25519.publicKeyFromSeed(seed), message, sig)) fail("sol verify");
        else System.out.println("sol signature ok");

        byte[] zeros = ChainSend.b58("11111111111111111111111111111111");
        boolean allZero = zeros.length == 32;
        for (byte z : zeros) if (z != 0) allZero = false;
        if (!allZero) fail("solana system program decoded to " + zeros.length);
        else System.out.println("solana program id ok");

        String phrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
        roundTrip(phrase, ChainKeys.Chain.BTC);
        roundTrip(phrase, ChainKeys.Chain.LTC);
        roundTrip(phrase, ChainKeys.Chain.DOGE);

        byte[] priv = ChainKeys.privateKey(phrase, ChainKeys.Chain.BTC);
        byte[] txid = new byte[32];
        txid[0] = 1;
        java.util.List<BtcTx.In> ins = java.util.Collections.singletonList(new BtcTx.In(txid, 0, 100000));
        byte[] script = ChainSend.scriptFor(ChainKeys.address(phrase, ChainKeys.Chain.BTC), ChainKeys.Chain.BTC);
        java.util.List<BtcTx.Out> outs = java.util.Arrays.asList(new BtcTx.Out(50000, script), new BtcTx.Out(40000, script));
        String built = BtcTx.p2wpkh(priv, ins, outs);
        if (!built.startsWith("020000000001")) fail("segwit prefix " + built.substring(0, 16));
        int vb = BtcTx.vbytes(built);
        if (vb < 100 || vb > 200) fail("vbytes " + vb);
        else System.out.println("segwit tx ok vbytes=" + vb);

        System.out.println("ALL PASS");
    }

    private static void roundTrip(String phrase, ChainKeys.Chain chain) throws Exception {
        String address = ChainKeys.address(phrase, chain);
        byte[] script = ChainSend.scriptFor(address, chain);
        byte[] program = ChainKeys.hash160(ChainKeys.pub(ChainKeys.privateKey(phrase, chain)));
        int at = script[0] == 0x00 ? 2 : 3;
        byte[] got = java.util.Arrays.copyOfRange(script, at, at + 20);
        if (!java.util.Arrays.equals(program, got)) fail(chain.ticker + " address does not pay back to itself: " + address);
        else System.out.println(chain.ticker + " " + address + " ok");
    }

    private static void check(String got, String want, String name) {
        if (!want.equals(got)) fail(name + "\n got  " + got + "\n want " + want);
        else System.out.println(name + " ok");
    }

    private static void fail(String m) { System.out.println("FAIL " + m); System.exit(1); }
}
