package com.harvana.app;

import com.harvana.app.utils.UiColors;
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
import com.harvana.app.wallet.WalletManager;
import java.net.URL;
import com.bumptech.glide.Glide;
import com.harvana.app.sync.FirestoreSyncManager;
import com.harvana.app.wallet.WalletNodeBalanceHelper;
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
	private RecyclerView recyclerview1;
	private LinearLayout chainAssets;
	private TextView tvNoWalletHint;
	
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
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		tvNoWalletHint = _view.findViewById(R.id.tv_no_wallet_hint);
		chainAssets = _view.findViewById(R.id.chain_assets);
		com.harvana.app.wallet.ChainAssetsSection.onTotal(chainAssets, sum -> {
			if (!isAdded()) return;
			androidx.fragment.app.Fragment parent = getParentFragment();
			if (parent instanceof AssetFragmentActivity) ((AssetFragmentActivity) parent).setChainFiat(sum);
		});
		walletsp = getContext().getSharedPreferences("walletsp", Activity.MODE_PRIVATE);
		String cachedFor = getContext().getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE).getString("active_wallet_address", "");
		java.util.List<HashMap<String, Object>> already = cachedFor.isEmpty() ? null
				: com.harvana.app.wallet.WalletCache.loadList(getContext(), "coins_" + cachedFor.trim());
		if (already == null || already.isEmpty()) {
			com.harvana.app.utils.Shimmer.hold(recyclerview1, com.harvana.app.utils.Shimmer.rows(recyclerview1.getContext(), 4), 4000, "WalletAssetFragmentActivity");
		}
		_setupSorted(_view);
		_setupSearch(_view);
		View addCoins = _view.findViewById(R.id.btn_add_coins);
		if (addCoins != null) addCoins.setOnClickListener(v -> com.harvana.app.wallet.ChainAssetsSection.showPicker(getContext(),
				() -> {
					com.harvana.app.wallet.ChainAssetsSection.render(getContext(), chainAssets, wallet_address);
					if (getView() != null) com.harvana.app.wallet.CustomAssets.render(getContext(), getView().findViewById(R.id.custom_assets), wallet_address);
					_renderStables();
				}));
		androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh = _view.findViewById(R.id.refresh_assets);
		if (refresh != null) {
			refresh.setColorSchemeResources(R.color.colorAccent);
			refresh.setProgressBackgroundColorSchemeResource(R.color.colorSurface);
			refresh.setOnRefreshListener(() -> {
				androidx.fragment.app.Fragment parent = getParentFragment();
				if (parent instanceof AssetFragmentActivity) ((AssetFragmentActivity) parent)._loadAvailableCoins();
				_loadAvailableCoins();
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
			if (tvNoWalletHint != null) tvNoWalletHint.setVisibility(View.VISIBLE);
			return;
		}
		
		wallet_address = address;
		userWalletAddress = address;
		activeAddress = address;
		if (tvNoWalletHint != null) tvNoWalletHint.setVisibility(View.GONE);
		_loadAvailableCoins();
	}
	
	public void repaintMoney() {
		com.harvana.app.wallet.ChainAssetsSection.repaint(chainAssets);
		_loadAvailableCoins();
	}

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
				userWalletAddress = freshAddress;
				activeAddress = freshAddress;
				if (tvNoWalletHint != null) tvNoWalletHint.setVisibility(View.GONE);
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
	/** Balance from node (available = spendable). No Firestore wallet sync. */
	private LinearLayout _husdBox() {
		return getView() == null ? null : getView().findViewById(R.id.husd_asset);
	}

	/** Every stable coin left on in Add coins; their value goes into the total. */
	private void _renderStables() {
		if (getContext() == null) return;
		com.harvana.app.husd.HusdRow.render(getContext(), _husdBox(), wallet_address, fiat -> {
			androidx.fragment.app.Fragment parent = getParentFragment();
			if (isAdded() && parent instanceof AssetFragmentActivity) ((AssetFragmentActivity) parent).setHusdFiat(fiat);
		});
	}

	private String assetQuery = "";
	/** HARV's row, drawn by the list adapter into a plain box so it can sit in the one sorted list (AssetSorter). */
	private LinearLayout harvBox;

	/** One list for every asset, largest holding first, with each coin's price (wallet/AssetSorter). */
	private void _setupSorted(View root) {
		if (recyclerview1 == null || !(recyclerview1.getParent() instanceof ViewGroup)) return;
		ViewGroup column = (ViewGroup) recyclerview1.getParent();
		harvBox = new LinearLayout(getContext());
		harvBox.setOrientation(LinearLayout.VERTICAL);
		column.addView(harvBox, column.indexOfChild(recyclerview1) + 1);
		recyclerview1.setVisibility(View.GONE);
		com.harvana.app.wallet.AssetSorter.attach(harvBox, root.findViewById(R.id.husd_asset), root.findViewById(R.id.chain_assets),
				root.findViewById(R.id.custom_assets));
	}

	/** Draws the adapter's rows (HARV) into harvBox with the adapter's own binding, so they look and act the same. */
	private void _drawHarvRows() {
		if (harvBox == null || recyclerview1 == null || recyclerview1.getAdapter() == null) return;
		Recyclerview1Adapter a = (Recyclerview1Adapter) recyclerview1.getAdapter();
		harvBox.removeAllViews();
		for (int i = 0; i < a.getItemCount(); i++) {
			Recyclerview1Adapter.ViewHolder h = a.onCreateViewHolder(harvBox, 0);
			a.onBindViewHolder(h, i);
			h.itemView.setTag("HARV");
			harvBox.addView(h.itemView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		if (!assetQuery.isEmpty()) _applySearch();
	}

	/**
	 * Search over every row on this page: HARV, the stable coins, the other coins and the ones added by hand. A row
	 * shows when its name or symbol contains the text. Rows arrive later as balances load, so the filter is applied
	 * again on every layout pass while a search is typed.
	 */
	private void _setupSearch(View root) {
		android.widget.EditText et = root.findViewById(R.id.et_asset_search);
		if (et == null) return;
		et.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(android.text.Editable s) {
				assetQuery = s.toString().trim().toLowerCase(java.util.Locale.US);
				_applySearch();
			}
		});
		if (linear1 != null) linear1.getViewTreeObserver().addOnGlobalLayoutListener(() -> { if (!assetQuery.isEmpty()) _applySearch(); });
	}

	private void _applySearch() {
		if (getView() == null) return;
		if (recyclerview1 != null) for (int i = 0; i < recyclerview1.getChildCount(); i++) _filterRow(recyclerview1.getChildAt(i));
		if (harvBox != null) for (int i = 0; i < harvBox.getChildCount(); i++) _filterRow(harvBox.getChildAt(i));
		for (int id : new int[]{R.id.husd_asset, R.id.chain_assets, R.id.custom_assets}) {
			ViewGroup box = getView().findViewById(id);
			if (box != null) for (int i = 0; i < box.getChildCount(); i++) _filterRow(box.getChildAt(i));
		}
	}

	private void _filterRow(View row) {
		TextView name = row.findViewById(R.id.tv_asset_name);
		TextView symbol = row.findViewById(R.id.tv_asset_symbol);
		if (name == null && symbol == null) return;
		String text = ((name == null ? "" : name.getText()) + " " + (symbol == null ? "" : symbol.getText())).toLowerCase(java.util.Locale.US);
		int want = assetQuery.isEmpty() || text.contains(assetQuery) ? View.VISIBLE : View.GONE;
		if (row.getVisibility() != want) row.setVisibility(want);
	}

	public void _loadAvailableCoins() {
		if (getContext() == null || wallet_address == null || wallet_address.trim().isEmpty()) {
			return;
		}
		final Context context = getContext();
		com.harvana.app.wallet.ChainAssetsSection.render(context, chainAssets, wallet_address);
		if (getView() != null) com.harvana.app.wallet.CustomAssets.render(context, getView().findViewById(R.id.custom_assets), wallet_address);
		_renderStables();
		
		java.util.List<HashMap<String, Object>> cachedCoins =
		com.harvana.app.wallet.WalletCache.loadList(context, "coins_" + wallet_address);
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
			String symbol = com.harvana.app.wallet.FxRates.symbol(getContext());
			com.harvana.app.wallet.FiatValueHelper.valueOfHarv(getContext(), show, symbol, (fiatValue, rateAvailable) -> {
				if (!isAdded() || getContext() == null) return;
				final ArrayList<HashMap<String, Object>> loadedCoins = new ArrayList<>();
				HashMap<String, Object> nativeCoin = new HashMap<>();
				
				// Removed the "#,##" so it no longer generates commas
				java.text.DecimalFormat df = new java.text.DecimalFormat("0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
				
				nativeCoin.put("coin_name", com.harvana.app.CoinInfo.name());
				nativeCoin.put("coin_symbol", com.harvana.app.CoinInfo.ticker());
				nativeCoin.put("coin_balance", df.format(show));
				nativeCoin.put("coin_balance_confirmed", df.format(bal));
				nativeCoin.put("coin_pending_in", df.format(pin));
				nativeCoin.put("coin_pending_out", df.format(pout));
				nativeCoin.put("coin_value", rateAvailable
				? String.format(Locale.US, "%.2f", fiatValue)
				: "0");
				nativeCoin.put("coin_currency", symbol);
				nativeCoin.put("coin_logo", "");
				nativeCoin.put("contractAddress", "");
				nativeCoin.put("isNative", true);
				nativeCoin.put("isListed", true);
				nativeCoin.put("isVerified", true);
				loadedCoins.add(nativeCoin);
				
				com.harvana.app.wallet.WalletCache.saveList(context, "coins_" + wallet_address, loadedCoins);
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
			_drawHarvRows();
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
			
			final View cardview1 = _view.findViewById(R.id.cardview1);
			final LinearLayout linear7 = _view.findViewById(R.id.linear7);
			final androidx.cardview.widget.CardView cardview_logo = _view.findViewById(R.id.cardview_logo);
			final LinearLayout ll_asset_details = _view.findViewById(R.id.ll_asset_details);
			final ImageView iv_asset_logo = _view.findViewById(R.id.iv_asset_logo);
			final TextView tv_asset_name = _view.findViewById(R.id.tv_asset_name);
			final LinearLayout linear6 = _view.findViewById(R.id.linear6);
			final TextView tv_asset_balance = _view.findViewById(R.id.tv_asset_balance);
			final TextView tv_asset_symbol = _view.findViewById(R.id.tv_asset_symbol);
			final TextView tv_asset_amount = _view.findViewById(R.id.tv_asset_amount);
			com.harvana.app.utils.BalancePrivacy.guard(tv_asset_balance);
			com.harvana.app.utils.BalancePrivacy.guard(tv_asset_amount);
			
			HashMap<String, Object> item = _data.get(_position);
			
			// Asset name
			if (tv_asset_name != null && item.get("coin_name") != null) {
				tv_asset_name.setText(item.get("coin_name").toString());
			}
			
			// FIX 2: Symbol and logo — guard against null coin_symbol.
			String symbol = item.get("coin_symbol") != null
			? item.get("coin_symbol").toString() : "";
			if (tv_asset_symbol != null) {
				tv_asset_symbol.setText(symbol);
			}
			
			// FIX 3: Check logo URL is non-null AND non-empty before Glide load.
			String logoUrl = item.containsKey("coin_logo") && item.get("coin_logo") != null
			? item.get("coin_logo").toString().trim() : "";
			if (iv_asset_logo != null) {
				if (!logoUrl.isEmpty()) {
					Glide.with(_view.getContext().getApplicationContext())
					.load(android.net.Uri.parse(logoUrl))
					.error(R.drawable.ic_coin_harv)
					.into(iv_asset_logo);
				} else {
					iv_asset_logo.setImageResource(R.drawable.ic_coin_harv);
				}
			}
			
			// FIX 4: coin_balance — clean any trailing coin text (e.g. " Pi") before parsing.
			if (tv_asset_balance != null && item.containsKey("coin_balance") && item.get("coin_balance") != null) {
				try {
					String rawBalance = item.get("coin_balance").toString();
					String cleanBalance = rawBalance.split(" ")[0];
					double bal = Double.parseDouble(cleanBalance);
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
			
			// FIX 5: coin_value — wrap Double.parseDouble() in try/catch AND use user's currency symbol.
			if (tv_asset_amount != null && item.containsKey("coin_value")) {
				Object savedCurrency = item.get("coin_currency");
				String currencySymbol = com.harvana.app.wallet.FxRates.symbol(_view.getContext());
				if (savedCurrency != null && !currencySymbol.equals(String.valueOf(savedCurrency))) {
					tv_asset_amount.setText("…");
				} else try {
					double val = Double.parseDouble(item.get("coin_value").toString());
					tv_asset_amount.setText(com.harvana.app.wallet.FxRates.format(currencySymbol, val));
				} catch (NumberFormatException e) {
					tv_asset_amount.setText(com.harvana.app.wallet.FxRates.format(currencySymbol, 0));
				}
			}
			
			if (cardview1 != null) {
				cardview1.setOnClickListener(_v -> {
					intent.putExtra("walletAddress", activeAddress);
					intent.setClass(_v.getContext().getApplicationContext(),
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
