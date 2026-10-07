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
			// No wallet yet → show create wallet card
			return;
		}
		
		// We have a wallet
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
	/** Other coins' recent transactions (Pi, Bitcoin, Litecoin when turned on under Add coins). Not cached with HARV's. */
	private final java.util.List<java.util.HashMap<String, Object>> chainTx = new java.util.ArrayList<>();

	private boolean _harvOnly() {
		return getArguments() != null && getArguments().getBoolean("harvOnly", false);
	}

	private void _loadChainHistory() {
		if (_harvOnly() || getContext() == null) return;
		final Context app = getContext().getApplicationContext();
		final String addr = wallet_address;
		new Thread(() -> {
			java.util.List<java.util.HashMap<String, Object>> rows = com.harvana.app.wallet.ChainHistory.load(app, addr);
			if (!isAdded() || getActivity() == null) return;
			getActivity().runOnUiThread(() -> {
				if (!isAdded() || getContext() == null || !addr.equals(wallet_address)) return;
				chainTx.clear();
				chainTx.addAll(rows);
				if (rows.isEmpty()) return;
				java.util.ArrayList<java.util.HashMap<String, Object>> display = new java.util.ArrayList<>(
						com.harvana.app.wallet.PendingTxStore.loadAll(getContext(), wallet_address));
				display.addAll(allNodeTx.isEmpty() ? _cachedHarv() : allNodeTx);
				display.addAll(chainTx);
				_sortByTimestampDesc(display);
				if (list_history.getLayoutManager() == null)
					list_history.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
				list_history.setAdapter(new TransactionHistoryAdapter(getContext(), filterFailedTx(display), wallet_address));
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
		
		// Render cached history instantly
		com.harvana.app.wallet.PendingTxStore.checkStaleAndMarkFailed(context, wallet_address);
		java.util.List<java.util.HashMap<String, Object>> cachedTx =
		com.harvana.app.wallet.WalletCache.loadList(context, "tx_" + wallet_address);
		java.util.List<java.util.HashMap<String, Object>> pendingTx =
		com.harvana.app.wallet.PendingTxStore.loadAll(context, wallet_address);
		if (cachedTx == null) cachedTx = new java.util.ArrayList<>();
		
		java.util.ArrayList<java.util.HashMap<String, Object>> mergedCache =
		new java.util.ArrayList<>(pendingTx);
		mergedCache.addAll(cachedTx);
		_sortByTimestampDesc(mergedCache);
		
		if (!mergedCache.isEmpty()) {
			if (list_history.getLayoutManager() == null) {
				list_history.setLayoutManager(
				new androidx.recyclerview.widget.LinearLayoutManager(context));
			}
			// ★ Filtered — old failed txs are hidden
			list_history.setAdapter(
			new TransactionHistoryAdapter(context, filterFailedTx(mergedCache), wallet_address));
		} else {
			// No cached/pending tx for THIS wallet yet — e.g. just switched
			// to a wallet never viewed on this device before. Clear the
			// list rather than leaving the previously active wallet's
			// history visibly (and misleadingly) on screen while the fresh
			// node fetch below is in flight.
			if (list_history.getLayoutManager() == null) {
				list_history.setLayoutManager(
				new androidx.recyclerview.widget.LinearLayoutManager(context));
			}
			list_history.setAdapter(
			new TransactionHistoryAdapter(context, new java.util.ArrayList<>(), wallet_address));
		}
		
		// Reset pagination and fetch page 0 fresh - this is a full reload
		// (initial open / onResume), not a "load more" continuation.
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
							entry.put("value", com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(tx.optLong("amount", 0L)));
							entry.put("tokenSymbol", com.harvana.app.CoinInfo.ticker());
							entry.put("status", "pending");
							entry.put("timestamp", System.currentTimeMillis());
							knownPending.add(entry);
							addedAny = true;
						}
						
						if (addedAny) {
							com.harvana.app.wallet.WalletCache.saveList(
							getContext(), "pending_" + wallet_address, knownPending);
							_loadTransactionHistory();
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
								
								double displayValue;
								if (tx.has("amount") && !tx.isNull("amount")) {
									long rawAmount = tx.optLong("amount", 0L);
									displayValue = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(rawAmount);
								} else {
									displayValue = tx.optDouble("value", tx.optDouble("value_harv", 0.0));
								}
								item.put("value", displayValue);
								
								item.put("tokenSymbol", tx.optString("tokenSymbol", tx.optString("token", com.harvana.app.CoinInfo.ticker())));
								item.put("txHash", tx.optString("txHash", ""));
								item.put("status", tx.optString("status", "confirmed"));
								
								double displayFee;
								if (tx.has("gas") && !tx.isNull("gas")) {
									displayFee = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(tx.optLong("gas", 0L));
								} else {
									displayFee = tx.optDouble("harvFee", tx.optDouble("fee_harv", 0.0));
								}
								item.put("harvFee", displayFee);
								item.put("burnAmount", tx.optDouble("burnAmount", 0.0));
								item.put("blockNumber", tx.optLong("blockHeight", 0L));
								item.put("nonce", tx.optLong("nonce", 0L));
								item.put("confirmations", tx.optLong("confirmations", 0L));
								
								long ts = tx.optLong("timestamp", System.currentTimeMillis());
								if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
								item.put("timestamp", ts);
								
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
						
						if (list_history.getLayoutManager() == null) {
							list_history.setLayoutManager(
							new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
						}
						java.util.ArrayList<java.util.HashMap<String, Object>> withChains = new java.util.ArrayList<>(display);
						withChains.addAll(chainTx);
						_sortByTimestampDesc(withChains);
						list_history.setAdapter(
						new TransactionHistoryAdapter(getContext(), filterFailedTx(withChains), wallet_address));
					} else {
						android.util.Log.d("loadTxHistory", "Node returned no history.");
					}
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
	
	private void _sortByTimestampDesc(java.util.List<java.util.HashMap<String, Object>> list) {
		if (list == null) return;
		java.util.Collections.sort(list, (a, b) -> {
			long ta = _tsOf(a);
			long tb = _tsOf(b);
			return Long.compare(tb, ta);
		});
	}
	
	private long _tsOf(java.util.HashMap<String, Object> item) {
		Object v = item.get("timestamp");
		if (v instanceof Number) {
			long ts = ((Number) v).longValue();
			if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
			return ts;
		}
		return 0L;
	}
	
	public void _extra_codes() {
	}
	
	// --- CORRECT ADAPTER RESTORED HERE ---
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
			this.items         = items;
			this.walletAddress = walletAddress != null ? walletAddress : "";
			this.COLOR_FAILED   = androidx.core.content.ContextCompat.getColor(context, R.color.colorError);
			this.COLOR_INCOMING = androidx.core.content.ContextCompat.getColor(context, R.color.colorAccent);
			this.COLOR_PENDING  = androidx.core.content.ContextCompat.getColor(context, R.color.colorWarning);
			this.COLOR_OUTGOING = androidx.core.content.ContextCompat.getColor(context, R.color.colorError);
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
			
			double amount = 0.0;
			Object valObj = item.get("value");
			if (valObj instanceof Number) {
				amount = ((Number) valObj).doubleValue();
			} else if (valObj != null) {
				try {
					amount = Double.parseDouble(valObj.toString().replace(",", ""));
				} catch (Exception ignored) {}
			}
			
			String formattedAmount = _formatUIBalance(amount);
			
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
					holder.ivIcon.setImageResource(R.drawable.icon_call_made_twotone);
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
					holder.ivIcon.setImageResource(isPending ? R.drawable.icon_access_time_outline : R.drawable.icon_call_received_twotone);
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
					holder.ivIcon.setImageResource(isPending ? R.drawable.icon_access_time_outline : R.drawable.icon_call_made_twotone);
					holder.ivIcon.setColorFilter(isPending ? COLOR_PENDING : COLOR_OUTGOING);
				}
			}
			
			// Name and icon by what the transaction is for (staking, group creation, contribution, loan, gift…)
			if (!isFailed) {
				com.harvana.app.tx.TxKind kind = com.harvana.app.tx.TxKind.of(
				item.get("memo") != null ? String.valueOf(item.get("memo")) : "", isIncoming);
				if (kind != null) {
					if (holder.tvType != null) holder.tvType.setText(isPending ? kind.title + "…" : kind.title);
					if (holder.tvAddress != null) holder.tvAddress.setText(kind.detail + " · " + (isIncoming ? "From: " + shortenAddress(sender) : "To: " + shortenAddress(recipient)));
					if (holder.ivIcon != null) holder.ivIcon.setImageResource(kind.icon);
				}
			}
			
			final Object chainName = item.get("chain");
			if (chainName != null && holder.tvAddress != null) {
				holder.tvAddress.setText(chainName + " · " + holder.tvAddress.getText());
			}
			
			if (holder.tvDate != null) {
				CharSequence rel = android.text.format.DateUtils.getRelativeTimeSpanString(
				timestamp, 
				System.currentTimeMillis(), 
				android.text.format.DateUtils.MINUTE_IN_MILLIS
				);
				holder.tvDate.setText(rel);
			}
			
			final long   finalTimestamp = timestamp;
			final double finalAmount    = amount;
			final String finalAsset     = asset;
			
			// Click Listener bound directly to the properly declared cardview1
			if (holder.cardview1 != null) {
				holder.cardview1.setOnClickListener(v -> {
					Intent intent = new Intent(v.getContext(), TransactionDetailActivity.class);
					
					intent.putExtra("txHash",           String.valueOf(item.get("txHash")));
					intent.putExtra("status",           String.valueOf(item.get("status")));
					intent.putExtra("senderAddress",    sender);
					intent.putExtra("recipientAddress", recipient);
					intent.putExtra("tokenSymbol",      finalAsset);
					intent.putExtra("value",            finalAmount);
					intent.putExtra("timestamp",        finalTimestamp);
					
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
					if (burnObj instanceof Number)  intent.putExtra("burnAmount", ((Number) burnObj).doubleValue());
					if (blockObj instanceof Number) intent.putExtra("blockNumber", ((Number) blockObj).longValue());
					if (nonceObj instanceof Number) intent.putExtra("nonce", ((Number) nonceObj).longValue());
					if (chainName != null) {
						intent.putExtra("chain", String.valueOf(chainName));
						intent.putExtra("explorerUrl", String.valueOf(item.get("explorerUrl")));
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
