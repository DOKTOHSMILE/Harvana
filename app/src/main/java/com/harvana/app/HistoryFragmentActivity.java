package com.harvana.app;

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
import com.harvana.app.sync.HarvanaNodeClient;
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
		com.harvana.app.utils.Shimmer.hold(recycler_wallet_activity, com.harvana.app.utils.Shimmer.rows(recycler_wallet_activity.getContext(), 6), 4000, "HistoryFragmentActivity");
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
		if (groupId.isEmpty()) return;
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) {
			_loadGroupWalletAddress();
		} else {
			_loadWalletActivity();
		}
	}

	@Override
	public void onPause() {
		pendingPoll.removeCallbacks(pollPending);
		super.onPause();
	}
	

	public void _MORE() {
	}
	private String groupId = "";
	private String groupWalletAddress = "";
	private String groupName = "Harvana Group";
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
		Map<String, Object> cached = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "group_" + groupId);
		if (cached != null) {
			_rememberProfile(cached);
			if (groupWalletAddress != null && groupWalletAddress.startsWith("HV0x")) {
				_paintCachedHistory();
				_loadWalletActivity();
			}
			String nodeId = com.harvana.app.group.GroupKinds.nodeId(cached);
			if (!nodeId.isEmpty()) _resolveEscrow(nodeId);
		}
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getGroup(groupId, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				if ((data.get("groupWalletAddress") == null || !String.valueOf(data.get("groupWalletAddress")).startsWith("HV0x"))
						&& groupWalletAddress != null && groupWalletAddress.startsWith("HV0x")) {
					data.put("groupWalletAddress", groupWalletAddress);
				}
				com.harvana.app.offline.OfflineCacheManager.save(getContext(), "group_" + groupId, data);
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					_rememberProfile(data);
					String nodeId = com.harvana.app.group.GroupKinds.nodeId(data);
					if (!nodeId.isEmpty()) _resolveEscrow(nodeId);
					_loadWalletActivity();
				});
			}
			@Override public void onError(String error) {}
		});
	}

	/** Firestore is the profile. The node escrow is the wallet the history must read. */
	private void _rememberProfile(Map<String, Object> data) {
		if (data == null) return;
		Object w = data.get("groupWalletAddress");
		if (w == null) w = data.get("group_wallet");
		if (w == null) w = data.get("escrowAddress");
		String wallet = w == null ? "" : w.toString().trim();
		if (wallet.startsWith("HV0x") && (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x"))) {
			groupWalletAddress = wallet;
		}
		groupName = str(data, "groupName", str(data, "name", "Harvana Group"));
		groupType = str(data, "groupType", "");
		groupLocation = _extractLocation(data);
	}

	private String escrowAsked = "";

	private void _resolveEscrow(String nodeId) {
		if (nodeId == null || nodeId.isEmpty() || nodeId.equals(escrowAsked)) return;
		escrowAsked = nodeId;
		com.harvana.app.sync.NodeGroupClient.Callback cb = new com.harvana.app.sync.NodeGroupClient.Callback() {
			@Override public void onSuccess(org.json.JSONObject json) {
				String escrow = json.optString("escrowAddress", "");
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> _useEscrow(nodeId, escrow));
			}
			@Override public void onError(String error) { escrowAsked = ""; }
		};
		String family = com.harvana.app.sync.NodeGroupClient.familyOf(nodeId);
		if ("CIRCLE".equals(family)) com.harvana.app.sync.NodeGroupClient.getCircle(nodeId, cb);
		else if ("ACCUM".equals(family)) com.harvana.app.sync.NodeGroupClient.getAccumGroup(nodeId, cb);
		else if ("FIXED".equals(family)) com.harvana.app.sync.NodeGroupClient.getFixedGroup(nodeId, cb);
		else escrowAsked = "";
	}

	private void _useEscrow(String nodeId, String escrow) {
		if (escrow == null || !escrow.startsWith("HV0x")) {
			_loadWalletActivity();
			return;
		}
		boolean changed = groupWalletAddress == null || !escrow.equalsIgnoreCase(groupWalletAddress);
		groupWalletAddress = escrow;
		if (changed) {
			allTransactions.clear();
			shownSig = "";
			loaded = false;
			_paintCachedHistory();
		}
		_loadWalletActivity();
		if (getContext() == null) return;
		Map<String, Object> cached = com.harvana.app.offline.OfflineCacheManager.load(getContext(), "group_" + groupId);
		Object had = cached == null ? null : cached.get("groupWalletAddress");
		if (had != null && escrow.equalsIgnoreCase(String.valueOf(had))) return;
		if (cached == null) cached = new HashMap<>();
		cached.put("groupWalletAddress", escrow);
		cached.put("nodeGroupId", nodeId);
		com.harvana.app.offline.OfflineCacheManager.save(getContext(), "group_" + groupId, cached);
		if (had == null || !String.valueOf(had).startsWith("HV0x")) {
			Map<String, Object> patch = new HashMap<>();
			patch.put("groupWalletAddress", escrow);
			com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
				.updateGroupFields(groupId, patch, new com.harvana.app.firestore.GroupFirestoreManager.SimpleCallback() {
					@Override public void onSuccess() {}
					@Override public void onError(String error) {}
				});
		}
	}
	
	private void _loadWalletActivity() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		com.harvana.app.group.MemberNameResolver.preloadGroupWallets(getContext(), groupId, () -> {
			if (getActivity() == null) return;
			getActivity().runOnUiThread(this::_fetchAndBindWalletActivity);
		});
	}
	
	private void _paintCachedHistory() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		List<HashMap<String, Object>> cached = com.harvana.app.offline.OfflineCacheManager.loadList(
		getContext(), "wallet_history_" + groupWalletAddress);
		
		if (cached != null && !cached.isEmpty()) {
			allTransactions.clear();
			allTransactions.addAll(cached);
			_applyFilters();
		}
	}
	
	/** One fetch at a time; the cached list paints only while nothing is on screen yet (never over fresher rows). */
	private boolean fetching, loaded;
	private String shownSig = "";
	private static final int HISTORY_PAGE = 100;
	private static final int HISTORY_PAGES = 5;
	private final android.os.Handler pendingPoll = new android.os.Handler(android.os.Looper.getMainLooper());
	private final Runnable pollPending = () -> {
		if (!isAdded()) return;
		_loadWalletActivity();
	};

	private void _fetchAndBindWalletActivity() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		if (allTransactions.isEmpty()) _paintCachedHistory();
		if (fetching) return;
		fetching = true;
		final String wallet = groupWalletAddress;
		final ArrayList<HashMap<String, Object>> confirmed = new ArrayList<>();
		final ArrayList<HashMap<String, Object>> pending = new ArrayList<>();
		final boolean[] histOk = {false};
		final boolean[] pendOk = {false};
		final int[] left = {2};
		Runnable finish = () -> {
			if (--left[0] > 0) return;
			if (!isAdded()) { fetching = false; return; }
			fetching = false;
			loaded = true;
			if (!histOk[0] && !pendOk[0]) {
				if (allTransactions.isEmpty() && tv_wallet_activity_empty != null) {
					tv_wallet_activity_empty.setVisibility(View.VISIBLE);
					tv_wallet_activity_empty.setText("Couldn't load wallet activity. Please check your connection.");
				}
				return;
			}
			ArrayList<HashMap<String, Object>> rows = _mergeHistory(histOk[0] ? confirmed : new ArrayList<>(allTransactions), pendOk[0] ? pending : java.util.Collections.emptyList());
			com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "wallet_history_" + wallet, rows);
			if (rows.toString().equals(shownSig) && !allTransactions.isEmpty()) {
				_schedulePendingPoll();
				return;
			}
			shownSig = rows.toString();
			allTransactions.clear();
			allTransactions.addAll(rows);
			_applyFilters();
			_schedulePendingPoll();
		};
		_fetchHistoryPages(wallet, 0, confirmed, () -> { histOk[0] = true; finish.run(); }, () -> finish.run());
		HarvanaNodeClient.getPending(wallet, new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(org.json.JSONObject json) {
				org.json.JSONArray arr = json.optJSONArray("pending");
				if (arr != null) {
					for (int i = 0; i < arr.length(); i++) {
						org.json.JSONObject tx = arr.optJSONObject(i);
						HashMap<String, Object> row = _rowFromTx(tx, true);
						if (row != null) pending.add(row);
					}
				}
				pendOk[0] = true;
				finish.run();
			}
			@Override public void onError(String error) { finish.run(); }
		});
	}

	/** Confirmed pages, newest first, then stop. A later page failing keeps what already arrived. */
	private void _fetchHistoryPages(String wallet, int page, ArrayList<HashMap<String, Object>> into, Runnable ok, Runnable err) {
		HarvanaNodeClient.getHistory(wallet, page, HISTORY_PAGE, new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(org.json.JSONObject json) {
				org.json.JSONArray arr = json.optJSONArray("transactions");
				int n = arr == null ? 0 : arr.length();
				for (int i = 0; i < n; i++) {
					HashMap<String, Object> row = _rowFromTx(arr.optJSONObject(i), false);
					if (row != null) into.add(row);
				}
				if (n >= HISTORY_PAGE && page + 1 < HISTORY_PAGES) _fetchHistoryPages(wallet, page + 1, into, ok, err);
				else ok.run();
			}
			@Override public void onError(String error) { if (page == 0) err.run(); else ok.run(); }
		});
	}

	private static HashMap<String, Object> _rowFromTx(org.json.JSONObject tx, boolean pending) {
		if (tx == null) return null;
		String hash = tx.optString("txHash", "");
		if (hash.isEmpty()) return null;
		HashMap<String, Object> row = new HashMap<>();
		row.put("txHash", hash);
		row.put("from", tx.optString("from", ""));
		row.put("to", tx.optString("to", ""));
		row.put("amount", tx.optLong("amount", 0L));
		row.put("memo", tx.optString("memo", ""));
		long ts = tx.optLong("timestamp", tx.optLong("receivedAt", 0L));
		row.put("timestamp", ts);
		row.put("confirmations", pending ? 0L : tx.optLong("confirmations", 0L));
		row.put("fee", tx.optLong("gas", tx.optLong("fee", 0L)));
		row.put("asset", tx.optString("asset", tx.optString("mintId", "")));
		if (tx.has("coinFeeRaw")) row.put("coinFeeRaw", tx.optLong("coinFeeRaw", 0L));
		if (tx.has("nonce")) row.put("nonce", tx.optLong("nonce", 0L));
		if (tx.has("blockHeight")) row.put("blockHeight", tx.optLong("blockHeight", 0L));
		if (tx.has("initiatedAt")) row.put("initiatedAt", tx.optLong("initiatedAt", 0L));
		else if (pending && ts > 0) row.put("initiatedAt", ts);
		if (tx.has("confirmedAt")) row.put("confirmedAt", tx.optLong("confirmedAt", 0L));
		return row;
	}

	/** A confirmed row wins over the same hash still sitting in the mempool. Newest first. */
	private ArrayList<HashMap<String, Object>> _mergeHistory(List<HashMap<String, Object>> confirmed, List<HashMap<String, Object>> pending) {
		java.util.LinkedHashMap<String, HashMap<String, Object>> byHash = new java.util.LinkedHashMap<>();
		ArrayList<HashMap<String, Object>> noHash = new ArrayList<>();
		if (pending != null) {
			for (HashMap<String, Object> tx : pending) _putRow(byHash, noHash, tx);
		}
		if (confirmed != null) {
			for (HashMap<String, Object> tx : confirmed) _putRow(byHash, noHash, tx);
		}
		ArrayList<HashMap<String, Object>> rows = new ArrayList<>(byHash.values());
		rows.addAll(noHash);
		java.util.Collections.sort(rows, (a, b) -> Long.compare(_txMillis(b), _txMillis(a)));
		return rows;
	}

	private static void _putRow(java.util.LinkedHashMap<String, HashMap<String, Object>> byHash, ArrayList<HashMap<String, Object>> noHash, HashMap<String, Object> tx) {
		if (tx == null) return;
		String hash = str(tx, "txHash", "");
		if (hash.isEmpty()) { noHash.add(tx); return; }
		HashMap<String, Object> had = byHash.get(hash);
		boolean hadPending = had != null && (long) num(had, "confirmations") < 1;
		boolean nowConfirmed = (long) num(tx, "confirmations") >= 1;
		if (had == null || (hadPending && nowConfirmed)) byHash.put(hash, tx);
	}

	private static long _txMillis(Map<String, Object> tx) {
		long ts = (long) num(tx, "timestamp");
		if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
		return ts;
	}

	private void _schedulePendingPoll() {
		pendingPoll.removeCallbacks(pollPending);
		for (HashMap<String, Object> tx : allTransactions) {
			if ((long) num(tx, "confirmations") < 1) {
				pendingPoll.postDelayed(pollPending, 8_000L);
				return;
			}
		}
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
			boolean pending = (long) num(tx, "confirmations") < 1;
			
			boolean matchesFilter = currentFilter.equals("ALL") || 
			(currentFilter.equals("IN") && !isOut) || 
			(currentFilter.equals("OUT") && isOut) ||
			(currentFilter.equals("PENDING") && pending);
			
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
			// "No wallet activity yet" only once the node has answered: an empty cache is not an empty wallet.
			boolean known = loaded || !allTransactions.isEmpty();
			tv_wallet_activity_empty.setVisibility(known && displayTransactions.isEmpty() ? View.VISIBLE : View.GONE);
			if (displayTransactions.isEmpty() && !allTransactions.isEmpty()) {
				tv_wallet_activity_empty.setText("PENDING".equals(currentFilter) ? "No pending transactions." : "No transactions match your search.");
				com.harvana.app.utils.EmptyState.illustrate(tv_wallet_activity_empty, com.harvana.app.R.drawable.illus_empty_search);
			} else if (displayTransactions.isEmpty()) {
				tv_wallet_activity_empty.setText("No wallet activity yet.");
				com.harvana.app.utils.EmptyState.illustrate(tv_wallet_activity_empty, com.harvana.app.R.drawable.illus_empty_activity);
			}
		}
	}
	
	private void _showFilterMenu() {
		if (getContext() == null) return;
		PopupMenu popup = new PopupMenu(getContext(), btn_filter_transactions);
		popup.getMenu().add(0, 0, 0, "All Transactions");
		popup.getMenu().add(0, 1, 0, "Received (In)");
		popup.getMenu().add(0, 2, 0, "Sent (Out)");
		popup.getMenu().add(0, 3, 0, "Pending");
		
		popup.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
				case 0: currentFilter = "ALL"; break;
				case 1: currentFilter = "IN"; break;
				case 2: currentFilter = "OUT"; break;
				case 3: currentFilter = "PENDING"; break;
			}
			_applyFilters();
			return true;
		});
		popup.show();
	}
	
	private void _showExportOptionsBottomSheet() {
		if (getContext() == null) return;
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
		new com.harvana.app.ui.AppSheet(getContext());
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
		
		File fallbackDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Harvana");
		if (!fallbackDir.exists()) fallbackDir.mkdirs();
		file = new File(fallbackDir, fileName);
		if (!file.exists()) file.createNewFile();
		return file;
	}
	
	private void _shareExportedFile(File file, String mimeType, String subject) {
		if (getActivity() == null) return;
		com.harvana.app.receipt.ReceiptShare.shareFile(getActivity(), file, mimeType, subject);
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
			writer.append(("Date,TxHash,Type,Amount(" + com.harvana.app.CoinInfo.ticker() + "),Counterparty,Memo,Status\n"));
			
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
				double amt = _amountOf(tx);
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
		if (displayTransactions.isEmpty() || getContext() == null || getActivity() == null) {
			SketchwareUtil.showMessage(getContext(), "No transactions to export");
			return;
		}
		try {
			File pdf = _buildStatementPdf();
			File png = com.harvana.app.receipt.ReceiptShare.cacheFile(getContext(), "images", "harvana-statement.png");
			com.harvana.app.receipt.ReceiptShare.pdfFirstPageToPng(pdf, png);
			com.harvana.app.receipt.ReceiptShare.send(getActivity(), png, "image/png", "Harvana transaction statement", null);
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
			StringBuilder docContent = new StringBuilder("HARVANA NETWORK - OFFICIAL TRANSACTION STATEMENT\n\n");
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
				double amt = _amountOf(tx);
				String memo = str(tx, "memo", "");
				String status = ((long) num(tx, "confirmations") > 0) ? "Confirmed" : "Pending";
				
				// APPLIED FIX
				docContent.append("• ").append(date).append(" | ").append(type).append(" | ").append(_formatUIBalance(amt)).append((" " + _symbolOf(tx) + " | ")).append(status).append("\n");
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
	
	private File _buildStatementPdf() throws Exception {
		{
			java.util.List<com.harvana.app.receipt.ReceiptRenderer.Row> rows = new java.util.ArrayList<>();
			double in = 0, out = 0;
			for (HashMap<String, Object> tx : displayTransactions) {
				long ts = (long) num(tx, "timestamp");
				String from = str(tx, "from", "");
				String to = str(tx, "to", "");
				boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
				double amt = _amountOf(tx);
				String memo = str(tx, "memo", "");
				com.harvana.app.tx.TxKind kind = com.harvana.app.tx.TxKind.of(memo, !isOut);
				com.harvana.app.receipt.ReceiptRenderer.Row r = new com.harvana.app.receipt.ReceiptRenderer.Row();
				r.when = ts > 0 ? new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(new java.util.Date(ts)) : "Pending";
				r.what = kind != null ? kind.title : (isOut ? "Sent" : "Received") + (memo.isEmpty() ? "" : " \u00B7 " + memo);
				String counterparty = isOut ? to : from;
				r.detail = (isOut ? "To " : "From ") + (counterparty.length() > 22 ? counterparty.substring(0, 12) + "\u2026" + counterparty.substring(counterparty.length() - 8) : counterparty);
				r.amount = _formatUIBalance(amt) + " " + com.harvana.app.CoinInfo.ticker();
				r.direction = isOut ? -1 : 1;
				r.status = ((long) num(tx, "confirmations") > 0) ? "Confirmed" : "Pending";
				if (isOut) out += amt; else in += amt;
				rows.add(r);
			}
			String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
			File pdf = com.harvana.app.receipt.ReceiptShare.cacheFile(getContext(), "docs", "harvana-statement-" + stamp + ".pdf");
			com.harvana.app.receipt.ReceiptRenderer.renderStatement(getContext(), "Transaction statement",
			"Group wallet \u00B7 " + displayTransactions.size() + " transactions", rows,
			_formatUIBalance(in) + " " + com.harvana.app.CoinInfo.ticker(), _formatUIBalance(out) + " " + com.harvana.app.CoinInfo.ticker(), pdf);
			return pdf;
		}
	}
	
	private void _exportTransactionsToPdf() {
		if (displayTransactions.isEmpty() || getContext() == null || getActivity() == null) {
			SketchwareUtil.showMessage(getContext(), "No transactions to export");
			return;
		}
		try {
			com.harvana.app.receipt.ReceiptShare.send(getActivity(), _buildStatementPdf(), "application/pdf", "Harvana transaction statement", null);
		} catch (Exception e) {
			SketchwareUtil.showMessage(getContext(), "PDF export failed: " + e.getMessage());
		}
	}
	
	private class WalletActivityAdapter extends RecyclerView.Adapter<WalletActivityAdapter.VH> {
		private final List<HashMap<String, Object>> data;
		WalletActivityAdapter(List<HashMap<String, Object>> data) { this.data = data; }
		
		/** The same row as the wallet's Activity list (item_transaction): same icons, colours and wording. */
		@NonNull @Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false));
		}
		
		@Override 
		public void onBindViewHolder(@NonNull VH h, int pos) {
			HashMap<String, Object> tx = data.get(pos);
			
			String from = str(tx, "from", "");
			String to = str(tx, "to", "");
			boolean isOut = groupWalletAddress != null && groupWalletAddress.equalsIgnoreCase(from);
			String counterparty = isOut ? to : from;
			double amountHarv = _amountOf(tx);
			long ts = (long) num(tx, "timestamp");
			long confirmations = (long) num(tx, "confirmations");
			String memo = str(tx, "memo", "");
			String txHash = str(tx, "txHash", "");
			boolean confirmed = confirmations >= 1;
			String status = confirmed ? "Confirmed" : "Pending";
			String ticker = _symbolOf(tx);
			int incoming = androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), R.color.colorAccent);
			int outgoing = androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), R.color.colorError);
			int pending = androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), R.color.colorWarning);
			String shortAddr = counterparty.length() > 12
			? counterparty.substring(0, 8) + "…" + counterparty.substring(counterparty.length() - 4)
			: counterparty;
			
			// The group wallet is "me" here: money into it is received, money out of it is sent.
			com.harvana.app.tx.TxKind kind = com.harvana.app.tx.TxKind.of(memo, !isOut);
			if (h.icon != null) {
				int pad = Math.round(10 * h.icon.getResources().getDisplayMetrics().density);   // 20dp glyph in the 40dp circle
				h.icon.setPadding(pad, pad, pad, pad);
				h.icon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
				com.harvana.app.tx.TxIcon.circle(h.icon);
				h.icon.setImageResource(!confirmed ? R.drawable.icon_access_time_outline
					: kind != null ? kind.icon : isOut ? R.drawable.ic_action_send : R.drawable.ic_action_receive);
				h.icon.setColorFilter(!confirmed ? pending : isOut ? outgoing : incoming);
			}
			if (h.type != null) {
				String title = kind != null ? kind.title : (isOut ? "Sent " : "Received ") + ticker;
				h.type.setText(confirmed ? title : title + "…");
			}
			if (h.amount != null) {
				h.amount.setVisibility(View.VISIBLE);
				h.amount.setText((isOut ? "- " : "+ ") + _formatUIBalance(amountHarv) + " " + ticker);
				h.amount.setTextColor(!confirmed ? pending : isOut ? outgoing : incoming);
			}
			if (h.date != null) {
				long initiated = _millis(num(tx, "initiatedAt"));
				long confirmedAt = _millis(num(tx, "confirmedAt"));
				if (initiated <= 0) confirmedAt = 0;
				h.date.setText(initiated > 0
						? WalletActivityFragmentActivity._activityWhen(!confirmed, initiated, confirmedAt > 0 ? confirmedAt : ts)
						: (ts > 0 ? new SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(new java.util.Date(ts)) : "Pending"));
			}
			if (h.address != null) {
				h.address.setTag(counterparty);
				h.address.setText((isOut ? "To: " : "From: ") + shortAddr);
				// The member's name, when the wallet is someone in this group (any wallet linked to them counts).
				com.harvana.app.group.MemberNameResolver.resolveWallet(getContext(), groupId, counterparty, shortAddr, resolvedLabel -> {
					if (!counterparty.equals(h.address.getTag())) return;
					h.address.setText((kind != null ? kind.detail + " · " : "") + (isOut ? "To: " : "From: ") + resolvedLabel);
				});
			}
			
			// The inner row is clickable, so a listener on the wrapper never fires.
			View row = h.itemView.findViewById(R.id.cardview1);
			(row != null ? row : h.itemView).setOnClickListener(v -> {
				Intent intent = new Intent(v.getContext(), ReceiptActivity.class);
				
				intent.putExtra("txHash", txHash);
				intent.putExtra("status", status);
				intent.putExtra("senderAddress", from);
				intent.putExtra("recipientAddress", to);
				intent.putExtra("tokenSymbol", ticker);
				intent.putExtra("value", amountHarv);
				intent.putExtra("timestamp", ts);
				intent.putExtra("initiatedAt", (long) num(tx, "initiatedAt"));
				intent.putExtra("confirmedAt", (long) num(tx, "confirmedAt"));
				intent.putExtra("memo", memo);
				intent.putExtra("confirmations", confirmations);
				
				// Pass optional node telemetry if available in the HashMap
				// The tFare in the coin it was paid in: HARV gas, or the coin's own fare when no HARV was paid.
				if ((long) num(tx, "fee") == 0L && tx.containsKey("coinFeeRaw") && com.harvana.app.husd.Stable.isStable(str(tx, "asset", ""))) {
					intent.putExtra("harvFee", (long) num(tx, "coinFeeRaw") / (double) com.harvana.app.husd.HusdClient.UNIT);
					intent.putExtra("feeSymbol", ticker);
				} else {
					intent.putExtra("harvFee", com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay((long) num(tx, "fee")));
					intent.putExtra("feeSymbol", com.harvana.app.CoinInfo.ticker());
				}
				intent.putExtra("blockNumber", (long) num(tx, "blockHeight"));
				intent.putExtra("nonce", (long) num(tx, "nonce"));
				
				v.getContext().startActivity(intent);
			});
		}
		
		@Override public int getItemCount() { return data.size(); }
		
		class VH extends RecyclerView.ViewHolder {
			ImageView icon;
			TextView type, address, amount, date;
			VH(View v) {
				super(v);
				icon = v.findViewById(R.id.iv_tx_icon);
				type = v.findViewById(R.id.tv_tx_type);
				address = v.findViewById(R.id.tv_tx_address);
				amount = v.findViewById(R.id.tv_tx_amount);
				date = v.findViewById(R.id.tv_tx_date);
			}
		}
	}
	
	private static String str(Map<String, Object> m, String k, String d) {
		Object v = m.get(k);
		if (v == null) return d;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
	}
	
	/** A row's amount in its own coin: a stable coin has 6 decimals, HARV 8. */
	private static double _amountOf(java.util.Map<String, Object> tx) {
		Object raw = tx.get("amount");
		long v = raw instanceof Number ? ((Number) raw).longValue() : 0L;
		Object a = tx.get("asset");
		return a != null && com.harvana.app.husd.Stable.isStable(String.valueOf(a))
			? v / (double) com.harvana.app.husd.HusdClient.UNIT : HarvanaNodeClient.toHarvDisplay(v);
	}

	private static String _symbolOf(java.util.Map<String, Object> tx) {
		Object a = tx.get("asset");
		return a != null && com.harvana.app.husd.Stable.isStable(String.valueOf(a))
			? com.harvana.app.husd.Stable.symbol(String.valueOf(a)) : com.harvana.app.CoinInfo.ticker();
	}

	private static double num(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
	}

	private static long _millis(double v) {
		long ts = (long) v;
		if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
		return ts;
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
