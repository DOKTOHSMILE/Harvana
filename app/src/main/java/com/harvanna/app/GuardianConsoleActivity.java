package com.harvanna.app;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import java.util.Locale;
import android.util.Base64;
import com.harvanna.app.crypto.Ed25519;
import java.nio.charset.StandardCharsets;


public class GuardianConsoleActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private byte[] guardianSeed;
	private String guardianPublicKeyB64 = "";
	
	private LinearLayout linear32;
	private LinearLayout sectionKeyEntry;
	private LinearLayout sectionWorklist;
	private TextView textview46;
	private MaterialCardView cardview22;
	private TextView tvKeyError;
	private MaterialButton btnUnlockConsole;
	private EditText etGuardianKey;
	private TextView tvSigningAs;
	private MaterialButton btnRefreshDisputes;
	private TextView tvNoDisputes;
	private LinearLayout layoutDisputeCards;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.guardian_console);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		linear32 = findViewById(R.id.linear32);
		sectionKeyEntry = findViewById(R.id.sectionKeyEntry);
		sectionWorklist = findViewById(R.id.sectionWorklist);
		textview46 = findViewById(R.id.textview46);
		cardview22 = findViewById(R.id.cardview22);
		tvKeyError = findViewById(R.id.tvKeyError);
		btnUnlockConsole = findViewById(R.id.btnUnlockConsole);
		etGuardianKey = findViewById(R.id.etGuardianKey);
		tvSigningAs = findViewById(R.id.tvSigningAs);
		btnRefreshDisputes = findViewById(R.id.btnRefreshDisputes);
		tvNoDisputes = findViewById(R.id.tvNoDisputes);
		layoutDisputeCards = findViewById(R.id.layoutDisputeCards);
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvanna.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		btnUnlockConsole.setOnClickListener(v -> _unlock());
		btnRefreshDisputes.setOnClickListener(v -> _loadDisputes());
		
	}
	
	public void _MORE() {
	}
	private void _unlock() {
		String keyText = etGuardianKey.getText().toString().trim();
		if (keyText.isEmpty()) {
			_showKeyError("Enter a guardian private key.");
			return;
		}
		try {
			guardianSeed = Base64.decode(keyText, Base64.NO_WRAP);
			byte[] pubKey = Ed25519.publicKeyFromSeed(guardianSeed);
			guardianPublicKeyB64 = Base64.encodeToString(pubKey, Base64.NO_WRAP);
		} catch (Exception e) {
			_showKeyError("That doesn't look like a valid key.");
			guardianSeed = null;
			guardianPublicKeyB64 = null;
			return;
		}
		
		tvKeyError.setVisibility(View.GONE);
		// Clear the key field immediately once decoded — no reason for
		// the raw key to keep sitting visibly in a text field longer
		// than it takes to unlock.
		etGuardianKey.setText("");
		sectionKeyEntry.setVisibility(View.GONE);
		sectionWorklist.setVisibility(View.VISIBLE);
		tvSigningAs.setText("Signing as: " + _shortKey(guardianPublicKeyB64));
		_loadDisputes();
	}
	
	/** Signs HARVANNA_GENLOAN_APPROVE_V1 for one loan. The node checks this key is a guardian. */
	private void _addLoanApprovalCard() {
		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		int p = (int) (12 * getResources().getDisplayMetrics().density);
		card.setPadding(p, p, p, p);
		TextView title = new TextView(this);
		title.setText("Group loan");
		title.setTextSize(16);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		card.addView(title);
		TextView note = new TextView(this);
		note.setText("A loan stays pending until enough guardians sign it. This does not move the money by itself.");
		note.setTextSize(13);
		card.addView(note);
		EditText group = new EditText(this);
		group.setHint("Group id (ACCUM- or FIXED-)");
		EditText loan = new EditText(this);
		loan.setHint("Loan id (GENLOAN-)");
		Button sign = new Button(this);
		sign.setText("Sign this loan");
		sign.setOnClickListener(v -> {
			String gid = group.getText().toString().trim();
			String lid = loan.getText().toString().trim();
			if (guardianSeed == null) { SketchwareUtil.showMessage(this, "Console locked."); return; }
			if (gid.isEmpty() || !lid.startsWith("GENLOAN-")) {
				SketchwareUtil.showMessage(this, "Enter the group id and the GENLOAN id.");
				return;
			}
			try {
				String msg = "HARVANNA_GENLOAN_APPROVE_V1:" + gid + ":" + lid;
				byte[] sig = com.harvanna.app.crypto.Ed25519.sign(guardianSeed, msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				String signature = android.util.Base64.encodeToString(sig, android.util.Base64.NO_WRAP);
				com.harvanna.app.sync.NodeGroupClient.stageGenLoanGuardian(gid, lid, guardianPublicKeyB64, signature,
						new com.harvanna.app.sync.NodeGroupClient.Callback() {
							@Override public void onSuccess(org.json.JSONObject json) {
								runOnUiThread(() -> SketchwareUtil.showMessage(GuardianConsoleActivity.this, "Signature recorded."));
							}
							@Override public void onError(String error) {
								runOnUiThread(() -> SketchwareUtil.showMessage(GuardianConsoleActivity.this, error));
							}
						});
			} catch (Exception e) {
				SketchwareUtil.showMessage(this, "Could not sign.");
			}
		});
		card.addView(group);
		card.addView(loan);
		card.addView(sign);
		layoutDisputeCards.addView(card);
	}

	private void _showKeyError(String message) {
		tvKeyError.setText(message);
		tvKeyError.setVisibility(View.VISIBLE);
	}
	
	private void _loadDisputes() {
		layoutDisputeCards.removeAllViews();
		_addLoanApprovalCard();
		tvNoDisputes.setVisibility(View.GONE);
		
		com.harvanna.app.sync.HarvannaNodeClient.getDisputedOrders(new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject result) {
				runOnUiThread(() -> {
					JSONArray orders = result.optJSONArray("orders");
					if (orders == null || orders.length() == 0) {
						tvNoDisputes.setVisibility(View.VISIBLE);
						return;
					}
					for (int i = 0; i < orders.length(); i++) {
						JSONObject order = orders.optJSONObject(i);
						if (order != null) _addDisputeCard(order);
					}
				});
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> SketchwareUtil.showMessage(getApplicationContext(),
				"Couldn't load disputes: " + error));
			}
		});
	}
	
	private void _addDisputeCard(JSONObject order) {
		String orderId = order.optString("orderId", "");
		String sellerAddress = order.optString("sellerAddress", "");
		String buyerAddress = order.optString("buyerAddress", "");
		double amount = com.harvanna.app.sync.HarvannaNodeClient.toHarvDisplay(order.optLong("amount", 0));
		String currency = order.optString("currency", "");
		long priceForAmount = order.optLong("priceForAmount", 0);
		String reason = order.optString("disputeReason", "No reason provided.");
		
		View card = LayoutInflater.from(this).inflate(R.layout.guardian_dispute_card, layoutDisputeCards, false);
		TextView tvOrderId = card.findViewById(R.id.tv_card_order_id);
		TextView tvAmount = card.findViewById(R.id.tv_card_amount);
		TextView tvParties = card.findViewById(R.id.tv_card_parties);
		TextView tvReason = card.findViewById(R.id.tv_card_reason);
		TextView tvSigCounts = card.findViewById(R.id.tv_card_sig_counts);
		Button btnReleaseBuyer = card.findViewById(R.id.btn_card_release_buyer);
		Button btnReturnSeller = card.findViewById(R.id.btn_card_return_seller);
		
		tvOrderId.setText("Order " + _shortKey(orderId));
		tvAmount.setText(String.format(Locale.US, ("%.4f " + com.harvanna.app.CoinInfo.ticker() + "  \u00b7  %,d %s"),
		amount, priceForAmount, currency));
		tvParties.setText("Seller: " + _shortKey(sellerAddress) + "   Buyer: " + _shortKey(buyerAddress));
		tvReason.setText(reason);
		
		_refreshCardStatus(orderId, tvSigCounts, btnReleaseBuyer, btnReturnSeller);
		
		btnReleaseBuyer.setOnClickListener(v -> _confirmAndSign(orderId, "RELEASE_TO_BUYER",
		tvSigCounts, btnReleaseBuyer, btnReturnSeller));
		btnReturnSeller.setOnClickListener(v -> _confirmAndSign(orderId, "RETURN_TO_SELLER",
		tvSigCounts, btnReleaseBuyer, btnReturnSeller));
		
		layoutDisputeCards.addView(card);
	}
	
	private void _refreshCardStatus(String orderId, TextView tvSigCounts, Button btnReleaseBuyer, Button btnReturnSeller) {
		com.harvanna.app.sync.HarvannaNodeClient.getDisputeStatus(orderId, new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject result) {
				runOnUiThread(() -> {
					int releaseCount = result.optInt("releaseToBuyerCount", 0);
					int returnCount = result.optInt("returnToSellerCount", 0);
					tvSigCounts.setText(String.format(Locale.US,
					"Signatures so far \u2014 Release: %d \u00b7 Return: %d", releaseCount, returnCount));
				});
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> tvSigCounts.setText("Couldn't load signature status."));
			}
		});
	}
	
	private void _confirmAndSign(String orderId, String resolution, TextView tvSigCounts,
	Button btnReleaseBuyer, Button btnReturnSeller) {
		String actionLabel = "RELEASE_TO_BUYER".equals(resolution) ? ("release this trade's " + com.harvanna.app.CoinInfo.ticker() + " to the buyer") : ("return this trade's " + com.harvanna.app.CoinInfo.ticker() + " to the seller");
		new androidx.appcompat.app.AlertDialog.Builder(this)
		.setTitle("Confirm your vote")
		.setMessage("You're about to " + actionLabel + ". This adds your signature toward resolving this dispute \u2014 it only takes effect once enough guardians agree.")
		.setPositiveButton("Sign & Submit", (dialog, which) -> _doSign(orderId, resolution, tvSigCounts, btnReleaseBuyer, btnReturnSeller))
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _doSign(String orderId, String resolution, TextView tvSigCounts,
	Button btnReleaseBuyer, Button btnReturnSeller) {
		if (guardianSeed == null) {
			SketchwareUtil.showMessage(getApplicationContext(), "Console locked \u2014 unlock again.");
			return;
		}
		btnReleaseBuyer.setEnabled(false);
		btnReturnSeller.setEnabled(false);
		
		// Nonce is fetched fresh right before signing, not cached from
		// when the card was first drawn — another guardian's resolution
		// on a different order could have advanced the shared guardian
		// nonce counter in the meantime.
		com.harvanna.app.sync.HarvannaNodeClient.getDisputeStatus(orderId, new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject statusResult) {
				long actionNonce = statusResult.optLong("nextActionNonce", 0);
				try {
					String canonical = "P2P_RESOLVE_DISPUTE:" + orderId + ":" + resolution + ":" + actionNonce;
					byte[] sig = Ed25519.sign(guardianSeed, canonical.getBytes(StandardCharsets.UTF_8));
					String signatureB64 = Base64.encodeToString(sig, Base64.NO_WRAP);
					
					com.harvanna.app.sync.HarvannaNodeClient.submitGuardianSignature(orderId, resolution, actionNonce,
					guardianPublicKeyB64, signatureB64, new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
						@Override
						public void onSuccess(JSONObject result) {
							runOnUiThread(() -> {
								btnReleaseBuyer.setEnabled(true);
								btnReturnSeller.setEnabled(true);
								boolean finalized = result.optBoolean("finalized", false);
								boolean alreadySigned = result.optBoolean("alreadySigned", false);
								if (finalized) {
									SketchwareUtil.showMessage(getApplicationContext(),
									"Threshold reached \u2014 dispute resolved.");
									_loadDisputes();
								} else if (alreadySigned) {
									SketchwareUtil.showMessage(getApplicationContext(),
									"You already signed this outcome.");
								} else {
									SketchwareUtil.showMessage(getApplicationContext(),
									"Signature recorded \u2014 waiting on more guardians.");
									_refreshCardStatus(orderId, tvSigCounts, btnReleaseBuyer, btnReturnSeller);
								}
							});
						}
						
						@Override
						public void onError(String error) {
							runOnUiThread(() -> {
								btnReleaseBuyer.setEnabled(true);
								btnReturnSeller.setEnabled(true);
								SketchwareUtil.showMessage(getApplicationContext(), "Couldn't submit: " + error);
							});
						}
					});
				} catch (Exception e) {
					runOnUiThread(() -> {
						btnReleaseBuyer.setEnabled(true);
						btnReturnSeller.setEnabled(true);
						SketchwareUtil.showMessage(getApplicationContext(), "Signing failed: " + e.getMessage());
					});
				}
			}
			
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					btnReleaseBuyer.setEnabled(true);
					btnReturnSeller.setEnabled(true);
					SketchwareUtil.showMessage(getApplicationContext(), "Couldn't fetch current nonce: " + error);
				});
			}
		});
	}
	
	private String _shortKey(String s) {
		// THE UPGRADE: threshold raised from 12 to 20 — order IDs are
		// now short, human-readable references (HARV-P2P-00000001, 17
		// chars) rather than 64-char hashes, and would otherwise get
		// needlessly truncated to "HARV-P2P-…0001" for no real
		// readability benefit. A guardian's public key (still a long
		// base64 string, the other thing this same helper truncates)
		// stays comfortably above this threshold either way.
		if (s == null || s.length() < 20) return s != null ? s : "";
		return s.substring(0, 8) + "\u2026" + s.substring(s.length() - 4);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Best-effort scrub — the JVM doesn't guarantee this actually
		// clears the underlying memory, but there's no reason to leave
		// an obvious reference to the raw seed around any longer than
		// the screen is alive.
		if (guardianSeed != null) java.util.Arrays.fill(guardianSeed, (byte) 0);
		guardianSeed = null;
	}
	
	{
	}
	
}
