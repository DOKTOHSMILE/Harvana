package com.ajoo.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.button.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import com.ajoo.app.wallet.WalletManager;
import java.net.URL;
import com.bumptech.glide.Glide;
import com.ajoo.app.sync.FirestoreSyncManager;
import com.ajoo.app.wallet.WalletNodeBalanceHelper;
import java.net.HttpURLConnection;

public class WalletAssetFragmentActivity extends Fragment {
	
	private HashMap<String, Object> wallet_data_map = new HashMap<>();
	private ListView listViewAssets;
	private String userWalletAddress = "";
	private String privateKeyString = "";
	private FirestoreSyncManager syncManager;
	private String wallet_address = "";
	private String activeAddress = "";
	
	private ArrayList<HashMap<String, Object>> newWalletDataListMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> assetListMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> coinListMap = new ArrayList<>();
	
	private LinearLayout linear1;
	private CardView cardview4;
	private RecyclerView recyclerview1;
	private LinearLayout linear7;
	private ImageView imageview4;
	private TextView textview7;
	private MaterialButton button1;
	private MaterialButton materialbutton1;
	
	private SharedPreferences walletsp;
	private Intent intent = new Intent();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.wallet_asset_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		linear1 = _view.findViewById(R.id.linear1);
		cardview4 = _view.findViewById(R.id.cardview4);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		linear7 = _view.findViewById(R.id.linear7);
		imageview4 = _view.findViewById(R.id.imageview4);
		textview7 = _view.findViewById(R.id.textview7);
		button1 = _view.findViewById(R.id.button1);
		materialbutton1 = _view.findViewById(R.id.materialbutton1);
		walletsp = getContext().getSharedPreferences("walletsp", Activity.MODE_PRIVATE);
		
		button1.setOnClickListener(_v -> {
			Intent createIntent = new Intent(getContext(), CreateWalletActivity.class);
			startActivityForResult(createIntent, RC_CREATE_WALLET);
			
		});
		
		materialbutton1.setOnClickListener(_v -> {
			Intent restoreIntent = new Intent(getContext(), com.ajoo.app.RestoreWalletActivity.class);
			restoreIntent.putExtra(com.ajoo.app.RestoreWalletActivity.EXTRA_EMBEDDED, true);
			startActivityForResult(restoreIntent, RC_RESTORE_WALLET);
			
		});
	}
	
	private void initializeLogic() {
		SharedPreferences prefs = getContext().getSharedPreferences("ajc_wallet_prefs", Context.MODE_PRIVATE);
		String address = prefs.getString("active_wallet_address", "").trim();
		if (address.isEmpty()) {
			address = WalletManager.getActiveWalletAddress(getContext());
			if (address == null) address = "";
		}
		
		if (address.isEmpty()) {
			cardview4.setVisibility(View.VISIBLE);
			return;
		}
		
		wallet_address = address;
		userWalletAddress = address;
		activeAddress = address;
		cardview4.setVisibility(View.GONE);
		_loadAvailableCoins();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (getContext() != null) {
			SharedPreferences prefs = getContext().getSharedPreferences("ajc_wallet_prefs", Context.MODE_PRIVATE);
			String freshAddress = prefs.getString("active_wallet_address", "").trim();
			if (freshAddress.isEmpty()) {
				freshAddress = WalletManager.getActiveWalletAddress(getContext());
				if (freshAddress == null) freshAddress = "";
			}
			if (!freshAddress.isEmpty() && !freshAddress.equals(wallet_address)) {
				wallet_address = freshAddress;
				userWalletAddress = freshAddress;
				activeAddress = freshAddress;
				if (cardview4 != null) cardview4.setVisibility(View.GONE);
			}
		}
		
		if (wallet_address != null && !wallet_address.trim().isEmpty()) {
			_loadAvailableCoins();
		} else {
			initializeLogic();
		}
		
	}
	
	public void _MORE() {
	}
	private static final int RC_RESTORE_WALLET = 7301;
	private static final int RC_CREATE_WALLET = 7302;
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_RESTORE_WALLET && resultCode == Activity.RESULT_OK && data != null) {
			String restoredAddress = data.getStringExtra(com.ajoo.app.RestoreWalletActivity.EXTRA_RESULT_ADDRESS);
			if (restoredAddress != null) {
				userWalletAddress = restoredAddress;
				wallet_address = restoredAddress;
				activeAddress = restoredAddress;
				if (cardview4 != null) cardview4.setVisibility(View.GONE);
				_loadAvailableCoins();
			}
		} else if (requestCode == RC_CREATE_WALLET && resultCode == Activity.RESULT_OK && data != null) {
			String createdAddress = data.getStringExtra("wallet_address");
			if (createdAddress != null) {
				userWalletAddress = createdAddress;
				wallet_address = createdAddress;
				activeAddress = createdAddress;
				if (cardview4 != null) cardview4.setVisibility(View.GONE);
				_loadAvailableCoins();
			}
		}
	}
	
	public void _showCreateWalletBottomSheet() {
		final Context context = getContext();
		if (context == null) return;
		
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
		new com.google.android.material.bottomsheet.BottomSheetDialog(context);
		
		View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_create_wallet, null);
		bottomSheetDialog.setContentView(sheetView);
		// Was rounded but missing STATE_EXPANDED/MATCH_PARENT — opened as a
		// half-peek sheet instead of full screen. Shared helper covers both.
		com.ajoo.app.group.BottomSheetUtils.expandFullScreen(bottomSheetDialog, sheetView);
		
		MaterialButton btnGenerate = sheetView.findViewById(R.id.btn_generate_wallet);
		TextView tvCancel = sheetView.findViewById(R.id.tv_cancel);
		
		if (btnGenerate != null) {
			btnGenerate.setOnClickListener(v -> {
				bottomSheetDialog.dismiss();
				_showRecoveryPhraseBottomSheet();
			});
		}
		if (tvCancel != null) {
			tvCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
		}
		
		bottomSheetDialog.show();
	}
	
	public void _showRecoveryPhraseBottomSheet() {
		final Context context = getContext();
		if (context == null) return;
		
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
		new com.google.android.material.bottomsheet.BottomSheetDialog(context);
		
		View sheetView = LayoutInflater.from(context)
		.inflate(R.layout.bottom_sheet_recovery_phrase, null);
		
		GradientDrawable bg = new GradientDrawable();
		bg.setShape(GradientDrawable.RECTANGLE);
		bg.setColor(Color.parseColor("#FF0F1623"));
		bg.setCornerRadii(new float[]{48, 48, 48, 48, 0, 0, 0, 0});
		sheetView.setBackground(bg);
		bottomSheetDialog.setContentView(sheetView);
		
		bottomSheetDialog.setOnShowListener(dialog -> {
			com.google.android.material.bottomsheet.BottomSheetDialog d =
			(com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
			android.widget.FrameLayout inner =
			d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (inner != null) {
				inner.setBackgroundColor(Color.TRANSPARENT);
				com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> b =
				com.google.android.material.bottomsheet.BottomSheetBehavior.from(inner);
				b.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
				b.setSkipCollapsed(true);
				b.setFitToContents(false);
				b.setDraggable(true);
				ViewGroup.LayoutParams lp = inner.getLayoutParams();
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				inner.setLayoutParams(lp);
			}
		});
		
		final int[] tvNumIds = {
			R.id.tv_num_1, R.id.tv_num_2, R.id.tv_num_3, R.id.tv_num_4,
			R.id.tv_num_5, R.id.tv_num_6, R.id.tv_num_7, R.id.tv_num_8,
			R.id.tv_num_9, R.id.tv_num_10, R.id.tv_num_11, R.id.tv_num_12
		};
		final int[] tvWordIds = {
			R.id.tv_word_1, R.id.tv_word_2, R.id.tv_word_3, R.id.tv_word_4,
			R.id.tv_word_5, R.id.tv_word_6, R.id.tv_word_7, R.id.tv_word_8,
			R.id.tv_word_9, R.id.tv_word_10, R.id.tv_word_11, R.id.tv_word_12
		};
		
		final CheckBox cbAgree = sheetView.findViewById(R.id.cb_agree);
		final MaterialButton btnFinish = sheetView.findViewById(R.id.btn_finish_setup);
		if (btnFinish == null) {
			SketchwareUtil.showMessage(context, "Finish button not found.");
			return;
		}
		
		for (int i = 0; i < 12; i++) {
			TextView tvNum = sheetView.findViewById(tvNumIds[i]);
			TextView tvWord = sheetView.findViewById(tvWordIds[i]);
			if (tvNum != null) tvNum.setText((i + 1) + ".");
			if (tvWord != null) tvWord.setText("…");
		}
		btnFinish.setEnabled(false);
		
		new Thread(() -> {
			try {
				final String mnemonic =
				com.ajoo.app.wallet.WalletGenerator.generateMnemonic(context);
				if (mnemonic == null || mnemonic.trim().isEmpty()) {
					postToUi(() -> SketchwareUtil.showMessage(context, "Failed to generate recovery phrase."));
					return;
				}
				
				final String walletAddress =
				com.ajoo.app.wallet.WalletGenerator.deriveAddressFromMnemonic(mnemonic);
				if (walletAddress == null
				|| !walletAddress.startsWith("HV0x")
				|| walletAddress.length() != 44) {
					postToUi(() -> SketchwareUtil.showMessage(context, "Failed to derive valid HV0x address."));
					return;
				}
				
				final String[] phraseWords = mnemonic.trim().split("\\s+");
				if (phraseWords.length != 12) {
					postToUi(() -> SketchwareUtil.showMessage(context, "Invalid phrase length."));
					return;
				}
				
				final String seedB64;
				final String pubKeyB64;
				try {
					javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
					mnemonic.toCharArray(),
					"mnemonic".getBytes(java.nio.charset.StandardCharsets.UTF_8),
					2048,
					512
					);
					javax.crypto.SecretKeyFactory skf =
					javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
					byte[] fullSeed = skf.generateSecret(spec).getEncoded();
					byte[] ed25519Seed = java.util.Arrays.copyOf(fullSeed, 32);
					byte[] publicKey = com.ajoo.app.crypto.Ed25519.publicKeyFromSeed(ed25519Seed);
					seedB64 = android.util.Base64.encodeToString(ed25519Seed, android.util.Base64.NO_WRAP);
					pubKeyB64 = android.util.Base64.encodeToString(publicKey, android.util.Base64.NO_WRAP);
					spec.clearPassword();
				} catch (Exception e) {
					postToUi(() -> SketchwareUtil.showMessage(context, "Key derivation failed: " + e.getMessage()));
					return;
				}
				
				postToUi(() -> {
					for (int i = 0; i < 12; i++) {
						TextView tvNum = sheetView.findViewById(tvNumIds[i]);
						TextView tvWord = sheetView.findViewById(tvWordIds[i]);
						if (tvNum != null) tvNum.setText((i + 1) + ".");
						if (tvWord != null) tvWord.setText(phraseWords[i]);
					}
					
					btnFinish.setEnabled(true);
					btnFinish.setOnClickListener(v -> {
						if (cbAgree != null && !cbAgree.isChecked()) {
							SketchwareUtil.showMessage(context, "Please confirm you have backed up the phrase.");
							return;
						}
						try {
							SharedPreferences prefs = getContext().getSharedPreferences("ajc_wallet_prefs", Context.MODE_PRIVATE);
							prefs.edit()
							.putString("active_wallet_address", walletAddress)
							.putString("active_address", walletAddress)
							.putString("wallet_address", walletAddress)
							.putBoolean("is_wallet_created", true)
							.putBoolean("wallet_initialized", true)
							.putLong("wallet_created_timestamp", System.currentTimeMillis())
							.apply();
							
							com.ajoo.app.wallet.SecureWalletStorage.saveMnemonic(context, walletAddress, mnemonic);
							WalletManager.saveKeysLocally(context, walletAddress, seedB64, pubKeyB64);
							
							userWalletAddress = walletAddress;
							wallet_address = walletAddress;
							activeAddress = walletAddress;
							
							// Local only — no FirestoreSyncManager
							cardview4.setVisibility(View.GONE);
							_loadAvailableCoins();
							bottomSheetDialog.dismiss();
							SketchwareUtil.showMessage(context,
							"Wallet Created!\n" + walletAddress.substring(0, 10) + "…");
							
							new androidx.appcompat.app.AlertDialog.Builder(context)
							.setTitle("Back up this wallet?")
							.setMessage("A printable Ajoo Access Card is a physical backup that doesn't depend on remembering 12 words. You can also do this later from Settings.")
							.setPositiveButton("Create Ajoo Card", (d, w) -> {
								Intent cardIntent = new Intent(context, com.ajoo.app.CreateAjooCardActivity.class);
								startActivity(cardIntent);
							})
							.setNegativeButton("Not now", null)
							.show();
						} catch (Exception e) {
							SketchwareUtil.showMessage(context, "Save failed: " + e.getMessage());
						}
					});
				});
			} catch (Exception e) {
				postToUi(() -> SketchwareUtil.showMessage(context, "Error: " + e.getMessage()));
			}
		}).start();
		
		bottomSheetDialog.show();
	}
	
	public void _showRestoreWalletBottomSheet() {
		final Context context = getContext();
		if (context == null) return;
		
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
		new com.google.android.material.bottomsheet.BottomSheetDialog(context);
		
		View sheetView = LayoutInflater.from(context)
		.inflate(R.layout.bottom_sheet_recovery_phrase_old, null);
		
		GradientDrawable bgDrawable = new GradientDrawable();
		bgDrawable.setShape(GradientDrawable.RECTANGLE);
		bgDrawable.setColor(Color.parseColor("#FF0F1623"));
		bgDrawable.setCornerRadii(new float[]{48, 48, 48, 48, 0, 0, 0, 0});
		sheetView.setBackground(bgDrawable);
		bottomSheetDialog.setContentView(sheetView);
		
		bottomSheetDialog.setOnShowListener(dialog -> {
			com.google.android.material.bottomsheet.BottomSheetDialog d =
			(com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
			android.widget.FrameLayout inner =
			d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (inner != null) {
				inner.setBackgroundColor(Color.TRANSPARENT);
				com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> b =
				com.google.android.material.bottomsheet.BottomSheetBehavior.from(inner);
				b.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
				b.setSkipCollapsed(true);
				b.setFitToContents(false);
				b.setDraggable(true);
				ViewGroup.LayoutParams lp = inner.getLayoutParams();
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				inner.setLayoutParams(lp);
			}
		});
		
		final int[] etWordIds = {
			R.id.et_word_1, R.id.et_word_2, R.id.et_word_3, R.id.et_word_4,
			R.id.et_word_5, R.id.et_word_6, R.id.et_word_7, R.id.et_word_8,
			R.id.et_word_9, R.id.et_word_10, R.id.et_word_11, R.id.et_word_12
		};
		
		final EditText[] wordFields = new EditText[12];
		for (int i = 0; i < 12; i++) {
			wordFields[i] = sheetView.findViewById(etWordIds[i]);
			if (wordFields[i] != null) {
				wordFields[i].setFocusable(true);
				wordFields[i].setFocusableInTouchMode(true);
				wordFields[i].setEnabled(true);
				wordFields[i].setCursorVisible(true);
				wordFields[i].setInputType(
				android.text.InputType.TYPE_CLASS_TEXT
				| android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			}
		}
		
		final CheckBox cbAgree = sheetView.findViewById(R.id.cb_agree);
		final MaterialButton btnRestore = sheetView.findViewById(R.id.btn_restore);
		
		if (btnRestore != null) {
			btnRestore.setOnClickListener(v -> {
				if (cbAgree != null && !cbAgree.isChecked()) {
					SketchwareUtil.showMessage(context, "Please confirm the recovery phrase is correct.");
					return;
				}
				
				StringBuilder mnemonicBuilder = new StringBuilder();
				for (int i = 0; i < 12; i++) {
					if (wordFields[i] == null) {
						SketchwareUtil.showMessage(context, "Word field " + (i + 1) + " is missing.");
						return;
					}
					String word = wordFields[i].getText().toString().trim().toLowerCase(Locale.US);
					if (word.isEmpty()) {
						SketchwareUtil.showMessage(context, "Please enter all 12 words.");
						return;
					}
					if (i > 0) mnemonicBuilder.append(" ");
					mnemonicBuilder.append(word);
				}
				final String mnemonic = mnemonicBuilder.toString();
				
				btnRestore.setEnabled(false);
				btnRestore.setText("Restoring…");
				
				new Thread(() -> {
					if (!com.ajoo.app.wallet.WalletGenerator.isValidMnemonic(context, mnemonic)) {
						postToUi(() -> {
							btnRestore.setEnabled(true);
							btnRestore.setText("Restore Wallet");
							SketchwareUtil.showMessage(context,
							"Invalid recovery phrase. Must be exactly 12 BIP-39 words.");
						});
						return;
					}
					
					final String walletAddress =
					WalletManager.deriveAddressFromSeed(mnemonic);
					if (walletAddress == null || walletAddress.trim().isEmpty()) {
						postToUi(() -> {
							btnRestore.setEnabled(true);
							btnRestore.setText("Restore Wallet");
							SketchwareUtil.showMessage(context, "Failed to derive wallet address from the phrase.");
						});
						return;
					}
					
					final String seedB64;
					final String pubKeyB64;
					try {
						javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
						mnemonic.toCharArray(),
						"mnemonic".getBytes(java.nio.charset.StandardCharsets.UTF_8),
						2048,
						512
						);
						javax.crypto.SecretKeyFactory skf =
						javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
						byte[] fullSeed = skf.generateSecret(spec).getEncoded();
						byte[] ed25519Seed = java.util.Arrays.copyOf(fullSeed, 32);
						byte[] publicKey = com.ajoo.app.crypto.Ed25519.publicKeyFromSeed(ed25519Seed);
						seedB64 = android.util.Base64.encodeToString(ed25519Seed, android.util.Base64.NO_WRAP);
						pubKeyB64 = android.util.Base64.encodeToString(publicKey, android.util.Base64.NO_WRAP);
						spec.clearPassword();
					} catch (Exception e) {
						postToUi(() -> {
							btnRestore.setEnabled(true);
							btnRestore.setText("Restore Wallet");
							SketchwareUtil.showMessage(context, "Key derivation failed: " + e.getMessage());
						});
						return;
					}
					
					postToUi(() -> {
						try {
							SharedPreferences prefs =
							getContext().getSharedPreferences("ajc_wallet_prefs", Context.MODE_PRIVATE);
							prefs.edit()
							.putString("active_wallet_address", walletAddress)
							.putString("active_address", walletAddress)
							.putString("wallet_address", walletAddress)
							.putBoolean("is_wallet_created", true)
							.putBoolean("wallet_initialized", true)
							.putLong("wallet_created_timestamp", System.currentTimeMillis())
							.apply();
							
							com.ajoo.app.wallet.SecureWalletStorage.saveMnemonic(context, walletAddress, mnemonic);
							WalletManager.saveKeysLocally(context, walletAddress, seedB64, pubKeyB64);
							
							userWalletAddress = walletAddress;
							activeAddress = walletAddress;
							wallet_address = walletAddress;
							
							// Local only — no FirestoreSyncManager
							cardview4.setVisibility(View.GONE);
							_loadAvailableCoins();
							bottomSheetDialog.dismiss();
							
							String s = walletAddress.length() > 14
							? walletAddress.substring(0, 10) + "…"
							+ walletAddress.substring(walletAddress.length() - 4)
							: walletAddress;
							SketchwareUtil.showMessage(context, "Wallet Restored!\nAddress: " + s);
						} catch (Exception e) {
							btnRestore.setEnabled(true);
							btnRestore.setText("Restore Wallet");
							SketchwareUtil.showMessage(context, "Failed to save restored wallet: " + e.getMessage());
						}
					});
				}).start();
			});
		}
		
		bottomSheetDialog.show();
	}
	
	/** Balance from node (available = spendable). No Firestore wallet sync. */
	public void _loadAvailableCoins() {
		if (getContext() == null || wallet_address == null || wallet_address.trim().isEmpty()) {
			return;
		}
		final Context context = getContext();
		
		java.util.List<HashMap<String, Object>> cachedCoins =
		com.ajoo.app.wallet.WalletCache.loadList(context, "coins_" + wallet_address);
		if (cachedCoins != null && !cachedCoins.isEmpty()) {
			updateCoinListOnUiThread(new ArrayList<>(cachedCoins));
		}
		
		WalletNodeBalanceHelper.load(wallet_address,
		(bal, avail, pin, pout, pending, err) -> {
			if (!isAdded() || getContext() == null) return;
			
			// FIX: Exit early if there's a network error so we don't overwrite the cache with 0
			if (err != null && !err.isEmpty()) {
				android.util.Log.w("loadAvailableCoins", "Node: " + err);
				return; 
			}
			
			double show = avail >= 0 ? avail : bal;
			SharedPreferences currencySp = getContext().getSharedPreferences("currencysp", Context.MODE_PRIVATE);
			String currencyCode = com.ajoo.app.wallet.FiatValueHelper.symbolToCurrencyCode(
			currencySp.getString("currency", "₦"));
			
			com.ajoo.app.wallet.FiatValueHelper.convertAjcToFiat(show, currencyCode, (fiatValue, rateAvailable) -> {
				if (!isAdded() || getContext() == null) return;
				final ArrayList<HashMap<String, Object>> loadedCoins = new ArrayList<>();
				HashMap<String, Object> nativeCoin = new HashMap<>();
				
				// Removed the "#,##" so it no longer generates commas
				java.text.DecimalFormat df = new java.text.DecimalFormat("0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
				
				nativeCoin.put("coin_name", "Ajoo Coin");
				nativeCoin.put("coin_symbol", "AJC");
				nativeCoin.put("coin_balance", df.format(show));
				nativeCoin.put("coin_balance_confirmed", df.format(bal));
				nativeCoin.put("coin_pending_in", df.format(pin));
				nativeCoin.put("coin_pending_out", df.format(pout));
				nativeCoin.put("coin_value", rateAvailable
				? String.format(Locale.US, "%.2f", fiatValue)
				: "0");
				nativeCoin.put("coin_logo", "");
				nativeCoin.put("contractAddress", "");
				nativeCoin.put("isNative", true);
				nativeCoin.put("isListed", true);
				nativeCoin.put("isVerified", true);
				loadedCoins.add(nativeCoin);
				
				com.ajoo.app.wallet.WalletCache.saveList(context, "coins_" + wallet_address, loadedCoins);
				updateCoinListOnUiThread(loadedCoins);
			});
		});
	}
	
	private void updateCoinListOnUiThread(final ArrayList<HashMap<String, Object>> loadedCoins) {
		if (getActivity() == null || recyclerview1 == null) return;
		
		getActivity().runOnUiThread(() -> {
			synchronized (coinListMap) {
				coinListMap.clear();
				coinListMap.addAll(loadedCoins);
			}
			if (recyclerview1.getAdapter() == null) {
				recyclerview1.setLayoutManager(new LinearLayoutManager(getContext()));
				recyclerview1.setAdapter(new Recyclerview1Adapter(coinListMap));
			} else {
				recyclerview1.getAdapter().notifyDataSetChanged();
			}
		});
	}
	
	private void postToUi(Runnable r) {
		if (getActivity() != null) getActivity().runOnUiThread(r);
	}
	
	{
	}
	
	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.coin_list_card, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			
			final com.google.android.material.card.MaterialCardView cardview1 = _view.findViewById(R.id.cardview1);
			final LinearLayout linear7 = _view.findViewById(R.id.linear7);
			final androidx.cardview.widget.CardView cardview_logo = _view.findViewById(R.id.cardview_logo);
			final LinearLayout ll_asset_details = _view.findViewById(R.id.ll_asset_details);
			final ImageView iv_asset_logo = _view.findViewById(R.id.iv_asset_logo);
			final TextView tv_asset_name = _view.findViewById(R.id.tv_asset_name);
			final LinearLayout linear6 = _view.findViewById(R.id.linear6);
			final TextView tv_asset_balance = _view.findViewById(R.id.tv_asset_balance);
			final TextView tv_asset_symbol = _view.findViewById(R.id.tv_asset_symbol);
			final TextView tv_asset_amount = _view.findViewById(R.id.tv_asset_amount);
			
			HashMap<String, Object> item = _data.get(_position);
			
			// Asset name
			if (tv_asset_name != null && item.get("coin_name") != null) {
				tv_asset_name.setText(item.get("coin_name").toString());
			}
			
			// FIX 2: Symbol and logo — guard against null coin_symbol.
			// Original called .toString().equals("") without null check → NPE
			// if the key exists but value is null.
			String symbol = item.get("coin_symbol") != null
			? item.get("coin_symbol").toString() : "";
			if (tv_asset_symbol != null) {
				tv_asset_symbol.setText(symbol);
			}
			
			// FIX 3: Check logo URL is non-null AND non-empty before Glide load.
			// The original checked containsKey("coin_logo") but not whether the
			// value was an empty string. Glide.load("") logs an error, fails
			// silently, and leaves the ImageView blank instead of showing the
			// default logo. Now falls back to the app logo for empty/null URLs.
			String logoUrl = item.containsKey("coin_logo") && item.get("coin_logo") != null
			? item.get("coin_logo").toString().trim() : "";
			if (iv_asset_logo != null) {
				if (!logoUrl.isEmpty()) {
					Glide.with(_view.getContext().getApplicationContext())
					.load(android.net.Uri.parse(logoUrl))
					.error(R.drawable.main_app_logo)
					.into(iv_asset_logo);
				} else {
					iv_asset_logo.setImageResource(R.drawable.main_app_logo);
				}
			}
			
			// FIX 4: coin_balance — wrap Double.parseDouble() in try/catch.
			// The original crashed with NumberFormatException if coin_balance was
			// null, missing, or in scientific notation (e.g. "1.5E-7").
			// Also removed the redundant (int) cast on _position — it is already int.
			if (tv_asset_balance != null && item.containsKey("coin_balance")) {
				try {
					double bal = Double.parseDouble(item.get("coin_balance").toString());
					if (bal > 999) {
						tv_asset_balance.setText(
						new java.text.DecimalFormat("#,##0.####").format(bal));
					} else {
						tv_asset_balance.setText(
						new java.text.DecimalFormat("0.####").format(bal));
					}
				} catch (NumberFormatException e) {
					tv_asset_balance.setText("0.0");
				}
			}
			
			// FIX 5: coin_value — wrap Double.parseDouble() in try/catch AND use
			// the user's selected currency symbol from SharedPreferences instead of
			// the hardcoded "₦". The original hardcode meant users who selected "$"
			// or "GH₵" still saw "₦" on every balance row, and a non-numeric
			// coin_value string would crash onBindViewHolder with NumberFormatException.
			if (tv_asset_amount != null && item.containsKey("coin_value")) {
				try {
					SharedPreferences currencySp = _view.getContext().getSharedPreferences("currencysp", Context.MODE_PRIVATE);
					String currencySymbol = currencySp.getString("currency", "₦");
					
					double val = Double.parseDouble(item.get("coin_value").toString());
					String formatted = val > 999
					? new java.text.DecimalFormat("#,##0.##").format(val)
					: new java.text.DecimalFormat("0.##").format(val);
					tv_asset_amount.setText(currencySymbol + formatted);
				} catch (NumberFormatException e) {
					tv_asset_amount.setText("₦0.00");
				}
			}
			
			if (cardview1 != null) {
				cardview1.setOnClickListener(_v -> {
					intent.putExtra("walletAddress", activeAddress);
					intent.setClass(_view.getContext().getApplicationContext(),
					CoinDetailActivity.class);
					startActivity(intent);
				});
			}
			
		}
		
		@Override
		public int getItemCount() {
			return _data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}