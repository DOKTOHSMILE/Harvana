package com.harvanna.wallet;

import com.harvanna.wallet.utils.UiColors;
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


public class TransactionDetailActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private String txHash = "";
	private String status = "";
	private String sender = "";
	private String recipient = "";
	private double value = 0;
	private double ajcFee = 0;
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
			window.setStatusBarColor(backgroundColor);
		}
		
		setTitle("Transaction details");
		
		txHash = getIntent().getStringExtra("txHash");
		status = getIntent().getStringExtra("status");
		sender = getIntent().getStringExtra("senderAddress");
		recipient = getIntent().getStringExtra("recipientAddress");
		memo = getIntent().getStringExtra("memo"); 
		value = getIntent().getDoubleExtra("value", 0.0);
		ajcFee = getIntent().getDoubleExtra("ajcFee", 0.0);
		burnAmount = getIntent().getDoubleExtra("burnAmount", 0.0);
		blockNumber = getIntent().getLongExtra("blockNumber", 0);
		nonce = getIntent().getLongExtra("nonce", 0);
		timestamp = getIntent().getLongExtra("timestamp", System.currentTimeMillis());
		
		String tokenSymbol = getIntent().getStringExtra("tokenSymbol");
		if (tokenSymbol == null || tokenSymbol.trim().isEmpty()) tokenSymbol = "AJC";
		
		confirmations = getIntent().getLongExtra("confirmations", 0);
		
		// Resolve active wallet
		SharedPreferences prefs = getSharedPreferences("ajc_wallet_prefs", Context.MODE_PRIVATE);
		String activeAddress = prefs.getString("active_wallet_address", "").trim();
		if (activeAddress.isEmpty()) {
			activeAddress = com.harvanna.wallet.wallet.WalletManager.getActiveWalletAddress(this);
			if (activeAddress == null) activeAddress = "";
		}
		boolean isIncoming = recipient != null && !recipient.isEmpty() && recipient.equalsIgnoreCase(activeAddress);
		
		String valuePrefix = isIncoming ? "+ " : "- ";
		
		// APPLIED FIX
		tv_tx_value.setText(valuePrefix + _formatUIBalance(value) + " " + tokenSymbol);
		
		// Status rendering logic
		if (status != null && status.equalsIgnoreCase("pending")) {
			tv_tx_status.setText("PENDING");
			tv_tx_status.setTextColor(UiColors.warning());
			img_status_icon.setImageResource(R.drawable.icon_access_time_outline);
			img_status_icon.setColorFilter(UiColors.warning());
			
		} else if (status != null && (status.equalsIgnoreCase("confirmed") || status.equalsIgnoreCase("success"))) {
			tv_tx_status.setText("CONFIRMED");
			tv_tx_status.setTextColor(UiColors.success());
			
			img_status_icon.setImageResource(isIncoming ? R.drawable.icon_call_received_twotone : R.drawable.icon_call_made_twotone);
			img_status_icon.setColorFilter(isIncoming ? UiColors.success() : UiColors.error());
		} else {
			tv_tx_status.setText(status != null ? status.toUpperCase() : "FAILED");
			tv_tx_status.setTextColor(UiColors.error()); 
			
			img_status_icon.setImageResource(isIncoming ? R.drawable.icon_call_received_twotone : R.drawable.icon_call_made_twotone);
			img_status_icon.setColorFilter(UiColors.error()); 
		}
		
		tv_from_address.setText(sender != null && !sender.isEmpty() ? sender : "Unknown Address");
		tv_to_address.setText(recipient != null && !recipient.isEmpty() ? recipient : "Unknown Address");
		
		if (memo == null || memo.trim().isEmpty()) {
			tv_memo.setText("None");
			tv_memo.setTextColor(UiColors.textTertiary());
		} else {
			tv_memo.setText(memo);
			tv_memo.setTextColor(UiColors.textSecondary());
		}
		
		// APPLIED FIX
		tv_network_fee.setText(_formatUIBalance(ajcFee) + " AJC");
		tv_burn_amount.setText(_formatUIBalance(burnAmount) + " AJC");
		
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US);
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("Africa/Lagos"));
		tv_timestamp.setText(sdf.format(new Date(timestamp)));
		
		tv_tx_hash.setText(txHash != null && !txHash.isEmpty() ? txHash : "N/A");
		tv_block_number.setText(blockNumber > 0 ? String.valueOf(blockNumber) : "Pending");
		tv_confirmations.setText(confirmations > 0 ? confirmations + "+" : "0 (Pending)");
		tv_nonce.setText(String.valueOf(nonce));
		
		View.OnClickListener copyListener = v -> {
			if (v instanceof TextView) {
				String text = ((TextView) v).getText().toString();
				if (!text.isEmpty() && !text.equalsIgnoreCase("N/A") && !text.equalsIgnoreCase("Unknown Address") && !text.equalsIgnoreCase("None")) {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("Copied Data", text);
					clipboard.setPrimaryClip(clip);
					Toast.makeText(TransactionDetailActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
				}
			}
		};
		
		tv_tx_hash.setOnClickListener(copyListener);
		tv_from_address.setOnClickListener(copyListener);
		tv_to_address.setOnClickListener(copyListener);
		tv_memo.setOnClickListener(copyListener);
		
		// Wire up the export button
		btn_export_transaction.setOnClickListener(v -> _showExportOptionsBottomSheet());
		
		// ... (Other activity code)
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// Add to onStart
		androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
		globalTxReceiver, 
		new android.content.IntentFilter("com.harvanna.wallet.ACTION_WALLET_RECEIVED")
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
			if (intent != null && "com.harvanna.wallet.ACTION_WALLET_RECEIVED".equals(intent.getAction())) {
				double amount = intent.getDoubleExtra("amount", 0.0);
				String sender = intent.getStringExtra("sender");
				
				// Pass 4 arguments: Activity, Title, Message, Sender
				com.harvanna.wallet.utils.InAppNotificationHelper.showTransactionNotification(
				TransactionDetailActivity.this, // Replace with your current Activity
				"Incoming Transfer", 
				"Received " + String.format(Locale.US, "%.5f", amount) + " AJC",
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
		new com.google.android.material.bottomsheet.BottomSheetDialog(this);
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
		File primaryDir = new File(Environment.getExternalStorageDirectory(), "Ajoo/media/documents");
		if (!primaryDir.exists()) primaryDir.mkdirs();
		File file = new File(primaryDir, fileName);
		try {
			if (!file.exists()) file.createNewFile();
			if (file.canWrite()) return file;
		} catch (Exception ignored) {}
		
		File secondaryDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Ajoo");
		if (!secondaryDir.exists()) secondaryDir.mkdirs();
		file = new File(secondaryDir, fileName);
		try {
			if (!file.exists()) file.createNewFile();
			if (file.canWrite()) return file;
		} catch (Exception ignored) {}
		
		File fallbackDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Ajoo");
		if (!fallbackDir.exists()) fallbackDir.mkdirs();
		file = new File(fallbackDir, fileName);
		if (!file.exists()) file.createNewFile();
		return file;
	}
	
	private void _shareExportedFile(File file, String mimeType, String subject) {
		Toast.makeText(this, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
		try {
			Uri contentUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType(mimeType);
			shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(shareIntent, "Export Complete"));
		} catch (IllegalArgumentException ex) {
			StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
			StrictMode.setVmPolicy(builder.build());
			Uri contentUri = Uri.fromFile(file);
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType(mimeType);
			shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
			startActivity(Intent.createChooser(shareIntent, "Export Complete"));
		}
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
		canvas.drawText("Network: AjooChain", textX, y + 35, paint);
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
			if (tokenSymbol == null || tokenSymbol.isEmpty()) tokenSymbol = "AJC";
			
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
			.append(dataDf.format(ajcFee)).append(",") // APPLIED FIX
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
	
	
	private void _exportTransactionsToImage() {
		if (!_checkStoragePermission()) return;
		try {
			// Create a high-res canvas canvas 
			int w = 840;
			int h = 1600; 
			Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			
			// 1. Draw Receipt Background (Matching your app's Dark Theme)
			canvas.drawColor(Color.parseColor("#0B132B")); 
			
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			
			// 2. Draw Header & Logo
			Bitmap logo = _getAppLogo();
			if (logo != null) {
				// Scale logo slightly for the high-res canvas
				Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 72, 72, true);
				canvas.drawBitmap(scaledLogo, 40, 40, paint);
			}
			
			paint.setColor(Color.WHITE);
			paint.setTextSize(26f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Transaction Receipt", 130, 70, paint);
			
			paint.setColor(Color.parseColor("#8592AD"));
			paint.setTextSize(16f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
			String genDate = new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(new Date());
			canvas.drawText("AjooChain Network  ·  Generated: " + genDate, 130, 100, paint);
			
			// Header Separator Line
			paint.setColor(Color.parseColor("#1C2A4A"));
			paint.setStrokeWidth(2f);
			canvas.drawLine(40, 140, w - 40, 140, paint);
			
			int y = 220;
			
			// 3. Draw Main Value & Status
			SharedPreferences prefs = getSharedPreferences("ajc_wallet_prefs", Context.MODE_PRIVATE);
			String activeAddress = prefs.getString("active_wallet_address", "").trim();
			if (activeAddress.isEmpty()) {
				activeAddress = com.harvanna.wallet.wallet.WalletManager.getActiveWalletAddress(this);
				if (activeAddress == null) activeAddress = "";
			}
			boolean isIncoming = recipient != null && !recipient.isEmpty() && recipient.equalsIgnoreCase(activeAddress);
			
			String valuePrefix = isIncoming ? "+ " : "- ";
			String tokenSymbol = getIntent().getStringExtra("tokenSymbol");
			if (tokenSymbol == null || tokenSymbol.isEmpty()) tokenSymbol = "AJC";
			
			// APPLIED FIX
			String amountStr = valuePrefix + _formatUIBalance(value) + " " + tokenSymbol;
			
			paint.setTextAlign(Paint.Align.CENTER);
			paint.setTextSize(46f);
			paint.setColor(Color.WHITE);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText(amountStr, w / 2f, y, paint);
			
			y += 50;
			String safeStatus = (status == null) ? "UNKNOWN" : status.toUpperCase();
			paint.setTextSize(20f);
			if (safeStatus.equals("PENDING")) {
				paint.setColor(Color.parseColor("#FF9800"));
			} else if (safeStatus.equals("CONFIRMED") || safeStatus.equals("SUCCESS")) {
				paint.setColor(Color.parseColor("#4CAF50"));
			} else {
				paint.setColor(Color.parseColor("#F44336"));
			}
			canvas.drawText(safeStatus, w / 2f, y, paint);
			
			// Reset text alignment for rows
			paint.setTextAlign(Paint.Align.LEFT);
			y += 90;
			int paddingX = 50;
			
			// 4. Draw Section: Routing
			paint.setColor(Color.WHITE);
			paint.setTextSize(22f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Routing", paddingX, y, paint);
			y += 45;
			y = _drawCanvasRow(canvas, paint, "From Address", sender, paddingX, y);
			y = _drawCanvasRow(canvas, paint, "To Address", recipient, paddingX, y);
			y = _drawCanvasRow(canvas, paint, "Memo / Note", (memo != null && !memo.trim().isEmpty()) ? memo : "None", paddingX, y);
			
			y += 20;
			
			// 5. Draw Section: Network Data
			paint.setColor(Color.WHITE);
			paint.setTextSize(22f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Network Data", paddingX, y, paint);
			y += 45;
			
			// APPLIED FIX
			y = _drawCanvasRow(canvas, paint, "Network Fee", _formatUIBalance(ajcFee) + " AJC", paddingX, y);
			y = _drawCanvasRow(canvas, paint, "Amount Burned", _formatUIBalance(burnAmount) + " AJC", paddingX, y);
			
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US);
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("Africa/Lagos"));
			y = _drawCanvasRow(canvas, paint, "Timestamp", sdf.format(new Date(timestamp)), paddingX, y);
			
			y += 20;
			
			// 6. Draw Section: Blockchain Details
			paint.setColor(Color.WHITE);
			paint.setTextSize(22f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Blockchain Details", paddingX, y, paint);
			y += 45;
			y = _drawCanvasRow(canvas, paint, "Transaction Hash", txHash != null && !txHash.isEmpty() ? txHash : "N/A", paddingX, y);
			y = _drawCanvasRow(canvas, paint, "Block Number", blockNumber > 0 ? String.valueOf(blockNumber) : "Pending", paddingX, y);
			y = _drawCanvasRow(canvas, paint, "Confirmations", confirmations > 0 ? confirmations + "+" : "0 (Pending)", paddingX, y);
			y = _drawCanvasRow(canvas, paint, "Nonce", String.valueOf(nonce), paddingX, y);
			
			// 7. Crop the bitmap height precisely to where the text ended (plus bottom margin)
			int finalHeight = y + 60;
			Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, Math.min(finalHeight, h));
			
			// 8. Save & Share
			String timeStampName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File imageFile = _getSafeExportFile("receipt_" + timeStampName + ".png");
			FileOutputStream fos = new FileOutputStream(imageFile);
			croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
			
			_shareExportedFile(imageFile, "image/png", "Transaction Receipt Image");
			
		} catch (Exception e) {
			Toast.makeText(this, "Image export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void _exportTransactionsToDocx() {
		if (!_checkStoragePermission()) return;
		try {
			StringBuilder doc = new StringBuilder();
			doc.append("==================================================\n");
			doc.append("            AJOO NETWORK TRANSACTION              \n");
			doc.append("==================================================\n\n");
			
			String safeStatus = (status == null) ? "UNKNOWN" : status.toUpperCase();
			String tokenSymbol = getIntent().getStringExtra("tokenSymbol");
			if (tokenSymbol == null || tokenSymbol.isEmpty()) tokenSymbol = "AJC";
			
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
			doc.append("NETWORK FEE : ").append(_formatUIBalance(ajcFee)).append(" AJC\n");
			doc.append("BURN AMOUNT : ").append(_formatUIBalance(burnAmount)).append(" AJC\n\n");
			
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
	
	private void _exportTransactionsToPdf() {
		if (!_checkStoragePermission()) return;
		try {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File pdfFile = _getSafeExportFile("receipt_" + timeStamp + ".pdf");
			
			android.graphics.pdf.PdfDocument pdfDocument = new android.graphics.pdf.PdfDocument();
			// Standard A4 dimensions (595 x 842)
			android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
			android.graphics.pdf.PdfDocument.Page page = pdfDocument.startPage(pageInfo);
			
			Canvas canvas = page.getCanvas();
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			
			// 1. Draw Dark Background
			canvas.drawColor(Color.parseColor("#0B132B")); 
			
			// 2. Draw Header & Logo
			Bitmap logo = _getAppLogo();
			if (logo != null) {
				Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 48, 48, true);
				canvas.drawBitmap(scaledLogo, 35, 35, paint);
			}
			
			paint.setColor(Color.WHITE);
			paint.setTextSize(20f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Transaction Receipt", 100, 55, paint);
			
			paint.setColor(Color.parseColor("#8592AD"));
			paint.setTextSize(12f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
			String genDate = new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(new Date());
			canvas.drawText("AjooChain Network  ·  Generated: " + genDate, 100, 75, paint);
			
			paint.setColor(Color.parseColor("#1C2A4A"));
			paint.setStrokeWidth(1.5f);
			canvas.drawLine(35, 110, 560, 110, paint);
			
			int y = 170;
			
			// 3. Draw Main Value & Status
			SharedPreferences prefs = getSharedPreferences("ajc_wallet_prefs", Context.MODE_PRIVATE);
			String activeAddress = prefs.getString("active_wallet_address", "").trim();
			if (activeAddress.isEmpty()) {
				activeAddress = com.harvanna.wallet.wallet.WalletManager.getActiveWalletAddress(this);
				if (activeAddress == null) activeAddress = "";
			}
			boolean isIncoming = recipient != null && !recipient.isEmpty() && recipient.equalsIgnoreCase(activeAddress);
			
			String valuePrefix = isIncoming ? "+ " : "- ";
			String tokenSymbol = getIntent().getStringExtra("tokenSymbol");
			if (tokenSymbol == null || tokenSymbol.isEmpty()) tokenSymbol = "AJC";
			
			// APPLIED FIX
			String amountStr = valuePrefix + _formatUIBalance(value) + " " + tokenSymbol;
			
			paint.setTextAlign(Paint.Align.CENTER);
			paint.setTextSize(34f);
			paint.setColor(Color.WHITE);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText(amountStr, 595 / 2f, y, paint);
			
			y += 35;
			String safeStatus = (status == null) ? "UNKNOWN" : status.toUpperCase();
			paint.setTextSize(16f);
			if (safeStatus.equals("PENDING")) {
				paint.setColor(Color.parseColor("#FF9800"));
			} else if (safeStatus.equals("CONFIRMED") || safeStatus.equals("SUCCESS")) {
				paint.setColor(Color.parseColor("#4CAF50"));
			} else {
				paint.setColor(Color.parseColor("#F44336"));
			}
			canvas.drawText(safeStatus, 595 / 2f, y, paint);
			
			// Reset text alignment for rows
			paint.setTextAlign(Paint.Align.LEFT);
			y += 65;
			int paddingX = 40;
			
			// 4. Draw Section: Routing
			paint.setColor(Color.WHITE);
			paint.setTextSize(16f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Routing", paddingX, y, paint);
			y += 35;
			y = _drawPdfRow(canvas, paint, "From Address", sender, paddingX, y);
			y = _drawPdfRow(canvas, paint, "To Address", recipient, paddingX, y);
			y = _drawPdfRow(canvas, paint, "Memo / Note", (memo != null && !memo.trim().isEmpty()) ? memo : "None", paddingX, y);
			
			y += 15;
			
			// 5. Draw Section: Network Data
			paint.setColor(Color.WHITE);
			paint.setTextSize(16f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Network Data", paddingX, y, paint);
			y += 35;
			
			// APPLIED FIX
			y = _drawPdfRow(canvas, paint, "Network Fee", _formatUIBalance(ajcFee) + " AJC", paddingX, y);
			y = _drawPdfRow(canvas, paint, "Amount Burned", _formatUIBalance(burnAmount) + " AJC", paddingX, y);
			
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US);
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("Africa/Lagos"));
			y = _drawPdfRow(canvas, paint, "Timestamp", sdf.format(new Date(timestamp)), paddingX, y);
			
			y += 15;
			
			// 6. Draw Section: Blockchain Details
			paint.setColor(Color.WHITE);
			paint.setTextSize(16f);
			paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
			canvas.drawText("Blockchain Details", paddingX, y, paint);
			y += 35;
			y = _drawPdfRow(canvas, paint, "Transaction Hash", txHash != null && !txHash.isEmpty() ? txHash : "N/A", paddingX, y);
			y = _drawPdfRow(canvas, paint, "Block Number", blockNumber > 0 ? String.valueOf(blockNumber) : "Pending", paddingX, y);
			y = _drawPdfRow(canvas, paint, "Confirmations", confirmations > 0 ? confirmations + "+" : "0 (Pending)", paddingX, y);
			y = _drawPdfRow(canvas, paint, "Nonce", String.valueOf(nonce), paddingX, y);
			
			pdfDocument.finishPage(page);
			FileOutputStream fos = new FileOutputStream(pdfFile);
			pdfDocument.writeTo(fos);
			pdfDocument.close();
			fos.close();
			
			_shareExportedFile(pdfFile, "application/pdf", "Transaction Receipt PDF");
		} catch (Exception e) {
			Toast.makeText(this, "PDF export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
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
