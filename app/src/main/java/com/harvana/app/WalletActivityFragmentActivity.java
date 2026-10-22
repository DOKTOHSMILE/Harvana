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
		com.harvana.app.utils.Shimmer.hold(list_history, com.harvana.app.utils.Shimmer.rows(list_history.getContext(), 6), 25000, "WalletActivityFragmentActivity");   // until every source has answered
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
	
	@Override
	public void onPause() {
		super.onPause();
		pendingPoll.removeCallbacks(pollPending);
	}

	/** While a row is pending (a send, a swap, a group payment), the list asks again every 8 seconds until it settles. */
	private final android.os.Handler pendingPoll = new android.os.Handler(android.os.Looper.getMainLooper());
	private int pollTick;
	private final Runnable pollPending = () -> {
		if (!isResumed()) return;
		pollChain = ++pollTick % 4 == 0;                 // other chains' explorers every 32 seconds, the Harvana node every 8
		_loadTransactionHistory();
		pollChain = true;
	};
	private boolean pollChain = true;

	private void _pollWhilePending(java.util.List<java.util.HashMap<String, Object>> shown) {
		pendingPoll.removeCallbacks(pollPending);
		for (java.util.HashMap<String, Object> m : shown) {
			if ("pending".equalsIgnoreCase(String.valueOf(m.get("status")))) { pendingPoll.postDelayed(pollPending, 8_000L); return; }
		}
	}

	public void _MORE() {
	}

	private boolean _harvOnly() {
		return getArguments() != null && getArguments().getBoolean("harvOnly", false);
	}

	/** Every source (Harvana node, its pending pool, this phone's sends, other chains and added coins) comes through
	 *  one ActivityFeed, which publishes the whole sorted list once all have answered: the rows never jump. */
	private com.harvana.app.wallet.ActivityFeed feed;
	private java.util.List<java.util.HashMap<String, Object>> feedRows;
	private final com.harvana.app.wallet.ActivityFeed.Listener feedListener = (rows, fresh) -> {
		if (!isAdded() || getContext() == null) return;
		feedRows = rows;
		if (fresh && list_history != null) com.harvana.app.utils.Shimmer.release(list_history);
		_renderMergedTransactions();
	};


	public void _loadTransactionHistory() {
		if (wallet_address == null || wallet_address.trim().isEmpty()) return;
		if (getContext() == null) return;
		com.harvana.app.wallet.ActivityFeed f = com.harvana.app.wallet.ActivityFeed.of(wallet_address);
		if (feed != f) {
			if (feed != null) feed.unlisten(feedListener);
			feed = f;
			feed.listen(feedListener);
			feedRows = feed.cached(getContext());                // the last complete list, at once
			// A saved list is shown as it is; the placeholder is only for a wallet with nothing saved yet.
			if (feedRows != null && !feedRows.isEmpty() && list_history != null) com.harvana.app.utils.Shimmer.release(list_history);
		}
		_renderMergedTransactions();
		feed.refresh(getContext(), pollChain && !_harvOnly());
	}

	@Override
	public void onDestroyView() {
		if (feed != null) feed.unlisten(feedListener);
		super.onDestroyView();
	}

	private void _loadMoreNodeHistory() {
		if (feed != null) feed.loadMore(getContext());
	}

	/** Unified single rendering method to prevent UI flickering/glitching races */
	private void _renderMergedTransactions() {
		if (!isAdded() || getContext() == null || list_history == null) return;
		java.util.ArrayList<java.util.HashMap<String, Object>> display =
		new java.util.ArrayList<>(feedRows == null ? java.util.Collections.emptyList() : feedRows);
		if (_harvOnly()) {
			java.util.ArrayList<java.util.HashMap<String, Object>> harv = new java.util.ArrayList<>();
			for (java.util.HashMap<String, Object> m : display) if (m.get("chain") == null) harv.add(m);   // other chains' rows carry their chain
			display = harv;
		}
		
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
			list_history.setItemAnimator(null);                // no fade or slide when a row updates: the rows stay still
		}
		
		_pollWhilePending(filtered);
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

		/**
		 * Only rows that really changed are redrawn, and only when something changed at all: the list is refreshed
		 * from several sources (cache, node, pending, other chains) and every refresh used to redraw every row.
		 */
		public void updateItems(java.util.List<java.util.HashMap<String, Object>> newItems) {
			final java.util.List<java.util.HashMap<String, Object>> old = new java.util.ArrayList<>(items);
			final java.util.List<java.util.HashMap<String, Object>> next = newItems != null ? new java.util.ArrayList<>(newItems) : new java.util.ArrayList<>();
			androidx.recyclerview.widget.DiffUtil.DiffResult diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
				@Override public int getOldListSize() { return old.size(); }
				@Override public int getNewListSize() { return next.size(); }
				@Override public boolean areItemsTheSame(int o, int n) { return _rowId(old.get(o)).equals(_rowId(next.get(n))); }
				@Override public boolean areContentsTheSame(int o, int n) { return _rowLook(old.get(o)).equals(_rowLook(next.get(n))); }
			});
			items.clear();
			items.addAll(next);
			diff.dispatchUpdatesTo(this);
		}

		private String _rowId(java.util.Map<String, Object> m) {
			String h = String.valueOf(m.get("txHash"));
			return h.isEmpty() || "null".equals(h) ? "t" + m.get("timestamp") + "/" + m.get("value") : h + "/" + m.get("tokenSymbol");
		}

		private String _rowLook(java.util.Map<String, Object> m) {
			return m.get("status") + "|" + m.get("value") + "|" + m.get("timestamp") + "|" + m.get("initiatedAt") + "|" + m.get("memo")
					+ "|" + m.get("tokenSymbol") + "|" + m.get("incoming") + "|" + m.get("senderAddress") + "|" + m.get("recipientAddress") + "|" + m.get("chain");
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
			// Show the divider between rows; hide it under the last row so the list does not end with a line.
			if (holder.txDivider != null) holder.txDivider.setVisibility(position == items.size() - 1 ? View.GONE : View.VISIBLE);
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
					// One icon style on every row (owner, 19 Oct 2026): the app's duotone line glyph, 24dp, in the row's
					// colour. Coin logos (a full-colour coin, the Swap app icon) are no longer mixed in.
					if (!isPending && holder.ivIcon != null && !_isCoinLogo(kind.icon)) holder.ivIcon.setImageResource(kind.icon);
				}
			}
			
			// A reused row must show its icon at once (a drawable changed off screen could stay blank).
			if (holder.ivIcon != null) holder.ivIcon.jumpDrawablesToCurrentState();
			final Object chainName = item.get("chain");
			if (chainName != null && holder.tvAddress != null) {
				holder.tvAddress.setText(chainName + " · " + holder.tvAddress.getText());
			}
			
			if (holder.tvDate != null) {
				holder.tvDate.setText(_activityWhen(isPending, _longOf(item, "initiatedAt"), timestamp));
			}
			if (holder.tvDay != null) {
				String day = _dayLabel(timestamp);
				String prev = position > 0 ? _dayLabel(_txMillis(items.get(position - 1))) : "";
				boolean show = !day.isEmpty() && !day.equals(prev);
				holder.tvDay.setVisibility(show ? View.VISIBLE : View.GONE);
				if (show) holder.tvDay.setText(day);
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
		
		private long _txMillis(java.util.Map<String, Object> item) {
			long initiated = _longOf(item, "initiatedAt");
			if (initiated > 0) return initiated;
			Object tsObj = item.get("timestamp");
			long timestamp = tsObj instanceof Number ? ((Number) tsObj).longValue() : 0L;
			if (timestamp > 0 && timestamp < 1_000_000_000_000L) timestamp *= 1000L;
			return timestamp;
		}

		private String _dayLabel(long ms) {
			if (ms <= 0) return "";
			java.util.Calendar day = java.util.Calendar.getInstance();
			day.setTimeInMillis(ms);
			java.util.Calendar today = java.util.Calendar.getInstance();
			if (sameDay(day, today)) return "Today";
			today.add(java.util.Calendar.DAY_OF_YEAR, -1);
			if (sameDay(day, today)) return "Yesterday";
			return new java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.US).format(new java.util.Date(ms));
		}

		private boolean sameDay(java.util.Calendar a, java.util.Calendar b) {
			return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR)
					&& a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR);
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
			TextView  tvDay;
			View cardview1;
			View txDivider;

			public TxViewHolder(@NonNull View itemView) {
				super(itemView);
				ivIcon    = itemView.findViewById(R.id.iv_tx_icon);
				tvType    = itemView.findViewById(R.id.tv_tx_type);
				tvAddress = itemView.findViewById(R.id.tv_tx_address);
				tvAmount  = itemView.findViewById(R.id.tv_tx_amount);
				tvDate    = itemView.findViewById(R.id.tv_tx_date);
				tvDay     = itemView.findViewById(R.id.tv_tx_day);
				cardview1 = itemView.findViewById(R.id.cardview1);
				txDivider = itemView.findViewById(R.id.tx_divider);
			}
		}
	}
	
	/** A line icon in the row's colour: 20dp inside its 40dp circle (rows are reused, so every row sets this again). */
	private static void _lineIcon(android.widget.ImageView iv) {
		int p = Math.round(10 * iv.getResources().getDisplayMetrics().density);
		iv.setPadding(p, p, p, p);
		iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
		iv.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(iv.getContext(), R.color.colorAccent)));
		com.harvana.app.tx.TxIcon.circle(iv);
	}

	private static boolean _isCoinLogo(int res) {
		return res == R.drawable.ic_coin_husd || res == R.drawable.ic_coin_stable || res == R.drawable.ic_app_swap;
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
