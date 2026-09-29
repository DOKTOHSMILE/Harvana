package com.harvanna.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import com.harvanna.app.sync.HarvannaNodeClient;
import androidx.core.content.FileProvider;


public class HistoryFragmentActivity extends Fragment {
	
	private NestedScrollView vscroll2;
	private LinearLayout linear21;
	private LinearLayout linear22;
	private LinearLayout linear32;
	private TextView tv_most_recent_first;
	private RecyclerView recycler_wallet_activity;
	private TextView tv_wallet_activity_empty;
	private TextView textview16;
	private TextView textview17;
	private MaterialCardView cardview20;
	private MaterialCardView cardview19;
	private MaterialCardView cardview21;
	private LinearLayout linear33;
	private ImageView imageview17;
	private EditText et_search_transactions;
	private ImageView btn_filter_transactions;
	private ImageView btn_export_transactions;
	
	private SharedPreferences groupsp;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.history_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		vscroll2 = _view.findViewById(R.id.vscroll2);
		linear21 = _view.findViewById(R.id.linear21);
		linear22 = _view.findViewById(R.id.linear22);
		linear32 = _view.findViewById(R.id.linear32);
		tv_most_recent_first = _view.findViewById(R.id.tv_most_recent_first);
		recycler_wallet_activity = _view.findViewById(R.id.recycler_wallet_activity);
		tv_wallet_activity_empty = _view.findViewById(R.id.tv_wallet_activity_empty);
		textview16 = _view.findViewById(R.id.textview16);
		textview17 = _view.findViewById(R.id.textview17);
		cardview20 = _view.findViewById(R.id.cardview20);
		cardview19 = _view.findViewById(R.id.cardview19);
		cardview21 = _view.findViewById(R.id.cardview21);
		linear33 = _view.findViewById(R.id.linear33);
		imageview17 = _view.findViewById(R.id.imageview17);
		et_search_transactions = _view.findViewById(R.id.et_search_transactions);
		btn_filter_transactions = _view.findViewById(R.id.btn_filter_transactions);
		btn_export_transactions = _view.findViewById(R.id.btn_export_transactions);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		
		et_search_transactions.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				currentSearchQuery = _charSeq.toString().trim().toLowerCase(Locale.US);
				_applyFilters();
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
	}
	
	private void initializeLogic() {
		groupId = getArguments() != null ? getArguments().getString("groupId", "") : "";
		if (groupId.isEmpty() || getContext() == null) return;
		_loadGroupWalletAddress();
		btn_filter_transactions.setOnClickListener(v -> _showFilterMenu());
		btn_export_transactions.setOnClickListener(v -> _showExportOptionsBottomSheet());
		
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (!groupId.isEmpty() && !groupWalletAddress.isEmpty()) {
			_loadWalletActivity();
		}
	}
	

	public void _MORE() {
	}
	private String groupId = "";
	private String groupWalletAddress = "";
	private String groupName = "Harvanna Group";
	private String groupType = "Rotating";
	private String groupLocation = "";
	private List<HashMap<String, Object>> allTransactions = new ArrayList<>();
	private List<HashMap<String, Object>> displayTransactions = new ArrayList<>();
	private WalletActivityAdapter adapter;
	private String currentSearchQuery = "";
	private String currentFilter = "ALL";
	
	
	private String _extractLocation(Map<String, Object> data) {
		String city = str(data, "city", "");
		String state = str(data, "state", "");
		String country = str(data, "country", "");
		String loc = "";
		
		if (!city.isEmpty()) loc += city;
		if (!state.isEmpty()) loc += (loc.isEmpty() ? "" : ", ") + state;
		if (!country.isEmpty()) loc += (loc.isEmpty() ? "" : ", ") + country;
		
		if (loc.isEmpty()) {
			loc = str(data, "location", str(data, "groupLocation", "Unknown"));
		}
		return loc.isEmpty() ? "Unknown" : loc;
	}
	
	private void _loadGroupWalletAddress() {
		Map<String, Object> cached = com.harvanna.app.offline.OfflineCacheManager.load(
		getContext(), "group_" + groupId);
		if (cached != null) {
			Object w = cached.get("groupWalletAddress");
			if (w == null) w = cached.get("group_wallet");
			if (w != null) groupWalletAddress = w.toString();
			
			groupName = str(cached, "groupName", str(cached, "name", "Harvanna Group"));
			groupType = str(cached, "groupType", "");
			groupLocation = _extractLocation(cached);
			
			if (groupWalletAddress != null && groupWalletAddress.startsWith("HV0x")) {
				_paintCachedHistory();
				_loadWalletActivity();
			}
		}
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getGroup(groupId, new com.harvanna.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				com.harvanna.app.offline.OfflineCacheManager.save(getContext(), "group_" + groupId, data);
				Object walletObj = data.get("groupWalletAddress");
				groupWalletAddress = walletObj != null ? walletObj.toString() : "";
				
				groupName = str(data, "groupName", str(data, "name", "Harvanna Group"));
				groupType = str(data, "groupType", "");
				groupLocation = _extractLocation(data);
				
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> _loadWalletActivity());
			}
			@Override public void onError(String error) {}
		});
	}
	
	private void _loadWalletActivity() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		com.harvanna.app.group.MemberNameResolver.preloadGroupWallets(getContext(), groupId, () -> {
			if (getActivity() == null) return;
			getActivity().runOnUiThread(this::_fetchAndBindWalletActivity);
		});
	}
	
	private void _paintCachedHistory() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		List<HashMap<String, Object>> cached = com.harvanna.app.offline.OfflineCacheManager.loadList(
		getContext(), "wallet_history_" + groupWalletAddress);
		
		if (cached != null && !cached.isEmpty()) {
			allTransactions.clear();
			allTransactions.addAll(cached);
			_applyFilters();
		}
	}
	
	private void _fetchAndBindWalletActivity() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		_paintCachedHistory();
		HarvannaNodeClient.getHistory(groupWalletAddress, new HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				ArrayList<HashMap<String, Object>> rows = new ArrayList<>();
				org.json.JSONArray arr = json.optJSONArray("transactions");
				if (arr != null) {
					for (int i = 0; i < arr.length(); i++) {
						org.json.JSONObject tx = arr.optJSONObject(i);
						if (tx == null) continue;
						HashMap<String, Object> row = new HashMap<>();
						row.put("txHash", tx.optString("txHash", ""));
						row.put("from", tx.optString("from", ""));
						row.put("to", tx.optString("to", ""));
						row.put("amount", tx.optLong("amount", 0L));
						row.put("memo", tx.optString("memo", ""));
						row.put("timestamp", tx.optLong("timestamp", 0L));
						row.put("confirmations", tx.optLong("confirmations", 0L));
						rows.add(row);
					}
				}
				if (getActivity() == null) return;
				com.harvanna.app.offline.OfflineCacheManager.saveList(
				getContext(), "wallet_history_" + groupWalletAddress, rows);
				
				getActivity().runOnUiThread(() -> {
					allTransactions.clear();
					allTransactions.addAll(rows);
					_applyFilters();
				});
			}
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (allTransactions.isEmpty()) {
						if (tv_wallet_activity_empty != null) {
							tv_wallet_activity_empty.setVisibility(View.VISIBLE);
							tv_wallet_activity_empty.setText("Couldn't load wallet activity. Please check your connection.");
						}
					}
				});
			}
		});
	}
	
	private void _applyFilters() {
		if (currentFilter == null) currentFilter = "ALL";
		if (currentSearchQuery == null) currentSearchQuery = "";
		
		displayTransactions.clear();
		for (HashMap<String, Object> tx : allTransactions) {
			String from = str(tx, "from", "");
			String to = str(tx, "to", "");
			String txHash = str(tx, "txHash", "").toLowerCase(Locale.US);
			String memo = str(tx, "memo", "").toLowerCase(Locale.US);
			
			boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
			String counterparty = isOut ? to : from;
			
			boolean matchesFilter = currentFilter.equals("ALL") || 
			(currentFilter.equals("IN") && !isOut) || 
			(currentFilter.equals("OUT") && isOut);
			
			boolean matchesSearch = currentSearchQuery.isEmpty() || 
			txHash.contains(currentSearchQuery) || 
			counterparty.toLowerCase(Locale.US).contains(currentSearchQuery) || 
			memo.contains(currentSearchQuery);
			
			if (matchesFilter && matchesSearch) {
				displayTransactions.add(tx);
			}
		}
		
		if (adapter == null) {
			adapter = new WalletActivityAdapter(displayTransactions);
			if (recycler_wallet_activity != null) {
				if (recycler_wallet_activity.getLayoutManager() == null) {
					recycler_wallet_activity.setLayoutManager(new LinearLayoutManager(getContext()));
				}
				recycler_wallet_activity.setAdapter(adapter);
			}
		}
		adapter.notifyDataSetChanged();
		
		if (tv_wallet_activity_empty != null) {
			tv_wallet_activity_empty.setVisibility(displayTransactions.isEmpty() ? View.VISIBLE : View.GONE);
			if (displayTransactions.isEmpty() && !allTransactions.isEmpty()) {
				tv_wallet_activity_empty.setText("No transactions match your search.");
			} else if (displayTransactions.isEmpty()) {
				tv_wallet_activity_empty.setText("No wallet activity yet.");
			}
		}
	}
	
	private void _showFilterMenu() {
		if (getContext() == null) return;
		PopupMenu popup = new PopupMenu(getContext(), btn_filter_transactions);
		popup.getMenu().add(0, 0, 0, "All Transactions");
		popup.getMenu().add(0, 1, 0, "Received (In)");
		popup.getMenu().add(0, 2, 0, "Sent (Out)");
		
		popup.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
				case 0: currentFilter = "ALL"; break;
				case 1: currentFilter = "IN"; break;
				case 2: currentFilter = "OUT"; break;
			}
			_applyFilters();
			return true;
		});
		popup.show();
	}
	
	private void _showExportOptionsBottomSheet() {
		if (getContext() == null) return;
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
		new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
		View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_export_options, null);
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
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
			return true;
		}
		if (androidx.core.content.ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
			androidx.core.app.ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
			SketchwareUtil.showMessage(getContext(), "Permission requested. Please grant and click export again.");
			return false;
		}
		return true;
	}
	
	private File _getSafeExportFile(String fileName) throws IOException {
		File primaryDir = new File(Environment.getExternalStorageDirectory(), "Harvanna/media/documents");
		if (!primaryDir.exists()) primaryDir.mkdirs();
		File file = new File(primaryDir, fileName);
		try {
			if (!file.exists()) file.createNewFile();
			if (file.canWrite()) return file;
		} catch (Exception ignored) {}
		
		File secondaryDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Harvanna");
		if (!secondaryDir.exists()) secondaryDir.mkdirs();
		file = new File(secondaryDir, fileName);
		try {
			if (!file.exists()) file.createNewFile();
			if (file.canWrite()) return file;
		} catch (Exception ignored) {}
		
		File fallbackDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Harvanna");
		if (!fallbackDir.exists()) fallbackDir.mkdirs();
		file = new File(fallbackDir, fileName);
		if (!file.exists()) file.createNewFile();
		return file;
	}
	
	private void _shareExportedFile(File file, String mimeType, String subject) {
		SketchwareUtil.showMessage(getContext(), "Saved to " + file.getAbsolutePath());
		try {
			Uri contentUri = androidx.core.content.FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType(mimeType);
			shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
			shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(shareIntent, "Export Complete"));
		} catch (IllegalArgumentException ex) {
			android.os.StrictMode.VmPolicy.Builder builder = new android.os.StrictMode.VmPolicy.Builder();
			android.os.StrictMode.setVmPolicy(builder.build());
			Uri contentUri = Uri.fromFile(file);
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType(mimeType);
			shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
			startActivity(Intent.createChooser(shareIntent, "Export Complete"));
		}
	}
	
	private Bitmap _getAppLogo() {
		try {
			int resId = getResources().getIdentifier("app_icon", "drawable", getContext().getPackageName());
			if (resId == 0) resId = getResources().getIdentifier("ic_launcher", "mipmap", getContext().getPackageName());
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
		canvas.drawText(groupName, textX, y + 15, paint);
		
		paint.setTextSize(11f);
		paint.setFakeBoldText(false);
		paint.setColor(Color.DKGRAY);
		canvas.drawText("Group Type: " + groupType + (groupLocation.isEmpty() ? "" : "  |  Location: " + groupLocation), textX, y + 35, paint);
		canvas.drawText("OFFICIAL TRANSACTION STATEMENT  ·  Generated: " + genDate, textX, y + 55, paint);
		
		paint.setColor(Color.LTGRAY);
		paint.setStrokeWidth(1f);
		canvas.drawLine(40, y + 75, 555, y + 75, paint);
	}
	
	private void _exportTransactionsToCSV() {
		if (displayTransactions.isEmpty() || getContext() == null) {
			SketchwareUtil.showMessage(getContext(), "No transactions to export");
			return;
		}
		if (!_checkStoragePermission()) return;
		
		try {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File csvFile = _getSafeExportFile("transactions_" + timeStamp + ".csv");
			FileWriter writer = new FileWriter(csvFile);
			
			writer.append("Group Name: ").append(groupName).append("\n");
			writer.append("Group Type: ").append(groupType).append("\n");
			writer.append("Location: ").append(groupLocation).append("\n\n");
			writer.append(("Date,TxHash,Type,Amount(" + com.harvanna.app.CoinInfo.ticker() + "),Counterparty,Memo,Status\n"));
			
			// APPLIED FIX: Data layer formatter for CSV (NO COMMAS)
			java.text.DecimalFormat dataDf = new java.text.DecimalFormat("0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			
			for (HashMap<String, Object> tx : displayTransactions) {
				long ts = (long) num(tx, "timestamp");
				String date = ts > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date(ts)) : "Pending";
				String hash = str(tx, "txHash", "");
				String from = str(tx, "from", "");
				String to = str(tx, "to", "");
				boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
				String type = isOut ? "Sent" : "Received";
				String counterparty = isOut ? to : from;
				double amt = HarvannaNodeClient.toHarvDisplay((long) num(tx, "amount"));
				String memo = str(tx, "memo", "").replace(",", " ");
				long conf = (long) num(tx, "confirmations");
				String status = conf > 0 ? "Confirmed" : "Pending";
				
				writer.append(date).append(",")
				.append(hash).append(",")
				.append(type).append(",")
				.append(dataDf.format(amt)).append(",") // APPLIED FIX
				.append(counterparty).append(",")
				.append(memo).append(",")
				.append(status).append("\n");
			}
			writer.flush();
			writer.close();
			_shareExportedFile(csvFile, "text/csv", "Group Transaction History");
			
		} catch (Exception e) {
			SketchwareUtil.showMessage(getContext(), "Export failed: " + e.getMessage());
		}
	}
	
	private void _exportTransactionsToImage() {
		if (displayTransactions.isEmpty() || getContext() == null) {
			SketchwareUtil.showMessage(getContext(), "No transactions to export");
			return;
		}
		if (!_checkStoragePermission()) return;
		
		try {
			int limit = Math.min(displayTransactions.size(), 200);
			int headerHeight = 150;
			int rowHeight = 70;
			int totalHeight = headerHeight + (limit * rowHeight) + 40; 
			int width = 595; 
			
			Bitmap bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);
			Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			
			_drawDocumentHeader(canvas, paint, _getAppLogo());
			
			int y = headerHeight + 20;
			
			for (int i = 0; i < limit; i++) {
				HashMap<String, Object> tx = displayTransactions.get(i);
				
				long ts = (long) num(tx, "timestamp");
				String date = ts > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date(ts)) : "Pending";
				String hash = str(tx, "txHash", "");
				String from = str(tx, "from", "");
				String to = str(tx, "to", "");
				boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
				String type = isOut ? "Sent" : "Received";
				String counterparty = isOut ? to : from;
				double amt = HarvannaNodeClient.toHarvDisplay((long) num(tx, "amount"));
				String memo = str(tx, "memo", "");
				String status = ((long) num(tx, "confirmations") > 0) ? "Confirmed" : "Pending";
				
				paint.setColor(Color.BLACK);
				paint.setTextSize(11f);
				paint.setFakeBoldText(true);
				// APPLIED FIX
				canvas.drawText(date + "  |  " + type + "  |  " + _formatUIBalance(amt) + (" " + com.harvanna.app.CoinInfo.ticker() + "  |  ") + status, 40, y, paint);
				
				paint.setFakeBoldText(false);
				paint.setColor(Color.DKGRAY);
				paint.setTextSize(10f);
				y += 15;
				canvas.drawText("Wallet: " + counterparty, 40, y, paint);
				y += 15;
				canvas.drawText("TxHash: " + hash, 40, y, paint);
				
				if (!memo.isEmpty()) {
					y += 15;
					canvas.drawText("Memo: " + memo, 40, y, paint);
				}
				
				y += 15;
				paint.setColor(Color.parseColor("#EEEEEE"));
				canvas.drawLine(40, y, 555, y, paint);
				y += 10;
			}
			
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File imageFile = _getSafeExportFile("transactions_" + timeStamp + ".png");
			FileOutputStream fos = new FileOutputStream(imageFile);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
			
			_shareExportedFile(imageFile, "image/png", "Group Transaction Image");
			
		} catch (OutOfMemoryError oom) {
			SketchwareUtil.showMessage(getContext(), "List is too long for image. Try PDF.");
		} catch (Exception e) {
			SketchwareUtil.showMessage(getContext(), "Image export failed: " + e.getMessage());
		}
	}
	
	private void _exportTransactionsToDocx() {
		if (displayTransactions.isEmpty() || getContext() == null) {
			SketchwareUtil.showMessage(getContext(), "No transactions to export");
			return;
		}
		if (!_checkStoragePermission()) return;
		
		try {
			StringBuilder docContent = new StringBuilder("HARVANNA NETWORK - OFFICIAL TRANSACTION STATEMENT\n\n");
			docContent.append("Group Name: ").append(groupName).append("\n");
			docContent.append("Group Type: ").append(groupType).append("\n");
			docContent.append("Location: ").append(groupLocation).append("\n");
			docContent.append("Generated on: ").append(new Date().toString()).append("\n-----------------------------------\n\n");
			
			for (HashMap<String, Object> tx : displayTransactions) {
				long ts = (long) num(tx, "timestamp");
				String date = ts > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date(ts)) : "Pending";
				String hash = str(tx, "txHash", "");
				String from = str(tx, "from", "");
				String to = str(tx, "to", "");
				boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
				String type = isOut ? "Sent" : "Received";
				String counterparty = isOut ? to : from;
				double amt = HarvannaNodeClient.toHarvDisplay((long) num(tx, "amount"));
				String memo = str(tx, "memo", "");
				String status = ((long) num(tx, "confirmations") > 0) ? "Confirmed" : "Pending";
				
				// APPLIED FIX
				docContent.append("• ").append(date).append(" | ").append(type).append(" | ").append(_formatUIBalance(amt)).append((" " + com.harvanna.app.CoinInfo.ticker() + " | ")).append(status).append("\n");
				docContent.append("    Wallet: ").append(counterparty).append("\n");
				docContent.append("    TxHash: ").append(hash).append("\n");
				if (!memo.isEmpty()) {
					docContent.append("    Memo: \"").append(memo).append("\"\n");
				}
				docContent.append("\n");
			}
			
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File docFile = _getSafeExportFile("transactions_" + timeStamp + ".docx");
			FileWriter writer = new FileWriter(docFile);
			writer.append(docContent.toString());
			writer.flush();
			writer.close();
			
			_shareExportedFile(docFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Group Transaction DOCX");
		} catch (Exception e) {
			SketchwareUtil.showMessage(getContext(), "DOCX export failed: " + e.getMessage());
		}
	}
	
	private void _exportTransactionsToPdf() {
		if (displayTransactions.isEmpty() || getContext() == null) {
			SketchwareUtil.showMessage(getContext(), "No transactions to export");
			return;
		}
		if (!_checkStoragePermission()) return;
		
		try {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File pdfFile = _getSafeExportFile("transactions_" + timeStamp + ".pdf");
			
			android.graphics.pdf.PdfDocument pdfDocument = new android.graphics.pdf.PdfDocument();
			int pageNumber = 1;
			android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
			android.graphics.pdf.PdfDocument.Page page = pdfDocument.startPage(pageInfo);
			
			android.graphics.Canvas canvas = page.getCanvas();
			android.graphics.Paint paint = new android.graphics.Paint(Paint.ANTI_ALIAS_FLAG);
			Bitmap logo = _getAppLogo();
			
			_drawDocumentHeader(canvas, paint, logo);
			int y = 140; 
			
			for (HashMap<String, Object> tx : displayTransactions) {
				if (y > 750) {
					pdfDocument.finishPage(page);
					pageNumber++;
					pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
					page = pdfDocument.startPage(pageInfo);
					canvas = page.getCanvas();
					_drawDocumentHeader(canvas, paint, logo);
					y = 140;
				}
				
				long ts = (long) num(tx, "timestamp");
				String date = ts > 0 ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date(ts)) : "Pending";
				String hash = str(tx, "txHash", "");
				String from = str(tx, "from", "");
				String to = str(tx, "to", "");
				boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
				String type = isOut ? "Sent" : "Received";
				String counterparty = isOut ? to : from;
				double amt = HarvannaNodeClient.toHarvDisplay((long) num(tx, "amount"));
				String memo = str(tx, "memo", "");
				String status = ((long) num(tx, "confirmations") > 0) ? "Confirmed" : "Pending";
				
				paint.setColor(Color.BLACK);
				paint.setTextSize(11f);
				paint.setFakeBoldText(true);
				// APPLIED FIX
				canvas.drawText(date + "  |  " + type + "  |  " + _formatUIBalance(amt) + (" " + com.harvanna.app.CoinInfo.ticker() + "  |  ") + status, 40, y, paint);
				
				paint.setFakeBoldText(false);
				paint.setColor(Color.DKGRAY);
				paint.setTextSize(10f);
				y += 15;
				canvas.drawText("Wallet: " + counterparty, 40, y, paint);
				y += 15;
				canvas.drawText("TxHash: " + hash, 40, y, paint);
				
				if (!memo.isEmpty()) {
					y += 15;
					canvas.drawText("Memo: " + memo, 40, y, paint);
				}
				
				y += 15;
				paint.setColor(Color.parseColor("#EEEEEE"));
				canvas.drawLine(40, y, 555, y, paint);
				y += 10;
			}
			
			pdfDocument.finishPage(page);
			FileOutputStream fos = new FileOutputStream(pdfFile);
			pdfDocument.writeTo(fos);
			pdfDocument.close();
			fos.close();
			
			_shareExportedFile(pdfFile, "application/pdf", "Group Transaction PDF");
		} catch (Exception e) {
			SketchwareUtil.showMessage(getContext(), "PDF export failed: " + e.getMessage());
		}
	}
	
	private class WalletActivityAdapter extends RecyclerView.Adapter<WalletActivityAdapter.VH> {
		private final List<HashMap<String, Object>> data;
		WalletActivityAdapter(List<HashMap<String, Object>> data) { this.data = data; }
		
		@NonNull @Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			int lid = getResources().getIdentifier("payment_row_card", "layout", getContext().getPackageName());
			View v = lid != 0
			? LayoutInflater.from(parent.getContext()).inflate(lid, parent, false)
			: LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
			return new VH(v);
		}
		
		@Override 
		public void onBindViewHolder(@NonNull VH h, int pos) {
			HashMap<String, Object> tx = data.get(pos);
			
			// Extract core transaction details
			String from = str(tx, "from", "");
			String to = str(tx, "to", "");
			boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
			String counterparty = isOut ? to : from;
			double amountHarv = HarvannaNodeClient.toHarvDisplay((long) num(tx, "amount"));
			long ts = (long) num(tx, "timestamp");
			long confirmations = (long) num(tx, "confirmations");
			String memo = str(tx, "memo", "");
			String txHash = str(tx, "txHash", "");
			boolean confirmed = confirmations >= 1;
			String status = confirmed ? "Confirmed" : "Pending";
			
			String time = ts > 0 ? new SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(new java.util.Date(ts)) : "—";
			String shortAddr = counterparty.length() > 12
			? counterparty.substring(0, 8) + "…" + counterparty.substring(counterparty.length() - 4)
			: counterparty;
			
			// Bind UI elements
			if (h.name != null) {
				String prefix = isOut ? "Sent out · " : "Received · ";
				// APPLIED FIX
				String amountText = _formatUIBalance(amountHarv);
				h.name.setText(prefix + amountText + (" " + com.harvanna.app.CoinInfo.ticker()));
				h.name.setTextColor(isOut ? 0xFFF44336 : 0xFF4CAF50);
			}
			
			if (h.meta != null) {
				h.meta.setTag(counterparty);
				com.harvanna.app.group.MemberNameResolver.resolveWallet(getContext(), groupId, counterparty, shortAddr, resolvedLabel -> {
					if (!counterparty.equals(h.meta.getTag())) return;
					String line = resolvedLabel + " · " + time;
					if (!memo.isEmpty()) line += " · \"" + memo + "\"";
					if (!confirmed) line += " · pending";
					h.meta.setText(line);
				});
			}
			
			if (h.amount != null) h.amount.setVisibility(View.GONE);
			
			if (h.status != null) {
				h.status.setText(status);
				h.status.setBackgroundResource(confirmed
				? getResources().getIdentifier("bg_chip_confirmed", "drawable", getContext().getPackageName())
				: getResources().getIdentifier("bg_chip_pending", "drawable", getContext().getPackageName()));
			}
			
			// Attach Click Listener to launch TransactionDetailActivity
			h.itemView.setOnClickListener(v -> {
				Intent intent = new Intent(v.getContext(), TransactionDetailActivity.class);
				
				intent.putExtra("txHash", txHash);
				intent.putExtra("status", status);
				intent.putExtra("senderAddress", from);
				intent.putExtra("recipientAddress", to);
				intent.putExtra("tokenSymbol", com.harvanna.app.CoinInfo.ticker());
				intent.putExtra("value", amountHarv);
				intent.putExtra("timestamp", ts);
				intent.putExtra("memo", memo);
				intent.putExtra("confirmations", confirmations);
				
				// Pass optional node telemetry if available in the HashMap
				intent.putExtra("harvFee", num(tx, "fee")); 
				intent.putExtra("blockNumber", (long) num(tx, "blockHeight"));
				intent.putExtra("nonce", (long) num(tx, "nonce"));
				
				v.getContext().startActivity(intent);
			});
		}
		
		@Override public int getItemCount() { return data.size(); }
		
		class VH extends RecyclerView.ViewHolder {
			TextView name, meta, amount, status;
			VH(View v) {
				super(v);
				int n = v.getResources().getIdentifier("tv_pay_name", "id", v.getContext().getPackageName());
				int m = v.getResources().getIdentifier("tv_pay_meta", "id", v.getContext().getPackageName());
				int a = v.getResources().getIdentifier("tv_pay_amount", "id", v.getContext().getPackageName());
				int s = v.getResources().getIdentifier("tv_pay_status", "id", v.getContext().getPackageName());
				if (n != 0) name = v.findViewById(n);
				if (m != 0) meta = v.findViewById(m);
				if (a != 0) amount = v.findViewById(a);
				if (s != 0) status = v.findViewById(s);
				if (name == null) name = v.findViewById(android.R.id.text1);
				if (meta == null) meta = v.findViewById(android.R.id.text2);
			}
		}
	}
	
	private static String str(Map<String, Object> m, String k, String d) {
		Object v = m.get(k);
		if (v == null) return d;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
	}
	
	private static double num(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
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
