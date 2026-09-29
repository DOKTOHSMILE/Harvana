package com.harvanna.wallet;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
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
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.widget.CompoundButtonCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;


public class P2pMarketplaceActivity extends AppCompatActivity {
	
	private LinearLayout linear52;
	private LinearLayout linear57;
	private MaterialCardView cardview1;
	private ImageView imageview1;
	private MaterialCardView cardview3;
	private CardView cardview_choose_currency;
	private Space space1;
	private ImageView imageview2;
	private ImageView imageview3;
	private MaterialCardView cardview2;
	private LinearLayout linear56;
	private MaterialButton express_button;
	private MaterialCardView cardview_btn_buy;
	private MaterialCardView cardview_btn_sell;
	private MaterialTextView textview1;
	private MaterialTextView textview2;
	private LinearLayout linear58;
	private TextView textview3;
	private ImageView imageview4;
	private FrameLayout linear_fragment;
	
	private Intent i = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.p2p_marketplace);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear52 = findViewById(R.id.linear52);
		linear57 = findViewById(R.id.linear57);
		cardview1 = findViewById(R.id.cardview1);
		imageview1 = findViewById(R.id.imageview1);
		cardview3 = findViewById(R.id.cardview3);
		cardview_choose_currency = findViewById(R.id.cardview_choose_currency);
		space1 = findViewById(R.id.space1);
		imageview2 = findViewById(R.id.imageview2);
		imageview3 = findViewById(R.id.imageview3);
		cardview2 = findViewById(R.id.cardview2);
		linear56 = findViewById(R.id.linear56);
		express_button = findViewById(R.id.express_button);
		cardview_btn_buy = findViewById(R.id.cardview_btn_buy);
		cardview_btn_sell = findViewById(R.id.cardview_btn_sell);
		textview1 = findViewById(R.id.textview1);
		textview2 = findViewById(R.id.textview2);
		linear58 = findViewById(R.id.linear58);
		textview3 = findViewById(R.id.textview3);
		imageview4 = findViewById(R.id.imageview4);
		linear_fragment = findViewById(R.id.linear_fragment);
		
		imageview1.setOnClickListener(_v -> finish());
		
		cardview_choose_currency.setOnClickListener(_v -> _showCurrencyPickerSheet());
		
		imageview2.setOnClickListener(_v -> {
			i.setClass(getApplicationContext(), P2pMyTradesActivity.class);
			startActivity(i);
		});
		
		imageview3.setOnClickListener(_v -> {
			i.setClass(getApplicationContext(), P2pMerchantManagerActivity.class);
			startActivity(i);
		});
		
		express_button.setOnClickListener(_v -> {
			i.setClass(getApplicationContext(), P2pExpressActivity.class);
			startActivity(i);
		});
		
		cardview_btn_buy.setOnClickListener(_v -> {
			setupMarketFragments();
			_setSellMode(false);
		});
		
		cardview_btn_sell.setOnClickListener(_v -> {
			setupMarketFragments();
			_setSellMode(true);
		});
	}
	
	private void initializeLogic() {
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvanna.wallet.utils.StatusBarUtil.matchToolbar(this);
		}
		
		setupMarketFragments();
		_setSellMode(false);
		textview3.setText(currentSelectedCurrencyCode);
		
	}
	
	public void _MORE() {
	}
	private String currentSelectedCurrencyCode = com.harvanna.wallet.p2p.P2pCurrencyFilterState.getCurrency();
	private void _showCurrencyPickerSheet() {
		BottomSheetDialog dialog = com.harvanna.wallet.p2p.ThemedDialogs.sheet(this);
		LinearLayout root = _sheetRoot();
		root.addView(_sheetTitle("Select Currency"));
		
		// Resolve dynamic theme colors for Day/Night support
		int colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
		int colorOnSurfaceVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
		int colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLUE);
		
		// Wrap the list in a ScrollView to prevent layout cut-offs on smaller screens
		ScrollView scrollView = new ScrollView(this);
		scrollView.setLayoutParams(new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.MATCH_PARENT, 
		LinearLayout.LayoutParams.WRAP_CONTENT
		));
		
		LinearLayout listContainer = new LinearLayout(this);
		listContainer.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(listContainer);
		root.addView(scrollView);
		
		// Define the currency list (Fiat + Crypto)
		List<CurrencyItem> currencies = Arrays.asList(
		new CurrencyItem("NGN", "₦", "Nigerian Naira"),
		new CurrencyItem("USD", "$", "US Dollar"),
		new CurrencyItem("EUR", "€", "Euro"),
		new CurrencyItem("GHS", "₵", "Ghanaian Cedi"),
		new CurrencyItem("GBP", "£", "British Pound"),
		new CurrencyItem("ZAR", "R", "South African Rand"),
		new CurrencyItem("USDT", "₮", "Tether"),
		new CurrencyItem("USDC", "USDC", "USD Coin"),
		new CurrencyItem("BTC", "₿", "Bitcoin")
		);
		
		// Build the interactive rows dynamically
		for (CurrencyItem currency : currencies) {
			LinearLayout row = _sheetRow();
			row.setClickable(true);
			row.setFocusable(true);
			
			MaterialTextView label = new MaterialTextView(this);
			label.setText(currency.symbol + "  " + currency.code);
			label.setTextColor(colorOnSurface);
			label.setTextSize(16f);
			label.setTypeface(null, Typeface.BOLD);
			
			MaterialTextView name = new MaterialTextView(this);
			name.setText(" — " + currency.name);
			name.setTextColor(colorOnSurfaceVariant);
			name.setTextSize(14f);
			
			MaterialTextView check = new MaterialTextView(this);
			check.setText("✓");
			check.setTextColor(colorPrimary);
			check.setTextSize(20f);
			check.setTypeface(null, Typeface.BOLD);
			check.setVisibility(currency.code.equals(currentSelectedCurrencyCode) ? View.VISIBLE : View.GONE);
			
			row.addView(label);
			row.addView(name);
			row.addView(_spacer());
			row.addView(check);
			listContainer.addView(row);
			
			// Handle row clicks
			row.setOnClickListener(v -> {
				currentSelectedCurrencyCode = currency.code;
				textview3.setText(currency.code); // Update UI
				com.harvanna.wallet.p2p.P2pCurrencyFilterState.setCurrency(currency.code);
				dialog.dismiss();
			});
		}
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	private static class CurrencyItem {
		String code;
		String symbol;
		String name;
		
		CurrencyItem(String code, String symbol, String name) {
			this.code = code;
			this.symbol = symbol;
			this.name = name;
		}
	}
	
	private LinearLayout _sheetRoot() {
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(48, 48, 48, 48);
		return root;
	}
	
	private MaterialTextView _sheetTitle(String title) {
		MaterialTextView tv = new MaterialTextView(this);
		tv.setText(title);
		tv.setTextSize(20f);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setPadding(0, 0, 0, 32);
		
		int colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
		tv.setTextColor(colorOnSurface);
		return tv;
	}
	
	private LinearLayout _sheetRow() {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(android.view.Gravity.CENTER_VERTICAL);
		row.setPadding(0, 32, 0, 32);
		return row;
	}
	
	private View _spacer() {
		View spacer = new View(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 1, 1.0f);
		spacer.setLayoutParams(params);
		return spacer;
	}
	
	
	private Fragment buy = new P2pBuyFragmentActivity();
	private Fragment sell = new P2pSellFragmentActivity();
	private Fragment activeFragment = null;
	private boolean fragmentsReady = false;
	
	private void setupMarketFragments() {
		if (fragmentsReady) return;
		getSupportFragmentManager().beginTransaction()
		.add(R.id.linear_fragment, sell, "p2p_sell").hide(sell)
		.add(R.id.linear_fragment, buy, "p2p_buy")
		.commitNow();
		fragmentsReady = true;
		activeFragment = buy;
	}
	
	private void showFragment(Fragment fragment) {
		if (fragment == null) return;
		if (!fragmentsReady) setupMarketFragments();
		if (fragment == activeFragment) return;
		
		androidx.fragment.app.FragmentTransaction ft =
		getSupportFragmentManager().beginTransaction();
		if (activeFragment != null && activeFragment.isAdded()) ft.hide(activeFragment);
		if (fragment.isAdded()) {
			ft.show(fragment);
		} else {
			ft.add(R.id.linear_fragment, fragment,
			fragment == sell ? "p2p_sell" : "p2p_buy");
		}
		ft.commitNow();
		activeFragment = fragment;
	}
	
	private void _setSellMode(boolean sellMode) {
		showFragment(sellMode ? sell : buy);
		int accent = getResources().getColor(R.color.colorP2pAccent);
		int error = getResources().getColor(R.color.colorError);
		int onAccent = getResources().getColor(R.color.colorP2pOnAccent);
		int primary = getResources().getColor(R.color.colorP2pTextPrimary);
		cardview_btn_buy.setCardBackgroundColor(sellMode ? Color.TRANSPARENT : accent);
		cardview_btn_sell.setCardBackgroundColor(sellMode ? error : Color.TRANSPARENT);
		textview1.setText("BUY");
		textview2.setText("SELL");
		textview1.setTextColor(sellMode ? primary : onAccent);
		textview2.setTextColor(sellMode ? onAccent : primary);
	}
	{
	}
	
}
