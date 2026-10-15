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
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import com.harvana.app.wallet.WalletManager;
import java.net.URL;
import java.net.HttpURLConnection;


public class WalletActivityFragmentActivity extends Fragment {
	
	private String wallet_address = "";
	private static final int PAGE_SIZE = 25;
	private int nodePage = 0;
	private boolean nodeHasMore = true;
	private boolean isLoadingMore = false;
	private final java.util.ArrayList<java.util.HashMap<String, Object>> allNodeTx = new java.util.ArrayList<>();
	private final java.util.List<java.util.HashMap<String, Object>> chainTx = new java.util.ArrayList<>();
	
	private LinearLayout linear1;
	private CardView cardview1;
	private RecyclerView list_history;
	
	private SharedPreferences walletsp;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.wallet_activity_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		linear1 = _view.findViewById(R.id.linear1);
		cardview1 = _view.findViewById(R.id.cardview1);
		list_history = _view.findViewById(R.id.list_history);
		com.harvana.app.utils.Shimmer.hold(list_history, com.harvana.app.utils.Shimmer.rows(list_history.getContext(), 6), 4000, "WalletActivityFragmentActivity");
		androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh = _view.findViewById(R.id.refresh_activity);
		if (refresh != null) {
			refresh.setColorSchemeResources(R.color.colorAccent);
			refresh.setProgressBackgroundColorSchemeResource(R.color.colorSurface);
			refresh.setOnRefreshListener(() -> {
				String fresh = getContext() == null ? "" : getContext().getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE)
						.getString("active_wallet_address", "").trim();
				if (!fresh.isEmpty()) wallet_address = fresh;           // pull to refresh also picks up a wallet switch
				_loadTransactionHistory();
				refresh.postDelayed(() -> { if (isAdded()) refresh.setRefreshing(false); }, 1800);
			});
		}
		walletsp = getContext().getSharedPreferences("walletsp", Activity.MODE_PRIVATE);
	}
	
	private void initializeLogic() {
		SharedPreferences prefs = getContext().getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
		
		String address = prefs.getString("active_wallet_address", "").trim();
		
		if (address.isEmpty()) {
			address = WalletManager.getActiveWalletAddress(getContext());
			if (address == null) address = "";
		}
		
		if (address.isEmpty()) {
			return;
		}
		
		wallet_address = address;
		_loadTransactionHistory();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (getContext() != null) {
			SharedPreferences prefs = getContext().getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE);
			String freshAddress = prefs.getString("active_wallet_address", "").trim();
			if (freshAddress.isEmpty()) {
				freshAddress = WalletManager.getActiveWalletAddress(getContext());
				if (freshAddress == null) freshAddress = "";
			}
			if (!freshAddress.isEmpty() && !freshAddress.equals(wallet_address)) {
				wallet_address = freshAddress;
			}
		}
		_loadTransactionHistory();
	}
	
	public void _MORE() {
	}

	private boolean _harvOnly() {
		return getArguments() != null && getArguments().getBoolean("harvOnly", false);
	}

	private void _loadChainHistory() {
		if (_harvOnly() || getContext() == null || wallet_address.isEmpty()) return;
		final Context app = getContext().getApplicationContext();
		final String addr = wallet_address;
		new Thread(() -> {
			java.util.List<java.util.HashMap<String, Object>> rows = com.harvana.app.wallet.ChainHistory.load(app, addr);
			if (!isAdded() || getActivity() == null) return;
			getActivity().runOnUiThread(() -> {
				if (!isAdded() || getContext() == null || !addr.equals(wallet_address)) return;
				chainTx.clear();
				if (rows != null) chainTx.addAll(rows);
				_renderMergedTransactions();
			});
		}).start();
	}

	private java.util.List<java.util.HashMap<String, Object>> _cachedHarv() {
		java.util.List<java.util.HashMap<String, Object>> c = com.harvana.app.wallet.WalletCache.loadList(getContext(), "tx_" + wallet_address);
		java.util.List<java.util.HashMap<String, Object>> out = new java.util.ArrayList<>();
		if (c != null) for (java.util.HashMap<String, Object> m : c) if (!"pending".equalsIgnoreCase(String.valueOf(m.get("status")))) out.add(m);
		return out;
	}

	public void _loadTransactionHistory() {
		if (wallet_address == null || wallet_address.trim().isEmpty()) return;
		if (getContext() == null) return;
		
		final Context context = getContext();
		
		com.harvana.app.wallet.PendingTxStore.checkStaleAndMarkFailed(context, wallet_address);
		
		// Render cache & pending instantly without flickering
		_renderMergedTransactions();
		
		// Reset pagination and fetch fresh
		nodePage = 0;
		nodeHasMore = true;
		allNodeTx.clear();
		_fetchNodeHistoryPage();
		_loadChainHistory();
		
		// Server-reported pending
		com.harvana.app.sync.HarvanaNodeClient.getPending(wallet_address,
		new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				if (!isAdded() || getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (!isAdded() || getContext() == null) return;
					try {
						org.json.JSONArray arr = json.optJSONArray("pending");
						if (arr == null || arr.length() == 0) return;
						
						java.util.List<java.util.HashMap<String, Object>> knownPending =
						com.harvana.app.wallet.PendingTxStore.loadAll(getContext(), wallet_address);
						java.util.Set<String> knownHashes = new java.util.HashSet<>();
						for (java.util.HashMap<String, Object> e : knownPending) {
							knownHashes.add(String.valueOf(e.get("txHash")));
						}
						
						boolean addedAny = false;
						for (int i = 0; i < arr.length(); i++) {
							org.json.JSONObject tx = arr.getJSONObject(i);
							String hash = tx.optString("txHash", "");
							if (hash.isEmpty() || knownHashes.contains(hash)) continue;
							
							java.util.HashMap<String, Object> entry = new java.util.HashMap<>();
							entry.put("txHash", hash);
							entry.put("senderAddress", tx.optString("from", ""));
							entry.put("recipientAddress", tx.optString("to", ""));
							entry.put("memo", tx.optString("memo", "")); 
							String stableCode = tx.optString("asset", tx.optString("mintId", ""));
							boolean husdTx = com.harvana.app.husd.Stable.isStable(stableCode);
							entry.put("value", husdTx ? tx.optLong("amount", 0L) / (double) com.harvana.app.husd.HusdClient.UNIT
									: com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(tx.optLong("amount", 0L)));
							entry.put("tokenSymbol", husdTx ? com.harvana.app.husd.Stable.symbol(stableCode) : com.harvana.app.CoinInfo.ticker());
							entry.put("status", "pending");
							long initiated = tx.optLong("receivedAt", tx.optLong("timestamp", 0L));
							if (initiated <= 0) initiated = System.currentTimeMillis();
							else if (initiated < 1_000_000_000_000L) initiated *= 1000L;
							entry.put("timestamp", initiated);
							entry.put("initiatedAt", initiated);
							entry.put("confirmedAt", 0L);
							knownPending.add(entry);
							addedAny = true;
						}
						
						if (addedAny) {
							com.harvana.app.wallet.WalletCache.saveList(
							getContext(), "pending_" + wallet_address, knownPending);
							_renderMergedTransactions();
						}
					} catch (Exception e) {
						android.util.Log.e("loadTxHistory", "Pending parse error: " + e.getMessage());
					}
				});
			}
			
			@Override
			public void onError(String error) {
				android.util.Log.w("loadTxHistory", "Pending fetch failed: " + error);
			}
		});
	}
	
	private void _fetchNodeHistoryPage() {
		if (getContext() == null) return;
		isLoadingMore = true;
		
		com.harvana.app.sync.HarvanaNodeClient.getHistory(wallet_address, nodePage, PAGE_SIZE,
		new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
			
			@Override
			public void onSuccess(final org.json.JSONObject json) {
				if (!isAdded() || getActivity() == null) return;
				
				getActivity().runOnUiThread(() -> {
					if (!isAdded() || getContext() == null) return;
					isLoadingMore = false;
					
					int returnedCount = 0;
					try {
						org.json.JSONArray transactions = json.optJSONArray("transactions");
						if (transactions == null) {
							transactions = json.optJSONArray("data");
						}
						
						if (transactions != null) {
							returnedCount = transactions.length();
							for (int i = 0; i < transactions.length(); i++) {
								org.json.JSONObject tx = transactions.getJSONObject(i);
								java.util.HashMap<String, Object> item = new java.util.HashMap<>();
								
								String sender = tx.optString("senderAddress", tx.optString("from", ""));
								String recipient = tx.optString("recipientAddress", tx.optString("to", ""));
								
								item.put("senderAddress", sender);
								item.put("recipientAddress", recipient);
								item.put("memo", tx.optString("memo", "")); 
								
								// Stable assets (hUSD, hEUR, …) ride on the same history; their amounts have 6 decimals, not HARV's 8.
								String stableAsset = tx.optString("asset", tx.optString("mintId", ""));
								boolean husd = com.harvana.app.husd.Stable.isStable(stableAsset);
								double displayValue;
								if (tx.has("amount") && !tx.isNull("amount")) {
									long rawAmount = tx.optLong("amount", 0L);
									displayValue = husd ? rawAmount / (double) com.harvana.app.husd.HusdClient.UNIT
											: com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(rawAmount);
								} else {
									displayValue = tx.optDouble("value", tx.optDouble("value_harv", 0.0));
								}
								item.put("value", displayValue);
								
								item.put("tokenSymbol", husd ? com.harvana.app.husd.Stable.symbol(stableAsset)
										: tx.optString("tokenSymbol", tx.optString("token", com.harvana.app.CoinInfo.ticker())));
								item.put("txHash", tx.optString("txHash", ""));
								item.put("status", tx.optString("status", "confirmed"));
								
								double displayFee;
								String feeSymbol = com.harvana.app.CoinInfo.ticker();
								if (husd && tx.optLong("gas", 0L) == 0L && tx.has("coinFeeRaw")) {
									// A coin transfer that paid its tFare in the coin (no HARV): the node gives the fare it took.
									displayFee = tx.optLong("coinFeeRaw", 0L) / (double) com.harvana.app.husd.HusdClient.UNIT;
									feeSymbol = com.harvana.app.husd.Stable.symbol(stableAsset);
								} else if (tx.has("gas") && !tx.isNull("gas")) {
									displayFee = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(tx.optLong("gas", 0L));
								} else {
									displayFee = tx.optDouble("harvFee", tx.optDouble("fee_harv", 0.0));
								}
								item.put("harvFee", displayFee);
								item.put("feeSymbol", feeSymbol);
								item.put("burnAmount", tx.optDouble("burnAmount", 0.0));
								item.put("blockNumber", tx.optLong("blockHeight", 0L));
								item.put("nonce", tx.optLong("nonce", 0L));
								item.put("confirmations", tx.optLong("confirmations", 0L));
								
								long confirmed = tx.optLong("confirmedAt", tx.optLong("timestamp", 0L));
								long initiated = tx.optLong("initiatedAt", 0L);
								if (confirmed > 0 && confirmed < 1_000_000_000_000L) confirmed *= 1000L;
								if (initiated > 0 && initiated < 1_000_000_000_000L) initiated *= 1000L;
								long ts = confirmed > 0 ? confirmed : (initiated > 0 ? initiated : System.currentTimeMillis());
								item.put("timestamp", ts);
								item.put("initiatedAt", initiated);
								item.put("confirmedAt", confirmed);
								
								allNodeTx.add(item);
							}
						}
					} catch (Exception e) {
						android.util.Log.e("loadTxHistory", "Parse error: " + e.getMessage());
					}
					
					nodeHasMore = returnedCount == PAGE_SIZE;
					nodePage++;
					
					if (!allNodeTx.isEmpty()) {
						java.util.ArrayList<String> confirmedHashes = new java.util.ArrayList<>();
						for (java.util.HashMap<String, Object> t : allNodeTx) {
							confirmedHashes.add(String.valueOf(t.get("txHash")));
						}
						com.harvana.app.wallet.PendingTxStore.pruneConfirmed(
						getContext(), wallet_address, confirmedHashes);
						
						java.util.List<java.util.HashMap<String, Object>> currentPendingList =
						com.harvana.app.wallet.PendingTxStore.loadAll(getContext(), wallet_address);
						java.util.ArrayList<java.util.HashMap<String, Object>> currentPending =
						currentPendingList != null
						? new java.util.ArrayList<>(currentPendingList)
						: new java.util.ArrayList<>();
						
						java.util.ArrayList<java.util.HashMap<String, Object>> display =
						new java.util.ArrayList<>(currentPending);
						display.addAll(allNodeTx);
						_sortByTimestampDesc(display);
						
						java.util.List<java.util.HashMap<String, Object>> toCache =
						display.size() > PAGE_SIZE ? display.subList(0, PAGE_SIZE) : display;
						com.harvana.app.wallet.WalletCache.saveList(
						getContext(), "tx_" + wallet_address, toCache);
					}
					
					_renderMergedTransactions();
				});
			}
			
			@Override
			public void onError(final String error) {
				isLoadingMore = false;
				if (!isAdded() || getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (!isAdded() || getContext() == null) return;
					android.util.Log.w("loadTxHistory", "Node error: " + error);
				});
			}
		});
	}
	
	private void _loadMoreNodeHistory() {
		if (isLoadingMore || !nodeHasMore) return;
		_fetchNodeHistoryPage();
	}

	/** Unified single rendering method to prevent UI flickering/glitching races */
	private void _renderMergedTransactions() {
		if (!isAdded() || getContext() == null || list_history == null) return;
		
		java.util.List<java.util.HashMap<String, Object>> pendingTx =
		com.harvana.app.wallet.PendingTxStore.loadAll(getContext(), wallet_address);
		if (pendingTx == null) pendingTx = new java.util.ArrayList<>();
		
		java.util.List<java.util.HashMap<String, Object>> harvTx =
		allNodeTx.isEmpty() ? _cachedHarv() : allNodeTx;
		
		java.util.ArrayList<java.util.HashMap<String, Object>> display = new java.util.ArrayList<>(pendingTx);
		display.addAll(harvTx);
		display.addAll(chainTx);
		
		_sortByTimestampDesc(display);
		java.util.ArrayList<java.util.HashMap<String, Object>> filtered = filterFailedTx(display);
		// One coin's page (CoinDetailActivity): only that coin's rows ("onlySymbol" = "hUSD", "HARV", …).
		String only = getArguments() == null ? null : getArguments().getString("onlySymbol");
		if (only != null && !only.isEmpty()) {
			java.util.ArrayList<java.util.HashMap<String, Object>> mine = new java.util.ArrayList<>();
			for (java.util.HashMap<String, Object> m : filtered) {
				Object sym = m.get("tokenSymbol");
				String t = sym == null ? com.harvana.app.CoinInfo.ticker() : String.valueOf(sym);
				if (t.equalsIgnoreCase(only)) mine.add(m);
			}
			filtered = mine;
		}
		
		if (list_history.getLayoutManager() == null) {
			list_history.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
		}
		
		RecyclerView.Adapter adapter = list_history.getAdapter();
		if (adapter instanceof TransactionHistoryAdapter) {
			((TransactionHistoryAdapter) adapter).updateItems(filtered);
		} else {
			list_history.setAdapter(new TransactionHistoryAdapter(getContext(), filtered, wallet_address));
		}
	}
	
	private void _sortByTimestampDesc(java.util.List<java.util.HashMap<String, Object>> list) {
		if (list == null) return;
		java.util.Collections.sort(list, (a, b) -> {
			long ta = _tsOf(a);
			long tb = _tsOf(b);
			return Long.compare(tb, ta);
		});
	}
	
	private long _tsOf(java.util.HashMap<String, Object> item) {
		if (item == null) return 0L;
		Object v = item.get("timestamp");
		if (v instanceof Number) {
			long ts = ((Number) v).longValue();
			if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
			return ts;
		} else if (v != null) {
			try {
				long ts = Long.parseLong(v.toString().trim());
				if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
				return ts;
			} catch (Exception ignored) {}
		}
		return 0L;
	}
	
	public void _extra_codes() {
	}
	
	public class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.TxViewHolder> {
		
		private final java.util.List<java.util.HashMap<String, Object>> items;
		private final String walletAddress;
		
		private final int COLOR_FAILED;
		private final int COLOR_INCOMING;
		private final int COLOR_PENDING;
		private final int COLOR_OUTGOING;
		
		public TransactionHistoryAdapter(
		Context context,
		java.util.List<java.util.HashMap<String, Object>> items,
		String walletAddress) {
			this.items         = items != null ? new java.util.ArrayList<>(items) : new java.util.ArrayList<>();
			this.walletAddress = walletAddress != null ? walletAddress : "";
			this.COLOR_FAILED   = androidx.core.content.ContextCompat.getColor(context, R.color.colorError);
			this.COLOR_INCOMING = androidx.core.content.ContextCompat.getColor(context, R.color.colorAccent);
			this.COLOR_PENDING  = androidx.core.content.ContextCompat.getColor(context, R.color.colorWarning);
			this.COLOR_OUTGOING = androidx.core.content.ContextCompat.getColor(context, R.color.colorError);
		}

		public void updateItems(java.util.List<java.util.HashMap<String, Object>> newItems) {
			items.clear();
			if (newItems != null) items.addAll(newItems);
			notifyDataSetChanged();
		}
		
		@NonNull
		@Override
		public TxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.item_transaction, parent, false);
			return new TxViewHolder(view);
		}
		
		@Override
		public void onBindViewHolder(@NonNull TxViewHolder holder, int position) {
			java.util.HashMap<String, Object> item = items.get(position);
			
			String sender    = safeAddress(item.get("senderAddress"));
			String recipient = safeAddress(item.get("recipientAddress"));
			
			String asset = item.get("tokenSymbol") != null 
			? item.get("tokenSymbol").toString().toUpperCase() : com.harvana.app.CoinInfo.ticker();
			if (asset.isEmpty() || "NULL".equals(asset)) asset = com.harvana.app.CoinInfo.ticker();
			final String coinCode = asset;
			if (com.harvana.app.husd.Stable.isStable(asset)) asset = com.harvana.app.husd.Stable.symbol(asset);   // "HNGN" reads hNGN
			
			double amount = 0.0;
			Object valObj = item.get("value");
			if (valObj instanceof Number) {
				amount = ((Number) valObj).doubleValue();
			} else if (valObj != null) {
				try {
					amount = Double.parseDouble(valObj.toString().replace(",", ""));
				} catch (Exception ignored) {}
			}
			
			String formattedAmount = _formatUIBalance(amount) + " " + asset;
			
			long timestamp = System.currentTimeMillis();
			Object tsObj = item.get("timestamp");
			if (tsObj instanceof Number) {
				timestamp = ((Number) tsObj).longValue();
				if (timestamp > 0 && timestamp < 1_000_000_000_000L) timestamp *= 1000L;
			}
			
			String status = item.get("status") != null ? item.get("status").toString() : "confirmed";
			boolean isPending = "pending".equalsIgnoreCase(status);
			boolean isFailed  = "failed".equalsIgnoreCase(status);
			boolean isIncoming = item.get("incoming") instanceof Boolean
			? (Boolean) item.get("incoming")
			: !recipient.isEmpty() && recipient.equalsIgnoreCase(walletAddress);
			
			if (isFailed) {
				if (holder.tvType != null) holder.tvType.setText("Failed " + asset);
				if (holder.tvAddress != null) holder.tvAddress.setText("To: " + shortenAddress(recipient));
				if (holder.tvAmount != null) {
					holder.tvAmount.setText("✕ " + formattedAmount);
					holder.tvAmount.setTextColor(COLOR_FAILED);
				}
				if (holder.ivIcon != null) {
					_lineIcon(holder.ivIcon);
					holder.ivIcon.setImageResource(R.drawable.ic_action_send);
					holder.ivIcon.setColorFilter(COLOR_FAILED);
				}
			} else if (isIncoming) {
				if (holder.tvType != null) {
					holder.tvType.setText(isPending ? "Receiving " + asset + "…" : "Received " + asset);
				}
				if (holder.tvAddress != null) holder.tvAddress.setText("From: " + shortenAddress(sender));
				if (holder.tvAmount != null) {
					holder.tvAmount.setText("+ " + formattedAmount);
					holder.tvAmount.setTextColor(isPending ? COLOR_PENDING : COLOR_INCOMING);
				}
				if (holder.ivIcon != null) {
					_lineIcon(holder.ivIcon);
					holder.ivIcon.setImageResource(isPending ? R.drawable.icon_access_time_outline : R.drawable.ic_action_receive);
					holder.ivIcon.setColorFilter(isPending ? COLOR_PENDING : COLOR_INCOMING);
				}
			} else {
				if (holder.tvType != null) {
					holder.tvType.setText(isPending ? "Sending " + asset + "…" : "Sent " + asset);
				}
				if (holder.tvAddress != null) holder.tvAddress.setText("To: " + shortenAddress(recipient));
				if (holder.tvAmount  != null) {
					holder.tvAmount.setText("- " + formattedAmount);
					holder.tvAmount.setTextColor(isPending ? COLOR_PENDING : COLOR_OUTGOING);
				}
				if (holder.ivIcon != null) {
					_lineIcon(holder.ivIcon);
					holder.ivIcon.setImageResource(isPending ? R.drawable.icon_access_time_outline : R.drawable.ic_action_send);
					holder.ivIcon.setColorFilter(isPending ? COLOR_PENDING : COLOR_OUTGOING);
				}
			}
			
			if (!isFailed) {
				com.harvana.app.tx.TxKind kind = com.harvana.app.tx.TxKind.of(
				item.get("memo") != null ? String.valueOf(item.get("memo")) : "", isIncoming);
				if (kind != null) {
					String memo0 = item.get("memo") != null ? String.valueOf(item.get("memo")) : "";
					if (holder.tvType != null) holder.tvType.setText(isPending ? kind.title + "…" : kind.title);
					if (holder.tvAddress != null) {
						String coinBit = memo0.startsWith("SWAP_") ? " · " + asset : "";
						holder.tvAddress.setText(kind.detail + coinBit + " · " + (isIncoming ? "From: " + shortenAddress(sender) : "To: " + shortenAddress(recipient)));
					}
					if (!isPending && holder.ivIcon != null) {
						if (memo0.startsWith("SWAP_") && !memo0.startsWith("SWAP_FARE:")) {
							_fullColourIcon(holder.ivIcon);                         // Harvana Swap's own icon
							holder.ivIcon.setImageResource(R.drawable.ic_app_swap);
						} else if (com.harvana.app.husd.Stable.isStable(coinCode)) {
							// The coin's own logo, in its colours: the layout's tint painted it as a plain cream disc.
							_fullColourIcon(holder.ivIcon);
							holder.ivIcon.setImageDrawable(com.harvana.app.husd.StableIcon.drawable(holder.ivIcon.getContext(), coinCode));
						} else {
							holder.ivIcon.setImageResource(kind.icon);
						}
					}
				}
			}
			
			final Object chainName = item.get("chain");
			if (chainName != null && holder.tvAddress != null) {
				holder.tvAddress.setText(chainName + " · " + holder.tvAddress.getText());
			}
			
			if (holder.tvDate != null) {
				holder.tvDate.setText(_activityWhen(isPending, _longOf(item, "initiatedAt"), timestamp));
			}
			
			final long   finalTimestamp = timestamp;
			final double finalAmount    = amount;
			final String finalAsset     = coinCode;              // the code ("HNGN"), which the receipt and coin pages look up
			
			if (holder.cardview1 != null) {
				holder.cardview1.setOnClickListener(v -> {
					Intent intent = new Intent(v.getContext(), ReceiptActivity.class);
					
					intent.putExtra("txHash",           String.valueOf(item.get("txHash")));
					intent.putExtra("status",           String.valueOf(item.get("status")));
					intent.putExtra("senderAddress",    sender);
					intent.putExtra("recipientAddress", recipient);
					intent.putExtra("tokenSymbol",      finalAsset);
					intent.putExtra("value",            finalAmount);
					intent.putExtra("timestamp",        finalTimestamp);
					intent.putExtra("initiatedAt",      _longOf(item, "initiatedAt"));
					intent.putExtra("confirmedAt",      _longOf(item, "confirmedAt"));
					
					String passedMemo = item.get("memo") != null ? String.valueOf(item.get("memo")) : "";
					intent.putExtra("memo", passedMemo);
					
					Object feeObj    = item.get("harvFee");
					Object burnObj   = item.get("burnAmount");
					Object blockObj  = item.get("blockNumber");
					Object nonceObj  = item.get("nonce");
					Object confObj = item.get("confirmations");
					
					if (confObj instanceof Number) {
						intent.putExtra("confirmations", ((Number) confObj).longValue());
					}
					
					if (feeObj instanceof Number)   intent.putExtra("harvFee", ((Number) feeObj).doubleValue());
					if (item.get("feeSymbol") != null) intent.putExtra("feeSymbol", String.valueOf(item.get("feeSymbol")));
					if (burnObj instanceof Number)  intent.putExtra("burnAmount", ((Number) burnObj).doubleValue());
					if (blockObj instanceof Number) intent.putExtra("blockNumber", ((Number) blockObj).longValue());
					if (nonceObj instanceof Number) intent.putExtra("nonce", ((Number) nonceObj).longValue());
					if (chainName != null) {
						intent.putExtra("chain", String.valueOf(chainName));
						intent.putExtra("assetKey", finalAsset);
						intent.putExtra("incoming", item.get("incoming") instanceof Boolean && (Boolean) item.get("incoming"));
					}
					
					v.getContext().startActivity(intent);
				});
			}
		}
		
		@Override
		public int getItemCount() {
			return items == null ? 0 : items.size();
		}
		
		private String safeAddress(Object obj) {
			if (obj == null) return "";
			String s = obj.toString().trim();
			return "null".equalsIgnoreCase(s) ? "" : s;
		}
		
		private String shortenAddress(String address) {
			if (address == null || address.isEmpty()) return "Unknown";
			if (address.length() <= 16) return address;
			return address.substring(0, 10) + "…" + address.substring(address.length() - 4);
		}
		
		public class TxViewHolder extends RecyclerView.ViewHolder {
			ImageView ivIcon;
			TextView  tvType;
			TextView  tvAddress;
			TextView  tvAmount;
			TextView  tvDate;
			View cardview1; 
			
			public TxViewHolder(@NonNull View itemView) {
				super(itemView);
				ivIcon    = itemView.findViewById(R.id.iv_tx_icon);
				tvType    = itemView.findViewById(R.id.tv_tx_type);
				tvAddress = itemView.findViewById(R.id.tv_tx_address);
				tvAmount  = itemView.findViewById(R.id.tv_tx_amount);
				tvDate    = itemView.findViewById(R.id.tv_tx_date);
				cardview1 = itemView.findViewById(R.id.cardview1); 
			}
		}
	}
	
	/** A line icon in the row's colour, 8dp inside its circle (rows are reused, so every row sets this again). */
	private static void _lineIcon(android.widget.ImageView iv) {
		int p = Math.round(8 * iv.getResources().getDisplayMetrics().density);
		iv.setPadding(p, p, p, p);
		iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
		iv.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(iv.getContext(), R.color.colorAccent)));
		com.harvana.app.tx.TxIcon.circle(iv);
	}

	/** A coin or app logo in its own colours, filling its circle. */
	private static void _fullColourIcon(android.widget.ImageView iv) {
		iv.clearColorFilter();
		iv.setImageTintList(null);
		iv.setPadding(0, 0, 0, 0);
		iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
		com.harvana.app.tx.TxIcon.circle(iv);
	}

	private static long _longOf(java.util.Map<String, Object> item, String key) {
		if (item == null) return 0L;
		Object v = item.get(key);
		long ts = 0L;
		if (v instanceof Number) ts = ((Number) v).longValue();
		else if (v != null) {
			try { ts = Long.parseLong(v.toString().trim()); } catch (Exception ignored) { return 0L; }
		}
		if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
		return ts;
	}

	/** Pending shows when it was sent. Confirmed shows that time, then the confirmation, when the node recorded both. */
	static CharSequence _activityWhen(boolean pending, long initiated, long confirmed) {
		long now = System.currentTimeMillis();
		if (pending) {
			long t = initiated > 0 ? initiated : confirmed;
			if (t <= 0) return "Waiting";
			return "Sent " + android.text.format.DateUtils.getRelativeTimeSpanString(t, now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
		}
		if (initiated > 0 && confirmed > 0 && Math.abs(confirmed - initiated) > 1_500L) {
			CharSequence sent = android.text.format.DateUtils.getRelativeTimeSpanString(initiated, now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
			CharSequence done = android.text.format.DateUtils.getRelativeTimeSpanString(confirmed, now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
			return "Sent " + sent + " · Confirmed " + done;
		}
		long t = confirmed > 0 ? confirmed : initiated;
		if (t <= 0) return "";
		return android.text.format.DateUtils.getRelativeTimeSpanString(t, now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
	}

	private java.util.ArrayList<java.util.HashMap<String, Object>> filterFailedTx(
	java.util.List<java.util.HashMap<String, Object>> source) {
		
		java.util.ArrayList<java.util.HashMap<String, Object>> clean = new java.util.ArrayList<>();
		if (source == null) return clean;
		
		java.util.HashSet<String> seenHashes = new java.util.HashSet<>();
		long now = System.currentTimeMillis();
		long maxAgeMs = 24L * 60 * 60 * 1000; 
		
		for (java.util.HashMap<String, Object> item : source) {
			String hash = item.get("txHash") != null ? item.get("txHash").toString() : "";
			if (!hash.isEmpty() && !"null".equals(hash)) {
				if (seenHashes.contains(hash)) {
					continue;
				}
				seenHashes.add(hash);
			}
			
			String status = item.get("status") != null ? item.get("status").toString() : "";
			
			if ("failed".equalsIgnoreCase(status)) {
				long ts = 0;
				Object tsObj = item.get("timestamp");
				if (tsObj instanceof Number) {
					ts = ((Number) tsObj).longValue();
				}
				if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
				
				if (ts > 0 && (now - ts) > maxAgeMs) {
					continue;
				}
			}
			clean.add(item);
		}
		return clean;
	}
	
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
