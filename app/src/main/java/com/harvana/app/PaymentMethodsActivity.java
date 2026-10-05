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
		new androidx.appcompat.app.AlertDialog.Builder(this)
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
		View sheetView = LayoutInflater.from(this)
		.inflate(R.layout.bottom_sheet_payment_method_form, null);
		sheet.setContentView(sheetView);
		
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
		
		final PaymentMethod.Type[] selectedType = {PaymentMethod.Type.BANK};
		
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
			etPrimaryField.setHint(isBank ? "Bank name" : "Provider (e.g. MTN Mobile Money)");
			etAccountIdentifier.setHint(isBank ? "Account number" : "Phone number");
		};
		
		btnTypeBank.setOnClickListener(v -> { selectedType[0] = PaymentMethod.Type.BANK; applyTypeStyle.run(); });
		btnTypeMobileMoney.setOnClickListener(v -> { selectedType[0] = PaymentMethod.Type.MOBILE_MONEY; applyTypeStyle.run(); });
		
		if (editingMethod != null) {
			tvFormTitle.setText("Edit Payment Method");
			selectedType[0] = editingMethod.type;
			etPrimaryField.setText(editingMethod.primaryField);
			etAccountIdentifier.setText(editingMethod.accountIdentifier);
			etAccountName.setText(editingMethod.accountName);
			etNickname.setText(editingMethod.label);
			cbSetAsDefault.setChecked(editingMethod.isDefault);
		}
		applyTypeStyle.run();
		sheetView.findViewById(R.id.linear2).setOnClickListener(v -> cbSetAsDefault.toggle());
		
		btnSaveMethod.setOnClickListener(v -> {
			String primaryField = etPrimaryField.getText().toString().trim();
			String accountIdentifier = etAccountIdentifier.getText().toString().trim();
			String accountName = etAccountName.getText().toString().trim();
			String nickname = etNickname.getText().toString().trim();
			
			if (primaryField.isEmpty() || accountIdentifier.isEmpty() || accountName.isEmpty()) {
				tvFormError.setText("Please fill in all required fields.");
				tvFormError.setVisibility(View.VISIBLE);
				return;
			}
			tvFormError.setVisibility(View.GONE);
			
			String label = nickname.isEmpty() ? primaryField : nickname;
			
			if (editingMethod != null) {
				editingMethod.type = selectedType[0];
				editingMethod.primaryField = primaryField;
				editingMethod.accountIdentifier = accountIdentifier;
				editingMethod.accountName = accountName;
				editingMethod.label = label;
				editingMethod.isDefault = cbSetAsDefault.isChecked();
				PaymentMethodManager.update(this, editingMethod);
			} else {
				PaymentMethod method = new PaymentMethod(null, selectedType[0], label,
				primaryField, accountIdentifier, accountName, cbSetAsDefault.isChecked());
				PaymentMethodManager.add(this, method);
			}
			
			sheet.dismiss();
			_refreshList();
			Toast.makeText(this, "Payment method saved", Toast.LENGTH_SHORT).show();
		});
		
		sheet.show();
	}
	
	{
	}
	
}
