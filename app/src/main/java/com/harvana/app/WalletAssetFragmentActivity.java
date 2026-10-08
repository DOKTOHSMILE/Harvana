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
		walletsp = getContext().getSharedPreferences("walletsp", Activity.MODE_PRIVATE);
		String cachedFor = getContext().getSharedPreferences("harv_wallet_prefs", Context.MODE_PRIVATE).getString("active_wallet_address", "");
		java.util.List<HashMap<String, Object>> already = cachedFor.isEmpty() ? null
				: com.harvana.app.wallet.WalletCache.loadList(getContext(), "coins_" + cachedFor.trim());
		if (already == null || already.isEmpty()) {
			com.harvana.app.utils.Shimmer.hold(recyclerview1, com.harvana.app.utils.Shimmer.rows(recyclerview1.getContext(), 4), 4000, "WalletAssetFragmentActivity");
		}
		View addCoins = _view.findViewById(R.id.btn_add_coins);
		if (addCoins != null) addCoins.setOnClickListener(v -> com.harvana.app.wallet.ChainAssetsSection.showPicker(getContext(),
				() -> com.harvana.app.wallet.ChainAssetsSection.render(getContext(), chainAssets, wallet_address)));
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
	public void _loadAvailableCoins() {
		if (getContext() == null || wallet_address == null || wallet_address.trim().isEmpty()) {
			return;
		}
		final Context context = getContext();
		com.harvana.app.wallet.ChainAssetsSection.render(context, chainAssets, wallet_address);
		
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
			SharedPreferences currencySp = getContext().getSharedPreferences("currencysp", Context.MODE_PRIVATE);
			String currencyCode = com.harvana.app.wallet.FiatValueHelper.symbolToCurrencyCode(
			currencySp.getString("currency", "₦"));
			
			com.harvana.app.wallet.FiatValueHelper.convertHarvToFiat(show, currencyCode, (fiatValue, rateAvailable) -> {
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
			
			final com.google.android.material.card.MaterialCardView cardview1 = _view.findViewById(R.id.cardview1);
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
			// Original called .toString().equals("") without null check → NPE
			// if the key exists but value is null.
			String symbol = item.get("coin_symbol") != null
			? item.get("coin_symbol").toString() : "";
			if (tv_asset_symbol != null) {
				tv_asset_symbol.setText(symbol);
			}
			
			// FIX 3: Check logo URL is non-null AND non-empty before Glide load.
			// The original checked containsKey("coin_logo") but not whether the
			// value was an empty string. Glide.load("") logs an error, fails
			// silently, and leaves the ImageView blank instead of showing the
			// default logo. Now falls back to the app logo for empty/null URLs.
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
			
			// FIX 4: coin_balance — wrap Double.parseDouble() in try/catch.
			// The original crashed with NumberFormatException if coin_balance was
			// null, missing, or in scientific notation (e.g. "1.5E-7").
			// Also removed the redundant (int) cast on _position — it is already int.
			if (tv_asset_balance != null && item.containsKey("coin_balance")) {
				try {
					double bal = Double.parseDouble(item.get("coin_balance").toString());
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
			
			// FIX 5: coin_value — wrap Double.parseDouble() in try/catch AND use
			// the user's selected currency symbol from SharedPreferences instead of
			// the hardcoded "₦". The original hardcode meant users who selected "$"
			// or "GH₵" still saw "₦" on every balance row, and a non-numeric
			// coin_value string would crash onBindViewHolder with NumberFormatException.
			if (tv_asset_amount != null && item.containsKey("coin_value")) {
				try {
					SharedPreferences currencySp = _view.getContext().getSharedPreferences("currencysp", Context.MODE_PRIVATE);
					String currencySymbol = currencySp.getString("currency", "₦");
					
					double val = Double.parseDouble(item.get("coin_value").toString());
					String formatted = val > 999
					? new java.text.DecimalFormat("#,##0.##").format(val)
					: new java.text.DecimalFormat("0.##").format(val);
					tv_asset_amount.setText(currencySymbol + formatted);
				} catch (NumberFormatException e) {
					tv_asset_amount.setText("₦0.00");
				}
			}
			
			if (cardview1 != null) {
				cardview1.setOnClickListener(_v -> {
					intent.putExtra("walletAddress", activeAddress);
					intent.setClass(_view.getContext().getApplicationContext(),
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
