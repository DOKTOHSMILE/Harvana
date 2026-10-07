"""Builds the same three Pi transactions as PiTxCheck.java with the official Stellar SDK. Usage: python3 pi_tx_check.py "<24 words>" """
import sys, hmac, hashlib
from stellar_sdk import Keypair, TransactionBuilder, Account, Asset, TimeBounds
from stellar_sdk import Network
from embit import bip39
def slip10(seed, path):
    I = hmac.new(b'ed25519 seed', seed, hashlib.sha512).digest(); k, c = I[:32], I[32:]
    for i in path:
        I = hmac.new(c, b'\0' + k + (i | 0x80000000).to_bytes(4, 'big'), hashlib.sha512).digest(); k, c = I[:32], I[32:]
    return k
kp = Keypair.from_raw_ed25519_seed(slip10(bip39.mnemonic_to_seed(sys.argv[1]), [44, 314159, 0]))
dest = "GC3MMSXBWHL6CPOAVERSJITX7BH76YU252WGLUOM5CJX3E7UCYZBTPJQ"
def build(seq, amount, max_time, memo, create):
    b = TransactionBuilder(Account(kp.public_key, seq), network_passphrase="Pi Network", base_fee=100000)
    b.add_time_bounds(0, max_time)
    if memo: b.add_text_memo(memo)
    if create: b.append_create_account_op(dest, amount)
    else: b.append_payment_op(dest, Asset.native(), amount)
    t = b.build(); t.sign(kp); return t.to_xdr()
print(build(1234567890123, "12.5", 1800000000, "", False))
print(build(1234567890123, "3", 1800000000, "harvana test", False))
print(build(77, "1.0000001", 0, "", True))
