package com.harvana.app;


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
import org.json.JSONObject;

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

public class GroupSettingsActivity extends AppCompatActivity implements com.harvana.app.group.GroupSettingsSectionFragment.Host {

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
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, com.harvana.app.R.layout.spinner_dropdown_item, visibilityOptions);
		spinVisibility.setAdapter(adapter);
		
		// Same option sets as CreateNewGroupActivity's own disbursement/
		// withdrawal spinners, so a value saved here means the same thing
		// there and vice versa.
		String[] disbursementOptions = new String[]{"All at Once at Goal", "Admin-Triggered"};
		spinDisbursementRule.setAdapter(new ArrayAdapter<>(this, com.harvana.app.R.layout.spinner_dropdown_item, disbursementOptions));
		
		String[] withdrawalOptions = new String[]{"Anytime", "Cycle-End Only"};
		spinWithdrawalRule.setAdapter(new ArrayAdapter<>(this, com.harvana.app.R.layout.spinner_dropdown_item, withdrawalOptions));
		
		_setupGroupNotificationToggles();
		_hideSwitchSentences();
		_buildSectionTabs();
		
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
		_showNetworkPanel();
		_organize();
		_loadTreasurer();
		_buildCoinRow();
	}
	
	private com.harvana.app.group.SettingsSections sections;
	private String treasurerWallet = "";
	private JSONObject treasurerProposal;
	
	private static String treasurerError(String e) {
		String u = e == null ? "" : e;
		if (com.harvana.app.utils.ErrorText.is(u, "TREASURER_PROPOSAL_ALREADY_OPEN")) return "A treasurer vote is already open. Wait for it to finish.";
		if (com.harvana.app.utils.ErrorText.is(u, "NOT_AN_ELIGIBLE_VOTER")) return "Only the admin and the group's most active members can vote on this.";
		if (com.harvana.app.utils.ErrorText.is(u, "NOT_A_GROUP_MEMBER")) return "Only members of the group can propose this.";
		if (com.harvana.app.utils.ErrorText.is(u, "NO_TREASURER_TO_REMOVE")) return "This group has no treasurer to remove.";
		if (com.harvana.app.utils.ErrorText.is(u, "ALREADY_THE_TREASURER")) return "That member is already the treasurer.";
		return com.harvana.app.circle.CircleUi.friendly(e);
	}
	
	private View coinRow;

	/** The group's coin row; the creator changes it while nothing is in the group (GroupCoinSetting). */
	private void _buildCoinRow() {
		if (btnRestoreWallet == null || !(btnRestoreWallet.getParent() instanceof ViewGroup)) return;
		ViewGroup host = (ViewGroup) btnRestoreWallet.getParent();
		if (coinRow != null && coinRow.getParent() != null) ((ViewGroup) coinRow.getParent()).removeView(coinRow);
		coinRow = com.harvana.app.group.GroupCoinSetting.attach(this, host, host.indexOfChild(btnRestoreWallet), groupId, groupData, isAdmin);
	}

	/** Reads the treasurer's wallet from the network record of a goal or savings group, then redraws the Members section. */
	private void _loadTreasurer() {
		GroupIdentity gi = GroupIdentityResolver.resolve(groupId, groupData);
		if (!gi.onChain() || !("ACCUM".equals(gi.family) || "FIXED".equals(gi.family))) return;
		com.harvana.app.sync.NodeGroupClient.Callback got = new com.harvana.app.sync.NodeGroupClient.Callback() {
			@Override public void onSuccess(JSONObject json) {
				treasurerWallet = json == null ? "" : json.optString("treasurerAddress", "");
				runOnUiThread(() -> _organize());
			}
			@Override public void onError(String e) { }
		};
		com.harvana.app.sync.NodeGroupClient.getTreasurerProposal(gi.protocolId(), new com.harvana.app.sync.NodeGroupClient.Callback() {
			@Override public void onSuccess(JSONObject json) { treasurerProposal = json; runOnUiThread(() -> _organize()); }
			@Override public void onError(String e) { treasurerProposal = null; runOnUiThread(() -> _organize()); }
		});
		if ("ACCUM".equals(gi.family)) com.harvana.app.sync.NodeGroupClient.getAccumGroup(gi.protocolId(), got);
		else com.harvana.app.sync.NodeGroupClient.getFixedGroup(gi.protocolId(), got);
	}
	
	/** Each section's page while its fragment's view exists (General, Access, Members, Money & rules, Network, More). */
	private final LinearLayout[] sectionPages = new LinearLayout[com.harvana.app.group.SettingsSections.NAMES.length];
	
	/**
	 * Tabs over swipeable pages, one fragment per section, in place of the old single scrolling page. The cards are still
	 * built on that page (linear39, now off screen) and then moved onto their section's page.
	 */
	/** The grey sentences under the Privacy switches: values only. */
	private void _hideSwitchSentences() {
		for (int id : new int[]{R.id.textview58, R.id.textview60, R.id.textview62}) {
			View v = findViewById(id);
			if (v != null) v.setVisibility(View.GONE);
		}
	}
	
	private void _buildSectionTabs() {
		if (vscroll2 == null || !(vscroll2.getParent() instanceof ViewGroup)) return;
		ViewGroup parent = (ViewGroup) vscroll2.getParent();
		int at = parent.indexOfChild(vscroll2);
		ViewGroup.LayoutParams lp = vscroll2.getLayoutParams();
		parent.removeView(vscroll2);
		
		LinearLayout holder = new LinearLayout(this);
		holder.setOrientation(LinearLayout.VERTICAL);
		com.google.android.material.tabs.TabLayout tabs = new com.google.android.material.tabs.TabLayout(this);
		tabs.setTabMode(com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE);
		tabs.setTabGravity(com.google.android.material.tabs.TabLayout.GRAVITY_START);
		tabs.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorSurface));
		tabs.setSelectedTabIndicatorColor(UiColors.accent());
		tabs.setTabTextColors(UiColors.textSecondary(), UiColors.accent());
		tabs.setTabRippleColor(android.content.res.ColorStateList.valueOf((UiColors.accent() & 0x00FFFFFF) | 0x1F000000));
		holder.addView(tabs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		androidx.viewpager2.widget.ViewPager2 pager = new androidx.viewpager2.widget.ViewPager2(this);
		pager.setId(R.id.vscroll2);                                   // a fixed id, so the pages come back after the screen is recreated
		pager.setOffscreenPageLimit(com.harvana.app.group.SettingsSections.NAMES.length);
		pager.setAdapter(new androidx.viewpager2.adapter.FragmentStateAdapter(this) {
			@Override public int getItemCount() { return com.harvana.app.group.SettingsSections.NAMES.length; }
			@androidx.annotation.NonNull @Override public androidx.fragment.app.Fragment createFragment(int position) {
				return com.harvana.app.group.GroupSettingsSectionFragment.newInstance(position);
			}
		});
		holder.addView(pager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		new com.google.android.material.tabs.TabLayoutMediator(tabs, pager,
			(tab, position) -> tab.setText(com.harvana.app.group.SettingsSections.NAMES[position])).attach();
		parent.addView(holder, at, lp);
	}
	
	@Override
	public void attachSection(int section, LinearLayout page) {
		if (section < 0 || section >= sectionPages.length) return;
		sectionPages[section] = page;
		if (sections != null) sections.attach(section);
	}
	
	@Override
	public void detachSection(int section) {
		if (section < 0 || section >= sectionPages.length) return;
		LinearLayout page = sectionPages[section];
		sectionPages[section] = null;
		if (page != null) page.removeAllViews();               // the cards wait off screen until the page is made again
	}
	
	/** Puts the page's cards into the standard sections (General, Access, Members, Money & rules, Network, More). */
	private void _organize() {
		LinearLayout page = linear39;
		if (page == null) return;
		if (sections == null) {
			sections = new com.harvana.app.group.SettingsSections(this, page, sectionPages,
				new com.harvana.app.group.SettingsSections.Host() {
					@Override public void invite() { com.harvana.app.group.InviteMemberSheet.show(GroupSettingsActivity.this, groupId, groupData); }
					@Override public void transferAdmin() { _showTransferAdminDialog(); }
					@Override public void leave() { _confirmLeaveGroup(); }
					@Override public void makeAdmin(String uid, String name) {
						String me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
							: com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
						_confirmTransferAdmin(me, uid, name);
					}
					@Override public void treasurer(final String uid, final String name, final String wallet, final boolean make) {
						final String me = WalletManager.getActiveWalletAddress(GroupSettingsActivity.this);
						final GroupIdentity gid = GroupIdentityResolver.resolve(groupId, groupData);
						new com.harvana.app.ui.SheetDialog.Builder(GroupSettingsActivity.this)
							.setTitle(make ? "Make " + name + " treasurer?" : "Remove " + name + " as treasurer?")
							.setMessage(make
								? "The treasurer can approve members' withdrawal requests and ask for payouts and milestone releases. A payout the treasurer asks for is paid only after the group votes and you vote yes as well. The treasurer gets no extra vote. You can remove the role at any time."
								: "They go back to being an ordinary member.")
							.setNegativeButton("Cancel", null)
							.setPositiveButton("Continue", (dl, w) -> com.harvana.app.wallet.PinPromptHelper.authorize(GroupSettingsActivity.this,
								make ? "Name a treasurer" : "Remove the treasurer", () ->
								com.harvana.app.sync.NodeGroupClient.setTreasurer(getApplicationContext(), me, gid.protocolId(), make ? wallet : "",
									new com.harvana.app.sync.NodeGroupClient.Callback() {
										@Override public void onSuccess(JSONObject json) { runOnUiThread(() -> { Toast.makeText(GroupSettingsActivity.this, make ? "Treasurer named." : "Treasurer removed.", Toast.LENGTH_SHORT).show(); _loadTreasurer(); }); }
										@Override public void onError(String error) { runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, com.harvana.app.circle.CircleUi.friendly(error), Toast.LENGTH_LONG).show()); }
									}))).show();
					}
					@Override public void proposeTreasurer(final String name, final String wallet) {
						final String me = WalletManager.getActiveWalletAddress(GroupSettingsActivity.this);
						final GroupIdentity gid = GroupIdentityResolver.resolve(groupId, groupData);
						new com.harvana.app.ui.SheetDialog.Builder(GroupSettingsActivity.this)
							.setTitle(wallet.isEmpty() ? "Propose removing the treasurer?" : "Propose " + name + " as treasurer?")
							.setMessage("The group votes on this. " + (wallet.isEmpty() ? "It passes on the usual group vote." : "It passes only if the group votes yes and the admin votes yes too.")
								+ " The vote stays open for 14 days. Only one proposal can be open at a time.")
							.setNegativeButton("Cancel", null)
							.setPositiveButton("Continue", (dl, w) -> com.harvana.app.wallet.PinPromptHelper.authorize(GroupSettingsActivity.this, "Propose a treasurer", () ->
								com.harvana.app.sync.NodeGroupClient.proposeTreasurer(getApplicationContext(), me, gid.protocolId(), wallet,
									new com.harvana.app.sync.NodeGroupClient.Callback() {
										@Override public void onSuccess(JSONObject json) { runOnUiThread(() -> { Toast.makeText(GroupSettingsActivity.this, "Proposal sent. The group can vote now.", Toast.LENGTH_SHORT).show(); _loadTreasurer(); }); }
										@Override public void onError(String error) { runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, treasurerError(error), Toast.LENGTH_LONG).show()); }
									}))).show();
					}
					@Override public void voteTreasurer(final boolean yes) {
						final String me = WalletManager.getActiveWalletAddress(GroupSettingsActivity.this);
						final GroupIdentity gid = GroupIdentityResolver.resolve(groupId, groupData);
						com.harvana.app.wallet.PinPromptHelper.authorize(GroupSettingsActivity.this, "Vote on the treasurer", () ->
							com.harvana.app.sync.NodeGroupClient.voteTreasurer(getApplicationContext(), me, gid.protocolId(), yes,
								new com.harvana.app.sync.NodeGroupClient.Callback() {
									@Override public void onSuccess(JSONObject json) { runOnUiThread(() -> { Toast.makeText(GroupSettingsActivity.this, json.optBoolean("finalized") ? "Decided. The change is in effect." : "Vote recorded.", Toast.LENGTH_SHORT).show(); _loadTreasurer(); }); }
									@Override public void onError(String error) { runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, treasurerError(error), Toast.LENGTH_LONG).show()); }
								}));
					}
					@Override public void remove(final String uid, final String name) {
						new com.harvana.app.ui.SheetDialog.Builder(GroupSettingsActivity.this).setTitle("Remove " + name + "?")
							.setMessage("They lose access to this group and cannot send a new join request unless you unblock them.")
							.setNegativeButton("Cancel", null)
							.setPositiveButton("Remove", (dl, w) -> JoinRequestManager.kickMember(GroupSettingsActivity.this, groupId, uid,
								new JoinRequestManager.Callback() {
									@Override public void onSuccess(String message) { runOnUiThread(() -> { Toast.makeText(GroupSettingsActivity.this, message, Toast.LENGTH_SHORT).show(); _loadMembers(); }); }
									@Override public void onError(String error) { runOnUiThread(() -> Toast.makeText(GroupSettingsActivity.this, error, Toast.LENGTH_LONG).show()); }
								})).show();
					}
				});
		}
		GroupIdentity gi = GroupIdentityResolver.resolve(groupId, groupData);
		sections.canHaveTreasurer = gi.onChain() && ("ACCUM".equals(gi.family) || "FIXED".equals(gi.family));
		sections.treasurerWallet = treasurerWallet;
		sections.treasurerProposal = treasurerProposal;
		sections.myWallet = String.valueOf(WalletManager.getActiveWalletAddress(this));
		sections.apply(groupId, groupData, isAdmin, memberRows == null ? 0 : memberRows.size(), _isOnChain(), memberRows,
			cardHero, cardview13, cardview11, cardview15, cardview19, cardview20, cardview10, linear72, networkPanelHost, closeSection,
			panel_fixed, panel_accumulative);
	}
	
	/** The network half of the settings: what the network says about this group, who decides, admin recovery. */
	private LinearLayout networkPanelHost;
	
	private void _showNetworkPanel() {
		ViewGroup page = linear39;
		if (page == null || groupId.isEmpty()) return;
		if (networkPanelHost == null) {
			networkPanelHost = new LinearLayout(this);
			networkPanelHost.setOrientation(LinearLayout.VERTICAL);
			page.addView(networkPanelHost);
		}
		networkPanelHost.removeAllViews();
		com.harvana.app.group.NodeSettingsPanel.attach(this, networkPanelHost, groupId, groupData);
	}
	
	private String typeOf() {
		Object t = groupData.get("groupType");
		return t != null ? t.toString() : "";
	}
	
	private boolean isRotating() { return typeOf().toLowerCase(Locale.US).contains("rotating"); }
	private boolean isFixed() { return typeOf().toLowerCase(Locale.US).contains("fixed"); }
	private boolean isAccum() { return typeOf().toLowerCase(Locale.US).contains("accumul"); }
	
	private static final int RC_GROUP_PHOTO = 731;
	private android.net.Uri pendingGroupPhoto;      // a cropped picture waiting on the phone; it uploads when Save Settings is pressed
	private boolean removeGroupPhoto;                // the admin pressed Remove; the picture is removed when Save Settings is pressed
	
	/** The group picture at the top of "About": a round preview with Change picture / Remove for the admin; members only see it. */
	private void _buildPhotoRow() {
		if (etDescription == null || !(etDescription.getParent() instanceof View) || !(((View) etDescription.getParent()).getParent() instanceof LinearLayout)) return;
		LinearLayout about = (LinearLayout) ((View) etDescription.getParent()).getParent();
		View old = about.findViewWithTag("group_photo_row");
		if (old != null) about.removeView(old);
		float d = getResources().getDisplayMetrics().density;
		LinearLayout row = new LinearLayout(this);
		row.setTag("group_photo_row");
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(android.view.Gravity.CENTER_VERTICAL);
		row.setPadding(0, 0, 0, (int) (14 * d));
		final String url = removeGroupPhoto ? "" : str(groupData, "groupPhoto", "");
		ImageView face = new ImageView(this);
		android.graphics.drawable.GradientDrawable disc = new android.graphics.drawable.GradientDrawable();
		disc.setShape(android.graphics.drawable.GradientDrawable.OVAL);
		disc.setColor((UiColors.accent() & 0x00FFFFFF) | 0x33000000);
		face.setBackground(disc);
		face.setClipToOutline(true);
		face.setOutlineProvider(new android.view.ViewOutlineProvider() {
			@Override public void getOutline(View v, android.graphics.Outline o) { o.setOval(0, 0, v.getWidth(), v.getHeight()); }
		});
		com.harvana.app.utils.GroupRing.apply(face, typeOf(), 1f, 0x00000000);
		if (pendingGroupPhoto != null) {
			face.setScaleType(ImageView.ScaleType.CENTER_CROP);
			com.bumptech.glide.Glide.with(this).load(pendingGroupPhoto).circleCrop().into(face);
		} else if (url.startsWith("http")) {
			face.setScaleType(ImageView.ScaleType.CENTER_CROP);
			com.bumptech.glide.Glide.with(this).load(url).circleCrop().into(face);
			face.setOnClickListener(v -> com.harvana.app.utils.ImageViewerActivity.open(this, url));
		} else {
			face.setImageResource(R.drawable.ic_coin_harv);
			face.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			int pad = (int) (20 * d);
			face.setPadding(pad, pad, pad, pad);
		}
		row.addView(face, new LinearLayout.LayoutParams((int) (76 * d), (int) (76 * d)));
		LinearLayout side = new LinearLayout(this);
		side.setOrientation(LinearLayout.VERTICAL);
		side.setPadding((int) (14 * d), 0, 0, 0);
		TextView t = new TextView(this);
		t.setText("Group picture");
		t.setTextSize(14.5f);
		t.setTypeface(null, android.graphics.Typeface.BOLD);
		t.setTextColor(UiColors.textPrimary());
		side.addView(t);
		TextView sub = new TextView(this);
		String waiting = pendingGroupPhoto != null ? " New picture chosen: it uploads when you press Save Settings." : (removeGroupPhoto ? " The picture is removed when you press Save Settings." : "");
		String ringNote = waiting + " The " + com.harvana.app.utils.GroupRing.colorName(typeOf()) + " ring shows the group type.";
		sub.setText((isAdmin ? "A square picture. You choose the part to keep." : (url.isEmpty() ? "No picture yet." : "Set by the group admin.")) + ringNote);
		sub.setTextSize(11.5f);
		sub.setTextColor(UiColors.textSecondary());
		side.addView(sub);
		if (isAdmin) {
			LinearLayout buttons = new LinearLayout(this);
			buttons.setPadding(0, (int) (8 * d), 0, 0);
			MaterialButton change = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
			change.setText(url.isEmpty() && pendingGroupPhoto == null ? "Add picture" : "Change picture");
			change.setAllCaps(false);
			change.setOnClickListener(v -> startActivityForResult(com.harvana.app.utils.GroupPhotoPickerActivity.intent(this), RC_GROUP_PHOTO));
			buttons.addView(change);
			if (!url.isEmpty() || pendingGroupPhoto != null) {
				MaterialButton remove = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
				remove.setText("Remove");
				remove.setAllCaps(false);
				remove.setOnClickListener(v -> { pendingGroupPhoto = null; removeGroupPhoto = true; _buildPhotoRow(); });
				LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				rl.setMarginStart((int) (8 * d));
				buttons.addView(remove, rl);
			}
			side.addView(buttons);
		}
		row.addView(side, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		about.addView(row, Math.min(1, about.getChildCount()));      // under the About heading, above the description
	}
	
	@Override
	protected void onActivityResult(int req, int res, Intent data) {
		super.onActivityResult(req, res, data);
		if (req == RC_GROUP_PHOTO && res == RESULT_OK && data != null && data.getData() != null) {
			pendingGroupPhoto = data.getData();
			removeGroupPhoto = false;
			_buildPhotoRow();
		}
	}
	
	/**
	 * Called by Save Settings. A chosen picture is uploaded first, under the group's own name (so it replaces the old one and only one is
	 * ever stored); the other settings are saved only if that works. If it fails, nothing is saved and the picture stays chosen for a retry.
	 */
	private void _savePhotoThen(final Map<String, Object> patch, final Runnable next) {
		if (removeGroupPhoto) patch.put("groupPhoto", "");
		if (pendingGroupPhoto == null) { next.run(); return; }
		com.harvana.app.utils.GroupPhotoPickerActivity.upload(this, pendingGroupPhoto, groupId, new com.harvana.app.utils.ImageHost.Callback() {
			@Override public void onSuccess(String url) {
				patch.put("groupPhoto", url);       // the picture stays "chosen" until the save succeeds, so a failed save can be retried
				next.run();
			}
			@Override public void onError(String message) {
				btnSaveSettings.setEnabled(true);
				btnSaveSettings.setText("Save Settings");
				Toast.makeText(GroupSettingsActivity.this, "Couldn't upload the picture: " + message + " Nothing was saved. Try again.", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	private void _rebuild() {
		String type = typeOf();
		_buildPhotoRow();
		tvHeroTypeName.setText(type.isEmpty() ? "Unknown Type" : type);
		tvHeroFrequency.setText(str(groupData, "frequency", "Daily"));
		com.harvana.app.utils.Icons.start(tvHeroFrequency, R.drawable.ic_calendar_line, 14);
		
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
			ivHeroTypeIcon.setImageResource(R.drawable.ic_type_circle);
			tvTypeSettingsTitle.setText("Rotating Pool");
			etGracePeriod.setText(str(groupData, "gracePeriodDays", "0"));
			etCommission.setText(str(groupData, "commissionPercent", "0"));
			tvTypeSettingsSubtitle.setText("Slots " + numStr(groupData, "totalSlots") + " · Round " + numStr(groupData, "currentRound"));
			tvRuleInfoBanner.setVisibility(View.GONE);
		} 
		else if (isFixed()) {
			ivHeroTypeIcon.setImageResource(R.drawable.ic_type_goal);
			etFixedGoal.setText(numStr(groupData, "targetGoalAmount"));
			_select(spinDisbursementRule, str(groupData, "disbursementRule", "All at Once at Goal"));
			// Values only (owner, 13 Oct 2026): no sentences under the fields.
			tv_fixed_progress.setVisibility(View.GONE);
			tvRuleInfoBanner.setVisibility(View.GONE);
		} 
		else if (isAccum()) {
			ivHeroTypeIcon.setImageResource(R.drawable.ic_type_savings);
			etLoanInterestRate.setText(numStr(groupData, "loanInterestRate"));
			_select(spinWithdrawalRule, str(groupData, "withdrawalRule", "Anytime"));
			tv_loan_active_count.setVisibility(View.GONE);
			tvRuleInfoBanner.setVisibility(View.GONE);
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
		GroupIdentity locked = GroupIdentityResolver.resolve(groupId, groupData);
		if (locked.onChain()) {
			etContribution.setEnabled(false);
			etGracePeriod.setEnabled(false);
			etCommission.setEnabled(false);
			etFixedGoal.setEnabled(false);
			spinDisbursementRule.setEnabled(false);
			etLoanInterestRate.setEnabled(false);
			spinWithdrawalRule.setEnabled(false);
			etContribution.setAlpha(0.6f);
			etGracePeriod.setAlpha(0.6f);
			etCommission.setAlpha(0.6f);
			etFixedGoal.setAlpha(0.6f);
			spinDisbursementRule.setAlpha(0.6f);
			etLoanInterestRate.setAlpha(0.6f);
			spinWithdrawalRule.setAlpha(0.6f);
		}
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
		if (sections != null) _organize();                           // the member count is known now
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
			new com.harvana.app.ui.SheetDialog.Builder(this)
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
			if (!identity.onChain()) {
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
		}
		else if (isAccum()) {
			if (!identity.onChain()) {
			String rate = etLoanInterestRate.getText().toString().trim();
			try {
				double r = Double.parseDouble(rate.isEmpty() ? "0" : rate);
				if (r < 0 || r > 5) throw new IllegalArgumentException();
				patch.put("loanInterestRate", r);
				patch.put("loanInterestPeriod", "DAILY");
			} catch (Exception e) {
				btnSaveSettings.setEnabled(true);
				btnSaveSettings.setText("Save Settings");
				Toast.makeText(this, "Interest per day must be between 0 and 5", Toast.LENGTH_SHORT).show();
				return;
			}
			if (spinWithdrawalRule.getSelectedItem() != null) {
				patch.put("withdrawalRule", spinWithdrawalRule.getSelectedItem().toString());
			}
			}
		}
		
		patch.put("blockedMemberUids", new ArrayList<>(blockedUids));
		
		_savePhotoThen(patch, () -> _patch(patch, "Settings saved successfully"));
	}
	
	private void _patch(Map<String, Object> patch, String okMsg) {
		GroupFirestoreManager.getInstance(this).updateGroupFields(groupId, patch, new GroupFirestoreManager.SimpleCallback() {
			@Override
			public void onSuccess() {
				groupData.putAll(patch);
				OfflineCacheManager.save(GroupSettingsActivity.this, "group_" + groupId, groupData);
				runOnUiThread(() -> {
					if (patch.containsKey("groupPhoto")) {
						// A removed picture is deleted from the picture host as well. (A replaced one is overwritten under the same name.)
						if ("".equals(patch.get("groupPhoto"))) com.harvana.app.utils.MediaCleaner.delete(GroupSettingsActivity.this, com.harvana.app.utils.MediaCleaner.GROUP_PHOTO, groupId);
						pendingGroupPhoto = null; removeGroupPhoto = false; _buildPhotoRow();
					}
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
		
		new com.harvana.app.ui.SheetDialog.Builder(this)
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
		new com.harvana.app.ui.SheetDialog.Builder(this)
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
		final BottomSheetDialog dialog = new com.harvana.app.ui.AppSheet(this);
		// One box for the whole phrase, words separated by spaces (12 or 24 words).
		final float dd = getResources().getDisplayMetrics().density;
		LinearLayout sheetView = new LinearLayout(this);
		sheetView.setOrientation(LinearLayout.VERTICAL);
		sheetView.setPadding((int) (20 * dd), (int) (20 * dd), (int) (20 * dd), (int) (24 * dd));
		TextView title = new TextView(this);
		title.setText("Restore group wallet");
		title.setTextSize(20);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		title.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
		sheetView.addView(title);
		TextView hint = new TextView(this);
		hint.setText("Type or paste the group wallet's recovery words, separated by spaces.");
		hint.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
		hint.setPadding(0, (int) (6 * dd), 0, (int) (12 * dd));
		sheetView.addView(hint);
		final com.google.android.material.textfield.TextInputLayout box = new com.google.android.material.textfield.TextInputLayout(this);
		final EditText phraseField = new com.google.android.material.textfield.TextInputEditText(box.getContext());
		phraseField.setHint("Recovery phrase");
		phraseField.setMinLines(3);
		phraseField.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
		phraseField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
				| android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		box.addView(phraseField);
		sheetView.addView(box);
		final MaterialButton btn = new MaterialButton(this);
		LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (52 * dd));
		blp.topMargin = (int) (16 * dd);
		sheetView.addView(btn, blp);
		dialog.setContentView(sheetView);
		btn.setText("Restore group wallet");
		btn.setOnClickListener(v -> {
			String phrase = phraseField.getText().toString().trim().toLowerCase().replaceAll("\\s+", " ");
			if (!com.harvana.app.wallet.WalletGenerator.isValidMnemonic(this, phrase)) {
				Toast.makeText(this, "Check the words: they must be 12 or 24 valid recovery words.", Toast.LENGTH_LONG).show();
				return;
			}
			_restoreGroupWallet(phrase, dialog, btn);
		});
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
					Toast.makeText(this, "Restore failed: " + com.harvana.app.utils.ErrorText.of(e), Toast.LENGTH_LONG).show();
				});
			}
		}).start();
	}
	
	private LinearLayout backupSection;
	
	/** New-phone recovery: download the group's published encrypted card, decrypt with the admin passphrase. */
	private void _restoreFromCard() {
		final com.harvana.app.ui.ProgressSheet pd = com.harvana.app.ui.ProgressSheet.show(this, null, "Fetching backup card…", true, false);
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
		final com.google.android.material.textfield.TextInputLayout passBox = new com.google.android.material.textfield.TextInputLayout(this);
		final EditText pass = new com.google.android.material.textfield.TextInputEditText(passBox.getContext());
		pass.setHint("Access Card passphrase");
		pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passBox.addView(pass);
		passBox.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE);   // show / hide
		new com.harvana.app.ui.SheetDialog.Builder(this)
			.setTitle("Restore from Access Card")
			.setMessage("Enter the passphrase you chose when the card was created.")
			.setView(passBox)
			.setNegativeButton("Cancel", null)
			.setPositiveButton("Restore", (dlg, w) -> {
				final String pw = pass.getText().toString();
				final com.harvana.app.ui.ProgressSheet pd = com.harvana.app.ui.ProgressSheet.show(this, null, "Decrypting…", true, false);
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
							Toast.makeText(this, "Restore failed: " + com.harvana.app.utils.ErrorText.of(e), Toast.LENGTH_LONG).show();
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
		androidx.appcompat.widget.SwitchCompat loanSw = new com.google.android.material.switchmaterial.SwitchMaterial(this);
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
		ViewGroup host = linear39;
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
		info.setText("");
		info.setVisibility(View.GONE);
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
		final boolean refund = dec.action == GroupClosePolicy.Action.REFUND_AND_DELETE;
		final boolean delete = dec.action == GroupClosePolicy.Action.DELETE || refund;
		final String groupName = str(groupData, "groupName", "");
		final String phrase = delete ? groupName : "CLOSE";

		LinearLayout box = new LinearLayout(this);
		box.setOrientation(LinearLayout.VERTICAL);
		int pad = (int) (20 * getResources().getDisplayMetrics().density);
		box.setPadding(pad, pad / 2, pad, 0);
		TextView msg = new TextView(this);
		msg.setTextColor(UiColors.textPrimary());
		msg.setTextSize(13);
		msg.setText(sb.toString().trim() + "\n\n" + (refund
			? "The group is closed on the network and its wallet is sent back to you. Then it is removed from the app. It cannot be undone."
			: delete
			? "This permanently removes the group and its records from the app. It cannot be undone."
			: "Members are notified, joining stops, and the group leaves Discover. Everyone keeps their history and anything they are owed on the network. You can reopen it later."));
		box.addView(msg);
		final EditText reason = new com.google.android.material.textfield.TextInputEditText(this);
		if (!delete) { reason.setHint("Reason for members (optional)"); reason.setTextColor(UiColors.textPrimary()); box.addView(reason); }
		final EditText confirm = new com.google.android.material.textfield.TextInputEditText(this);
		confirm.setHint("Type " + phrase + " to confirm");
		confirm.setTextColor(UiColors.textPrimary());
		box.addView(confirm);

		com.harvana.app.ui.SheetDialog dialog = ThemedDialogs.alert(this)
			.setTitle(dec.headline).setView(box)
			.setPositiveButton(refund ? "Refund and delete" : delete ? "Delete" : "Close group", null)
			.setNegativeButton("Cancel", null).create();
		dialog.setOnShowListener(x -> dialog.getButton(com.harvana.app.ui.SheetDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			if (!confirm.getText().toString().trim().equalsIgnoreCase(phrase)) {
				confirm.setError("Type " + phrase + " exactly");
				return;
			}
			dialog.dismiss();
			com.harvana.app.wallet.PinPromptHelper.verifyPinThenProceed(GroupSettingsActivity.this, () ->
				_runClose(delete, refund, reason.getText().toString().trim()));
		}));
		dialog.show();
	}

	private void _runClose(boolean delete, boolean refund, String reason) {
		final com.harvana.app.ui.ProgressSheet pd = new com.harvana.app.ui.ProgressSheet(this);
		pd.setMessage(refund ? "Closing on the network and refunding you\u2026" : delete ? "Deleting\u2026" : "Closing the group\u2026");
		pd.setCancelable(false);
		pd.show();
		GroupCloser.DoneCb cb = new GroupCloser.DoneCb() {
			@Override public void onDone() {
				runOnUiThread(() -> {
					pd.dismiss();
					Toast.makeText(GroupSettingsActivity.this, refund ? "Group closed and deleted. Your refund arrives in your wallet shortly."
						: delete ? "Group deleted." : "Group closed.", Toast.LENGTH_LONG).show();
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
				boolean deleteOk = dec.action == GroupClosePolicy.Action.DELETE || dec.action == GroupClosePolicy.Action.REFUND_AND_DELETE;
				boolean isRefund = dec.action == GroupClosePolicy.Action.REFUND_AND_DELETE;
				if (dec.action == GroupClosePolicy.Action.NONE || (delete && !deleteOk) || refund != isRefund) {
					runOnUiThread(() -> { pd.dismiss(); _showCloseDecision(dec); });
					return;
				}
				if (dec.action == GroupClosePolicy.Action.REFUND_AND_DELETE) {
					GroupCloser.refundAndDelete(GroupSettingsActivity.this, groupId, f.nodeId, cb);
					return;
				}
				// The picture goes first: the node checks the group's admin from the group profile, which the delete removes.
				if (dec.action == GroupClosePolicy.Action.DELETE && delete) com.harvana.app.utils.MediaCleaner.delete(GroupSettingsActivity.this, com.harvana.app.utils.MediaCleaner.GROUP_PHOTO, groupId,
						() -> GroupCloser.delete(GroupSettingsActivity.this, groupId, cb));
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
			btnRestoreWallet.setTextColor(com.harvana.app.utils.UiColors.onAccent());
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
