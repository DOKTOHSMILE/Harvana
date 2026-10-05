package com.harvana.app;

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
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.firestore.P2PTradeStatsManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.widget.CompoundButtonCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.harvana.app.p2p.P2pCurrencyFilterState;
import com.harvana.app.p2p.P2pMoney;
import com.harvana.app.p2p.ThemedDialogs;



public class P2pBuyFragmentActivity extends Fragment {
	
	private LinearLayout linear1;
	private LinearLayout linear2;
	private RecyclerView recyclerview_fragment;
	private LinearLayout layout_fragment_empty;
	private MaterialCardView cardview_select_coin_btn;
	private MaterialCardView cardview_enter_amount;
	private MaterialCardView cardview_select_payment;
	private Space space10;
	private ImageView imageview_filter_btn;
	private LinearLayout linear7;
	private CircleImageView circleimageview1;
	private MaterialTextView textview1;
	private ImageView imageview4;
	private LinearLayout linear8;
	private MaterialTextView textview2;
	private ImageView imageview5;
	private LinearLayout linear9;
	private MaterialTextView textview3;
	private ImageView imageview6;
	private ImageView imageview7;
	private MaterialTextView tv_fragment_empty_title;
	private MaterialTextView tv_fragment_empty_subtitle;
	private MaterialButton btn_fragment_empty_cta;
	
	/** Application context, kept so background callbacks that finish after the fragment is gone do not hit a null getContext(). */
	private android.content.Context appCtx;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.p2p_buy_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		appCtx = _view.getContext().getApplicationContext();
		FirebaseApp.initializeApp(appCtx);
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		_main = _view.findViewById(R.id._main);
		linear1 = _view.findViewById(R.id.linear1);
		linear2 = _view.findViewById(R.id.linear2);
		recyclerview_fragment = _view.findViewById(R.id.recyclerview_fragment);
		com.harvana.app.utils.Shimmer.hold(recyclerview_fragment, com.harvana.app.utils.Shimmer.rows(recyclerview_fragment.getContext(), 5), 4000);
		layout_fragment_empty = _view.findViewById(R.id.layout_fragment_empty);
		cardview_select_coin_btn = _view.findViewById(R.id.cardview_select_coin_btn);
		cardview_enter_amount = _view.findViewById(R.id.cardview_enter_amount);
		cardview_select_payment = _view.findViewById(R.id.cardview_select_payment);
		space10 = _view.findViewById(R.id.space10);
		imageview_filter_btn = _view.findViewById(R.id.imageview_filter_btn);
		linear7 = _view.findViewById(R.id.linear7);
		circleimageview1 = _view.findViewById(R.id.circleimageview1);
		textview1 = _view.findViewById(R.id.textview1);
		imageview4 = _view.findViewById(R.id.imageview4);
		linear8 = _view.findViewById(R.id.linear8);
		textview2 = _view.findViewById(R.id.textview2);
		imageview5 = _view.findViewById(R.id.imageview5);
		linear9 = _view.findViewById(R.id.linear9);
		textview3 = _view.findViewById(R.id.textview3);
		imageview6 = _view.findViewById(R.id.imageview6);
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
		
		tv_fragment_empty_title.setText("No sellers right now");
		tv_fragment_empty_subtitle.setText("Check back soon, or post a Buy Offer and let a seller come to you.");
		btn_fragment_empty_cta.setVisibility(View.VISIBLE);
		btn_fragment_empty_cta.setOnClickListener(v ->
		startActivity(new Intent(requireContext(), CreateP2pBuyOfferActivity.class)));
		if (textview1 != null) textview1.setText(com.harvana.app.CoinInfo.ticker());
		
		_main.setOnRefreshListener(this::_loadListings);
		
		cardview_select_coin_btn.setOnClickListener(v -> _showCoinSheet());
		cardview_enter_amount.setOnClickListener(v -> _showAmountSheet());
		cardview_select_payment.setOnClickListener(v -> _showPaymentSheet());
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
	
	private static class EnrichedListing {
		JSONObject raw;
		String listingId = "";
		String posterAddress = "";
		String sellerUid = "";
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
		String trustLabel = "";
		int completedTrades = 0;
		int uniqueCounterparties = 0;
	}
	
	private final List<EnrichedListing> allEnriched = new ArrayList<>();
	private final List<EnrichedListing> visible = new ArrayList<>();
	private RowAdapter adapter;
	private String myAddress = "";
	
	// ── Filter state ─────────────────────────────────────────────────────
	private Double targetAmount = null;
	private final Set<String> selectedPaymentLabels = new LinkedHashSet<>();
	private boolean verifiedOnly = false;
	private boolean insuredOnly = false;
	private boolean premiumOnly = false;
	private int sortMode = 0; // 0 = none, 1 = price low-high, 2 = price high-low
	private final P2pCurrencyFilterState.Listener currencyListener = code -> _applyFilters();
	
	private void _loadListings() {
		HarvanaNodeClient.getP2POpenListings(0, 50, "SELL", new HarvanaNodeClient.JsonCallback() {
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
				if (raws.isEmpty()) {
					_finishLoad(new ArrayList<>());
					return;
				}
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
		e.posterAddress = "BUY".equals(raw.optString("side", ""))
		? raw.optString("buyerAddress", "")
		: raw.optString("sellerAddress", "");
		e.remainingHarv = HarvanaNodeClient.toHarvDisplay(raw.optLong("remainingAmount", 0));
		e.minOrderHarv = HarvanaNodeClient.toHarvDisplay(raw.optLong("minOrderAmount", 0));
		
		if (e.listingId.isEmpty() || e.posterAddress.isEmpty()) { done.onDone(e); return; }
		
		P2PPaymentDetailsManager.getInstance(appCtx)
		.get(e.listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object priceObj = data.get("unitPrice");
				e.unitPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : -1;
				Object currencyObj = data.get("currency");
				if (currencyObj != null && !String.valueOf(currencyObj).trim().isEmpty()) e.currency = String.valueOf(currencyObj).trim();
				Object sellerUidObj = data.get("sellerUid");
				e.sellerUid = sellerUidObj != null ? String.valueOf(sellerUidObj) : "";
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
				
				if (e.sellerUid.isEmpty()) { done.onDone(e); return; }
				
				java.util.concurrent.atomic.AtomicInteger innerRemaining = new java.util.concurrent.atomic.AtomicInteger(2);
				Runnable maybeDone = () -> { if (innerRemaining.decrementAndGet() == 0) done.onDone(e); };
				
				UserProfileManager.getInstance(appCtx)
				.getProfile(e.sellerUid, new UserProfileManager.DocCallback() {
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
				
				com.harvana.app.TrustScoreEngine.compute(appCtx, e.sellerUid,
				new com.harvana.app.TrustScoreEngine.Callback() {
					@Override
					public void onComplete(com.harvana.app.TrustScoreEngine.Result result) {
						e.verified = result.verified;
						int reportCount = result.reportCount;
						com.harvana.app.trust.NetworkTrust.wallet(e.posterAddress, score -> {
						e.trustScore = score.published ? score.score : -1;
						e.trustLabel = score.label();
						
						HarvanaNodeClient.getP2PInsuranceStatus(e.posterAddress, new HarvanaNodeClient.JsonCallback() {
							@Override
							public void onSuccess(JSONObject insuranceJson) {
								boolean meetsDeposit = insuranceJson.optBoolean("meetsInsuranceThreshold", false);
								e.insured = meetsDeposit && e.verified && reportCount == 0;
								
								P2PTradeStatsManager.getInstance(appCtx)
								.getFullStats(e.sellerUid, new P2PTradeStatsManager.FullStatsCallback() {
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
	
	// ── Filtering + sorting ──────────────────────────────────────────────
	
	private void _applyFilters() {
		visible.clear();
		for (EnrichedListing e : allEnriched) {
			if (!e.currency.equalsIgnoreCase(P2pCurrencyFilterState.getCurrency())) continue;
			if (targetAmount != null && !(e.minOrderHarv <= targetAmount && targetAmount <= e.remainingHarv)) continue;
			if (!selectedPaymentLabels.isEmpty() && Collections.disjoint(e.paymentLabels, selectedPaymentLabels)) continue;
			if (verifiedOnly && !e.verified) continue;
			if (insuredOnly && !e.insured) continue;
			if (premiumOnly && !e.premium) continue;
			visible.add(e);
		}
		if (sortMode == 1) java.util.Collections.sort(visible, (a, b) -> Double.compare(a.unitPrice, b.unitPrice));
		else if (sortMode == 2) java.util.Collections.sort(visible, (a, b) -> Double.compare(b.unitPrice, a.unitPrice));
		
		adapter.notifyDataSetChanged();
		boolean empty = visible.isEmpty();
		recyclerview_fragment.setVisibility(empty ? View.GONE : View.VISIBLE);
		layout_fragment_empty.setVisibility(empty ? View.VISIBLE : View.GONE);
		if (empty) {
			boolean anyFilterActive = targetAmount != null || !selectedPaymentLabels.isEmpty() || verifiedOnly || insuredOnly || premiumOnly;
			tv_fragment_empty_title.setText(anyFilterActive ? "No matches for these filters" : "No sellers right now");
			tv_fragment_empty_subtitle.setText(anyFilterActive
			? "Try clearing a filter to see more listings."
			: "Check back soon, or post a Buy Offer and let a seller come to you.");
		}
		_refreshFilterLabels();
	}
	
	private void _refreshFilterLabels() {
		textview2.setText(targetAmount != null ? BalanceFormatter.smartDecimal(targetAmount) : "Amount");
		textview3.setText(selectedPaymentLabels.isEmpty() ? "Payment" : "Payment (" + selectedPaymentLabels.size() + ")");
		int activeFilterCount = (verifiedOnly ? 1 : 0) + (insuredOnly ? 1 : 0) + (premiumOnly ? 1 : 0) + (sortMode != 0 ? 1 : 0);
		imageview_filter_btn.setAlpha(activeFilterCount > 0 ? 1f : 0.6f);
	}
	
	// ── Select Coin bottom sheet — one real entry, nothing invented ─────
	
	private void _showCoinSheet() {
		BottomSheetDialog dialog = com.harvana.app.p2p.ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("Select Coin"));
		
		// 1. Resolve dynamic theme colors for Day/Night support
		int colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
		int colorOnSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
		int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.GREEN);
		
		// 2. Style the section label
		MaterialTextView header = _sheetLabel("P2P Supported Coins");
		header.setTextColor(colorOnSurfaceVariant); // Softer color for headers
		root.addView(header);
		
		// 3. Enhance the interactive row
		LinearLayout row = _sheetRow();
		row.setClickable(true);
		row.setFocusable(true);
		// Add a click listener so the user can actually select the coin
		row.setOnClickListener(v -> {
			// TODO: Handle the coin selection state here
			dialog.dismiss();
		});
		
		MaterialTextView label = new MaterialTextView(requireContext());
		label.setText((com.harvana.app.CoinInfo.ticker() + " — Harvana Protocol"));
		label.setTextColor(colorOnSurface);
		label.setTextSize(16f);
		label.setTypeface(null, Typeface.BOLD); // Bolder text for main assets improves readability
		
		MaterialTextView check = new MaterialTextView(requireContext());
		check.setText("✓");
		check.setTextColor(colorPrimary); // Tie the checkmark to the app's primary theme color
		check.setTextSize(20f);
		check.setTypeface(null, Typeface.BOLD);
		
		row.addView(label);
		row.addView(_spacer());
		row.addView(check);
		root.addView(row);
		
		// 4. Refine the footnote for a professional tone
		MaterialTextView note = new MaterialTextView(requireContext());
		note.setText("Additional assets will be supported in future updates.");
		note.setTextColor(colorOnSurfaceVariant);
		note.setTextSize(13f);
		note.setGravity(Gravity.CENTER_HORIZONTAL);
		note.setPadding(32, 48, 32, 16); // Increased padding for better breathing room at the bottom
		root.addView(note);
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	// ── Amount bottom sheet ──────────────────────────────────────────────
	
	private void _showAmountSheet() {
		BottomSheetDialog dialog = com.harvana.app.p2p.ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("I Want to Buy"));
		
		int colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
		int colorOnSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
		int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		int colorOnPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
		
		EditText input = new EditText(requireContext());
		input.setHint(("Enter " + com.harvana.app.CoinInfo.ticker() + " amount"));
		input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		input.setTextColor(colorOnSurface);
		input.setHintTextColor(colorOnSurfaceVariant);
		
		if (targetAmount != null) input.setText(BalanceFormatter.smartDecimal(targetAmount));
		root.addView(input);
		
		LinearLayout buttonRow = _sheetButtonRow();
		MaterialButton reset = _outlineButton("Reset");
		MaterialButton confirm = _filledButton("Confirm");
		
		reset.setTextColor(colorPrimary);
		reset.setStrokeColor(ColorStateList.valueOf(colorPrimary));
		
		confirm.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
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
		
		// 1. Force the dialog window to resize when the keyboard appears
		if (dialog.getWindow() != null) {
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		
		// 2. Expand the bottom sheet fully so it doesn't get squished halfway
		dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
		
		dialog.show();
	}
	
	// ── Payment bottom sheet — built from real, currently-live method names ──
	
	private void _showPaymentSheet() {
		Set<String> availableLabels = new LinkedHashSet<>();
		for (EnrichedListing e : allEnriched) availableLabels.addAll(e.paymentLabels);
		
		BottomSheetDialog dialog = com.harvana.app.p2p.ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("Pay With"));
		
		// Resolve dynamic colors
		int colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
		int colorOnSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
		int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		int colorOnPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
		
		EditText search = new EditText(requireContext());
		search.setHint("Search");
		search.setTextColor(colorOnSurface);
		search.setHintTextColor(colorOnSurfaceVariant);
		root.addView(search);
		
		LinearLayout listContainer = new LinearLayout(requireContext());
		listContainer.setOrientation(LinearLayout.VERTICAL);
		root.addView(listContainer);
		
		Map<String, CheckBox> checkboxes = new LinkedHashMap<>();
		final Runnable[] rebuildHolder = new Runnable[1];
		rebuildHolder[0] = () -> {
			String query = search.getText().toString().trim().toLowerCase(Locale.US);
			listContainer.removeAllViews();
			checkboxes.clear();
			
			LinearLayout allRow = _sheetRow();
			allRow.setClickable(true);
			allRow.setFocusable(true);
			
			MaterialTextView allLabel = new MaterialTextView(requireContext());
			allLabel.setText("All Payment Methods");
			allLabel.setTextColor(colorOnSurface);
			
			CheckBox allCheck = new CheckBox(requireContext());
			CompoundButtonCompat.setButtonTintList(allCheck, ColorStateList.valueOf(colorPrimary));
			allCheck.setChecked(selectedPaymentLabels.isEmpty());
			
			// Make the entire row clickable for better UX
			View.OnClickListener allClickListener = v -> {
				selectedPaymentLabels.clear();
				rebuildHolder[0].run();
			};
			allCheck.setOnClickListener(allClickListener);
			allRow.setOnClickListener(allClickListener);
			
			allRow.addView(allLabel);
			allRow.addView(_spacer());
			allRow.addView(allCheck);
			listContainer.addView(allRow);
			
			if (availableLabels.isEmpty()) {
				MaterialTextView none = new MaterialTextView(requireContext());
				none.setText("No payment methods on any listing right now.");
				none.setTextColor(colorOnSurfaceVariant);
				none.setPadding(0, 24, 0, 0);
				listContainer.addView(none);
				return;
			}
			
			for (String label : availableLabels) {
				if (!query.isEmpty() && !label.toLowerCase(Locale.US).contains(query)) continue;
				
				LinearLayout row = _sheetRow();
				row.setClickable(true);
				row.setFocusable(true);
				
				MaterialTextView tv = new MaterialTextView(requireContext());
				tv.setText(label);
				tv.setTextColor(colorOnSurface);
				
				CheckBox cb = new CheckBox(requireContext());
				CompoundButtonCompat.setButtonTintList(cb, ColorStateList.valueOf(colorPrimary));
				cb.setChecked(selectedPaymentLabels.contains(label));
				
				View.OnClickListener rowClickListener = v -> {
					// Determine new state and apply logic
					boolean willBeChecked = (v == cb) ? cb.isChecked() : !cb.isChecked();
					if (v != cb) cb.setChecked(willBeChecked); // visually update if row was clicked
					
					if (willBeChecked) selectedPaymentLabels.add(label); 
					else selectedPaymentLabels.remove(label);
					
					rebuildHolder[0].run();
				};
				
				cb.setOnClickListener(rowClickListener);
				row.setOnClickListener(rowClickListener);
				
				checkboxes.put(label, cb);
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
		
		// Apply dynamic colors to buttons
		reset.setTextColor(colorPrimary);
		reset.setStrokeColor(ColorStateList.valueOf(colorPrimary));
		confirm.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
		confirm.setTextColor(colorOnPrimary);
		
		reset.setOnClickListener(v -> { selectedPaymentLabels.clear(); rebuildHolder[0].run(); });
		confirm.setOnClickListener(v -> { _applyFilters(); dialog.dismiss(); });
		
		buttonRow.addView(reset);
		buttonRow.addView(confirm);
		root.addView(buttonRow);
		
		dialog.setContentView(root);
		
		// Force layout resizing and full expansion so the sheet sits above the soft keyboard
		if (dialog.getWindow() != null) {
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
		
		dialog.show();
	}
	
	// ── Filter (funnel) bottom sheet — real toggles + real sort ─────────
	
	private void _showFilterSheet() {
		BottomSheetDialog dialog = com.harvana.app.p2p.ThemedDialogs.sheet(requireContext());
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("Filter"));
		
		// Resolve dynamic theme colors for Day/Night mode
		int colorOnSurface = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
		int colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		int colorOnPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
		
		// Checkboxes (Assuming _sheetToggleRow handles its own text styling appropriately)
		CheckBox verifiedBox = _sheetToggleRow(root, "Verified sellers only", verifiedOnly);
		CheckBox insuredBox = _sheetToggleRow(root, "Insured Traders only", insuredOnly);
		CheckBox premiumBox = _sheetToggleRow(root, "Premium Traders only", premiumOnly);
		
		root.addView(_sheetLabel("Sort by price"));
		RadioGroup sortGroup = new RadioGroup(requireContext());
		sortGroup.setOrientation(RadioGroup.VERTICAL);
		
		RadioButton none = new RadioButton(requireContext());
		none.setText("Default order");
		none.setTextColor(colorOnSurface);
		CompoundButtonCompat.setButtonTintList(none, ColorStateList.valueOf(colorPrimary));
		
		RadioButton lowHigh = new RadioButton(requireContext());
		lowHigh.setText("Price: low to high");
		lowHigh.setTextColor(colorOnSurface);
		CompoundButtonCompat.setButtonTintList(lowHigh, ColorStateList.valueOf(colorPrimary));
		
		RadioButton highLow = new RadioButton(requireContext());
		highLow.setText("Price: high to low");
		highLow.setTextColor(colorOnSurface);
		CompoundButtonCompat.setButtonTintList(highLow, ColorStateList.valueOf(colorPrimary));
		
		sortGroup.addView(none);
		sortGroup.addView(lowHigh);
		sortGroup.addView(highLow);
		
		if (sortMode == 1) lowHigh.setChecked(true); 
		else if (sortMode == 2) highLow.setChecked(true); 
		else none.setChecked(true);
		
		root.addView(sortGroup);
		
		LinearLayout buttonRow = _sheetButtonRow();
		MaterialButton reset = _outlineButton("Reset");
		MaterialButton confirm = _filledButton("Confirm");
		
		// Apply dynamic colors to the bottom buttons
		reset.setTextColor(colorPrimary);
		reset.setStrokeColor(ColorStateList.valueOf(colorPrimary));
		
		confirm.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
		confirm.setTextColor(colorOnPrimary);
		
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
	
	// ── Small view-builder helpers (no new XML needed for these sheets) ──
	
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
		row.setMinimumHeight(96);
		row.setClickable(true);
		cb.setClickable(false);
		row.setOnClickListener(v -> cb.toggle());
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
	
	// ── Row adapter — binds synchronously from already-enriched data ────
	
	public class RowAdapter extends RecyclerView.Adapter<RowAdapter.ViewHolder> {
		
		List<EnrichedListing> _data;
		
		public RowAdapter(List<EnrichedListing> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.p2p_buy_listing_row, parent, false);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			EnrichedListing e = _data.get(_position);
			
			MaterialTextView txt_avatar = _view.findViewById(R.id.txt_avatar);
			ImageView iv_avatar_photo = _view.findViewById(R.id.iv_avatar_photo);
			MaterialTextView txt_username = _view.findViewById(R.id.txt_username);
			MaterialCardView cardview3 = _view.findViewById(R.id.cardview3);
			MaterialCardView card_status_dot = _view.findViewById(R.id.card_status_dot);
			MaterialTextView txt_online = _view.findViewById(R.id.txt_online);
			MaterialTextView txt_trusted = _view.findViewById(R.id.txt_trusted);
			MaterialTextView txt_available = _view.findViewById(R.id.txt_available);
			MaterialTextView txt_rating = _view.findViewById(R.id.txt_rating);
			MaterialTextView txt_trades = _view.findViewById(R.id.txt_trades);
			MaterialTextView txt_unique = _view.findViewById(R.id.txt_unique);
			MaterialTextView txt_price = _view.findViewById(R.id.txt_price);
			MaterialTextView txt_min = _view.findViewById(R.id.txt_min);
			MaterialTextView txt_max = _view.findViewById(R.id.txt_max);
			LinearLayout payment_one = _view.findViewById(R.id.payment_one);
			MaterialTextView txt_payment_one = _view.findViewById(R.id.txt_payment_one);
			LinearLayout payment_two = _view.findViewById(R.id.payment_two);
			MaterialTextView txt_payment_two = _view.findViewById(R.id.txt_payment_two);
			MaterialButton btn_buy = _view.findViewById(R.id.btn_buy);
			
			txt_username.setText(e.displayName);
			if (e.iconUrl.startsWith("http")) {
				txt_avatar.setVisibility(View.GONE);
				iv_avatar_photo.setVisibility(View.VISIBLE);
				Glide.with(appCtx).load(Uri.parse(e.iconUrl)).circleCrop().into(iv_avatar_photo);
			} else {
				iv_avatar_photo.setVisibility(View.GONE);
				txt_avatar.setVisibility(View.VISIBLE);
				txt_avatar.setText(e.displayName.length() > 0 ? e.displayName.substring(0, 1).toUpperCase(Locale.US) : "?");
			}
			card_status_dot.setVisibility(e.online ? View.VISIBLE : View.GONE);
			txt_online.setVisibility(e.online ? View.VISIBLE : View.GONE);
			cardview3.setVisibility(e.verified ? View.VISIBLE : View.GONE);
			txt_trusted.setText(e.premium ? "Premium Trader" : (e.insured ? "Insured Trader" : ""));
			txt_rating.setText(e.trustLabel.isEmpty() ? "\u2014" : "Trust " + e.trustLabel);
			txt_trades.setText(e.completedTrades + " trades");
			txt_unique.setText("• " + e.uniqueCounterparties + " unique");
			txt_available.setText(String.format(Locale.US, ("%.4f " + com.harvana.app.CoinInfo.ticker()), e.remainingHarv));
			txt_min.setText(String.format(Locale.US, ("Min %.4f " + com.harvana.app.CoinInfo.ticker()), e.minOrderHarv));
			txt_max.setText(String.format(Locale.US, ("Max %.4f " + com.harvana.app.CoinInfo.ticker()), e.remainingHarv));
			txt_price.setText(e.unitPrice >= 0 ? String.format(Locale.US, "%,.2f", e.unitPrice) : "Unavailable");
			
			payment_one.setVisibility(View.INVISIBLE);
			payment_two.setVisibility(View.INVISIBLE);
			Iterator<String> labels = e.paymentLabels.iterator();
			if (labels.hasNext()) {
				String l1 = labels.next();
				txt_payment_one.setText(l1.length() > 10 ? l1.substring(0, 9) + "…" : l1);
				payment_one.setVisibility(View.VISIBLE);
			}
			if (labels.hasNext()) {
				String l2 = labels.next();
				txt_payment_two.setText(l2.length() > 10 ? l2.substring(0, 9) + "…" : l2);
				payment_two.setVisibility(View.VISIBLE);
			}
			
			View.OnClickListener openOrder = v -> {
				Intent intent = new Intent(requireContext(), P2pBuyOrderActivity.class);
				intent.putExtra("listingId", e.listingId);
				startActivity(intent);
			};
			btn_buy.setOnClickListener(openOrder);
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
	
}
