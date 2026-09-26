package com.ajoo.app;

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
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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


public class CreateP2pBuyOfferActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout create_buy_offer_root;
	private LinearLayout panel_buy_step_1;
	private LinearLayout panel_buy_step_2;
	private LinearLayout panel_buy_step_3;
	private TextView tv_buy_step_label;
	private MaterialCardView card_buy_info;
	private TextView tv_buy_amount_section;
	private MaterialCardView card_buy_amount;
	private TextView tv_buy_amount_note;
	private TextView tv_buy_price_section;
	private MaterialButtonToggleGroup toggle_buy_pricing_mode;
	private LinearLayout group_buy_price_fixed;
	private LinearLayout group_buy_price_floating;
	private TextView tv_buy_payment_section;
	private MaterialCardView card_buy_payment_methods;
	private TextView tv_buy_settings_section;
	private MaterialCardView card_buy_settings;
	private TextView tv_buy_terms_section;
	private MaterialCardView card_buy_terms;
	private TextView tv_buy_terms_counter;
	private TextView tv_buy_step1_error;
	private MaterialButton btn_preview_buy_offer;
	private LinearLayout layout_buy_info;
	private ImageView imageview_buy_info;
	private TextView tv_buy_info;
	private LinearLayout layout_buy_amount;
	private EditText et_buy_amount;
	private TextView tv_buy_amount_currency;
	private MaterialButton btn_buy_price_fixed;
	private MaterialButton btn_buy_price_floating;
	private MaterialCardView card_buy_price;
	private TextView tv_buy_price_note;
	private LinearLayout layout_buy_price;
	private EditText et_buy_price;
	private TextView tv_buy_price_currency;
	private MaterialCardView card_buy_margin;
	private TextView tv_buy_calculated_price;
	private LinearLayout linear4;
	private EditText et_buy_margin;
	private TextView tv_buy_margin_percent_label;
	private LinearLayout layout_buy_payment_methods;
	private LinearLayout layout_buy_settings;
	private LinearLayout row_buy_trade_limit;
	private View view10;
	private LinearLayout row_buy_offer_duration;
	private View view_buy_payment_window_divider;
	private LinearLayout row_buy_payment_window;
	private TextView tv_buy_trade_limit_label;
	private TextView tv_buy_trade_limit_value;
	private TextView tv_buy_offer_duration_label;
	private TextView tv_buy_offer_duration_value;
	private TextView tv_buy_payment_window_label;
	private TextView tv_buy_payment_window_value;
	private EditText et_buy_terms;
	private TextView tv_buy_summary_title;
	private MaterialCardView card_buy_review;
	private MaterialCardView card_buy_review_note;
	private TextView tv_buy_step2_error;
	private MaterialButton btn_publish_buy_offer;
	private MaterialButton btn_buy_back_to_step1;
	private LinearLayout layout_buy_review;
	private LinearLayout linear11;
	private LinearLayout linear12;
	private View view13;
	private LinearLayout linear14;
	private LinearLayout linear15;
	private TextView textview11;
	private TextView tv_buy_review_amount;
	private TextView textview12;
	private TextView tv_buy_review_unit_price;
	private TextView textview13;
	private TextView tv_buy_review_total_price;
	private TextView textview14;
	private TextView tv_buy_review_fee;
	private LinearLayout linear16;
	private TextView textview15;
	private TextView textview16;
	private ImageView imageview2;
	private TextView textview17;
	private TextView textview18;
	private MaterialButton btn_buy_view_my_offers;
	private MaterialButton btn_buy_create_another;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.create_p2p_buy_offer);
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
		create_buy_offer_root = findViewById(R.id.create_buy_offer_root);
		panel_buy_step_1 = findViewById(R.id.panel_buy_step_1);
		panel_buy_step_2 = findViewById(R.id.panel_buy_step_2);
		panel_buy_step_3 = findViewById(R.id.panel_buy_step_3);
		tv_buy_step_label = findViewById(R.id.tv_buy_step_label);
		card_buy_info = findViewById(R.id.card_buy_info);
		tv_buy_amount_section = findViewById(R.id.tv_buy_amount_section);
		card_buy_amount = findViewById(R.id.card_buy_amount);
		tv_buy_amount_note = findViewById(R.id.tv_buy_amount_note);
		tv_buy_price_section = findViewById(R.id.tv_buy_price_section);
		toggle_buy_pricing_mode = findViewById(R.id.toggle_buy_pricing_mode);
		group_buy_price_fixed = findViewById(R.id.group_buy_price_fixed);
		group_buy_price_floating = findViewById(R.id.group_buy_price_floating);
		tv_buy_payment_section = findViewById(R.id.tv_buy_payment_section);
		card_buy_payment_methods = findViewById(R.id.card_buy_payment_methods);
		tv_buy_settings_section = findViewById(R.id.tv_buy_settings_section);
		card_buy_settings = findViewById(R.id.card_buy_settings);
		tv_buy_terms_section = findViewById(R.id.tv_buy_terms_section);
		card_buy_terms = findViewById(R.id.card_buy_terms);
		tv_buy_terms_counter = findViewById(R.id.tv_buy_terms_counter);
		tv_buy_step1_error = findViewById(R.id.tv_buy_step1_error);
		btn_preview_buy_offer = findViewById(R.id.btn_preview_buy_offer);
		layout_buy_info = findViewById(R.id.layout_buy_info);
		imageview_buy_info = findViewById(R.id.imageview_buy_info);
		tv_buy_info = findViewById(R.id.tv_buy_info);
		layout_buy_amount = findViewById(R.id.layout_buy_amount);
		et_buy_amount = findViewById(R.id.et_buy_amount);
		tv_buy_amount_currency = findViewById(R.id.tv_buy_amount_currency);
		btn_buy_price_fixed = findViewById(R.id.btn_buy_price_fixed);
		btn_buy_price_floating = findViewById(R.id.btn_buy_price_floating);
		card_buy_price = findViewById(R.id.card_buy_price);
		tv_buy_price_note = findViewById(R.id.tv_buy_price_note);
		layout_buy_price = findViewById(R.id.layout_buy_price);
		et_buy_price = findViewById(R.id.et_buy_price);
		tv_buy_price_currency = findViewById(R.id.tv_buy_price_currency);
		card_buy_margin = findViewById(R.id.card_buy_margin);
		tv_buy_calculated_price = findViewById(R.id.tv_buy_calculated_price);
		linear4 = findViewById(R.id.linear4);
		et_buy_margin = findViewById(R.id.et_buy_margin);
		tv_buy_margin_percent_label = findViewById(R.id.tv_buy_margin_percent_label);
		layout_buy_payment_methods = findViewById(R.id.layout_buy_payment_methods);
		layout_buy_settings = findViewById(R.id.layout_buy_settings);
		row_buy_trade_limit = findViewById(R.id.row_buy_trade_limit);
		view10 = findViewById(R.id.view10);
		row_buy_offer_duration = findViewById(R.id.row_buy_offer_duration);
		view_buy_payment_window_divider = findViewById(R.id.view_buy_payment_window_divider);
		row_buy_payment_window = findViewById(R.id.row_buy_payment_window);
		tv_buy_trade_limit_label = findViewById(R.id.tv_buy_trade_limit_label);
		tv_buy_trade_limit_value = findViewById(R.id.tv_buy_trade_limit_value);
		tv_buy_offer_duration_label = findViewById(R.id.tv_buy_offer_duration_label);
		tv_buy_offer_duration_value = findViewById(R.id.tv_buy_offer_duration_value);
		tv_buy_payment_window_label = findViewById(R.id.tv_buy_payment_window_label);
		tv_buy_payment_window_value = findViewById(R.id.tv_buy_payment_window_value);
		et_buy_terms = findViewById(R.id.et_buy_terms);
		tv_buy_summary_title = findViewById(R.id.tv_buy_summary_title);
		card_buy_review = findViewById(R.id.card_buy_review);
		card_buy_review_note = findViewById(R.id.card_buy_review_note);
		tv_buy_step2_error = findViewById(R.id.tv_buy_step2_error);
		btn_publish_buy_offer = findViewById(R.id.btn_publish_buy_offer);
		btn_buy_back_to_step1 = findViewById(R.id.btn_buy_back_to_step1);
		layout_buy_review = findViewById(R.id.layout_buy_review);
		linear11 = findViewById(R.id.linear11);
		linear12 = findViewById(R.id.linear12);
		view13 = findViewById(R.id.view13);
		linear14 = findViewById(R.id.linear14);
		linear15 = findViewById(R.id.linear15);
		textview11 = findViewById(R.id.textview11);
		tv_buy_review_amount = findViewById(R.id.tv_buy_review_amount);
		textview12 = findViewById(R.id.textview12);
		tv_buy_review_unit_price = findViewById(R.id.tv_buy_review_unit_price);
		textview13 = findViewById(R.id.textview13);
		tv_buy_review_total_price = findViewById(R.id.tv_buy_review_total_price);
		textview14 = findViewById(R.id.textview14);
		tv_buy_review_fee = findViewById(R.id.tv_buy_review_fee);
		linear16 = findViewById(R.id.linear16);
		textview15 = findViewById(R.id.textview15);
		textview16 = findViewById(R.id.textview16);
		imageview2 = findViewById(R.id.imageview2);
		textview17 = findViewById(R.id.textview17);
		textview18 = findViewById(R.id.textview18);
		btn_buy_view_my_offers = findViewById(R.id.btn_buy_view_my_offers);
		btn_buy_create_another = findViewById(R.id.btn_buy_create_another);
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(backgroundColor);
		}
		
		com.ajoo.app.p2p.P2pOfferWizard.attachBuy(this);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		com.ajoo.app.p2p.P2pOfferWizard.refreshPayments(this);
	}
	public void _MORE() {
		
	}	
}
// Force update