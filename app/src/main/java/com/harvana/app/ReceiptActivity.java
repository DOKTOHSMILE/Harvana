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
			icon = R.drawable.icon_call_made_twotone;
			tint = androidx.core.content.ContextCompat.getColor(this, R.color.colorError);
		} else if (isIncoming) {
			icon = R.drawable.icon_call_received_twotone;
			tint = androidx.core.content.ContextCompat.getColor(this, R.color.colorAccent);
		} else {
			icon = R.drawable.icon_call_made_twotone;
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
		
		tv_network_fee.setText(_formatUIBalance(harvFee) + " " + tokenSymbol);
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
			// Another network's transaction: show the network and its explorer instead of HARV-only fields.
			final String explorer = getIntent().getStringExtra("explorerUrl");
			if (cardview4 != null) cardview4.setVisibility(View.VISIBLE);
			tv_memo.setText("Network: " + otherChain + (explorer != null && explorer.startsWith("http") ? "\nTap to open it in the " + otherChain + " explorer" : ""));
			tv_memo.setTextColor(UiColors.textPrimary());
			tv_memo.setTag(null);
			if (explorer != null && explorer.startsWith("http")) {
				tv_memo.setOnClickListener(v -> {
					try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(explorer))); } catch (Exception ignored) { }
				});
			}
			if (tv_burn_amount.getParent() instanceof View) ((View) tv_burn_amount.getParent()).setVisibility(View.GONE);
			if (tv_nonce.getParent() instanceof View) ((View) tv_nonce.getParent()).setVisibility(View.GONE);
			if (confirmations <= 0 && tv_confirmations.getParent() instanceof View) ((View) tv_confirmations.getParent()).setVisibility(View.GONE);
			if (harvFee <= 0 && tv_network_fee.getParent() instanceof View) ((View) tv_network_fee.getParent()).setVisibility(View.GONE);
			if (blockNumber <= 0) tv_block_number.setText(pending ? "Pending" : "—");
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
			writer.append("Date,TxHash,Status,Amount,Token,From,To,Network Fee,Burn Amount,Memo,Block Number,Confirmations,Nonce\n");
			
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
			Toast.makeText(this, "CSV export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
			Toast.makeText(this, "DOCX export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
