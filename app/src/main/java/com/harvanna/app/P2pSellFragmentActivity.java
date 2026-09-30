package com.harvanna.app;

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
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import de.hdodenhof.circleimageview.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import com.harvanna.app.firestore.P2PPaymentDetailsManager;
import com.harvanna.app.firestore.P2PTradeStatsManager;
import com.harvanna.app.firestore.UserProfileManager;
import com.harvanna.app.sync.HarvannaNodeClient;
import com.harvanna.app.wallet.BalanceFormatter;
import com.harvanna.app.wallet.WalletManager;
import com.google.firebase.auth.FirebaseAuth;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.harvanna.app.p2p.P2pCurrencyFilterState;
import com.harvanna.app.p2p.P2pMoney;
import com.harvanna.app.p2p.ThemedDialogs;
import androidx.core.widget.CompoundButtonCompat;


public class P2pSellFragmentActivity extends Fragment {
	
	private LinearLayout linear1;
	private LinearLayout linear2;
	private RecyclerView recyclerview_fragment;
	private LinearLayout layout_fragment_empty;
	private MaterialCardView cardview_select_coin_btn;
	private MaterialCardView cardview_enter_quantity_btn;
	private MaterialCardView cardview_select_payment_btn;
	private Space space6;
	private ImageView imageview_filter_btn;
	private LinearLayout linear3;
	private CircleImageView circleimageview1;
	private MaterialTextView textview1;
	private ImageView imageview1;
	private LinearLayout linear4;
	private MaterialTextView textview2;
	private ImageView imageview2;
	private LinearLayout linear5;
	private MaterialTextView textview3;
	private ImageView imageview3;
	private ImageView imageview7;
	private MaterialTextView tv_fragment_empty_title;
	private MaterialTextView tv_fragment_empty_subtitle;
	private MaterialButton btn_fragment_empty_cta;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.p2p_sell_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		_main = _view.findViewById(R.id._main);
		linear1 = _view.findViewById(R.id.linear1);
		linear2 = _view.findViewById(R.id.linear2);
		recyclerview_fragment = _view.findViewById(R.id.recyclerview_fragment);
		layout_fragment_empty = _view.findViewById(R.id.layout_fragment_empty);
		cardview_select_coin_btn = _view.findViewById(R.id.cardview_select_coin_btn);
		cardview_enter_quantity_btn = _view.findViewById(R.id.cardview_enter_quantity_btn);
		cardview_select_payment_btn = _view.findViewById(R.id.cardview_select_payment_btn);
		space6 = _view.findViewById(R.id.space6);
		imageview_filter_btn = _view.findViewById(R.id.imageview_filter_btn);
		linear3 = _view.findViewById(R.id.linear3);
		circleimageview1 = _view.findViewById(R.id.circleimageview1);
		textview1 = _view.findViewById(R.id.textview1);
		imageview1 = _view.findViewById(R.id.imageview1);
		linear4 = _view.findViewById(R.id.linear4);
		textview2 = _view.findViewById(R.id.textview2);
		imageview2 = _view.findViewById(R.id.imageview2);
		linear5 = _view.findViewById(R.id.linear5);
		textview3 = _view.findViewById(R.id.textview3);
		imageview3 = _view.findViewById(R.id.imageview3);
		imageview7 = _view.findViewById(R.id.imageview7);
		tv_fragment_empty_title = _view.findViewById(R.id.tv_fragment_empty_title);
		tv_fragment_empty_subtitle = _view.findViewById(R.id.tv_fragment_empty_subtitle);
		btn_fragment_empty_cta = _view.findViewById(R.id.btn_fragment_empty_cta);
	}
	
	private void initializeLogic() {
		myAddress = WalletManager.getActiveWalletAddress(requireContext());
		
		adapter = new RowAdapter(visible);
		recyclerview_fragment.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerview_fragment.setAdapter(adapter);
		
		tv_fragment_empty_title.setText("No buyers right now");
		tv_fragment_empty_subtitle.setText("Check back soon, or post a Sell Offer and let a buyer come to you.");
		btn_fragment_empty_cta.setVisibility(View.VISIBLE);
		btn_fragment_empty_cta.setOnClickListener(v ->
		startActivity(new Intent(requireContext(), CreateP2pSellOfferActivity.class)));
		if (textview1 != null) textview1.setText(com.harvanna.app.CoinInfo.ticker());
		
		_main.setOnRefreshListener(this::_loadListings);
		
		cardview_select_coin_btn.setOnClickListener(v -> _showCoinSheet());
		cardview_enter_quantity_btn.setOnClickListener(v -> _showAmountSheet());
		cardview_select_payment_btn.setOnClickListener(v -> _showPaymentSheet());
		imageview_filter_btn.setOnClickListener(v -> _showFilterSheet());
		
		_loadListings();
		
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		P2pCurrencyFilterState.addListener(currencyListener);
		_loadListings();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		P2pCurrencyFilterState.removeListener(currencyListener);
	}
	public void _MORE() {
	}
	private SwipeRefreshLayout _main;
	private final P2pCurrencyFilterState.Listener currencyListener = code -> _applyFilters();
	
	private static class EnrichedListing {
		JSONObject raw;
		String listingId = "";
		String posterAddress = "";
		String posterUid = "";
		double remainingHarv, minOrderHarv, unitPrice = -1;
		Set<String> paymentLabels = new LinkedHashSet<>();
		String displayName = "Trader";
		String currency = "NGN";
		String iconUrl = "";
		boolean online = false;
		boolean verified = false;
		boolean insured = false;
		boolean premium = false;
		int trustScore = -1;
		int completedTrades = 0;
		int uniqueCounterparties = 0;
	}
	
	private final List<EnrichedListing> allEnriched = new ArrayList<>();
	private final List<EnrichedListing> visible = new ArrayList<>();
	private RowAdapter adapter;
	private String myAddress = "";
	
	private Double targetAmount = null;
	private final Set<String> selectedPaymentLabels = new LinkedHashSet<>();
	private boolean verifiedOnly = false;
	private boolean insuredOnly = false;
	private boolean premiumOnly = false;
	private int sortMode = 0;
	
	private void _loadListings() {
		HarvannaNodeClient.getP2POpenListings(0, 50, "BUY", new HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject result) {
				List<JSONObject> raws = new ArrayList<>();
				JSONArray arr = result.optJSONArray("listings");
				if (arr != null) {
					for (int i = 0; i < arr.length(); i++) {
						JSONObject l = arr.optJSONObject(i);
						if (l != null) raws.add(l);
					}
				}
				if (raws.isEmpty()) { _finishLoad(new ArrayList<>()); return; }
				List<EnrichedListing> collected = Collections.synchronizedList(new ArrayList<>());
				java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(raws.size());
				for (JSONObject raw : raws) {
					_enrichOne(raw, e -> {
						collected.add(e);
						if (remaining.decrementAndGet() == 0) _finishLoad(new ArrayList<>(collected));
					});
				}
			}
			@Override
			public void onError(String error) {
				if (!isAdded()) return;
				requireActivity().runOnUiThread(() -> {
					_main.setRefreshing(false);
					tv_fragment_empty_title.setText("Couldn't load the market");
					tv_fragment_empty_subtitle.setText(error);
					recyclerview_fragment.setVisibility(View.GONE);
					layout_fragment_empty.setVisibility(View.VISIBLE);
				});
			}
		});
	}
	
	private interface EnrichCallback { void onDone(EnrichedListing e); }
	
	private void _enrichOne(JSONObject raw, EnrichCallback done) {
		EnrichedListing e = new EnrichedListing();
		e.raw = raw;
		e.listingId = raw.optString("listingId", "");
		// This is always a BUY listing here, so the poster is the buyer.
		e.posterAddress = raw.optString("buyerAddress", raw.optString("sellerAddress", ""));
		e.remainingHarv = HarvannaNodeClient.toHarvDisplay(raw.optLong("remainingAmount", 0));
		e.minOrderHarv = HarvannaNodeClient.toHarvDisplay(raw.optLong("minOrderAmount", 0));
		
		if (e.listingId.isEmpty() || e.posterAddress.isEmpty()) { done.onDone(e); return; }
		
		P2PPaymentDetailsManager.getInstance(getContext().getApplicationContext())
		.get(e.listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object priceObj = data.get("unitPrice");
				e.unitPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
				Object currencyObj = data.get("currency");
				if (currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()) e.currency = String.valueOf(currencyObj).trim();
				// On a BUY listing, the person who posted it (wanting to
				// buy HARV) is stored under sellerUid in this collection —
				// same field name reused for "the poster's uid" on both
				// listing types, since P2P_PAYMENT_DETAILS was designed
				// around SELL listings first. Real data, just a field
				// name that no longer matches its meaning on this side.
				Object posterUidObj = data.get("sellerUid");
				e.posterUid = posterUidObj != null ? String.valueOf(posterUidObj) : "";
				Object methodsObj = data.get("paymentMethods");
				if (methodsObj instanceof List) {
					for (Object m : (List<?>) methodsObj) {
						if (!(m instanceof Map)) continue;
						Object primary = ((Map<?, ?>) m).get("primaryField");
						if (primary != null && !String.valueOf(primary).trim().isEmpty()) {
							e.paymentLabels.add(String.valueOf(primary).trim());
						}
					}
				}
				
				if (e.posterUid.isEmpty()) { done.onDone(e); return; }
				
				java.util.concurrent.atomic.AtomicInteger innerRemaining = new java.util.concurrent.atomic.AtomicInteger(2);
				Runnable maybeDone = () -> { if (innerRemaining.decrementAndGet() == 0) done.onDone(e); };
				
				UserProfileManager.getInstance(getContext().getApplicationContext())
				.getProfile(e.posterUid, new UserProfileManager.DocCallback() {
					@Override
					public void onSuccess(Map<String, Object> profile) {
						Object first = profile.get("first_name");
						Object last = profile.get("last_name");
						String firstStr = first != null ? String.valueOf(first).trim() : "";
						String lastStr = last != null ? String.valueOf(last).trim() : "";
						e.displayName = !firstStr.isEmpty()
						? (lastStr.isEmpty() ? firstStr : firstStr + " " + lastStr.substring(0, 1).toUpperCase(Locale.US) + ".")
						: "Trader";
						Object iconObj = profile.get("icon");
						e.iconUrl = iconObj != null ? String.valueOf(iconObj) : "";
						e.online = UserProfileManager.isRecentlyActive(profile);
						maybeDone.run();
					}
					@Override
					public void onError(String error) { maybeDone.run(); }
				});
				
				com.harvanna.app.TrustScoreEngine.compute(getContext().getApplicationContext(), e.posterUid,
				new com.harvanna.app.TrustScoreEngine.Callback() {
					@Override
					public void onComplete(com.harvanna.app.TrustScoreEngine.Result result) {
						e.verified = result.verified;
						e.trustScore = result.overallScore;
						int reportCount = result.reportCount;
						
						HarvannaNodeClient.getP2PInsuranceStatus(e.posterAddress, new HarvannaNodeClient.JsonCallback() {
							@Override
							public void onSuccess(JSONObject insuranceJson) {
								boolean meetsDeposit = insuranceJson.optBoolean("meetsInsuranceThreshold", false);
								e.insured = meetsDeposit && e.verified && reportCount == 0;
								
								P2PTradeStatsManager.getInstance(getContext().getApplicationContext())
								.getFullStats(e.posterUid, new P2PTradeStatsManager.FullStatsCallback() {
									@Override
									public void onSuccess(int totalTrades, int successfulTrades, double successRatePercent, int uniqueCounterparties) {
										e.completedTrades = totalTrades;
										e.uniqueCounterparties = uniqueCounterparties;
										e.premium = e.insured && totalTrades >= 100;
										maybeDone.run();
									}
									@Override
									public void onError(String error) { maybeDone.run(); }
								});
							}
							@Override
							public void onError(String error) { maybeDone.run(); }
						});
					}
					@Override
					public void onError(String error) { maybeDone.run(); }
				});
			}
			@Override
			public void onError(String error) { done.onDone(e); }
		});
	}
	
	private void _finishLoad(List<EnrichedListing> results) {
		if (!isAdded()) return;
		requireActivity().runOnUiThread(() -> {
			_main.setRefreshing(false);
			allEnriched.clear();
			allEnriched.addAll(results);
			_applyFilters();
		});
	}
	
	private void _applyFilters() {
		visible.clear();
		for (EnrichedListing e : allEnriched) {
			if (myAddress != null && !myAddress.isEmpty() && myAddress.equals(e.posterAddress)) continue;
			if (e.unitPrice < 0) continue;
			if (!e.currency.equalsIgnoreCase(P2pCurrencyFilterState.getCurrency())) continue;
			if (targetAmount != null && !(e.minOrderHarv <= targetAmount && targetAmount <= e.remainingHarv)) continue;
			if (!selectedPaymentLabels.isEmpty() && Collections.disjoint(e.paymentLabels, selectedPaymentLabels)) continue;
			if (verifiedOnly && !e.verified) continue;
			if (insuredOnly && !e.insured) continue;
			if (premiumOnly && !e.premium) continue;
			visible.add(e);
		}
		if (sortMode == 1) visible.sort((a, b) -> Double.compare(a.unitPrice, b.unitPrice));
		else if (sortMode == 2) visible.sort((a, b) -> Double.compare(b.unitPrice, a.unitPrice));
		
		adapter.notifyDataSetChanged();
		boolean empty = visible.isEmpty();
		recyclerview_fragment.setVisibility(empty ? View.GONE : View.VISIBLE);
		layout_fragment_empty.setVisibility(empty ? View.VISIBLE : View.GONE);
		if (empty) {
			boolean anyFilterActive = targetAmount != null || !selectedPaymentLabels.isEmpty() || verifiedOnly || insuredOnly || premiumOnly;
			tv_fragment_empty_title.setText(anyFilterActive ? "No matches for these filters" : "No buyers right now");
			tv_fragment_empty_subtitle.setText(anyFilterActive
			? "Try clearing a filter to see more listings."
			: "Check back soon, or post a Sell Offer and let a buyer come to you.");
		}
		_refreshFilterLabels();
	}
	
	private void _refreshFilterLabels() {
		textview2.setText(targetAmount != null ? BalanceFormatter.smartDecimal(targetAmount) : "Quantity");
		textview3.setText(selectedPaymentLabels.isEmpty() ? "Payment" : "Payment (" + selectedPaymentLabels.size() + ")");
		int activeFilterCount = (verifiedOnly ? 1 : 0) + (insuredOnly ? 1 : 0) + (premiumOnly ? 1 : 0) + (sortMode != 0 ? 1 : 0);
		imageview_filter_btn.setAlpha(activeFilterCount > 0 ? 1f : 0.6f);
	}
	
	private void _showCoinSheet() {
		BottomSheetDialog dialog = ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("Select Coin"));
		root.addView(_sheetLabel("P2P Supported Coins"));
		
		int colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
		int colorOnSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
		int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.GREEN);
		
		LinearLayout row = _sheetRow();
		MaterialTextView label = new MaterialTextView(requireContext());
		label.setText((com.harvanna.app.CoinInfo.ticker() + " — Harvanna Protocol"));
		label.setTextColor(colorOnSurface);
		label.setTextSize(15f);
		MaterialTextView check = new MaterialTextView(requireContext());
		check.setText("✓");
		check.setTextColor(colorPrimary);
		check.setTextSize(18f);
		row.addView(label);
		row.addView(_spacer());
		row.addView(check);
		row.setOnClickListener(v -> {
			if (textview1 != null) textview1.setText(com.harvanna.app.CoinInfo.ticker());
			dialog.dismiss();
		});
		root.addView(row);
		
		MaterialTextView note = new MaterialTextView(requireContext());
		note.setText("More assets will appear here once this system supports them — nothing else is real yet.");
		note.setTextColor(colorOnSurfaceVariant);
		note.setTextSize(12f);
		note.setPadding(0, 24, 0, 0);
		root.addView(note);
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	private void _showAmountSheet() {
		BottomSheetDialog dialog = ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("I Want to Sell"));
		
		int colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
		int colorOnSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
		int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		int colorOnPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
		
		EditText input = new EditText(requireContext());
		input.setHint(("Enter " + com.harvanna.app.CoinInfo.ticker() + " amount"));
		input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
		input.setTextColor(colorOnSurface);
		input.setHintTextColor(colorOnSurfaceVariant);
		if (targetAmount != null) input.setText(BalanceFormatter.smartDecimal(targetAmount));
		root.addView(input);
		
		LinearLayout buttonRow = _sheetButtonRow();
		MaterialButton reset = _outlineButton("Reset");
		MaterialButton confirm = _filledButton("Confirm");
		reset.setTextColor(colorPrimary);
		reset.setStrokeColor(android.content.res.ColorStateList.valueOf(colorPrimary));
		confirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
		confirm.setTextColor(colorOnPrimary);
		reset.setOnClickListener(v -> input.setText(""));
		confirm.setOnClickListener(v -> {
			double amount = BalanceFormatter.parseUserInput(input.getText().toString());
			targetAmount = amount > 0 ? amount : null;
			_applyFilters();
			dialog.dismiss();
		});
		buttonRow.addView(reset);
		buttonRow.addView(confirm);
		root.addView(buttonRow);
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	private void _showPaymentSheet() {
		Set<String> availableLabels = new LinkedHashSet<>();
		for (EnrichedListing e : allEnriched) availableLabels.addAll(e.paymentLabels);
		
		BottomSheetDialog dialog = ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("Receive Via"));
		
		int colorOnSurface2 = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
		int colorOnSurfaceVariant2 = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
		int colorPrimary2 = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		int colorOnPrimary2 = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
		
		EditText search = new EditText(requireContext());
		search.setHint("Search");
		search.setTextColor(colorOnSurface2);
		search.setHintTextColor(colorOnSurfaceVariant2);
		root.addView(search);
		
		LinearLayout listContainer = new LinearLayout(requireContext());
		listContainer.setOrientation(LinearLayout.VERTICAL);
		root.addView(listContainer);
		
		final Runnable[] rebuildHolder = new Runnable[1];
		rebuildHolder[0] = () -> {
			String query = search.getText().toString().trim().toLowerCase(Locale.US);
			listContainer.removeAllViews();
			
			LinearLayout allRow = _sheetRow();
			MaterialTextView allLabel = new MaterialTextView(requireContext());
			allLabel.setText("All Payment Methods");
			allLabel.setTextColor(colorOnSurface2);
			CheckBox allCheck = new CheckBox(requireContext());
			CompoundButtonCompat.setButtonTintList(allCheck, android.content.res.ColorStateList.valueOf(colorPrimary2));
			allCheck.setChecked(selectedPaymentLabels.isEmpty());
			allCheck.setOnClickListener(v -> { selectedPaymentLabels.clear(); rebuildHolder[0].run(); });
			allRow.addView(allLabel);
			allRow.addView(_spacer());
			allRow.addView(allCheck);
			listContainer.addView(allRow);
			
			if (availableLabels.isEmpty()) {
				MaterialTextView none = new MaterialTextView(requireContext());
				none.setText("No payment methods on any listing right now.");
				none.setTextColor(colorOnSurfaceVariant2);
				none.setPadding(0, 24, 0, 0);
				listContainer.addView(none);
				return;
			}
			
			for (String label : availableLabels) {
				if (!query.isEmpty() && !label.toLowerCase(Locale.US).contains(query)) continue;
				LinearLayout row = _sheetRow();
				MaterialTextView tv = new MaterialTextView(requireContext());
				tv.setText(label);
				tv.setTextColor(colorOnSurface2);
				CheckBox cb = new CheckBox(requireContext());
				CompoundButtonCompat.setButtonTintList(cb, android.content.res.ColorStateList.valueOf(colorPrimary2));
				cb.setChecked(selectedPaymentLabels.contains(label));
				cb.setOnClickListener(v -> {
					if (cb.isChecked()) selectedPaymentLabels.add(label); else selectedPaymentLabels.remove(label);
					rebuildHolder[0].run();
				});
				row.addView(tv);
				row.addView(_spacer());
				row.addView(cb);
				listContainer.addView(row);
			}
		};
		search.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
			@Override public void afterTextChanged(Editable s) { rebuildHolder[0].run(); }
		});
		rebuildHolder[0].run();
		
		LinearLayout buttonRow = _sheetButtonRow();
		MaterialButton reset = _outlineButton("Reset");
		MaterialButton confirm = _filledButton("Confirm");
		reset.setTextColor(colorPrimary2);
		reset.setStrokeColor(android.content.res.ColorStateList.valueOf(colorPrimary2));
		confirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimary2));
		confirm.setTextColor(colorOnPrimary2);
		reset.setOnClickListener(v -> { selectedPaymentLabels.clear(); rebuildHolder[0].run(); });
		confirm.setOnClickListener(v -> { _applyFilters(); dialog.dismiss(); });
		buttonRow.addView(reset);
		buttonRow.addView(confirm);
		root.addView(buttonRow);
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	private void _showFilterSheet() {
		BottomSheetDialog dialog = ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("Filter"));
		
		CheckBox verifiedBox = _sheetToggleRow(root, "Verified buyers only", verifiedOnly);
		CheckBox insuredBox = _sheetToggleRow(root, "Insured Traders only", insuredOnly);
		CheckBox premiumBox = _sheetToggleRow(root, "Premium Traders only", premiumOnly);
		
		root.addView(_sheetLabel("Sort by price"));
		int colorOnSurface3 = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
		int colorPrimary3 = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		int colorOnPrimary3 = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
		
		RadioGroup sortGroup = new RadioGroup(requireContext());
		sortGroup.setOrientation(RadioGroup.VERTICAL);
		RadioButton none = new RadioButton(requireContext());
		none.setText("Default order");
		none.setTextColor(colorOnSurface3);
		CompoundButtonCompat.setButtonTintList(none, android.content.res.ColorStateList.valueOf(colorPrimary3));
		RadioButton lowHigh = new RadioButton(requireContext());
		lowHigh.setText("Price: low to high");
		lowHigh.setTextColor(colorOnSurface3);
		CompoundButtonCompat.setButtonTintList(lowHigh, android.content.res.ColorStateList.valueOf(colorPrimary3));
		RadioButton highLow = new RadioButton(requireContext());
		highLow.setText("Price: high to low");
		highLow.setTextColor(colorOnSurface3);
		CompoundButtonCompat.setButtonTintList(highLow, android.content.res.ColorStateList.valueOf(colorPrimary3));
		sortGroup.addView(none);
		sortGroup.addView(lowHigh);
		sortGroup.addView(highLow);
		if (sortMode == 1) lowHigh.setChecked(true); else if (sortMode == 2) highLow.setChecked(true); else none.setChecked(true);
		root.addView(sortGroup);
		
		LinearLayout buttonRow = _sheetButtonRow();
		MaterialButton reset = _outlineButton("Reset");
		MaterialButton confirm = _filledButton("Confirm");
		reset.setTextColor(colorPrimary3);
		reset.setStrokeColor(android.content.res.ColorStateList.valueOf(colorPrimary3));
		confirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimary3));
		confirm.setTextColor(colorOnPrimary3);
		reset.setOnClickListener(v -> {
			verifiedBox.setChecked(false);
			insuredBox.setChecked(false);
			premiumBox.setChecked(false);
			none.setChecked(true);
		});
		confirm.setOnClickListener(v -> {
			verifiedOnly = verifiedBox.isChecked();
			insuredOnly = insuredBox.isChecked();
			premiumOnly = premiumBox.isChecked();
			sortMode = lowHigh.isChecked() ? 1 : (highLow.isChecked() ? 2 : 0);
			_applyFilters();
			dialog.dismiss();
		});
		buttonRow.addView(reset);
		buttonRow.addView(confirm);
		root.addView(buttonRow);
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	// GROUND-UP FIX (Sept 17 2026): every bottom sheet built here previously
	// had NO explicit background at all, so it inherited whatever the
	// Material bottom sheet default is, while text colors were hardcoded —
	// on some themes that's white-on-white, invisible. Both background and
	// text now resolve from the SAME live theme attributes, so they can
	// never mismatch regardless of what the BottomSheetDialog's own
	// implicit wrapper theme happens to pick.
	private LinearLayout _sheetRoot() {
		LinearLayout root = new LinearLayout(requireContext());
		root.setOrientation(LinearLayout.VERTICAL);
		int pad = (int) (20 * getResources().getDisplayMetrics().density);
		root.setPadding(pad, pad, pad, pad);
		int colorSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurface, Color.DKGRAY);
		float radius = 20f * getResources().getDisplayMetrics().density;
		android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
		bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
		bg.setColor(colorSurface);
		bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
		root.setBackground(bg);
		return root;
	}
	
	private MaterialTextView _sheetTitle(String text) {
		MaterialTextView tv = new MaterialTextView(requireContext());
		tv.setText(text);
		tv.setTextColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.WHITE));
		tv.setTextSize(18f);
		tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
		tv.setPadding(0, 0, 0, 20);
		return tv;
	}
	
	private MaterialTextView _sheetLabel(String text) {
		MaterialTextView tv = new MaterialTextView(requireContext());
		tv.setText(text);
		tv.setTextColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY));
		tv.setTextSize(12f);
		tv.setPadding(0, 20, 0, 8);
		return tv;
	}
	
	private LinearLayout _sheetRow() {
		LinearLayout row = new LinearLayout(requireContext());
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(0, 16, 0, 16);
		return row;
	}
	
	private CheckBox _sheetToggleRow(LinearLayout root, String label, boolean initial) {
		LinearLayout row = _sheetRow();
		MaterialTextView tv = new MaterialTextView(requireContext());
		tv.setText(label);
		int colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
		int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		tv.setTextColor(colorOnSurface);
		CheckBox cb = new CheckBox(requireContext());
		androidx.core.widget.CompoundButtonCompat.setButtonTintList(cb, android.content.res.ColorStateList.valueOf(colorPrimary));
		cb.setChecked(initial);
		row.addView(tv);
		row.addView(_spacer());
		row.addView(cb);
		root.addView(row);
		return cb;
	}
	
	private View _spacer() {
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0, 1f);
		View v = new View(requireContext());
		v.setLayoutParams(lp);
		return v;
	}
	
	private LinearLayout _sheetButtonRow() {
		LinearLayout row = new LinearLayout(requireContext());
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setPadding(0, 24, 0, 0);
		row.setWeightSum(2f);
		return row;
	}
	
	private MaterialButton _outlineButton(String text) {
		MaterialButton b = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
		b.setText(text);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		lp.setMarginEnd(12);
		b.setLayoutParams(lp);
		return b;
	}
	
	private MaterialButton _filledButton(String text) {
		MaterialButton b = new MaterialButton(requireContext());
		b.setText(text);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		b.setLayoutParams(lp);
		return b;
	}
	
	public class RowAdapter extends RecyclerView.Adapter<RowAdapter.ViewHolder> {
		
		List<EnrichedListing> _data;
		
		public RowAdapter(List<EnrichedListing> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.p2p_sell_listing_row, parent, false);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			EnrichedListing e = _data.get(_position);
			
			TextView seller_initial = _view.findViewById(R.id.seller_initial);
			ImageView seller_avatar = _view.findViewById(R.id.seller_avatar);
			MaterialTextView seller_name = _view.findViewById(R.id.seller_name);
			MaterialCardView cardview_verified = _view.findViewById(R.id.cardview_verified);
			MaterialTextView seller_status = _view.findViewById(R.id.seller_status);
			MaterialTextView seller_trusted_badge = _view.findViewById(R.id.seller_trusted_badge);
			MaterialTextView trust_score = _view.findViewById(R.id.trust_score);
			MaterialTextView trades_and_unique = _view.findViewById(R.id.trades_and_unique);
			MaterialTextView sell_price = _view.findViewById(R.id.sell_price);
			MaterialTextView available_amount = _view.findViewById(R.id.available_amount);
			MaterialTextView trade_limits = _view.findViewById(R.id.trade_limits);
			MaterialTextView payment_method = _view.findViewById(R.id.payment_method);
			MaterialTextView payment_method_two = _view.findViewById(R.id.payment_method_two);
			MaterialButton sell_button = _view.findViewById(R.id.sell_button);
			
			seller_name.setText(e.displayName);
			if (e.iconUrl.startsWith("http")) {
				seller_initial.setVisibility(View.GONE);
				seller_avatar.setVisibility(View.VISIBLE);
				Glide.with(getContext().getApplicationContext()).load(Uri.parse(e.iconUrl)).circleCrop().into(seller_avatar);
			} else {
				seller_avatar.setVisibility(View.GONE);
				seller_initial.setVisibility(View.VISIBLE);
				seller_initial.setText(e.displayName.length() > 0 ? e.displayName.substring(0, 1).toUpperCase(Locale.US) : "?");
			}
			cardview_verified.setVisibility(e.verified ? View.VISIBLE : View.GONE);
			seller_status.setVisibility(e.online ? View.VISIBLE : View.GONE);
			seller_status.setText("● Online");
			seller_trusted_badge.setText(e.premium ? "Premium Trader" : (e.insured ? "Insured Trader" : ""));
			trust_score.setText(e.trustScore >= 0 ? String.valueOf(e.trustScore) : "—");
			trades_and_unique.setText(e.completedTrades + " trades · " + e.uniqueCounterparties + " unique");
			sell_price.setText(e.unitPrice >= 0 ? P2pMoney.perHarv(e.unitPrice, e.currency) : "Unavailable");
			available_amount.setText(String.format(Locale.US, ("%.4f " + com.harvanna.app.CoinInfo.ticker()), e.remainingHarv));
			trade_limits.setText(String.format(Locale.US, ("Min %.4f · Max %.4f " + com.harvanna.app.CoinInfo.ticker()), e.minOrderHarv, e.remainingHarv));
			
			payment_method.setVisibility(View.GONE);
			payment_method_two.setVisibility(View.GONE);
			Iterator<String> labels = e.paymentLabels.iterator();
			if (labels.hasNext()) {
				String l1 = labels.next();
				payment_method.setText(l1.length() > 14 ? l1.substring(0, 13) + "…" : l1);
				payment_method.setVisibility(View.VISIBLE);
			}
			if (labels.hasNext()) {
				String l2 = labels.next();
				payment_method_two.setText(l2.length() > 14 ? l2.substring(0, 13) + "…" : l2);
				payment_method_two.setVisibility(View.VISIBLE);
			}
			
			View.OnClickListener openOrder = v -> {
				Intent intent = new Intent(requireContext(), P2pSellOrderActivity.class);
				intent.putExtra("listingId", e.listingId);
				startActivity(intent);
			};
			sell_button.setOnClickListener(openOrder);
			_view.setOnClickListener(openOrder);
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
	
	{
	}
	
	public class Recyclerview_fragmentAdapter extends RecyclerView.Adapter<Recyclerview_fragmentAdapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Recyclerview_fragmentAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.p2p_sell_listing_row, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			
			final com.google.android.material.card.MaterialCardView cardview6 = _view.findViewById(R.id.cardview6);
			final LinearLayout linear23 = _view.findViewById(R.id.linear23);
			final LinearLayout linear24 = _view.findViewById(R.id.linear24);
			final com.google.android.material.divider.MaterialDivider material_divider27 = _view.findViewById(R.id.material_divider27);
			final LinearLayout linear28 = _view.findViewById(R.id.linear28);
			final LinearLayout linear31 = _view.findViewById(R.id.linear31);
			final com.google.android.material.button.MaterialButton sell_button = _view.findViewById(R.id.sell_button);
			final com.google.android.material.card.MaterialCardView cardview7 = _view.findViewById(R.id.cardview7);
			final LinearLayout linear25 = _view.findViewById(R.id.linear25);
			final LinearLayout linear26 = _view.findViewById(R.id.linear26);
			final TextView seller_initial = _view.findViewById(R.id.seller_initial);
			final ImageView seller_avatar = _view.findViewById(R.id.seller_avatar);
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final com.google.android.material.textview.MaterialTextView seller_status = _view.findViewById(R.id.seller_status);
			final com.google.android.material.textview.MaterialTextView seller_trusted_badge = _view.findViewById(R.id.seller_trusted_badge);
			final com.google.android.material.textview.MaterialTextView seller_name = _view.findViewById(R.id.seller_name);
			final com.google.android.material.card.MaterialCardView cardview_verified = _view.findViewById(R.id.cardview_verified);
			final TextView textview1 = _view.findViewById(R.id.textview1);
			final com.google.android.material.textview.MaterialTextView textview6 = _view.findViewById(R.id.textview6);
			final com.google.android.material.textview.MaterialTextView trust_score = _view.findViewById(R.id.trust_score);
			final com.google.android.material.textview.MaterialTextView trades_and_unique = _view.findViewById(R.id.trades_and_unique);
			final LinearLayout linear29 = _view.findViewById(R.id.linear29);
			final LinearLayout linear30 = _view.findViewById(R.id.linear30);
			final com.google.android.material.textview.MaterialTextView textview7 = _view.findViewById(R.id.textview7);
			final com.google.android.material.textview.MaterialTextView sell_price = _view.findViewById(R.id.sell_price);
			final com.google.android.material.textview.MaterialTextView textview8 = _view.findViewById(R.id.textview8);
			final com.google.android.material.textview.MaterialTextView available_amount = _view.findViewById(R.id.available_amount);
			final com.google.android.material.textview.MaterialTextView trade_limits = _view.findViewById(R.id.trade_limits);
			final com.google.android.material.textview.MaterialTextView payment_method = _view.findViewById(R.id.payment_method);
			final com.google.android.material.textview.MaterialTextView payment_method_two = _view.findViewById(R.id.payment_method_two);
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
	
	{
	}
	
}
