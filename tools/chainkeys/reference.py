import hashlib, hmac, base58, sys
from eth_account import Account
from embit import bip39, bip32, script
from embit.networks import NETWORKS
from embit.util import secp256k1
import nacl.signing
Account.enable_unaudited_hdwallet_features()
from embit import bech32 as eb
def h160(b): return hashlib.new('ripemd160', hashlib.sha256(b).digest()).digest()
def slip10(seed,path):
    I=hmac.new(b'ed25519 seed',seed,hashlib.sha512).digest(); k,c=I[:32],I[32:]
    for i in path:
        I=hmac.new(c,b'\0'+k+(i|0x80000000).to_bytes(4,'big'),hashlib.sha512).digest(); k,c=I[:32],I[32:]
    return k
for m in sys.argv[1:]:
    seed=bip39.mnemonic_to_seed(m); root=bip32.HDKey.from_seed(seed)
    pk=lambda p: root.derive(p)
    print('BTC', script.p2pkh(pk("m/44'/0'/0'/0/0").get_public_key()).address(NETWORKS['main']))   # legacy P2PKH since 13 Oct 2026
    print('LTC', eb.encode('ltc',0,h160(pk("m/84'/2'/0'/0/0").get_public_key().sec())))
    print('DOGE', base58.b58encode_check(b'\x1e'+h160(pk("m/44'/3'/0'/0/0").get_public_key().sec())).decode())
    print('ETH', Account.from_mnemonic(m, account_path="m/44'/60'/0'/0/0").address)
    tr=Account.from_mnemonic(m, account_path="m/44'/195'/0'/0/0").address
    print('TRX', base58.b58encode_check(b'\x41'+bytes.fromhex(tr[2:])).decode())
    print('SOL', base58.b58encode(bytes(nacl.signing.SigningKey(slip10(seed,[44,501,0,0])).verify_key)).decode())
