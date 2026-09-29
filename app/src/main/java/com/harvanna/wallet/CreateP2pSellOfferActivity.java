package com.harvanna.wallet;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;



public class CreateP2pSellOfferActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout create_sell_offer_root;
	private LinearLayout panel_sell_step_1;
	private LinearLayout panel_sell_step_2;
	private LinearLayout panel_sell_step_3;
	private TextView tv_sell_step_label;
	private MaterialCardView card_sell_info;
	private TextView tv_sell_amount_section;
	private MaterialCardView card_sell_amount;
	private LinearLayout layout_sell_available;
	private TextView tv_sell_price_section;
	private MaterialButtonToggleGroup toggle_sell_pricing_mode;
	private LinearLayout group_sell_price_fixed;
	private LinearLayout group_sell_price_floating;
	private TextView tv_sell_payment_section;
	private MaterialCardView card_sell_payment_methods;
	private TextView tv_sell_settings_section;
	private MaterialCardView card_sell_settings;
	private TextView tv_sell_terms_section;
	private MaterialCardView card_sell_terms;
	private TextView tv_sell_terms_counter;
	private TextView tv_sell_counterparty_section;
	private MaterialCardView card_sell_counterparty;
	private TextView tv_sell_step1_error;
	private MaterialButton btn_sell_deposit_ajc_1;
	private MaterialButton btn_preview_sell_offer;
	private LinearLayout layout_sell_info;
	private ImageView imageview_sell_info;
	private TextView tv_sell_info;
	private LinearLayout layout_sell_amount;
	private EditText et_sell_amount;
	private TextView tv_sell_amount_currency;
	private TextView tv_sell_available_label;
	private TextView tv_sell_available_balance;
	private MaterialButton btn_sell_max;
	private MaterialButton btn_sell_price_fixed;
	private MaterialButton btn_sell_price_floating;
	private MaterialCardView card_sell_price;
	private TextView tv_sell_price_note;
	private LinearLayout layout_sell_price;
	private EditText et_sell_price;
	private TextView tv_sell_price_currency;
	private MaterialCardView card_sell_margin;
	private TextView tv_sell_calculated_price;
	private LinearLayout linear7;
	private EditText et_sell_margin;
	private TextView tv_sell_margin_percent_label;
	private LinearLayout layout_sell_payment_methods;
	private LinearLayout layout_sell_settings;
	private LinearLayout row_sell_trade_limit;
	private View view3;
	private LinearLayout row_sell_offer_duration;
	private View view_sell_payment_window_divider;
	private LinearLayout row_sell_payment_window;
	private TextView tv_sell_trade_limit_label;
	private TextView tv_sell_trade_limit_value;
	private TextView tv_sell_offer_duration_label;
	private TextView tv_sell_offer_duration_value;
	private TextView tv_sell_payment_window_label;
	private TextView tv_sell_payment_window_value;
	private EditText et_sell_terms;
	private LinearLayout linear8;
	private LinearLayout row_sell_require_kyc;
	private View view10;
	private LinearLayout row_sell_min_account_age;
	private View view11;
	private LinearLayout row_sell_min_completion_rate;
	private LinearLayout linear9;
	private Switch switch_sell_require_kyc;
	private TextView textview2;
	private TextView textview3;
	private TextView textview4;
	private TextView tv_sell_min_account_age_value;
	private LinearLayout linear12;
	private TextView tv_sell_min_completion_rate_value;
	private TextView textview5;
	private TextView textview6;
	private TextView tv_sell_summary_title;
	private MaterialCardView card_sell_review;
	private MaterialCardView card_sell_review_note;
	private TextView tv_sell_step2_error;
	private MaterialButton btn_sell_deposit_ajc_2;
	private MaterialButton btn_publish_sell_offer;
	private MaterialButton btn_sell_back_to_step1;
	private LinearLayout layout_sell_review;
	private LinearLayout linear4;
	private LinearLayout linear5;
	private View view6;
	private LinearLayout linear13;
	private LinearLayout linear14;
	private TextView textview7;
	private TextView tv_sell_review_amount;
	private TextView textview8;
	private TextView tv_sell_review_unit_price;
	private TextView textview9;
	private TextView tv_sell_review_total_price;
	private TextView textview10;
	private TextView tv_sell_review_fee;
	private LinearLayout linear15;
	private TextView textview11;
	private TextView textview12;
	private ImageView imageview1;
	private TextView textview13;
	private TextView textview14;
	private MaterialButton btn_sell_view_my_offers;
	private MaterialButton btn_sell_create_another;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.create_p2p_sell_offer);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		create_sell_offer_root = findViewById(R.id.create_sell_offer_root);
		panel_sell_step_1 = findViewById(R.id.panel_sell_step_1);
		panel_sell_step_2 = findViewById(R.id.panel_sell_step_2);
		panel_sell_step_3 = findViewById(R.id.panel_sell_step_3);
		tv_sell_step_label = findViewById(R.id.tv_sell_step_label);
		card_sell_info = findViewById(R.id.card_sell_info);
		tv_sell_amount_section = findViewById(R.id.tv_sell_amount_section);
		card_sell_amount = findViewById(R.id.card_sell_amount);
		layout_sell_available = findViewById(R.id.layout_sell_available);
		tv_sell_price_section = findViewById(R.id.tv_sell_price_section);
		toggle_sell_pricing_mode = findViewById(R.id.toggle_sell_pricing_mode);
		group_sell_price_fixed = findViewById(R.id.group_sell_price_fixed);
		group_sell_price_floating = findViewById(R.id.group_sell_price_floating);
		tv_sell_payment_section = findViewById(R.id.tv_sell_payment_section);
		card_sell_payment_methods = findViewById(R.id.card_sell_payment_methods);
		tv_sell_settings_section = findViewById(R.id.tv_sell_settings_section);
		card_sell_settings = findViewById(R.id.card_sell_settings);
		tv_sell_terms_section = findViewById(R.id.tv_sell_terms_section);
		card_sell_terms = findViewById(R.id.card_sell_terms);
		tv_sell_terms_counter = findViewById(R.id.tv_sell_terms_counter);
		tv_sell_counterparty_section = findViewById(R.id.tv_sell_counterparty_section);
		card_sell_counterparty = findViewById(R.id.card_sell_counterparty);
		tv_sell_step1_error = findViewById(R.id.tv_sell_step1_error);
		btn_sell_deposit_ajc_1 = findViewById(R.id.btn_sell_deposit_ajc_1);
		btn_preview_sell_offer = findViewById(R.id.btn_preview_sell_offer);
		layout_sell_info = findViewById(R.id.layout_sell_info);
		imageview_sell_info = findViewById(R.id.imageview_sell_info);
		tv_sell_info = findViewById(R.id.tv_sell_info);
		layout_sell_amount = findViewById(R.id.layout_sell_amount);
		et_sell_amount = findViewById(R.id.et_sell_amount);
		tv_sell_amount_currency = findViewById(R.id.tv_sell_amount_currency);
		tv_sell_available_label = findViewById(R.id.tv_sell_available_label);
		tv_sell_available_balance = findViewById(R.id.tv_sell_available_balance);
		btn_sell_max = findViewById(R.id.btn_sell_max);
		btn_sell_price_fixed = findViewById(R.id.btn_sell_price_fixed);
		btn_sell_price_floating = findViewById(R.id.btn_sell_price_floating);
		card_sell_price = findViewById(R.id.card_sell_price);
		tv_sell_price_note = findViewById(R.id.tv_sell_price_note);
		layout_sell_price = findViewById(R.id.layout_sell_price);
		et_sell_price = findViewById(R.id.et_sell_price);
		tv_sell_price_currency = findViewById(R.id.tv_sell_price_currency);
		card_sell_margin = findViewById(R.id.card_sell_margin);
		tv_sell_calculated_price = findViewById(R.id.tv_sell_calculated_price);
		linear7 = findViewById(R.id.linear7);
		et_sell_margin = findViewById(R.id.et_sell_margin);
		tv_sell_margin_percent_label = findViewById(R.id.tv_sell_margin_percent_label);
		layout_sell_payment_methods = findViewById(R.id.layout_sell_payment_methods);
		layout_sell_settings = findViewById(R.id.layout_sell_settings);
		row_sell_trade_limit = findViewById(R.id.row_sell_trade_limit);
		view3 = findViewById(R.id.view3);
		row_sell_offer_duration = findViewById(R.id.row_sell_offer_duration);
		view_sell_payment_window_divider = findViewById(R.id.view_sell_payment_window_divider);
		row_sell_payment_window = findViewById(R.id.row_sell_payment_window);
		tv_sell_trade_limit_label = findViewById(R.id.tv_sell_trade_limit_label);
		tv_sell_trade_limit_value = findViewById(R.id.tv_sell_trade_limit_value);
		tv_sell_offer_duration_label = findViewById(R.id.tv_sell_offer_duration_label);
		tv_sell_offer_duration_value = findViewById(R.id.tv_sell_offer_duration_value);
		tv_sell_payment_window_label = findViewById(R.id.tv_sell_payment_window_label);
		tv_sell_payment_window_value = findViewById(R.id.tv_sell_payment_window_value);
		et_sell_terms = findViewById(R.id.et_sell_terms);
		linear8 = findViewById(R.id.linear8);
		row_sell_require_kyc = findViewById(R.id.row_sell_require_kyc);
		view10 = findViewById(R.id.view10);
		row_sell_min_account_age = findViewById(R.id.row_sell_min_account_age);
		view11 = findViewById(R.id.view11);
		row_sell_min_completion_rate = findViewById(R.id.row_sell_min_completion_rate);
		linear9 = findViewById(R.id.linear9);
		switch_sell_require_kyc = findViewById(R.id.switch_sell_require_kyc);
		textview2 = findViewById(R.id.textview2);
		textview3 = findViewById(R.id.textview3);
		textview4 = findViewById(R.id.textview4);
		tv_sell_min_account_age_value = findViewById(R.id.tv_sell_min_account_age_value);
		linear12 = findViewById(R.id.linear12);
		tv_sell_min_completion_rate_value = findViewById(R.id.tv_sell_min_completion_rate_value);
		textview5 = findViewById(R.id.textview5);
		textview6 = findViewById(R.id.textview6);
		tv_sell_summary_title = findViewById(R.id.tv_sell_summary_title);
		card_sell_review = findViewById(R.id.card_sell_review);
		card_sell_review_note = findViewById(R.id.card_sell_review_note);
		tv_sell_step2_error = findViewById(R.id.tv_sell_step2_error);
		btn_sell_deposit_ajc_2 = findViewById(R.id.btn_sell_deposit_ajc_2);
		btn_publish_sell_offer = findViewById(R.id.btn_publish_sell_offer);
		btn_sell_back_to_step1 = findViewById(R.id.btn_sell_back_to_step1);
		layout_sell_review = findViewById(R.id.layout_sell_review);
		linear4 = findViewById(R.id.linear4);
		linear5 = findViewById(R.id.linear5);
		view6 = findViewById(R.id.view6);
		linear13 = findViewById(R.id.linear13);
		linear14 = findViewById(R.id.linear14);
		textview7 = findViewById(R.id.textview7);
		tv_sell_review_amount = findViewById(R.id.tv_sell_review_amount);
		textview8 = findViewById(R.id.textview8);
		tv_sell_review_unit_price = findViewById(R.id.tv_sell_review_unit_price);
		textview9 = findViewById(R.id.textview9);
		tv_sell_review_total_price = findViewById(R.id.tv_sell_review_total_price);
		textview10 = findViewById(R.id.textview10);
		tv_sell_review_fee = findViewById(R.id.tv_sell_review_fee);
		linear15 = findViewById(R.id.linear15);
		textview11 = findViewById(R.id.textview11);
		textview12 = findViewById(R.id.textview12);
		imageview1 = findViewById(R.id.imageview1);
		textview13 = findViewById(R.id.textview13);
		textview14 = findViewById(R.id.textview14);
		btn_sell_view_my_offers = findViewById(R.id.btn_sell_view_my_offers);
		btn_sell_create_another = findViewById(R.id.btn_sell_create_another);
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvanna.wallet.utils.StatusBarUtil.matchToolbar(this);
		}
		
		com.harvanna.wallet.p2p.P2pOfferWizard.attachSell(this);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		com.harvanna.wallet.p2p.P2pOfferWizard.refreshPayments(this);
	}
	public void _MORE() {
		
	}
	
}
