package com.harvana.app;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import com.harvana.app.crypto.Ed25519;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.group.GroupClosePolicy;
import com.harvana.app.group.GroupCloser;
import com.harvana.app.group.GroupIdentity;
import com.harvana.app.group.GroupIdentityResolver;
import com.harvana.app.group.JoinRequestManager;
import com.harvana.app.offline.OfflineCacheManager;
import com.harvana.app.p2p.ThemedDialogs;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.SecureWalletStorage;
import com.harvana.app.wallet.WalletGenerator;
import com.harvana.app.wallet.WalletManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupSettingsActivity extends AppCompatActivity {

	private androidx.coordinatorlayout.widget.CoordinatorLayout _coordinator;
	private com.google.android.material.appbar.AppBarLayout _app_bar;
	private androidx.appcompat.widget.Toolbar _toolbar;
	private androidx.core.widget.NestedScrollView vscroll2;
	private LinearLayout linear74;
	private LinearLayout linear39;
	private MaterialCardView cardHero;

	private MaterialCardView cardview11;
	private MaterialCardView cardview13;
	private MaterialCardView cardview15;
	private MaterialCardView panel_fixed;
	private MaterialCardView panel_accumulative;
	private MaterialCardView cardview19;
	private MaterialCardView cardview20;
	private MaterialCardView cardview10;
	private LinearLayout linear72;
	private LinearLayout layoutBlockedMembers;
	private TextView tvBlockedEmpty;
	private LinearLayout linear73;
	private LinearLayout linear40;
	private ImageView imageview16;
	private LinearLayout linear41;
	private TextView textview56;
	private LinearLayout linear42;
	private TextView tvHeroFrequency;
	private ImageView ivHeroTypeIcon;
	private TextView tvHeroTypeName;
	private LinearLayout linear43;
	private LinearLayout rowVisibility;
	private View view45;
	private LinearLayout linear46;
	private View view48;
	private LinearLayout linear49;
	private ImageView imageview17;
	private LinearLayout linear44;
	private MaterialCardView cardview12;
	private TextView textview57;
	private TextView textview58;
	private Spinner spinVisibility;
	private ImageView imageview18;
	private LinearLayout linear47;
	private SwitchCompat switchApproval;
	private TextView textview59;
	private TextView textview60;
	private ImageView imageview19;
	private LinearLayout linear50;
	private SwitchCompat switchInvite;
	private TextView textview61;
	private TextView textview62;
	private LinearLayout linear51;
	private LinearLayout linear52;
	private MaterialCardView cardview14;
	private TextView tvDescriptionCount;
	private ImageView imageview20;
	private LinearLayout linear53;
	private TextView textview63;
	private TextView textview64;
	private EditText etDescription;
	private LinearLayout linear54;
	private LinearLayout linear55;
	private View view57;
	private LinearLayout panel_rotating;
	private ImageView imageview21;
	private LinearLayout linear56;
	private MaterialCardView cardview16;
	private TextView textview65;
	private TextView textview66;
	private EditText etContribution;
	private LinearLayout rowManageRotating;
	private View view59;
	private LinearLayout linear60;
	private View view62;
	private LinearLayout linear63;
	private ImageView imageview22;
	private LinearLayout linear58;
	private ImageView imageview23;
	private TextView tvTypeSettingsTitle;
	private TextView tvTypeSettingsSubtitle;
	private ImageView imageview24;
	private LinearLayout linear61;
	private MaterialCardView cardview17;
	private TextView textview67;
	private TextView textview68;
	private EditText etGracePeriod;
	private ImageView imageview25;
	private LinearLayout linear64;
	private MaterialCardView cardview18;
	private TextView textview69;
	private TextView textview70;
	private EditText etCommission;
	private LinearLayout linear18;
	private TextView textview13;
	private LinearLayout linear19;
	private View view21;
	private TextView tv_fixed_progress;
	private LinearLayout linear22;
	private LinearLayout linear20;
	private MaterialCardView cardview6;
	private TextView textview14;
	private TextView textview15;
	private EditText etFixedGoal;
	private TextView textview16;
	private MaterialCardView cardview7;
	private Spinner spinDisbursementRule;
	private LinearLayout linear23;
	private TextView textview17;
	private LinearLayout linear24;
	private View view26;
	private LinearLayout linear27;
	private View view28;
	private TextView tv_loan_active_count;
	private LinearLayout linear25;
	private MaterialCardView cardview8;
	private TextView textview18;
	private TextView textview19;
	private EditText etLoanInterestRate;
	private TextView textview20;
	private MaterialCardView cardview9;
	private Spinner spinWithdrawalRule;
	private LinearLayout linear67;
	private ImageView imageview26;
	private TextView tvRuleInfoBanner;
	private LinearLayout linear68;
	private LinearLayout linear69;
	private MaterialCardView cardWalletWarning;
	private MaterialButton btnRestoreWallet;
	private ImageView imageview27;
	private LinearLayout linear70;
	private ImageView btnCopyWallet;
	private TextView textview73;
	private TextView tvWalletAddress;
	private LinearLayout linear71;
	private ImageView imageview28;
	private TextView textview74;
	private LinearLayout linear29;
	private LinearLayout linear30;
	private View view32;
	private LinearLayout linear33;
	private ImageView imageview3;
	private LinearLayout linear31;
	private SwitchCompat switchMutePayments;
	private TextView textview21;
	private TextView textview22;
	private ImageView imageview4;
	private LinearLayout linear34;
	private SwitchCompat switchMuteNotices;
	private TextView textview23;
	private TextView textview24;
	private ImageView imageview29;
	private TextView textview75;
	private TextView tvBlockedCountBadge;
	private ImageView imageview30;
	private TextView textview76;
	private MaterialButton btnSaveSettings;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.group_settings);
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
		vscroll2 = findViewById(R.id.vscroll2);
		linear74 = findViewById(R.id.linear74);
		linear39 = findViewById(R.id.linear39);
		cardHero = findViewById(R.id.cardHero);
		cardview11 = findViewById(R.id.cardview11);
		cardview13 = findViewById(R.id.cardview13);
		cardview15 = findViewById(R.id.cardview15);
		panel_fixed = findViewById(R.id.panel_fixed);
		panel_accumulative = findViewById(R.id.panel_accumulative);
		cardview19 = findViewById(R.id.cardview19);
		cardview20 = findViewById(R.id.cardview20);
		cardview10 = findViewById(R.id.cardview10);
		linear72 = findViewById(R.id.linear72);
		layoutBlockedMembers = findViewById(R.id.layoutBlockedMembers);
		tvBlockedEmpty = findViewById(R.id.tvBlockedEmpty);
		linear73 = findViewById(R.id.linear73);
		linear40 = findViewById(R.id.linear40);
		imageview16 = findViewById(R.id.imageview16);
		linear41 = findViewById(R.id.linear41);
		textview56 = findViewById(R.id.textview56);
		linear42 = findViewById(R.id.linear42);
		tvHeroFrequency = findViewById(R.id.tvHeroFrequency);
		ivHeroTypeIcon = findViewById(R.id.ivHeroTypeIcon);
		tvHeroTypeName = findViewById(R.id.tvHeroTypeName);
		linear43 = findViewById(R.id.linear43);
		rowVisibility = findViewById(R.id.rowVisibility);
		view45 = findViewById(R.id.view45);
		linear46 = findViewById(R.id.linear46);
		view48 = findViewById(R.id.view48);
		linear49 = findViewById(R.id.linear49);
		imageview17 = findViewById(R.id.imageview17);
		linear44 = findViewById(R.id.linear44);
		cardview12 = findViewById(R.id.cardview12);
		textview57 = findViewById(R.id.textview57);
		textview58 = findViewById(R.id.textview58);
		spinVisibility = findViewById(R.id.spinVisibility);
		imageview18 = findViewById(R.id.imageview18);
		linear47 = findViewById(R.id.linear47);
		switchApproval = findViewById(R.id.switchApproval);
		textview59 = findViewById(R.id.textview59);
		textview60 = findViewById(R.id.textview60);
		imageview19 = findViewById(R.id.imageview19);
		linear50 = findViewById(R.id.linear50);
		switchInvite = findViewById(R.id.switchInvite);
		textview61 = findViewById(R.id.textview61);
		textview62 = findViewById(R.id.textview62);
		linear51 = findViewById(R.id.linear51);
		linear52 = findViewById(R.id.linear52);
		cardview14 = findViewById(R.id.cardview14);
		tvDescriptionCount = findViewById(R.id.tvDescriptionCount);
		imageview20 = findViewById(R.id.imageview20);
		linear53 = findViewById(R.id.linear53);
		textview63 = findViewById(R.id.textview63);
		textview64 = findViewById(R.id.textview64);
		etDescription = findViewById(R.id.etDescription);
		linear54 = findViewById(R.id.linear54);
		linear55 = findViewById(R.id.linear55);
		view57 = findViewById(R.id.view57);
		panel_rotating = findViewById(R.id.panel_rotating);
		imageview21 = findViewById(R.id.imageview21);
		linear56 = findViewById(R.id.linear56);
		cardview16 = findViewById(R.id.cardview16);
		textview65 = findViewById(R.id.textview65);
		textview66 = findViewById(R.id.textview66);
		etContribution = findViewById(R.id.etContribution);
		rowManageRotating = findViewById(R.id.rowManageRotating);
		view59 = findViewById(R.id.view59);
		linear60 = findViewById(R.id.linear60);
		view62 = findViewById(R.id.view62);
		linear63 = findViewById(R.id.linear63);
		imageview22 = findViewById(R.id.imageview22);
		linear58 = findViewById(R.id.linear58);
		imageview23 = findViewById(R.id.imageview23);
		tvTypeSettingsTitle = findViewById(R.id.tvTypeSettingsTitle);
		tvTypeSettingsSubtitle = findViewById(R.id.tvTypeSettingsSubtitle);
		imageview24 = findViewById(R.id.imageview24);
		linear61 = findViewById(R.id.linear61);
		cardview17 = findViewById(R.id.cardview17);
		textview67 = findViewById(R.id.textview67);
		textview68 = findViewById(R.id.textview68);
		etGracePeriod = findViewById(R.id.etGracePeriod);
		imageview25 = findViewById(R.id.imageview25);
		linear64 = findViewById(R.id.linear64);
		cardview18 = findViewById(R.id.cardview18);
		textview69 = findViewById(R.id.textview69);
		textview70 = findViewById(R.id.textview70);
		etCommission = findViewById(R.id.etCommission);
		linear18 = findViewById(R.id.linear18);
		textview13 = findViewById(R.id.textview13);
		linear19 = findViewById(R.id.linear19);
		view21 = findViewById(R.id.view21);
		tv_fixed_progress = findViewById(R.id.tv_fixed_progress);
		linear22 = findViewById(R.id.linear22);
		linear20 = findViewById(R.id.linear20);
		cardview6 = findViewById(R.id.cardview6);
		textview14 = findViewById(R.id.textview14);
		textview15 = findViewById(R.id.textview15);
		etFixedGoal = findViewById(R.id.etFixedGoal);
		textview16 = findViewById(R.id.textview16);
		cardview7 = findViewById(R.id.cardview7);
		spinDisbursementRule = findViewById(R.id.spinDisbursementRule);
		linear23 = findViewById(R.id.linear23);
		textview17 = findViewById(R.id.textview17);
		linear24 = findViewById(R.id.linear24);
		view26 = findViewById(R.id.view26);
		linear27 = findViewById(R.id.linear27);
		view28 = findViewById(R.id.view28);
		tv_loan_active_count = findViewById(R.id.tv_loan_active_count);
		linear25 = findViewById(R.id.linear25);
		cardview8 = findViewById(R.id.cardview8);
		textview18 = findViewById(R.id.textview18);
		textview19 = findViewById(R.id.textview19);
		etLoanInterestRate = findViewById(R.id.etLoanInterestRate);
		textview20 = findViewById(R.id.textview20);
		cardview9 = findViewById(R.id.cardview9);
		spinWithdrawalRule = findViewById(R.id.spinWithdrawalRule);
		linear67 = findViewById(R.id.linear67);
		imageview26 = findViewById(R.id.imageview26);
		tvRuleInfoBanner = findViewById(R.id.tvRuleInfoBanner);
		linear68 = findViewById(R.id.linear68);
		linear69 = findViewById(R.id.linear69);
		cardWalletWarning = findViewById(R.id.cardWalletWarning);
		btnRestoreWallet = findViewById(R.id.btnRestoreWallet);
		imageview27 = findViewById(R.id.imageview27);
		linear70 = findViewById(R.id.linear70);
		btnCopyWallet = findViewById(R.id.btnCopyWallet);
		textview73 = findViewById(R.id.textview73);
		tvWalletAddress = findViewById(R.id.tvWalletAddress);
		linear71 = findViewById(R.id.linear71);
		imageview28 = findViewById(R.id.imageview28);
		textview74 = findViewById(R.id.textview74);
		linear29 = findViewById(R.id.linear29);
		linear30 = findViewById(R.id.linear30);
		view32 = findViewById(R.id.view32);
		linear33 = findViewById(R.id.linear33);
		imageview3 = findViewById(R.id.imageview3);
		linear31 = findViewById(R.id.linear31);
		switchMutePayments = findViewById(R.id.switchMutePayments);
		textview21 = findViewById(R.id.textview21);
		textview22 = findViewById(R.id.textview22);
		imageview4 = findViewById(R.id.imageview4);
		linear34 = findViewById(R.id.linear34);
		switchMuteNotices = findViewById(R.id.switchMuteNotices);
		textview23 = findViewById(R.id.textview23);
		textview24 = findViewById(R.id.textview24);
		imageview29 = findViewById(R.id.imageview29);
		textview75 = findViewById(R.id.textview75);
		tvBlockedCountBadge = findViewById(R.id.tvBlockedCountBadge);
		imageview30 = findViewById(R.id.imageview30);
		textview76 = findViewById(R.id.textview76);
		btnSaveSettings = findViewById(R.id.btnSaveSettings);
		
		etDescription.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				tvDescriptionCount.setText(String.valueOf((long)(_charSeq.length())).concat("/500"));
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
		
		btnRestoreWallet.setOnClickListener(_v -> com.harvana.app.wallet.PinPromptHelper.verifyPinThenProceed(GroupSettingsActivity.this, () -> _showRestoreSheet()));
		
		btnCopyWallet.setOnClickListener(_v -> {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Wallet Address", tvWalletAddress.getText().toString());
			if (clipboard != null) clipboard.setPrimaryClip(clip);
			Toast.makeText(GroupSettingsActivity.this,
			"Wallet address copied", 
			Toast.LENGTH_SHORT).show();
			
		});
		
		btnSaveSettings.setOnClickListener(_v -> _save());
	}
	
	private void initializeLogic() {
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		int backgroundColor = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackground);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			com.harvana.app.utils.StatusBarUtil.matchToolbar(this);
		}
		
		groupId = getIntent() != null ? getIntent().getStringExtra("groupId") : "";
		if (groupId == null) groupId = "";
		
		String[] visibilityOptions = new String[]{"Public", "Private", "Invite-only"};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, visibilityOptions);
		spinVisibility.setAdapter(adapter);
		
		// Same option sets as CreateNewGroupActivity's own disbursement/
		// withdrawal spinners, so a value saved here means the same thing
		// there and vice versa.
		String[] disbursementOptions = new String[]{"All at Once at Goal", "Admin-Triggered"};
		spinDisbursementRule.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, disbursementOptions));
		
		String[] withdrawalOptions = new String[]{"Anytime", "Cycle-End Only"};
		spinWithdrawalRule.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, withdrawalOptions));
		
		_setupGroupNotificationToggles();
		
		_loadGroupDoc();
		
		
	}
	
	public void _MORE() {
	}
	private String groupId = "";
	private String expectedWallet = "";
	private boolean isAdmin = false;
	private Map<String, Object> groupData = new HashMap<>();
	private final List<String> blockedUids = new ArrayList<>();
	private List<Map<String, Object>> memberRows = new ArrayList<>();
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.group_settings_menu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Transfer admin only makes sense for the current admin, and
		// only if there's someone else in the group to hand it to.
		MenuItem transferItem = menu.findItem(R.id.menu_transfer_admin);
		if (transferItem != null) transferItem.setVisible(isAdmin);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.menu_transfer_admin) {
			_showTransferAdminDialog();
			return true;
		} else if (id == R.id.menu_leave_group) {
			_confirmLeaveGroup();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void _loadGroupDoc() {
		Map<String, Object> cached = OfflineCacheManager.load(this, "group_" + groupId);
		if (cached != null) _applyGroup(cached);
		
		if (groupId.isEmpty()) return;
		
		GroupFirestoreManager.getInstance(this)
		.getGroup(groupId, new GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				runOnUiThread(() -> _applyGroup(data));
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, error, Toast.LENGTH_LONG).show());
			}
		});
	}
	
	private void _applyGroup(Map<String, Object> data) {
		if (data == null) return;
		groupData = new HashMap<>(data);
		
		Object w = data.get("groupWalletAddress");
		if (w == null) w = data.get("group_wallet");
		expectedWallet = w != null ? w.toString().trim() : "";
		
		// THE FIX: was read from a global SharedPreferences flag shared
		// across every group — an admin of Group A who was last viewing
		// Group A would incorrectly see admin controls on Group B too.
		// Derived fresh here instead, per group, matching the exact
		// pattern GroupDetailsActivity already uses (compare the current
		// Firebase UID against this group's own creatorId).
		String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
		? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		String creatorId = str(groupData, "creatorId", "");
		isAdmin = !uid.isEmpty() && uid.equals(creatorId);
		invalidateOptionsMenu();
		com.harvana.app.utils.AppBar.setSubtitle(this, str(groupData, "groupName", ""));
		
		_rebuild();
		_loadMembers();
	}
	
	private String typeOf() {
		Object t = groupData.get("groupType");
		return t != null ? t.toString() : "";
	}
	
	private boolean isRotating() { return typeOf().toLowerCase(Locale.US).contains("rotating"); }
	private boolean isFixed() { return typeOf().toLowerCase(Locale.US).contains("fixed"); }
	private boolean isAccum() { return typeOf().toLowerCase(Locale.US).contains("accumul"); }
	
	private void _rebuild() {
		String type = typeOf();
		tvHeroTypeName.setText(type.isEmpty() ? "Unknown Type" : type);
		tvHeroFrequency.setText("📅 " + str(groupData, "frequency", "Daily"));
		
		_select(spinVisibility, str(groupData, "visibility", "Public"));
		switchApproval.setChecked(bool(groupData, "requireApprovalToJoin", true));
		switchInvite.setChecked(bool(groupData, "membersCanInvite", false));
		
		String desc = str(groupData, "groupDescription", str(groupData, "description", ""));
		etDescription.setText(desc);
		etContribution.setText(numStr(groupData, "contributionAmount"));
		
		if (panel_rotating != null) panel_rotating.setVisibility(isRotating() ? View.VISIBLE : View.GONE);
		if (panel_fixed != null) panel_fixed.setVisibility(isFixed() ? View.VISIBLE : View.GONE);
		if (panel_accumulative != null) panel_accumulative.setVisibility(isAccum() ? View.VISIBLE : View.GONE);
		
		if (isRotating()) {
			ivHeroTypeIcon.setImageResource(R.drawable.ic_refresh);
			tvTypeSettingsTitle.setText("Rotating Pool");
			etGracePeriod.setText(str(groupData, "gracePeriodDays", "0"));
			etCommission.setText(str(groupData, "commissionPercent", "0"));
			tvTypeSettingsSubtitle.setText("Slots " + numStr(groupData, "totalSlots") + " · Round " + numStr(groupData, "currentRound"));
			tvRuleInfoBanner.setText("Only the member whose slot is due can pay this round.");
		} 
		else if (isFixed()) {
			ivHeroTypeIcon.setImageResource(R.drawable.ic_pie_chart);
			etFixedGoal.setText(numStr(groupData, "targetGoalAmount"));
			_select(spinDisbursementRule, str(groupData, "disbursementRule", "All at Once at Goal"));
			// THE FIX: this TextView was bound but never populated at
			// all — live saved-vs-goal progress needs the group wallet's
			// actual balance, which this settings screen doesn't fetch
			// (only Group Overview does). Rather than show a fabricated
			// or stale number here, this points to where the real one
			// lives instead.
			tv_fixed_progress.setText("Live savings progress is shown on the group's Overview tab.");
			tvRuleInfoBanner.setText("The goal amount and disbursement rule apply to future contributions once saved.");
		} 
		else if (isAccum()) {
			ivHeroTypeIcon.setImageResource(R.drawable.ic_attach_money);
			etLoanInterestRate.setText(numStr(groupData, "loanInterestRate"));
			_select(spinWithdrawalRule, str(groupData, "withdrawalRule", "Anytime"));
			tv_loan_active_count.setText("Active loan count is shown on the group's Loans tab.");
			tvRuleInfoBanner.setText("Interest rate changes only apply to new loans, not ones already disbursed.");
		}
		
		tvWalletAddress.setText(expectedWallet.isEmpty() ? "No group wallet" : expectedWallet);
		_refreshKeyStatus();
		_buildBackupSection();
		_buildCloseSection();
		
		// Apply Admin States
		btnSaveSettings.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
		
		boolean edit = isAdmin;
		etDescription.setEnabled(edit);
		etContribution.setEnabled(edit);
		etGracePeriod.setEnabled(edit);
		etCommission.setEnabled(edit);
		etFixedGoal.setEnabled(edit);
		spinDisbursementRule.setEnabled(edit);
		etLoanInterestRate.setEnabled(edit);
		spinWithdrawalRule.setEnabled(edit);
		spinVisibility.setEnabled(edit);
		switchApproval.setEnabled(edit);
		switchInvite.setEnabled(edit);
		
		// Dim fields if disabled
		etContribution.setAlpha(edit ? 1.0f : 0.6f);
		etGracePeriod.setAlpha(edit ? 1.0f : 0.6f);
		etCommission.setAlpha(edit ? 1.0f : 0.6f);
		etFixedGoal.setAlpha(edit ? 1.0f : 0.6f);
		spinDisbursementRule.setAlpha(edit ? 1.0f : 0.6f);
		etLoanInterestRate.setAlpha(edit ? 1.0f : 0.6f);
		spinWithdrawalRule.setAlpha(edit ? 1.0f : 0.6f);
	}
	
	private void _select(Spinner sp, String value) {
		if (sp == null || sp.getAdapter() == null || value == null) return;
		for (int i = 0; i < sp.getAdapter().getCount(); i++) {
			if (value.equalsIgnoreCase(String.valueOf(sp.getAdapter().getItem(i)))) {
				sp.setSelection(i);
				return;
			}
		}
	}
	
	private void _loadMembers() {
		List<HashMap<String, Object>> cached = OfflineCacheManager.loadList(this, "members_" + groupId);
		if (cached != null) {
			memberRows = new ArrayList<>(cached);
			_renderBlocked();
		}
		
		GroupFirestoreManager.getInstance(this).getMembers(groupId, new GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> members) {
				memberRows = members != null ? members : new ArrayList<>();
				runOnUiThread(() -> _renderBlocked());
			}
			@Override public void onError(String error) {}
		});
	}
	
	private void _renderBlocked() {
		if (layoutBlockedMembers == null) return;
		layoutBlockedMembers.removeAllViews();
		blockedUids.clear();
		
		Object raw = groupData.get("blockedMemberUids");
		if (raw instanceof List) {
			for (Object o : (List<?>) raw) {
				if (o != null) blockedUids.add(o.toString());
			}
		} else if (raw instanceof String && !((String) raw).isEmpty()) {
			for (String p : ((String) raw).split(",")) {
				if (!p.trim().isEmpty()) blockedUids.add(p.trim());
			}
		}
		
		if (blockedUids.isEmpty()) {
			tvBlockedCountBadge.setText("0");
			tvBlockedEmpty.setVisibility(View.VISIBLE);
			return;
		}
		
		tvBlockedCountBadge.setText(String.valueOf(blockedUids.size()));
		tvBlockedEmpty.setVisibility(View.GONE);
		
		for (Map<String, Object> m : memberRows) {
			final String uid = str(m, "memberUid", "");
			if (uid.isEmpty() || !blockedUids.contains(uid)) continue;
			
			String name = str(m, "displayName", str(m, "name", "User"));
			int slot = (int) num(m, "slot");
			String details = (slot > 0 ? "Slot " + slot + " · " : "") + uid;
			String initials = name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.US) : "?";
			
			_addBlockedMemberRow(uid, initials, "#448AFF", details);
		}
	}
	
	private void _addBlockedMemberRow(String uid, String initials, String colorHex, String details) {
		View row;
		int rowId = getResources().getIdentifier("item_blocked_member", "layout", getPackageName());
		if (rowId != 0) {
			row = LayoutInflater.from(this).inflate(rowId, layoutBlockedMembers, false);
		} else {
			row = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, layoutBlockedMembers, false);
		}
		
		TextView tvInitials = row.findViewById(R.id.tvAvatarInitials);
		TextView tvDetails = row.findViewById(R.id.tvMemberDetails);
		MaterialButton btnUnblock = row.findViewById(R.id.btnUnblock);
		
		tvInitials.setText(initials);
		tvInitials.setTextColor(Color.parseColor(colorHex));
		tvInitials.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorHex.replace("#", "#20"))));
		tvDetails.setText(details);
		
		if (!isAdmin) {
			btnUnblock.setVisibility(View.GONE);
		}
		
		btnUnblock.setOnClickListener(v -> {
			new AlertDialog.Builder(this)
			.setTitle("Unblock Member")
			.setMessage("Are you sure you want to unblock this user? They will be able to send join requests again.")
			.setPositiveButton("Unblock", (dialog, which) -> {
				blockedUids.remove(uid);
				_saveBlockedOnly();
				layoutBlockedMembers.removeView(row);
				tvBlockedCountBadge.setText(String.valueOf(blockedUids.size()));
				if (blockedUids.isEmpty()) tvBlockedEmpty.setVisibility(View.VISIBLE);
			})
			.setNegativeButton("Cancel", null)
			.show();
		});
		
		layoutBlockedMembers.addView(row);
	}
	
	private void _saveBlockedOnly() {
		Map<String, Object> patch = new HashMap<>();
		patch.put("blockedMemberUids", new ArrayList<>(blockedUids));
		_patch(patch, "Block list updated");
	}
	
	private void _save() {
		btnSaveSettings.setEnabled(false);
		btnSaveSettings.setText("Saving...");
		
		String desc = etDescription.getText().toString().trim();
		if (desc.length() > 500) {
			btnSaveSettings.setEnabled(true);
			btnSaveSettings.setText("Save Settings");
			Toast.makeText(this, "Description must be 500 characters or less", Toast.LENGTH_SHORT).show();
			return;
		}
		double contribution;
		try {
			contribution = Double.parseDouble(etContribution.getText().toString().trim());
		} catch (Exception e) {
			btnSaveSettings.setEnabled(true);
			btnSaveSettings.setText("Save Settings");
			Toast.makeText(this, "Enter a valid contribution amount", Toast.LENGTH_SHORT).show();
			return;
		}
		if (contribution < 0) {
			btnSaveSettings.setEnabled(true);
			btnSaveSettings.setText("Save Settings");
			Toast.makeText(this, "Contribution cannot be negative", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Map<String, Object> patch = new HashMap<>();
		patch.put("groupDescription", desc);
		GroupIdentity identity = GroupIdentityResolver.resolve(groupId, groupData);
		if (identity.onChain()) {
			Toast.makeText(this, "Contribution and cycle rules stay as the network set them. This save only updates the profile.", Toast.LENGTH_LONG).show();
		} else {
			patch.put("contributionAmount", contribution);
		}
		
		if (spinVisibility.getSelectedItem() != null) {
			patch.put("visibility", spinVisibility.getSelectedItem().toString());
		}
		
		patch.put("requireApprovalToJoin", switchApproval.isChecked());
		String existingMethod = str(groupData, "joinApprovalMethod", "ADMIN");
		if (switchApproval.isChecked()) {
			// Keep MEMBER_VOTE if that's how the group was created. A
			// save here used to force ADMIN and silently kill voting.
			if (JoinRequestManager.isOpenJoin(existingMethod) || existingMethod.trim().isEmpty()) {
				patch.put("joinApprovalMethod", "ADMIN");
			}
		} else {
			patch.put("joinApprovalMethod", "OPEN");
		}
		patch.put("membersCanInvite", switchInvite.isChecked());
		
		if (isRotating() && !identity.onChain()) {
			String grace = etGracePeriod.getText().toString().trim();
			String comm = etCommission.getText().toString().trim();
			try {
				int g = Integer.parseInt(grace.isEmpty() ? "0" : grace);
				if (g < 0) throw new IllegalArgumentException();
				patch.put("gracePeriodDays", String.valueOf(g));
			} catch (Exception e) {
				btnSaveSettings.setEnabled(true);
				btnSaveSettings.setText("Save Settings");
				Toast.makeText(this, "Grace period must be 0 or more days", Toast.LENGTH_SHORT).show();
				return;
			}
			try {
				double c = Double.parseDouble(comm.isEmpty() ? "0" : comm);
				if (c < 0 || c > 100) throw new IllegalArgumentException();
				patch.put("commissionPercent", String.valueOf(c));
			} catch (Exception e) {
				btnSaveSettings.setEnabled(true);
				btnSaveSettings.setText("Save Settings");
				Toast.makeText(this, "Commission must be between 0 and 100", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		else if (isFixed()) {
			String goal = etFixedGoal.getText().toString().trim();
			try {
				double g = Double.parseDouble(goal.isEmpty() ? "0" : goal);
				if (g <= 0) throw new IllegalArgumentException();
				patch.put("targetGoalAmount", g);
			} catch (Exception e) {
				btnSaveSettings.setEnabled(true);
				btnSaveSettings.setText("Save Settings");
				Toast.makeText(this, "Target goal must be a positive amount", Toast.LENGTH_SHORT).show();
				return;
			}
			if (spinDisbursementRule.getSelectedItem() != null) {
				patch.put("disbursementRule", spinDisbursementRule.getSelectedItem().toString());
			}
		}
		else if (isAccum()) {
			String rate = etLoanInterestRate.getText().toString().trim();
			try {
				double r = Double.parseDouble(rate.isEmpty() ? "0" : rate);
				if (r < 0 || r > 100) throw new IllegalArgumentException();
				patch.put("loanInterestRate", r);
			} catch (Exception e) {
				btnSaveSettings.setEnabled(true);
				btnSaveSettings.setText("Save Settings");
				Toast.makeText(this, "Interest rate must be between 0 and 100", Toast.LENGTH_SHORT).show();
				return;
			}
			if (spinWithdrawalRule.getSelectedItem() != null) {
				patch.put("withdrawalRule", spinWithdrawalRule.getSelectedItem().toString());
			}
		}
		
		patch.put("blockedMemberUids", new ArrayList<>(blockedUids));
		
		_patch(patch, "Settings saved successfully");
	}
	
	private void _patch(Map<String, Object> patch, String okMsg) {
		GroupFirestoreManager.getInstance(this).updateGroupFields(groupId, patch, new GroupFirestoreManager.SimpleCallback() {
			@Override
			public void onSuccess() {
				groupData.putAll(patch);
				OfflineCacheManager.save(GroupSettingsActivity.this, "group_" + groupId, groupData);
				runOnUiThread(() -> {
					btnSaveSettings.setEnabled(true);
					btnSaveSettings.setText("Save Settings");
					Toast.makeText(GroupSettingsActivity.this, okMsg, Toast.LENGTH_SHORT).show();
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					btnSaveSettings.setEnabled(true);
					btnSaveSettings.setText("Save Settings");
					Toast.makeText(GroupSettingsActivity.this, error, Toast.LENGTH_LONG).show();
				});
			}
		});
	}
	
	/** Picks a target member (excluding self) from the group's active
*  roster, confirms explicitly, then requires PIN verification before
*  handing off control — this changes who can move the group's
*  shared funds, at least as sensitive as any other wallet action in
*  this app. */
	private void _showTransferAdminDialog() {
		String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
		? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		
		List<Map<String, Object>> candidates = new ArrayList<>();
		for (Map<String, Object> m : memberRows) {
			String uid = str(m, "memberUid", "");
			String status = str(m, "status", "ACTIVE");
			if (uid.isEmpty() || uid.equals(myUid)) continue;
			if (!"ACTIVE".equalsIgnoreCase(status)) continue;
			candidates.add(m);
		}
		
		if (candidates.isEmpty()) {
			Toast.makeText(this, "No other active members to transfer admin to.", Toast.LENGTH_LONG).show();
			return;
		}
		
		String[] labels = new String[candidates.size()];
		for (int i = 0; i < candidates.size(); i++) {
			Map<String, Object> m = candidates.get(i);
			String name = str(m, "displayName", str(m, "name", "Member"));
			int slot = (int) num(m, "slot");
			labels[i] = name + (slot > 0 ? " · Slot " + slot : "");
		}
		
		new AlertDialog.Builder(this)
		.setTitle("Transfer admin role")
		.setItems(labels, (dialog, which) -> {
			Map<String, Object> target = candidates.get(which);
			String targetUid = str(target, "memberUid", "");
			String targetName = str(target, "displayName", str(target, "name", "this member"));
			_confirmTransferAdmin(myUid, targetUid, targetName);
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _confirmTransferAdmin(String myUid, String targetUid, String targetName) {
		new AlertDialog.Builder(this)
		.setTitle("Transfer admin to " + targetName + "?")
		.setMessage("You will become a regular member and lose admin control over this group, including its settings and wallet. This can only be undone by " + targetName + " transferring it back to you.")
		.setPositiveButton("Transfer", (dialog, which) ->
		com.harvana.app.wallet.PinPromptHelper.verifyPinThenProceed(this, () -> _performTransferAdmin(myUid, targetUid))
		)
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void _performTransferAdmin(String myUid, String targetUid) {
		// THE FIX: order matters here. The member-role patches below are
		// authorized by isGroupCreator(groupId) in the Firestore rules,
		// which re-checks creatorId at request time — not a snapshot from
		// when this method started. Patching creatorId first would mean
		// by the time these two calls fire, the caller no longer IS the
		// creator, and both would be silently rejected, leaving the role
		// labels stale even though real control had already transferred.
		// Doing the role patches first, while still creator, then
		// creatorId last, keeps every write correctly authorized.
		Map<String, Object> promote = new HashMap<>();
		promote.put("role", "ADMIN");
		GroupFirestoreManager.getInstance(this).updateMemberFields(groupId, targetUid, promote, new GroupFirestoreManager.SimpleCallback() {
			@Override
			public void onSuccess() {
				Map<String, Object> demote = new HashMap<>();
				demote.put("role", "MEMBER");
				GroupFirestoreManager.getInstance(GroupSettingsActivity.this)
				.updateMemberFields(groupId, myUid, demote, new GroupFirestoreManager.SimpleCallback() {
					@Override
					public void onSuccess() {
						_finishAdminTransfer(targetUid);
					}
					@Override
					public void onError(String error) {
						// Target is already promoted at this point —
						// proceed with the actual handoff regardless,
						// since creatorId is what really governs
						// authorization; the demote label is display
						// polish and can be retried/reconciled later.
						_finishAdminTransfer(targetUid);
					}
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "Transfer failed: " + error, Toast.LENGTH_LONG).show());
			}
		});
	}
	
	private void _finishAdminTransfer(String targetUid) {
		Map<String, Object> groupPatch = new HashMap<>();
		groupPatch.put("creatorId", targetUid);
		GroupFirestoreManager.getInstance(this).updateGroupFields(groupId, groupPatch, new GroupFirestoreManager.SimpleCallback() {
			@Override
			public void onSuccess() {
				runOnUiThread(() -> {
					Toast.makeText(GroupSettingsActivity.this, "Admin role transferred.", Toast.LENGTH_SHORT).show();
					_loadGroupDoc();
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "Transfer failed: " + error, Toast.LENGTH_LONG).show());
			}
		});
	}
	
	/** An admin leaving would orphan the group's wallet — nobody left
*  with signing authority over shared funds — so this is blocked
*  until they transfer admin to someone else first. A regular
*  member leaving is a plain, reversible-by-rejoining action. */
	private void _confirmLeaveGroup() {
		if (isAdmin) {
			ThemedDialogs.show(ThemedDialogs.alert(this)
			.setTitle("Transfer admin first")
			.setMessage("You're this group's admin. Transfer admin role to another member before you can leave — otherwise nobody would be left with control of the group wallet.")
			.setPositiveButton("Transfer Admin", (dialog, which) -> _showTransferAdminDialog())
			.setNegativeButton("Cancel", null));
			return;
		}
		
		String leaveMsg = "You'll lose access to this group's payments, loans, and notices. You can rejoin later if the group is public or you're invited again.";
		if (isRotating()) {
			leaveMsg += "\n\nIf a cycle is in progress, leaving now may forfeit this round's payout.";
		}
		ThemedDialogs.show(ThemedDialogs.alert(this)
		.setTitle("Leave this group?")
		.setMessage(leaveMsg)
		.setPositiveButton("Leave", (dialog, which) -> _performLeaveGroup())
		.setNegativeButton("Cancel", null));
	}
	
	private void _performLeaveGroup() {
		String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
		? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		if (myUid.isEmpty()) return;
		
		JoinRequestManager.leaveGroup(this, groupId, myUid, new JoinRequestManager.Callback() {
			@Override
			public void onSuccess(String message) {
				runOnUiThread(() -> {
					Toast.makeText(GroupSettingsActivity.this,
					message != null ? message : "You have left the group.", Toast.LENGTH_SHORT).show();
					Intent home = new Intent(GroupSettingsActivity.this, MainActivity.class);
					home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startActivity(home);
					finish();
				});
			}
			@Override
			public void onError(String error) {
				runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this,
				"Couldn't leave group: " + error, Toast.LENGTH_LONG).show());
			}
		});
	}
	
	/** Same "notification_prefs" file the app-wide notification toggles
*  already use, just with group-scoped keys — so whenever the
*  enforcement logic for these preferences gets built, it only ever
*  needs to check one place, global or per-group. */
	private void _setupGroupNotificationToggles() {
		android.content.SharedPreferences prefs = getSharedPreferences(
		com.harvana.app.NotificationSettingsActivity.PREFS_NAME, MODE_PRIVATE);
		String paymentsKey = "group_" + groupId + "_mute_payments";
		String noticesKey = "group_" + groupId + "_mute_notices";
		
		switchMutePayments.setChecked(prefs.getBoolean(paymentsKey, false));
		switchMuteNotices.setChecked(prefs.getBoolean(noticesKey, false));
		
		switchMutePayments.setOnCheckedChangeListener((buttonView, isChecked) ->
		prefs.edit().putBoolean(paymentsKey, isChecked).apply());
		switchMuteNotices.setOnCheckedChangeListener((buttonView, isChecked) ->
		prefs.edit().putBoolean(noticesKey, isChecked).apply());
	}
	
	private void _showRestoreSheet() {
		final BottomSheetDialog dialog = new BottomSheetDialog(this);
		View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_recovery_phrase_old, null);
		dialog.setContentView(sheetView);
		
		TextView title = sheetView.findViewById(R.id.tv_title);
		if (title != null) title.setText("Restore group wallet");
		
		final int[] etWordIds = {
			R.id.et_word_1, R.id.et_word_2, R.id.et_word_3, R.id.et_word_4,
			R.id.et_word_5, R.id.et_word_6, R.id.et_word_7, R.id.et_word_8,
			R.id.et_word_9, R.id.et_word_10, R.id.et_word_11, R.id.et_word_12
		};
		final EditText[] wordFields = new EditText[12];
		for (int i = 0; i < 12; i++) {
			wordFields[i] = sheetView.findViewById(etWordIds[i]);
		}
		
		MaterialButton btn = sheetView.findViewById(R.id.btn_restore);
		if (btn != null) {
			btn.setText("Restore group wallet");
			btn.setOnClickListener(v -> {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < 12; i++) {
					String w = wordFields[i] != null ? wordFields[i].getText().toString().trim().toLowerCase() : "";
					if (w.isEmpty()) {
						Toast.makeText(this, "Enter all 12 words", Toast.LENGTH_SHORT).show();
						return;
					}
					if (i > 0) sb.append(' ');
					sb.append(w);
				}
				_restoreGroupWallet(sb.toString(), dialog, btn);
			});
		}
		dialog.show();
	}
	
	private void _restoreGroupWallet(String mnemonic, final BottomSheetDialog dialog, final MaterialButton btn) {
		btn.setEnabled(false);
		btn.setText("Restoring…");
		new Thread(() -> {
			try {
				String derived = WalletGenerator.deriveAddressFromMnemonic(mnemonic);
				if (derived == null || !derived.equalsIgnoreCase(expectedWallet)) {
					runOnUiThread(() -> {
						btn.setEnabled(true);
						btn.setText("Restore group wallet");
						Toast.makeText(this, "Phrase does not match this group wallet.", Toast.LENGTH_LONG).show();
					});
					return;
				}
				String priv = WalletGenerator.derivePrivateKey(mnemonic);
				byte[] seed = Base64.decode(priv, Base64.NO_WRAP);
				byte[] pub = Ed25519.publicKeyFromSeed(seed);
				String pubB64 = Base64.encodeToString(pub, Base64.NO_WRAP);
				
				SecureWalletStorage.saveMnemonic(this, expectedWallet, mnemonic);
				WalletManager.saveKeysLocally(this, expectedWallet, priv, pubB64);
				
				runOnUiThread(() -> {
					dialog.dismiss();
					_refreshKeyStatus();
					Toast.makeText(this, "Group wallet restored", Toast.LENGTH_SHORT).show();
				});
			} catch (Exception e) {
				runOnUiThread(() -> {
					btn.setEnabled(true);
					btn.setText("Restore group wallet");
					Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
				});
			}
		}).start();
	}
	
	private LinearLayout backupSection;
	
	/** New-phone recovery: download the group's published encrypted card, decrypt with the admin passphrase. */
	private void _restoreFromCard() {
		final ProgressDialog pd = ProgressDialog.show(this, null, "Fetching backup card…", true, false);
		com.harvana.app.group.GroupCardStore.fetch(this, groupId, new com.harvana.app.group.GroupCardStore.Callback() {
			@Override public void onSuccess(Map<String, Object> card) {
				runOnUiThread(() -> {
					pd.dismiss();
					_askCardPassphrase(String.valueOf(card.get("qrPayload")));
				});
			}
			@Override public void onError(String error) {
				runOnUiThread(() -> {
					pd.dismiss();
					Toast.makeText(GroupSettingsActivity.this,
						"No backup card found online. Use \"Restore group wallet\" with your 12 words instead.",
						Toast.LENGTH_LONG).show();
				});
			}
		});
	}
	
	private void _askCardPassphrase(final String qrPayload) {
		final EditText pass = new EditText(this);
		pass.setHint("Access Card passphrase");
		pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
		new androidx.appcompat.app.AlertDialog.Builder(this)
			.setTitle("Restore from Access Card")
			.setMessage("Enter the passphrase you chose when the card was created.")
			.setView(pass)
			.setNegativeButton("Cancel", null)
			.setPositiveButton("Restore", (dlg, w) -> {
				final String pw = pass.getText().toString();
				final ProgressDialog pd = ProgressDialog.show(this, null, "Decrypting…", true, false);
				new Thread(() -> {
					try {
						com.harvana.app.wallet.HarvanaCardCrypto.DecryptResult r =
							com.harvana.app.wallet.HarvanaCardCrypto.decryptCardPayload(this, qrPayload, pw);
						if (!r.success) throw new Exception(r.errorMessage);
						String mnemonic = r.mnemonic;
						String derived = WalletGenerator.deriveAddressFromMnemonic(mnemonic);
						if (derived == null || !derived.equalsIgnoreCase(expectedWallet))
							throw new Exception("This card doesn't match this group wallet.");
						String priv = WalletGenerator.derivePrivateKey(mnemonic);
						byte[] pub = Ed25519.publicKeyFromSeed(Base64.decode(priv, Base64.NO_WRAP));
						SecureWalletStorage.saveMnemonic(this, expectedWallet, mnemonic);
						WalletManager.saveKeysLocally(this, expectedWallet, priv, Base64.encodeToString(pub, Base64.NO_WRAP));
						runOnUiThread(() -> {
							pd.dismiss();
							_refreshKeyStatus();
							Toast.makeText(this, "Group wallet restored on this phone", Toast.LENGTH_LONG).show();
						});
					} catch (Exception e) {
						runOnUiThread(() -> {
							pd.dismiss();
							Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
						});
					}
				}).start();
			}).show();
	}
	
	/**
	 * Wallet Access Card backup. The admin (who holds the passphrase) can
	 * create/replace the card, which is published encrypted to every member;
	 * members can view, print and save it as a social backup.
	 */
	private void _buildBackupSection() {
		if (expectedWallet.isEmpty() || !expectedWallet.startsWith("HV0x") || _isOnChain()) return;
		ViewGroup host = (ViewGroup) btnRestoreWallet.getParent();
		if (backupSection != null && backupSection.getParent() != null) {
			((ViewGroup) backupSection.getParent()).removeView(backupSection);
		}
		backupSection = new LinearLayout(this);
		backupSection.setOrientation(LinearLayout.VERTICAL);
		float d = getResources().getDisplayMetrics().density;
		backupSection.setPadding(0, (int) (12 * d), 0, 0);
		
		TextView title = new TextView(this);
		title.setText("Wallet backup card");
		title.setTextSize(14);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		title.setTextColor(UiColors.textPrimary());
		backupSection.addView(title);
		
		TextView info = new TextView(this);
		info.setText(isAdmin
			? "Create an encrypted Access Card for this wallet. Members receive it as a backup but can't open it: only you know the passphrase."
			: "The admin's encrypted backup card for this wallet. Print or save a copy; it can't move funds without the admin's passphrase.");
		info.setTextSize(12);
		info.setTextColor(UiColors.textSecondary());
		info.setPadding(0, (int) (4 * d), 0, (int) (8 * d));
		backupSection.addView(info);
		
		if (isAdmin) {
			MaterialButton create = new MaterialButton(this);
			create.setText("Create / replace Access Card");
			create.setAllCaps(false);
			create.setOnClickListener(v -> {
				String mn = SecureWalletStorage.loadMnemonic(this, expectedWallet);
				if (mn == null || mn.isEmpty()) {
					Toast.makeText(this, "The group phrase isn't on this phone. Restore the group wallet first.", Toast.LENGTH_LONG).show();
					return;
				}
				Intent i = new Intent(this, CreateHarvanaCardActivity.class);
				i.putExtra(CreateHarvanaCardActivity.EXTRA_WALLET_ADDRESS, expectedWallet);
				i.putExtra(CreateHarvanaCardActivity.EXTRA_LABEL, str(groupData, "groupName", ""));
				i.putExtra(CreateHarvanaCardActivity.EXTRA_GROUP_ID, groupId);
				startActivity(i);
			});
			backupSection.addView(create);
		}
		
		if (isAdmin) {
			MaterialButton chat = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
			chat.setText("Group chat link (WhatsApp, Telegram...)");
			chat.setAllCaps(false);
			chat.setOnClickListener(v -> com.harvana.app.group.GroupChatConnector.edit(this, groupId));
			backupSection.addView(chat);
		}
		
		if (isAdmin) {
			MaterialButton fromCard = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
			fromCard.setText("Restore wallet from Access Card (new phone)");
			fromCard.setAllCaps(false);
			fromCard.setOnClickListener(v -> com.harvana.app.wallet.PinPromptHelper.verifyPinThenProceed(
				GroupSettingsActivity.this, () -> _restoreFromCard()));
			backupSection.addView(fromCard);
		}
		
		MaterialButton view = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
		view.setText(isAdmin ? "Preview member copy" : "View, print or save backup card");
		view.setAllCaps(false);
		view.setOnClickListener(v -> {
			Intent i = new Intent(this, com.harvana.app.group.GroupWalletBackupActivity.class);
			i.putExtra(com.harvana.app.group.GroupWalletBackupActivity.EXTRA_GROUP_ID, groupId);
			startActivity(i);
		});
		backupSection.addView(view);
		
		int idx = host.indexOfChild(btnRestoreWallet);
		host.addView(backupSection, idx + 1);
		
		// Loans on/off: turns the group's Loans tab on or off for everyone.
		Object al = groupData.get("allowLoans");
		boolean loansOn = !(al instanceof Boolean) || (Boolean) al;
		androidx.appcompat.widget.SwitchCompat loanSw = new androidx.appcompat.widget.SwitchCompat(this);
		loanSw.setText("Allow member loans");
		loanSw.setTextColor(UiColors.textPrimary());
		loanSw.setChecked(loansOn);
		loanSw.setEnabled(isAdmin);
		loanSw.setPadding(0, (int) (12 * d), 0, 0);
		loanSw.setOnCheckedChangeListener((b, on) -> {
			if (!b.isPressed()) return;
			Map<String, Object> patch = new HashMap<>();
			patch.put("allowLoans", on);
			_patch(patch, on ? "Loans enabled" : "Loans disabled");
		});
		if (!isRotating()) backupSection.addView(loanSw, 0);
	}
	/** On-chain rotating groups hold money in a network escrow that has no private key: nothing to restore or back up. */
	// ── Close / delete group ─────────────────────────────────────────────

	private LinearLayout closeSection;

	/** Admin-only "danger zone": checks what the network and the group's records allow, then closes or deletes. */
	private void _buildCloseSection() {
		ViewGroup host = findViewById(R.id.linear39);
		if (host == null) return;
		if (closeSection != null && closeSection.getParent() != null) ((ViewGroup) closeSection.getParent()).removeView(closeSection);
		if (!isAdmin) return;
		float d = getResources().getDisplayMetrics().density;
		boolean closed = GroupCloser.CLOSED.equalsIgnoreCase(str(groupData, "status", ""));

		closeSection = new LinearLayout(this);
		closeSection.setOrientation(LinearLayout.VERTICAL);
		closeSection.setPadding(0, (int) (24 * d), 0, 0);

		TextView title = new TextView(this);
		title.setText(closed ? "This group is closed" : "Close or delete this group");
		title.setTextSize(14);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		title.setTextColor(closed ? UiColors.textPrimary() : UiColors.error());
		closeSection.addView(title);

		TextView info = new TextView(this);
		info.setText(closed
			? "Closed groups are hidden from Discover and nobody can join. Members keep their history. You can reopen the group."
			: "We check the network first. A group that holds money can't be erased: it is closed once everyone has been paid out. A group nobody joined can be deleted.");
		info.setTextSize(12);
		info.setTextColor(UiColors.textSecondary());
		info.setPadding(0, (int) (4 * d), 0, (int) (8 * d));
		closeSection.addView(info);

		MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
		btn.setAllCaps(false);
		btn.setText(closed ? "Reopen group" : "Check if I can close this group");
		btn.setOnClickListener(v -> { if (closed) _confirmReopen(); else _checkClose(btn); });
		closeSection.addView(btn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * d)));
		host.addView(closeSection);
	}

	private void _checkClose(MaterialButton btn) {
		btn.setEnabled(false);
		btn.setText("Checking the network\u2026");
		GroupCloser.evaluate(this, groupId, new GroupCloser.EvalCb() {
			@Override public void onDecision(GroupClosePolicy.Decision dec, GroupClosePolicy.Facts f) {
				runOnUiThread(() -> { btn.setEnabled(true); btn.setText("Check if I can close this group"); _showCloseDecision(dec); });
			}
			@Override public void onError(String e) {
				runOnUiThread(() -> { btn.setEnabled(true); btn.setText("Check if I can close this group"); Toast.makeText(GroupSettingsActivity.this, e, Toast.LENGTH_LONG).show(); });
			}
		});
	}

	private void _showCloseDecision(GroupClosePolicy.Decision dec) {
		StringBuilder sb = new StringBuilder();
		for (String r : dec.reasons) sb.append("\u2022 ").append(r).append("\n");
		if (!dec.nextSteps.isEmpty()) {
			sb.append("\nWhat to do:\n");
			for (String n : dec.nextSteps) sb.append("\u2022 ").append(n).append("\n");
		}
		if (dec.action == GroupClosePolicy.Action.NONE) {
			ThemedDialogs.show(ThemedDialogs.alert(this).setTitle(dec.headline).setMessage(sb.toString().trim()).setPositiveButton("OK", null));
			return;
		}
		final boolean delete = dec.action == GroupClosePolicy.Action.DELETE;
		final String groupName = str(groupData, "groupName", "");
		final String phrase = delete ? groupName : "CLOSE";

		LinearLayout box = new LinearLayout(this);
		box.setOrientation(LinearLayout.VERTICAL);
		int pad = (int) (20 * getResources().getDisplayMetrics().density);
		box.setPadding(pad, pad / 2, pad, 0);
		TextView msg = new TextView(this);
		msg.setTextColor(UiColors.textPrimary());
		msg.setTextSize(13);
		msg.setText(sb.toString().trim() + "\n\n" + (delete
			? "This permanently removes the group and its records from the app. It cannot be undone."
			: "Members are notified, joining stops, and the group leaves Discover. Everyone keeps their history and anything they are owed on the network. You can reopen it later."));
		box.addView(msg);
		final EditText reason = new EditText(this);
		if (!delete) { reason.setHint("Reason for members (optional)"); reason.setTextColor(UiColors.textPrimary()); box.addView(reason); }
		final EditText confirm = new EditText(this);
		confirm.setHint("Type " + phrase + " to confirm");
		confirm.setTextColor(UiColors.textPrimary());
		box.addView(confirm);

		androidx.appcompat.app.AlertDialog dialog = ThemedDialogs.alert(this)
			.setTitle(dec.headline).setView(box)
			.setPositiveButton(delete ? "Delete" : "Close group", null)
			.setNegativeButton("Cancel", null).create();
		dialog.setOnShowListener(x -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			if (!confirm.getText().toString().trim().equalsIgnoreCase(phrase)) {
				confirm.setError("Type " + phrase + " exactly");
				return;
			}
			dialog.dismiss();
			com.harvana.app.wallet.PinPromptHelper.verifyPinThenProceed(GroupSettingsActivity.this, () ->
				_runClose(delete, reason.getText().toString().trim()));
		}));
		dialog.show();
	}

	private void _runClose(boolean delete, String reason) {
		final android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
		pd.setMessage(delete ? "Deleting\u2026" : "Closing the group\u2026");
		pd.setCancelable(false);
		pd.show();
		GroupCloser.DoneCb cb = new GroupCloser.DoneCb() {
			@Override public void onDone() {
				runOnUiThread(() -> {
					pd.dismiss();
					Toast.makeText(GroupSettingsActivity.this, delete ? "Group deleted." : "Group closed.", Toast.LENGTH_LONG).show();
					if (delete) {
						Intent home = new Intent(GroupSettingsActivity.this, MainActivity.class);
						home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(home);
						finish();
					} else {
						_loadGroupDoc();
					}
				});
			}
			@Override public void onError(String e) {
				runOnUiThread(() -> { pd.dismiss(); Toast.makeText(GroupSettingsActivity.this, "Couldn't finish: " + e, Toast.LENGTH_LONG).show(); });
			}
		};
		// Re-check right before acting: things may have changed since the first check.
		GroupCloser.evaluate(this, groupId, new GroupCloser.EvalCb() {
			@Override public void onDecision(GroupClosePolicy.Decision dec, GroupClosePolicy.Facts f) {
				if (dec.action == GroupClosePolicy.Action.NONE || (delete && dec.action != GroupClosePolicy.Action.DELETE)) {
					runOnUiThread(() -> { pd.dismiss(); _showCloseDecision(dec); });
					return;
				}
				if (dec.action == GroupClosePolicy.Action.DELETE && delete) GroupCloser.delete(GroupSettingsActivity.this, groupId, cb);
				else GroupCloser.archive(GroupSettingsActivity.this, groupId, str(groupData, "visibility", "Private"), reason, cb);
			}
			@Override public void onError(String e) { cb.onError(e); }
		});
	}

	private void _confirmReopen() {
		ThemedDialogs.show(ThemedDialogs.alert(this)
			.setTitle("Reopen this group?")
			.setMessage("It returns to its previous visibility and members are told it is open again.")
			.setPositiveButton("Reopen", (dialog, which) -> GroupCloser.reopen(this, groupId, str(groupData, "closedPrevVisibility", "Private"), new GroupCloser.DoneCb() {
				@Override public void onDone() { runOnUiThread(() -> { Toast.makeText(GroupSettingsActivity.this, "Group reopened.", Toast.LENGTH_SHORT).show(); _loadGroupDoc(); }); }
				@Override public void onError(String e) { runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, "Couldn't reopen: " + e, Toast.LENGTH_LONG).show()); }
			}))
			.setNegativeButton("Cancel", null));
	}

	private boolean _isOnChain() {
		return com.harvana.app.group.GroupKinds.isOnChain(groupData);
	}
	
	private void _refreshKeyStatus() {
		if (_isOnChain()) {
			cardWalletWarning.setVisibility(View.GONE);
			btnRestoreWallet.setVisibility(View.GONE);
			return;
		}
		if (expectedWallet.isEmpty() || !expectedWallet.startsWith("HV0x")) {
			cardWalletWarning.setVisibility(View.GONE);
			btnRestoreWallet.setVisibility(View.GONE);
			return;
		}
		
		if (!isAdmin) {
			cardWalletWarning.setVisibility(View.GONE);
			btnRestoreWallet.setVisibility(View.GONE);
			return;
		}
		
		btnRestoreWallet.setVisibility(View.VISIBLE);
		
		String pk = SecureWalletStorage.loadPrivateKey(this, expectedWallet);
		if (pk != null && !pk.isEmpty()) {
			cardWalletWarning.setVisibility(View.GONE);
			btnRestoreWallet.setText("Restore again (replace key)");
			btnRestoreWallet.setIconResource(R.drawable.ic_refresh);
			btnRestoreWallet.setBackgroundTintList(ColorStateList.valueOf(com.harvana.app.utils.UiColors.accent() & 0x33FFFFFF | 0x33000000));
			btnRestoreWallet.setTextColor(UiColors.accent());
			btnRestoreWallet.setIconTintResource(R.color.colorInfo);
		} else {
			cardWalletWarning.setVisibility(View.VISIBLE);
			btnRestoreWallet.setText("Restore group wallet");
			btnRestoreWallet.setIconResource(R.drawable.ic_shield);
			btnRestoreWallet.setBackgroundTintList(ColorStateList.valueOf(UiColors.success()));
			btnRestoreWallet.setTextColor(Color.WHITE);
		}
	}
	
	// Utility Parsing Methods
	private String str(Map<String, Object> m, String k, String fb) {
		if (m == null || m.get(k) == null) return fb;
		String s = String.valueOf(m.get(k)).trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? fb : s;
	}
	
	private String numStr(Map<String, Object> m, String k) {
		if (m == null || m.get(k) == null) return "";
		return String.valueOf(m.get(k));
	}
	
	private double num(Map<String, Object> m, String k) {
		if (m == null || m.get(k) == null) return 0;
		if (m.get(k) instanceof Number) return ((Number) m.get(k)).doubleValue();
		try { return Double.parseDouble(String.valueOf(m.get(k))); } catch (Exception e) { return 0; }
	}
	
	private boolean bool(Map<String, Object> m, String k, boolean fb) {
		if (m == null || m.get(k) == null) return fb;
		Object v = m.get(k);
		if (v instanceof Boolean) return (Boolean) v;
		return "true".equalsIgnoreCase(String.valueOf(v));
	}
	
}
