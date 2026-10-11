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
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import com.harvana.app.wallet.PaymentMethod;
import com.harvana.app.wallet.PaymentMethodManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;


public class PaymentMethodsActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private LinearLayout linear80;
	private TextView textview88;
	private TextView textview89;
	private LinearLayout layoutEmptyState;
	private LinearLayout layoutMethodsList;
	private MaterialButton btnAddPaymentMethod;
	private MaterialCardView cardview19;
	private TextView textview90;
	private TextView textview91;
	private ImageView imageview48;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.payment_methods);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		com.harvana.app.utils.AppBar.header(this, "Payment methods", "Saved for your P2P listings", R.id.textview88);
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
		linear80 = findViewById(R.id.linear80);
		textview88 = findViewById(R.id.textview88);
		textview89 = findViewById(R.id.textview89);
		layoutEmptyState = findViewById(R.id.layoutEmptyState);
		layoutMethodsList = findViewById(R.id.layoutMethodsList);
		btnAddPaymentMethod = findViewById(R.id.btnAddPaymentMethod);
		cardview19 = findViewById(R.id.cardview19);
		textview90 = findViewById(R.id.textview90);
		textview91 = findViewById(R.id.textview91);
		imageview48 = findViewById(R.id.imageview48);
	}
	
	private void initializeLogic() {
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("Payment Methods");
			getSupportActionBar().setSubtitle("Add and delete payment methods");
		}
		btnAddPaymentMethod.setOnClickListener(v -> _showFormSheet(null));
		_refreshList();
	}
	
	public void _MORE() {
	}
	@Override
	protected void onResume() {
		super.onResume();
		// Cheap enough to just rebuild the whole list on every return to
		// this screen rather than track incremental state — this list
		// only ever holds a handful of entries.
		_refreshList();
	}
	
	private void _refreshList() {
		List<PaymentMethod> methods = PaymentMethodManager.loadAll(this);
		layoutMethodsList.removeAllViews();
		
		if (methods.isEmpty()) {
			layoutEmptyState.setVisibility(View.VISIBLE);
			return;
		}
		layoutEmptyState.setVisibility(View.GONE);
		
		LayoutInflater inflater = LayoutInflater.from(this);
		for (PaymentMethod method : methods) {
			View row = inflater.inflate(R.layout.payment_method_row, layoutMethodsList, false);
			
			TextView tvLabel = row.findViewById(R.id.tvMethodLabel);
			TextView tvSummary = row.findViewById(R.id.tvMethodSummary);
			TextView tvDefaultBadge = row.findViewById(R.id.tvDefaultBadge);
			ImageView btnMenu = row.findViewById(R.id.btnMethodMenu);
			ImageView ivIcon = row.findViewById(R.id.ivMethodIcon);
			if (ivIcon != null && method.type == PaymentMethod.Type.BANK) {
				ivIcon.setPadding(0, 0, 0, 0);
				com.harvana.app.banks.BankLogo.into(ivIcon, method.primaryField);
			}
			
			tvLabel.setText(method.label != null && !method.label.isEmpty()
			? method.label : method.primaryField);
			tvSummary.setText(method.displaySummary());
			tvDefaultBadge.setVisibility(method.isDefault ? View.VISIBLE : View.GONE);
			
			btnMenu.setOnClickListener(v -> _showRowMenu(v, method));
			
			layoutMethodsList.addView(row);
		}
	}
	
	private void _showRowMenu(View anchor, PaymentMethod method) {
		PopupMenu menu = new PopupMenu(this, anchor);
		menu.getMenu().add("Edit");
		if (!method.isDefault) {
			menu.getMenu().add("Set as Default");
		}
		menu.getMenu().add("Delete");
		
		menu.setOnMenuItemClickListener(item -> {
			String title = item.getTitle().toString();
			if ("Edit".equals(title)) {
				_showFormSheet(method);
			} else if ("Set as Default".equals(title)) {
				method.isDefault = true;
				PaymentMethodManager.update(this, method);
				_refreshList();
			} else if ("Delete".equals(title)) {
				_confirmDelete(method);
			}
			return true;
		});
		menu.show();
	}
	
	private void _confirmDelete(PaymentMethod method) {
		new com.harvana.app.ui.SheetDialog.Builder(this)
		.setTitle("Remove payment method")
		.setMessage("Remove \"" + method.label + "\"? This only removes it from this device — it doesn't affect any listing you've already posted with it.")
		.setPositiveButton("Remove", (dialog, which) -> {
			PaymentMethodManager.remove(this, method.id);
			_refreshList();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _showFormSheet(PaymentMethod editingMethod) {
		BottomSheetDialog sheet = com.harvana.app.p2p.ThemedDialogs.sheet(this);
		sheet.getBehavior().setSkipCollapsed(true);
		sheet.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
		if (sheet.getWindow() != null) sheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_payment_method_form, null);
		// The form grows with a country's extra boxes (sort code, routing number…), so it scrolls.
		androidx.core.widget.NestedScrollView scroller = new androidx.core.widget.NestedScrollView(this);
		scroller.addView(sheetView);
		sheet.setContentView(scroller);
		
		TextView tvFormTitle = sheetView.findViewById(R.id.tvFormTitle);
		Button btnTypeBank = sheetView.findViewById(R.id.btnTypeBank);
		Button btnTypeMobileMoney = sheetView.findViewById(R.id.btnTypeMobileMoney);
		EditText etPrimaryField = sheetView.findViewById(R.id.etPrimaryField);
		EditText etAccountIdentifier = sheetView.findViewById(R.id.etAccountIdentifier);
		EditText etAccountName = sheetView.findViewById(R.id.etAccountName);
		EditText etNickname = sheetView.findViewById(R.id.etNickname);
		CheckBox cbSetAsDefault = sheetView.findViewById(R.id.cbSetAsDefault);
		TextView tvFormError = sheetView.findViewById(R.id.tvFormError);
		Button btnSaveMethod = sheetView.findViewById(R.id.btnSaveMethod);
		final ImageView ivBankLogo = sheetView.findViewById(R.id.ivBankLogo);
		final TextView tvChooseBank = sheetView.findViewById(R.id.tvChooseBank);
		final TextView tvAccountLabel = sheetView.findViewById(R.id.textview2);
		final View accountCard = sheetView.findViewById(R.id.cardview2);
		final LinearLayout form = (LinearLayout) sheetView;
		
		final PaymentMethod.Type[] selectedType = {PaymentMethod.Type.BANK};
		final String[] country = {editingMethod != null && editingMethod.country != null && !editingMethod.country.isEmpty()
				? editingMethod.country
				: (editingMethod != null ? "NG" : com.harvana.app.banks.BankDirectory.defaultCountry(this))};
		final com.harvana.app.banks.BankDirectory.Bank[] chosenBank = {null};
		final Map<String, EditText> extraBoxes = new LinkedHashMap<>();
		
		// ── Country row, above the bank ──
		final TextView tvCountry = new TextView(this);
		tvCountry.setTextSize(15);
		tvCountry.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextPrimary));
		tvCountry.setPadding(_dp(14), _dp(14), _dp(14), _dp(14));
		android.graphics.drawable.GradientDrawable cbg = new android.graphics.drawable.GradientDrawable();
		cbg.setCornerRadius(_dp(16));
		cbg.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorSurface));
		cbg.setStroke(_dp(1), androidx.core.content.ContextCompat.getColor(this, R.color.colorCardStroke));
		tvCountry.setBackground(cbg);
		TextView countryLabel = new TextView(this);
		countryLabel.setText("COUNTRY OF THE ACCOUNT");
		countryLabel.setTextSize(11);
		countryLabel.setTypeface(null, android.graphics.Typeface.BOLD);
		countryLabel.setLetterSpacing(0.06f);
		countryLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextTertiary));
		LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		clp.setMargins(_dp(4), _dp(16), 0, _dp(6));
		int bankLabelIndex = form.indexOfChild(sheetView.findViewById(R.id.textview1));
		form.addView(countryLabel, bankLabelIndex, clp);
		form.addView(tvCountry, bankLabelIndex + 1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		// ── The country's extra boxes, under the account number ──
		final LinearLayout extrasBox = new LinearLayout(this);
		extrasBox.setOrientation(LinearLayout.VERTICAL);
		form.addView(extrasBox, form.indexOfChild(accountCard) + 1);
		
		final TextView tvLookup = new TextView(this);
		tvLookup.setTextSize(12);
		tvLookup.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorSuccess));
		tvLookup.setPadding(_dp(4), _dp(6), 0, 0);
		tvLookup.setVisibility(View.GONE);
		form.addView(tvLookup, form.indexOfChild(extrasBox) + 1);
		
		final String[] scheme = {"NUBAN"};
		final Runnable[] buildFields = new Runnable[1];
		
		// Shows the logo of whatever bank the name matches in this country ("gtb" → GTBank, "chase" → Chase).
		final Runnable showBank = () -> {
			com.harvana.app.banks.BankDirectory.Bank b = com.harvana.app.banks.BankDirectory.match(this, country[0], etPrimaryField.getText().toString());
			if (b == null && chosenBank[0] != null && "custom".equals(chosenBank[0].kind)
					&& chosenBank[0].name.equalsIgnoreCase(etPrimaryField.getText().toString().trim())) b = chosenBank[0];
			boolean kindChanged = (b == null ? "" : b.kind).equals(chosenBank[0] == null ? "" : chosenBank[0].kind) == false;
			chosenBank[0] = b;
			if (b != null) {
				ivBankLogo.setVisibility(View.VISIBLE);
				com.harvana.app.banks.BankLogo.into(ivBankLogo, b);
			} else {
				ivBankLogo.setVisibility(View.GONE);
			}
			if (kindChanged && buildFields[0] != null) buildFields[0].run();
		};
		
		buildFields[0] = () -> {
			com.harvana.app.banks.BankDirectory.Country k = com.harvana.app.banks.BankDirectory.country(this, country[0]);
			scheme[0] = k.scheme;
			tvCountry.setText(k.flag() + "   " + k.name + (k.currency.isEmpty() ? "" : "  ·  " + k.currency) + "      Change");
			String kind = selectedType[0] == PaymentMethod.Type.MOBILE_MONEY ? "mobile_money"
					: (chosenBank[0] == null ? "bank" : chosenBank[0].kind);
			com.harvana.app.banks.AccountRules.Layout l = com.harvana.app.banks.AccountRules.layout(scheme[0], kind);
			tvAccountLabel.setText(l.mainLabel);
			etAccountIdentifier.setHint(l.mainHint);
			etAccountIdentifier.setInputType(l.mainDigits ? android.text.InputType.TYPE_CLASS_NUMBER
					: android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
			if (kind.equals("fintech")) etAccountIdentifier.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			if (kind.equals("mobile_money")) etAccountIdentifier.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
			Map<String, String> kept = new HashMap<>();
			for (Map.Entry<String, EditText> e : extraBoxes.entrySet()) kept.put(e.getKey(), e.getValue().getText().toString());
			extraBoxes.clear();
			extrasBox.removeAllViews();
			for (com.harvana.app.banks.AccountRules.Field f : l.extras) {
				TextView lab = new TextView(this);
				lab.setText(f.label);
				lab.setTextSize(11);
				lab.setTypeface(null, android.graphics.Typeface.BOLD);
				lab.setLetterSpacing(0.06f);
				lab.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextTertiary));
				LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				llp.setMargins(_dp(4), _dp(16), 0, _dp(6));
				extrasBox.addView(lab, llp);
				EditText box = new EditText(this);
				box.setSingleLine(true);
				box.setHint(f.hint);
				box.setTextSize(15);
				box.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextPrimary));
				box.setHintTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorTextTertiary));
				box.setInputType(f.digits ? android.text.InputType.TYPE_CLASS_NUMBER
						: android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
				android.graphics.drawable.GradientDrawable bb = new android.graphics.drawable.GradientDrawable();
				bb.setCornerRadius(_dp(16));
				bb.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorSurface));
				bb.setStroke(_dp(1), androidx.core.content.ContextCompat.getColor(this, R.color.colorCardStroke));
				box.setBackground(bb);
				box.setPadding(_dp(14), _dp(14), _dp(14), _dp(14));
				String old = kept.get(f.key);
				if (old == null && editingMethod != null && editingMethod.details != null) old = editingMethod.details.get(f.key);
				if (old != null) box.setText(old);
				extrasBox.addView(box, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				extraBoxes.put(f.key, box);
				if ("ifsc".equals(f.key)) box.addTextChangedListener(new android.text.TextWatcher() {
					@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
					@Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
					@Override public void afterTextChanged(android.text.Editable s) {
						final String code = s.toString();
						if (!com.harvana.app.banks.AccountRules.isIfsc(code)) return;
						new Thread(() -> {
							com.harvana.app.banks.BankLookup.Result r = com.harvana.app.banks.BankLookup.ifsc(code);
							if (r == null) return;
							runOnUiThread(() -> {
								if (etPrimaryField.getText().toString().trim().isEmpty()) etPrimaryField.setText(r.bankName);
								tvLookup.setText("✓ " + r.bankName + (r.branch.isEmpty() ? "" : ", " + r.branch));
								tvLookup.setVisibility(View.VISIBLE);
							});
						}).start();
					}
				});
			}
		};
		
		// An IBAN names its bank: fill in the bank and BIC where the free lookup covers it (DE, AT, CH, NL, BE, LU, LI).
		etAccountIdentifier.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
			@Override public void afterTextChanged(android.text.Editable s) {
				if (!"IBAN".equals(scheme[0])) return;
				final String iban = s.toString();
				if (!com.harvana.app.banks.AccountRules.isIban(iban)) { tvLookup.setVisibility(View.GONE); return; }
				new Thread(() -> {
					com.harvana.app.banks.BankLookup.Result r = com.harvana.app.banks.BankLookup.iban(iban);
					runOnUiThread(() -> {
						if (r == null) { tvLookup.setText("✓ IBAN checks out"); tvLookup.setVisibility(View.VISIBLE); return; }
						if (etPrimaryField.getText().toString().trim().isEmpty()) etPrimaryField.setText(r.bankName);
						EditText bic = extraBoxes.get("bic");
						if (bic != null && bic.getText().toString().trim().isEmpty() && !r.bic.isEmpty()) bic.setText(r.bic);
						tvLookup.setText("✓ " + r.bankName + (r.branch.isEmpty() ? "" : ", " + r.branch));
						tvLookup.setVisibility(View.VISIBLE);
					});
				}).start();
			}
		});
		
		Runnable openPicker = () -> com.harvana.app.banks.BankPickerSheet.show(this, country[0], "", bank -> {
			chosenBank[0] = bank;
			if ("mobile_money".equals(bank.kind)) selectedType[0] = PaymentMethod.Type.MOBILE_MONEY;
			else if (selectedType[0] == PaymentMethod.Type.MOBILE_MONEY) selectedType[0] = PaymentMethod.Type.BANK;
			etPrimaryField.setText(bank.name);
			etPrimaryField.setSelection(bank.name.length());
			showBank.run();
			if (buildFields[0] != null) buildFields[0].run();
			etAccountIdentifier.requestFocus();
		});
		tvChooseBank.setOnClickListener(v -> openPicker.run());
		tvCountry.setOnClickListener(v -> com.harvana.app.banks.CountryPickerSheet.show(this, k -> {
			country[0] = k.iso;
			chosenBank[0] = null;
			etPrimaryField.setText("");
			tvLookup.setVisibility(View.GONE);
			buildFields[0].run();
			openPicker.run();
		}));
		etPrimaryField.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) { showBank.run(); }
			@Override public void afterTextChanged(android.text.Editable s) {}
		});
		
		Runnable applyTypeStyle = () -> {
			boolean isBank = selectedType[0] == PaymentMethod.Type.BANK;
			btnTypeBank.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this,
			isBank ? R.color.colorAccent : R.color.colorSurfaceRaised));
			btnTypeBank.setTextColor(androidx.core.content.ContextCompat.getColor(this,
			isBank ? R.color.colorTextOnAccent : R.color.colorTextSecondary));
			btnTypeMobileMoney.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(this,
			!isBank ? R.color.colorAccent : R.color.colorSurfaceRaised));
			btnTypeMobileMoney.setTextColor(androidx.core.content.ContextCompat.getColor(this,
			!isBank ? R.color.colorTextOnAccent : R.color.colorTextSecondary));
			etPrimaryField.setHint(isBank ? "Bank or payment app" : "Provider (e.g. MTN MoMo, M-Pesa)");
			buildFields[0].run();
		};
		
		btnTypeBank.setOnClickListener(v -> { selectedType[0] = PaymentMethod.Type.BANK; applyTypeStyle.run(); });
		btnTypeMobileMoney.setOnClickListener(v -> { selectedType[0] = PaymentMethod.Type.MOBILE_MONEY; applyTypeStyle.run(); });
		
		if (editingMethod != null) {
			tvFormTitle.setText("Edit Payment Method");
			selectedType[0] = editingMethod.type;
			etPrimaryField.setText(editingMethod.primaryField);
			etAccountIdentifier.setText(editingMethod.accountMain != null && !editingMethod.accountMain.isEmpty()
					? editingMethod.accountMain : editingMethod.accountIdentifier);
			etAccountName.setText(editingMethod.accountName);
			etNickname.setText(editingMethod.label);
			cbSetAsDefault.setChecked(editingMethod.isDefault);
		}
		showBank.run();
		applyTypeStyle.run();
		if (editingMethod == null) openPicker.run();      // a new account starts from the country's bank list
		sheetView.findViewById(R.id.linear2).setOnClickListener(v -> cbSetAsDefault.toggle());
		
		btnSaveMethod.setOnClickListener(v -> {
			String primaryField = etPrimaryField.getText().toString().trim();
			String accountMain = etAccountIdentifier.getText().toString().trim();
			String accountName = etAccountName.getText().toString().trim();
			String nickname = etNickname.getText().toString().trim();
			
			if (primaryField.isEmpty() || accountMain.isEmpty() || accountName.isEmpty()) {
				tvFormError.setText("Please fill in the bank, the account and the account holder's name.");
				tvFormError.setVisibility(View.VISIBLE);
				return;
			}
			
			com.harvana.app.banks.BankDirectory.Bank bank = chosenBank[0];
			String kind = selectedType[0] == PaymentMethod.Type.MOBILE_MONEY ? "mobile_money"
					: (bank == null || "custom".equals(bank.kind) ? "bank" : bank.kind);
			Map<String, String> details = new LinkedHashMap<>();
			for (Map.Entry<String, EditText> e : extraBoxes.entrySet()) {
				String val = e.getValue().getText().toString().trim();
				if (!val.isEmpty()) details.put(e.getKey(), val);
			}
			// The country's own rules: IBAN check, routing checksum, sort code, IFSC, CLABE, digit counts.
			String problem = com.harvana.app.banks.AccountRules.check(scheme[0], kind, country[0], accountMain, details);
			if (problem != null) {
				tvFormError.setText(problem);
				tvFormError.setVisibility(View.VISIBLE);
				return;
			}
			if (bank != null && !"custom".equals(bank.kind)) {
				primaryField = bank.name;                   // the official name goes on the offer
				if ("NUBAN".equals(scheme[0]) && !com.harvana.app.banks.BankDirectory.nubanLooksValid(bank, accountMain.replaceAll("\\D", ""))
						&& tvFormError.getTag() == null) {
					tvFormError.setTag("warned");          // tap Save again to keep it as typed
					tvFormError.setText("This doesn't look like a " + bank.display() + " account number. Check it, or tap Save again to keep it.");
					tvFormError.setVisibility(View.VISIBLE);
					return;
				}
			}
			tvFormError.setVisibility(View.GONE);
			com.harvana.app.banks.BankDirectory.rememberCountry(this, country[0]);
			
			String accountIdentifier = com.harvana.app.banks.AccountRules.compose(scheme[0], kind, accountMain, details);
			String label = nickname.isEmpty() ? (bank != null && !"custom".equals(bank.kind) ? bank.display() : primaryField) : nickname;
			
			PaymentMethod method = editingMethod != null ? editingMethod
					: new PaymentMethod(null, selectedType[0], label, primaryField, accountIdentifier, accountName, cbSetAsDefault.isChecked());
			method.type = selectedType[0];
			method.primaryField = primaryField;
			method.accountIdentifier = accountIdentifier;
			method.accountName = accountName;
			method.label = label;
			method.isDefault = cbSetAsDefault.isChecked();
			method.country = country[0];
			method.accountMain = accountMain;
			method.details = details;
			if (editingMethod != null) PaymentMethodManager.update(this, method);
			else PaymentMethodManager.add(this, method);
			
			sheet.dismiss();
			_refreshList();
			Toast.makeText(this, "Payment method saved", Toast.LENGTH_SHORT).show();
		});
		
		sheet.show();
	}
	
	private int _dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
	
	{
	}
	
}
