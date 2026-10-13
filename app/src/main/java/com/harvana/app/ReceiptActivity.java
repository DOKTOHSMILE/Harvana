package com.harvana.app;

import com.harvana.app.utils.UiColors;
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
import android.widget.ImageView;
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
import android.content.ClipData;
import android.content.ClipboardManager;


public class ReceiptActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private String txHash = "";
	private String status = "";
	private String sender = "";
	private String recipient = "";
	private double value = 0;
	private double harvFee = 0;
	private double burnAmount = 0;
	long blockNumber;
	long nonce;
	long timestamp;
	private String memo = "";
	private double confirmations = 0;
	
	private LinearLayout linear16;
	private LinearLayout linear17;
	private MaterialCardView cardview8;
	private MaterialCardView cardview9;
	private MaterialCardView cardview10;
	private MaterialButton btn_export_transaction;
	private MaterialCardView cardview1;
	private TextView tv_tx_value;
	private TextView tv_tx_status;
	private ImageView img_status_icon;
	private LinearLayout linear9;
	private TextView textview29;
	private TextView textview30;
	private TextView tv_from_address;
	private TextView textview31;
	private TextView tv_to_address;
	private TextView textview40;
	private MaterialCardView cardview4;
	private TextView tv_memo;
	private LinearLayout linear10;
	private TextView textview32;
	private LinearLayout linear20;
	private LinearLayout linear21;
	private LinearLayout linear22;
	private TextView textview33;
	private TextView tv_network_fee;
	private TextView textview34;
	private TextView tv_burn_amount;
	private TextView textview35;
	private TextView tv_timestamp;
	private LinearLayout linear11;
	private TextView textview36;
	private TextView textview37;
	private MaterialCardView cardview5;
	private LinearLayout linear24;
	private LinearLayout linear_confirmations;
	private LinearLayout linear25;
	private TextView tv_tx_hash;
	private TextView textview38;
	private TextView tv_block_number;
	private TextView textview_conf_label;
	private TextView tv_confirmations;
	private TextView textview39;
	private TextView tv_nonce;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.transaction_detail);
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
		linear16 = findViewById(R.id.linear16);
		linear17 = findViewById(R.id.linear17);
		cardview8 = findViewById(R.id.cardview8);
		cardview9 = findViewById(R.id.cardview9);
		cardview10 = findViewById(R.id.cardview10);
		btn_export_transaction = findViewById(R.id.btn_export_transaction);
		cardview1 = findViewById(R.id.cardview1);
		tv_tx_value = findViewById(R.id.tv_tx_value);
		tv_tx_status = findViewById(R.id.tv_tx_status);
		img_status_icon = findViewById(R.id.img_status_icon);
		linear9 = findViewById(R.id.linear9);
		textview29 = findViewById(R.id.textview29);
		textview30 = findViewById(R.id.textview30);
		tv_from_address = findViewById(R.id.tv_from_address);
		textview31 = findViewById(R.id.textview31);
		tv_to_address = findViewById(R.id.tv_to_address);
		textview40 = findViewById(R.id.textview40);
		cardview4 = findViewById(R.id.cardview4);
		tv_memo = findViewById(R.id.tv_memo);
		linear10 = findViewById(R.id.linear10);
		textview32 = findViewById(R.id.textview32);
		linear20 = findViewById(R.id.linear20);
		linear21 = findViewById(R.id.linear21);
		linear22 = findViewById(R.id.linear22);
		textview33 = findViewById(R.id.textview33);
		tv_network_fee = findViewById(R.id.tv_network_fee);
		textview34 = findViewById(R.id.textview34);
		tv_burn_amount = findViewById(R.id.tv_burn_amount);
		textview35 = findViewById(R.id.textview35);
		tv_timestamp = findViewById(R.id.tv_timestamp);
		linear11 = findViewById(R.id.linear11);
		textview36 = findViewById(R.id.textview36);
		textview37 = findViewById(R.id.textview37);
		cardview5 = findViewById(R.id.cardview5);
		linear24 = findViewById(R.id.linear24);
		linear_confirmations = findViewById(R.id.linear_confirmations);
		linear25 = findViewById(R.id.linear25);
		tv_tx_hash = findViewById(R.id.tv_tx_hash);
		textview38 = findViewById(R.id.textview38);
		tv_block_number = findViewById(R.id.tv_block_number);
		textview_conf_label = findViewById(R.id.textview_conf_label);
		tv_confirmations = findViewById(R.id.tv_confirmations);
		textview39 = findViewById(R.id.textview39);
		tv_nonce = findViewById(R.id.tv_nonce);
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		setTitle("Receipt");
		
		txHash = getIntent().getStringExtra("txHash");
		status = getIntent().getStringExtra("status");
		sender = getIntent().getStringExtra("senderAddress");
		recipient = getIntent().getStringExtra("recipientAddress");
		memo = getIntent().getStringExtra("memo"); 
		value = getIntent().getDoubleExtra("value", 0.0);
		harvFee = getIntent().getDoubleExtra("harvFee", 0.0);
		burnAmount = getIntent().getDoubleExtra("burnAmount", 0.0);
		blockNumber = getIntent().getLongExtra("blockNumber", 0);
		nonce = getIntent().getLongExtra("nonce", 0);
		timestamp = getIntent().getLongExtra("timestamp", System.currentTimeMillis());
		
		String tokenSymbol = getIntent().getStringExtra("tokenSymbol");
		if (tokenSymbol == null || tokenSymbol.trim().isEmpty()) tokenSymbol = com.harvana.app.CoinInfo.ticker();
		
		confirmations = getIntent().getLongExtra("confirmations", 0);
		
		SharedPreferences prefs = getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
		String activeAddress = prefs.getString("active_wallet_address", "").trim();
		if (activeAddress.isEmpty()) {
			activeAddress = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
			if (activeAddress == null) activeAddress = "";
		}
		final String otherChain = getIntent().getStringExtra("chain");   // Pi, Bitcoin, Litecoin: not a HARV transaction
		boolean isIncoming = otherChain != null
		? getIntent().getBooleanExtra("incoming", false)
		: recipient != null && !recipient.isEmpty() && recipient.equalsIgnoreCase(activeAddress);
		boolean pending = status != null && status.equalsIgnoreCase("pending");
		boolean confirmed = status != null && (status.equalsIgnoreCase("confirmed") || status.equalsIgnoreCase("success"));
		
		tv_tx_value.setText(_formatUIBalance(value) + " " + tokenSymbol);
		com.harvana.app.tx.TxKind _kind = com.harvana.app.tx.TxKind.of(memo, isIncoming);
		textview40.setText(_kind != null ? _kind.title : (isIncoming ? "Received" : "Sent"));

		// Same icon the activity row uses: the kind (fee, stake, gift...) when we know it, otherwise the plain arrow.
		boolean failed = !pending && !confirmed;
		int icon;
		int tint;
		if (pending) {
			icon = R.drawable.icon_access_time_outline;
			tint = androidx.core.content.ContextCompat.getColor(this, R.color.colorWarning);
		} else if (failed) {
			icon = R.drawable.ic_action_send;
			tint = androidx.core.content.ContextCompat.getColor(this, R.color.colorError);
		} else if (isIncoming) {
			icon = R.drawable.ic_action_receive;
			tint = androidx.core.content.ContextCompat.getColor(this, R.color.colorAccent);
		} else {
			icon = R.drawable.ic_action_send;
			tint = androidx.core.content.ContextCompat.getColor(this, R.color.colorError);
		}
		if (confirmed && _kind != null) icon = _kind.icon;
		img_status_icon.setImageResource(icon);
		img_status_icon.setColorFilter(tint);

		if (pending) {
			tv_tx_status.setText("●  Pending");
			tv_tx_status.setTextColor(UiColors.warning());
		} else if (confirmed) {
			tv_tx_status.setText("●  Completed");
			tv_tx_status.setTextColor(UiColors.success());
		} else {
			tv_tx_status.setText("●  Failed");
			tv_tx_status.setTextColor(UiColors.error());
		}
		
		tv_from_address.setText(_shortAddr(sender));
		tv_from_address.setTag(sender);
		tv_to_address.setText(_shortAddr(recipient));
		tv_to_address.setTag(recipient);
		textview31.setText(isIncoming ? "Received from" : "Sent from");
		
		if (memo == null || memo.trim().isEmpty()) {
			if (cardview4 != null) cardview4.setVisibility(View.GONE);
		} else {
			tv_memo.setText(com.harvana.app.tx.TxKind.describe(memo, isIncoming));   // plain words; the raw memo is copied on tap
			tv_memo.setTextColor(UiColors.textPrimary());
			tv_memo.setTag(memo);
		}
		
		// The fare's own coin: HARV when it was paid in HARV, the coin when a coin transfer paid it in the coin.
		String feeSymbol = getIntent().getStringExtra("feeSymbol");
		if (feeSymbol == null || feeSymbol.isEmpty()) feeSymbol = tokenSymbol;
		tv_network_fee.setText(_formatUIBalance(harvFee) + " " + feeSymbol);
		tv_burn_amount.setText(_formatUIBalance(burnAmount) + " " + tokenSymbol);
		
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy | h:mma", Locale.US);
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("Africa/Lagos"));
		String when = sdf.format(new Date(timestamp));
		tv_timestamp.setText(when);
		TextView tl1 = findViewById(R.id.tv_tl1);
		TextView tl2 = findViewById(R.id.tv_tl2);
		TextView tl3 = findViewById(R.id.tv_tl3);
		TextView tl3Title = findViewById(R.id.tv_tl3_title);
		TextView tl3Body = findViewById(R.id.tv_tl3_body);
		TextView tl3Mark = findViewById(R.id.tv_tl3_mark);
		if (tl1 != null) tl1.setText(when);
		if (tl2 != null) tl2.setText(when);
		if (tl3 != null) tl3.setText(when);
		if (!confirmed) {
			if (tl3Title != null) tl3Title.setText(pending ? "Waiting to complete" : "Payment failed");
			if (tl3Body != null) tl3Body.setText(pending ? "This transfer is not confirmed yet" : "This transfer did not complete");
			if (tl3Mark != null) tl3Mark.setTextColor(pending ? UiColors.warning() : UiColors.error());
		}
		
		tv_tx_hash.setText(_shortAddr(txHash));
		tv_tx_hash.setTag(txHash);
		tv_block_number.setText(blockNumber > 0 ? String.valueOf(blockNumber) : "Pending");
		tv_confirmations.setText(confirmations > 0 ? String.valueOf((long) confirmations) : "0");
		tv_nonce.setText(String.valueOf(nonce));
		
		if (otherChain != null) {
			final String lookup = getIntent().getStringExtra("assetKey") != null
					&& !getIntent().getStringExtra("assetKey").isEmpty()
					? getIntent().getStringExtra("assetKey") : tokenSymbol;
			final String unit = "BASE".equals(lookup) ? "ETH" : tokenSymbol;
			_shape(lookup, otherChain, null, unit);
			final String hashNow = txHash == null ? "" : txHash;
			new Thread(() -> {
				java.util.HashMap<String, Object> fresh = com.harvana.app.wallet.ChainHistory.detail(lookup, hashNow);
				runOnUiThread(() -> {
					if (isFinishing()) return;
					if (fresh != null) _applyExplorer(fresh, unit);
					_shape(lookup, otherChain, fresh, unit);
				});
			}).start();
		}
		
		View.OnClickListener copyListener = v -> {
			String text = null;
			if (v.getTag() instanceof String) text = (String) v.getTag();
			else if (v instanceof TextView) text = ((TextView) v).getText().toString();
			if (text != null && !text.isEmpty() && !text.equalsIgnoreCase("N/A") && !text.equalsIgnoreCase("Unknown Address") && !text.equalsIgnoreCase("None") && !"—".equals(text)) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setPrimaryClip(ClipData.newPlainText("Copied", text));
				Toast.makeText(ReceiptActivity.this, "Copied", Toast.LENGTH_SHORT).show();
			}
		};
		
		tv_tx_hash.setOnClickListener(copyListener);
		tv_from_address.setOnClickListener(copyListener);
		tv_to_address.setOnClickListener(copyListener);
		if (otherChain == null) tv_memo.setOnClickListener(copyListener);
		
		textview37.setOnClickListener(v -> {
			boolean open = linear11.getVisibility() == View.VISIBLE;
			linear11.setVisibility(open ? View.GONE : View.VISIBLE);
			textview37.setText(open ? "See more" : "See less");
		});
		
		btn_export_transaction.setOnClickListener(v -> _showExportOptionsBottomSheet());
		
		// ... (Other activity code)
	}
	
	private android.view.ViewGroup factBox;

	/**
	 * Each chain keeps the facts that belong to it. Bitcoin has a size and no nonce. Ethereum has gas and a nonce.
	 * Solana has a slot. Pi has a ledger and a memo. Harvana's burn row is not shown for any of them.
	 */
	private void _shape(String key, String network, java.util.HashMap<String, Object> fresh, String unit) {
		String k = key == null ? "" : key;
		boolean evm = "ETH".equals(k) || "BNB".equals(k) || "POL".equals(k) || "BASE".equals(k);
		boolean utxo = "BTC".equals(k) || "LTC".equals(k) || "DOGE".equals(k);
		boolean sol = "SOL".equals(k);
		boolean pi = "PI".equals(k);
		if (textview29 != null) textview29.setText(network == null || network.isEmpty() ? "Transaction details" : network);
		if (textview33 != null) textview33.setText(evm ? "Gas fee" : "Network fee");
		if (textview36 != null) textview36.setText(sol ? "Signature" : "Transaction");
		TextView blockLabel = findViewById(R.id.textview38);
		if (blockLabel != null) blockLabel.setText(sol ? "Slot" : pi ? "Ledger" : "Block");
		if (tv_burn_amount != null && tv_burn_amount.getParent() instanceof View) ((View) tv_burn_amount.getParent()).setVisibility(View.GONE);
		if (tv_nonce != null && tv_nonce.getParent() instanceof View) ((View) tv_nonce.getParent()).setVisibility(evm ? View.VISIBLE : View.GONE);
		View confRow = findViewById(R.id.linear_confirmations);
		if (confRow != null) confRow.setVisibility(sol || pi ? View.GONE : View.VISIBLE);
		if (linear11 != null) linear11.setVisibility(View.VISIBLE);
		if (textview37 != null) textview37.setText("See less");
		String memoText = fresh != null && fresh.get("memo") instanceof String ? (String) fresh.get("memo") : "";
		if (pi && memoText != null && !memoText.trim().isEmpty() && !"none".equalsIgnoreCase(memoText.trim())) {
			if (cardview4 != null) cardview4.setVisibility(View.VISIBLE);
			if (cardview4 != null && cardview4.getChildCount() > 0 && cardview4.getChildAt(0) instanceof android.view.ViewGroup) {
				android.view.ViewGroup row = (android.view.ViewGroup) cardview4.getChildAt(0);
				if (row.getChildCount() > 0 && row.getChildAt(0) instanceof TextView) ((TextView) row.getChildAt(0)).setText("Memo");
			}
			tv_memo.setText(memoText);
			tv_memo.setTag(memoText);
		} else if (cardview4 != null) cardview4.setVisibility(View.GONE);
		org.json.JSONObject raw = null;
		if (fresh != null && fresh.get("raw") instanceof String) {
			try { raw = new org.json.JSONObject((String) fresh.get("raw")); } catch (Exception ignored) { }
		}
		_facts(k, evm, utxo, sol, fresh, raw, unit);
	}

	private void _facts(String key, boolean evm, boolean utxo, boolean sol, java.util.HashMap<String, Object> fresh, org.json.JSONObject raw, String unit) {
		if (factBox == null) {
			View block = findViewById(R.id.linear24);
			if (block != null && block.getParent() instanceof android.view.ViewGroup) factBox = (android.view.ViewGroup) block.getParent();
		}
		if (factBox == null) return;
		for (int i = factBox.getChildCount() - 1; i >= 0; i--) {
			if ("chain_fact".equals(factBox.getChildAt(i).getTag())) factBox.removeViewAt(i);
		}
		if (fresh == null && raw == null) return;
		if (evm) {
			long used = fresh != null && fresh.get("gasUsed") instanceof Number ? ((Number) fresh.get("gasUsed")).longValue() : 0;
			long limit = fresh != null && fresh.get("gasLimit") instanceof Number ? ((Number) fresh.get("gasLimit")).longValue() : 0;
			long price = fresh != null && fresh.get("gasPrice") instanceof Number ? ((Number) fresh.get("gasPrice")).longValue() : 0;
			if (price == 0 && raw != null) {
				String hex = raw.optString("gasPrice");
				if (hex.isEmpty()) hex = raw.optString("maxFeePerGas");
				price = _hex(hex);
			}
			if (used > 0) _fact("Gas used", String.valueOf(used));
			if (limit > 0) _fact("Gas limit", String.valueOf(limit));
			if (price > 0) _fact("Gas price", _trim(price / 1_000_000_000d) + " Gwei");
		} else if (utxo && raw != null) {
			int weight = raw.optInt("weight");
			int size = raw.optInt("vsize", raw.optInt("size"));
			int vsize = weight > 0 ? (weight + 3) / 4 : size;
			if (vsize > 0) _fact("Size", vsize + " vB");
			int inputs = raw.optInt("inputs_n", raw.optJSONArray("vin") == null ? raw.optJSONArray("inputs") == null ? 0 : raw.optJSONArray("inputs").length() : raw.optJSONArray("vin").length());
			int outputs = raw.optInt("outputs_n", raw.optJSONArray("vout") == null ? raw.optJSONArray("outputs") == null ? 0 : raw.optJSONArray("outputs").length() : raw.optJSONArray("vout").length());
			if (inputs > 0) _fact("Inputs", String.valueOf(inputs));
			if (outputs > 0) _fact("Outputs", String.valueOf(outputs));
		} else if ("TRX".equals(key) && raw != null) {
			String result = raw.optString("contractRet");
			if (!result.isEmpty()) _fact("Result", result);
			long energy = raw.optLong("energy_usage_total", raw.optLong("energy_usage"));
			long net = raw.optLong("net_usage");
			if (energy > 0) _fact("Energy", String.valueOf(energy));
			if (net > 0) _fact("Bandwidth", String.valueOf(net));
		} else if (sol && raw != null) {
			org.json.JSONObject meta = raw.optJSONObject("meta");
			if (meta != null && meta.has("computeUnitsConsumed")) _fact("Compute", String.valueOf(meta.optLong("computeUnitsConsumed")));
		}
	}

	private void _fact(String label, String value) {
		int pad = Math.round(12 * getResources().getDisplayMetrics().density);
		android.widget.LinearLayout row = new android.widget.LinearLayout(this);
		row.setTag("chain_fact");
		row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		row.setGravity(android.view.Gravity.CENTER_VERTICAL);
		row.setPadding(0, pad, 0, pad);
		TextView l = new TextView(this);
		l.setText(label);
		l.setTextColor(UiColors.textSecondary());
		l.setTextSize(15);
		android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		l.setLayoutParams(lp);
		TextView v = new TextView(this);
		v.setText(value);
		v.setTextColor(UiColors.textPrimary());
		v.setTextSize(15);
		v.setGravity(android.view.Gravity.END);
		row.addView(l);
		row.addView(v);
		factBox.addView(row);
	}

	private static long _hex(String hex) {
		if (hex == null) return 0;
		String s = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
		if (s.isEmpty()) return 0;
		try { return new java.math.BigInteger(s, 16).longValue(); } catch (Exception e) { return 0; }
	}

	private static String _trim(double n) {
		String s = String.format(java.util.Locale.US, "%.4f", n);
		int i = s.length();
		while (i > 0 && s.charAt(i - 1) == '0') i--;
		if (i > 0 && s.charAt(i - 1) == '.') i--;
		return s.substring(0, i);
	}

	/** Fills the receipt with the record that coin's explorer returned. */
	private void _applyExplorer(java.util.HashMap<String, Object> d, String ticker) {
		Object v = d.get("value");
		if (v instanceof Number && ((Number) v).doubleValue() > 0) {
			value = ((Number) v).doubleValue();
			tv_tx_value.setText(_formatUIBalance(value) + " " + ticker);
		}
		Object from = d.get("senderAddress");
		if (from instanceof String && !((String) from).isEmpty()) {
			sender = (String) from;
			tv_from_address.setText(_shortAddr(sender));
			tv_from_address.setTag(sender);
		}
		Object to = d.get("recipientAddress");
		if (to instanceof String && !((String) to).isEmpty()) {
			recipient = (String) to;
			tv_to_address.setText(_shortAddr(recipient));
			tv_to_address.setTag(recipient);
		}
		Object fee = d.get("harvFee");
		if (fee instanceof Number && ((Number) fee).doubleValue() > 0 && tv_network_fee != null) {
			if (tv_network_fee.getParent() instanceof View) ((View) tv_network_fee.getParent()).setVisibility(View.VISIBLE);
			tv_network_fee.setText(_formatUIBalance(((Number) fee).doubleValue()) + " " + ticker);
		}
		Object block = d.get("blockNumber");
		if (block instanceof Number && ((Number) block).longValue() > 0) tv_block_number.setText(String.valueOf(((Number) block).longValue()));
		Object conf = d.get("confirmations");
		if (conf instanceof Number && ((Number) conf).longValue() > 0 && tv_confirmations != null) {
			if (tv_confirmations.getParent() instanceof View) ((View) tv_confirmations.getParent()).setVisibility(View.VISIBLE);
			tv_confirmations.setText(String.valueOf(((Number) conf).longValue()));
		}
		Object nonceN = d.get("nonce");
		if (nonceN instanceof Number && tv_nonce != null) tv_nonce.setText(String.valueOf(((Number) nonceN).longValue()));
		Object st = d.get("status");
		if (st != null && tv_tx_status != null) {
			String s = String.valueOf(st);
			if ("pending".equalsIgnoreCase(s)) {
				tv_tx_status.setText("●  Pending");
				tv_tx_status.setTextColor(UiColors.warning());
			} else if ("failed".equalsIgnoreCase(s)) {
				tv_tx_status.setText("●  Failed");
				tv_tx_status.setTextColor(UiColors.error());
			} else {
				tv_tx_status.setText("●  Completed");
				tv_tx_status.setTextColor(UiColors.success());
			}
		}
		Object when = d.get("timestamp");
		if (when instanceof Number && ((Number) when).longValue() > 0 && tv_timestamp != null) {
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy | h:mma", java.util.Locale.US);
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("Africa/Lagos"));
			tv_timestamp.setText(sdf.format(new java.util.Date(((Number) when).longValue())));
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// Add to onStart
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
		globalTxReceiver, 
		new android.content.IntentFilter("com.harvana.app.ACTION_WALLET_RECEIVED")
		);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		// Add to onStop
		try {
			androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(globalTxReceiver);
		} catch (Exception ignored) {}
		
	}
	

	public void _inAppNotification() {
	}
	
	private final BroadcastReceiver globalTxReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && "com.harvana.app.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("amount", 0.0);
				String sender = intent.getStringExtra("sender");
				
				// Pass 4 arguments: Activity, Title, Message, Sender
				com.harvana.app.utils.InAppNotificationHelper.showTransactionNotification(
				ReceiptActivity.this, // Replace with your current Activity
				"Incoming Transfer", 
				"Received " + String.format(Locale.US, "%.5f", amount) + (" " + com.harvana.app.CoinInfo.ticker()),
				sender
				);
			}
		}
	};
	
	{
		
	}
	
	
	public void _MORE() {
	}
	private void _showExportOptionsBottomSheet() {
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
		new com.harvana.app.ui.AppSheet(this);
		View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_export_options, null);
		bottomSheetDialog.setContentView(sheetView);
		
		sheetView.findViewById(R.id.btn_export_image).setOnClickListener(v -> {
			bottomSheetDialog.dismiss();
			_exportTransactionsToImage();
		});
		sheetView.findViewById(R.id.btn_export_csv).setOnClickListener(v -> {
			bottomSheetDialog.dismiss();
			_exportTransactionsToCSV();
		});
		sheetView.findViewById(R.id.btn_export_docx).setOnClickListener(v -> {
			bottomSheetDialog.dismiss();
			_exportTransactionsToDocx();
		});
		sheetView.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> {
			bottomSheetDialog.dismiss();
			_exportTransactionsToPdf();
		});
		
		bottomSheetDialog.show();
	}
	
	private boolean _checkStoragePermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			return true;
		}
		if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
			androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
			Toast.makeText(this, "Permission requested. Please grant and click export again.", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	private File _getSafeExportFile(String fileName) throws IOException {
		File primaryDir = new File(Environment.getExternalStorageDirectory(), "Harvana/media/documents");
		if (!primaryDir.exists()) primaryDir.mkdirs();
		File file = new File(primaryDir, fileName);
		try {
			if (!file.exists()) file.createNewFile();
			if (file.canWrite()) return file;
		} catch (Exception ignored) {}
		
		File secondaryDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Harvana");
		if (!secondaryDir.exists()) secondaryDir.mkdirs();
		file = new File(secondaryDir, fileName);
		try {
			if (!file.exists()) file.createNewFile();
			if (file.canWrite()) return file;
		} catch (Exception ignored) {}
		
		File fallbackDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Harvana");
		if (!fallbackDir.exists()) fallbackDir.mkdirs();
		file = new File(fallbackDir, fileName);
		if (!file.exists()) file.createNewFile();
		return file;
	}
	
	private void _shareExportedFile(File file, String mimeType, String subject) {
		com.harvana.app.receipt.ReceiptShare.shareFile(this, file, mimeType, subject);
	}
	
	private Bitmap _getAppLogo() {
		try {
			int resId = getResources().getIdentifier("app_icon", "drawable", getPackageName());
			if (resId == 0) resId = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
			if (resId != 0) {
				Bitmap b = BitmapFactory.decodeResource(getResources(), resId);
				return Bitmap.createScaledBitmap(b, 60, 60, true);
			}
		} catch (Exception ignored) {}
		return null;
	}
	
	private void _drawDocumentHeader(Canvas canvas, Paint paint, Bitmap logo) {
		String genDate = new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(new Date());
		int y = 40;
		if (logo != null) {
			canvas.drawBitmap(logo, 40, y, paint);
		}
		
		int textX = (logo != null) ? 120 : 40;
		
		paint.setColor(Color.BLACK);
		paint.setTextSize(18f);
		paint.setFakeBoldText(true);
		canvas.drawText("Transaction Receipt", textX, y + 15, paint);
		
		paint.setTextSize(11f);
		paint.setFakeBoldText(false);
		paint.setColor(Color.DKGRAY);
		canvas.drawText("Network: Harvana Protocol", textX, y + 35, paint);
		canvas.drawText("Generated: " + genDate, textX, y + 55, paint);
		
		paint.setColor(Color.LTGRAY);
		paint.setStrokeWidth(1f);
		canvas.drawLine(40, y + 75, 555, y + 75, paint);
	}
	// Helper method to draw structured receipt rows
	private int _drawCanvasRow(Canvas canvas, Paint paint, String label, String value, int x, int y) {
		// Draw Label
		paint.setColor(Color.parseColor("#8592AD")); // Light grey/blue label
		paint.setTextSize(15f);
		paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		canvas.drawText(label.toUpperCase(), x, y, paint);
		
		y += 25;
		
		// Draw Value
		paint.setColor(Color.WHITE); // White text value
		paint.setTextSize(18f);
		paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)); // Monospace for clean hashes/addresses
		
		// Auto-wrap long strings (like 64-char TxHashes or 42-char addresses)
		int maxChars = 56;
		if (value.length() > maxChars) {
			String line1 = value.substring(0, maxChars);
			String line2 = value.substring(maxChars);
			canvas.drawText(line1, x, y, paint);
			y += 28;
			canvas.drawText(line2, x, y, paint);
		} else {
			canvas.drawText(value, x, y, paint);
		}
		
		return y + 50; // Return the new Y-position plus spacing for the next row
	}
	
	private void _exportTransactionsToCSV() {
		if (!_checkStoragePermission()) return;
		try {
			String timeStampStr = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File csvFile = _getSafeExportFile("receipt_" + timeStampStr + ".csv");
			FileWriter writer = new FileWriter(csvFile);
			
			// 1. Write standard CSV Headers
			writer.append("Date,TxHash,Status,Amount,Token,From,To,tFare,Burn Amount,Memo,Block Number,Confirmations,Nonce\n");
			
			// 2. Safely format data to prevent CSV column breakage
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("Africa/Lagos"));
			String dateStr = sdf.format(new Date(timestamp));
			
			String safeStatus = (status == null) ? "UNKNOWN" : status.toUpperCase();
			String tokenSymbol = getIntent().getStringExtra("tokenSymbol");
			if (tokenSymbol == null || tokenSymbol.isEmpty()) tokenSymbol = com.harvana.app.CoinInfo.ticker();
			
			// Replace commas in memo with spaces so it doesn't break the CSV columns
			String safeMemo = (memo == null || memo.trim().isEmpty()) ? "None" : memo.replace(",", " "); 
			String blockStr = blockNumber > 0 ? String.valueOf(blockNumber) : "Pending";
			String confStr = confirmations > 0 ? String.valueOf(confirmations) : "0";
			
			// APPLIED FIX: Data layer formatter (NO COMMAS, strips trailing zeros)
			java.text.DecimalFormat dataDf = new java.text.DecimalFormat("0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			
			// 3. Write data row
			writer.append(dateStr).append(",")
			.append(txHash != null ? txHash : "N/A").append(",")
			.append(safeStatus).append(",")
			.append(dataDf.format(value)).append(",") // APPLIED FIX
			.append(tokenSymbol).append(",")
			.append(sender != null ? sender : "Unknown").append(",")
			.append(recipient != null ? recipient : "Unknown").append(",")
			.append(dataDf.format(harvFee)).append(",") // APPLIED FIX
			.append(dataDf.format(burnAmount)).append(",") // APPLIED FIX
			.append(safeMemo).append(",")
			.append(blockStr).append(",")
			.append(confStr).append(",")
			.append(String.valueOf(nonce)).append("\n");
			
			writer.flush();
			writer.close();
			
			_shareExportedFile(csvFile, "text/csv", "Transaction Receipt CSV");
		} catch (Exception e) {
			Toast.makeText(this, "CSV export failed: " + com.harvana.app.utils.ErrorText.of(e), Toast.LENGTH_SHORT).show();
		}
	}
	
	
	private com.harvana.app.receipt.Receipt _buildReceipt() {
		SharedPreferences prefs = getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
		String activeAddress = prefs.getString("active_wallet_address", "").trim();
		if (activeAddress.isEmpty()) {
			activeAddress = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);
			if (activeAddress == null) activeAddress = "";
		}
		boolean incoming = recipient != null && !recipient.isEmpty() && recipient.equalsIgnoreCase(activeAddress);
		String symbol = getIntent().getStringExtra("tokenSymbol");
		if (symbol == null || symbol.isEmpty()) symbol = com.harvana.app.CoinInfo.ticker();
		com.harvana.app.tx.TxKind kind = com.harvana.app.tx.TxKind.of(memo, incoming);
		return com.harvana.app.receipt.Receipts.fromTransaction(kind != null ? kind.title : null, incoming, status,
		_formatUIBalance(value), symbol, timestamp, sender, recipient, memo,
		_formatUIBalance(harvFee) + " " + symbol, _formatUIBalance(burnAmount) + " " + symbol,
		txHash, blockNumber, (long) confirmations, nonce);
	}
	
	/** Shares the receipt as an image: the same drawing everywhere in the app. */
	private void _exportTransactionsToImage() {
		com.harvana.app.receipt.ReceiptShare.image(this, _buildReceipt());
	}
	
	
	private void _exportTransactionsToDocx() {
		if (!_checkStoragePermission()) return;
		try {
			StringBuilder doc = new StringBuilder();
			doc.append("==================================================\n");
			doc.append("            HARVANA NETWORK TRANSACTION              \n");
			doc.append("==================================================\n\n");
			
			String safeStatus = (status == null) ? "UNKNOWN" : status.toUpperCase();
			String tokenSymbol = getIntent().getStringExtra("tokenSymbol");
			if (tokenSymbol == null || tokenSymbol.isEmpty()) tokenSymbol = com.harvana.app.CoinInfo.ticker();
			
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US);
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("Africa/Lagos"));
			String dateStr = sdf.format(new Date(timestamp));
			
			// Main Info
			doc.append("STATUS      : ").append(safeStatus).append("\n");
			// APPLIED FIX
			doc.append("AMOUNT      : ").append(_formatUIBalance(value)).append(" ").append(tokenSymbol).append("\n");
			doc.append("DATE        : ").append(dateStr).append("\n\n");
			
			doc.append("----------------- ROUTING ------------------------\n");
			doc.append("FROM        : ").append(sender).append("\n");
			doc.append("TO          : ").append(recipient).append("\n");
			if (memo != null && !memo.trim().isEmpty()) {
				doc.append("MEMO        : ").append(memo).append("\n");
			}
			doc.append("\n");
			
			doc.append("-------------- NETWORK DATA ----------------------\n");
			// APPLIED FIX
			doc.append("NETWORK FEE : ").append(_formatUIBalance(harvFee)).append((" " + com.harvana.app.CoinInfo.ticker() + "\n"));
			doc.append("BURN AMOUNT : ").append(_formatUIBalance(burnAmount)).append((" " + com.harvana.app.CoinInfo.ticker() + "\n\n"));
			
			doc.append("----------- BLOCKCHAIN DETAILS -------------------\n");
			doc.append("TX HASH     : ").append(txHash).append("\n");
			doc.append("BLOCK NUM   : ").append(blockNumber > 0 ? blockNumber : "Pending").append("\n");
			doc.append("CONFIRMS    : ").append(confirmations > 0 ? confirmations + "+" : "0 (Pending)").append("\n");
			doc.append("NONCE       : ").append(nonce).append("\n\n");
			
			doc.append("==================================================\n");
			doc.append("Generated on: ").append(new Date().toString()).append("\n");
			
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File docFile = _getSafeExportFile("receipt_" + timeStamp + ".docx");
			FileWriter writer = new FileWriter(docFile);
			writer.append(doc.toString());
			writer.flush();
			writer.close();
			
			_shareExportedFile(docFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Transaction Receipt DOCX");
		} catch (Exception e) {
			Toast.makeText(this, "DOCX export failed: " + com.harvana.app.utils.ErrorText.of(e), Toast.LENGTH_SHORT).show();
		}
	}
	
	/** Shares the receipt as a PDF: the same drawing everywhere in the app. */
	private void _exportTransactionsToPdf() {
		com.harvana.app.receipt.ReceiptShare.pdf(this, _buildReceipt());
	}
	
	
	// Dedicated helper for scaling text correctly on the A4 PDF Canvas
	private int _drawPdfRow(Canvas canvas, Paint paint, String label, String value, int x, int y) {
		paint.setColor(Color.parseColor("#8592AD")); 
		paint.setTextSize(11f);
		paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		canvas.drawText(label.toUpperCase(), x, y, paint);
		
		y += 18; // Space between label and value
		
		paint.setColor(Color.WHITE);
		paint.setTextSize(12.5f);
		paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)); 
		
		// PDF width is 595, so we wrap strings past ~62 characters
		int maxChars = 62;
		if (value != null && value.length() > maxChars) {
			String line1 = value.substring(0, maxChars);
			String line2 = value.substring(maxChars);
			canvas.drawText(line1, x, y, paint);
			y += 18;
			canvas.drawText(line2, x, y, paint);
		} else {
			canvas.drawText(value != null ? value : "", x, y, paint);
		}
		
		return y + 32; // Bottom margin for the next row
	}
	

	private String _shortAddr(String a) {
		if (a == null || a.trim().isEmpty()) return "—";
		String v = a.trim();
		if (v.length() <= 16) return v;
		return v.substring(0, 8) + "…" + v.substring(v.length() - 4);
	}

	// NEW UI FORMATTER HELPER
	private String _formatUIBalance(double v) {
		try {
			java.text.DecimalFormat uiDf = new java.text.DecimalFormat("#,##0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return uiDf.format(v);
		} catch (Exception e) {
			return String.valueOf(v);
		}
	}
	
	{
	}
	
}
