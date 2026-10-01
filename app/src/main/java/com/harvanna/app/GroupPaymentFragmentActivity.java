package com.harvanna.app;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
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
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
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
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.*;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import com.harvanna.app.group.FixedDayGridAdapter;
import com.harvanna.app.group.FixedPaymentEngine;
import com.harvanna.app.sync.HarvannaNodeClient;
import java.util.concurrent.TimeUnit;
import com.harvanna.app.group.GroupPaymentHelper;
import com.harvanna.app.group.GroupValidation;
import android.os.CountDownTimer;


public class GroupPaymentFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private LinearLayout linear126;
	private MaterialCardView card_group_wallet;
	private MaterialCardView cardview23;
	private LinearLayout panel_fixed_days;
	private LinearLayout panel_type_board;
	private LinearLayout layout_type_board;
	private TextView tv_type_board_title;
	private TextView tv_type_board_subtitle;
	private TextView tv_type_board_legend;
	private LinearLayout linear129;
	private LinearProgressIndicator progress_round_paid;
	private RecyclerView recycler_payments;
	private TextView tv_payments_empty;
	private LinearLayout linear3;
	private TextView textview7;
	private TextView tv_group_wallet_balance;
	private TextView tv_group_wallet_ngn;
	private LinearLayout panel_per_slot_amount;
	private View view27;
	private LinearLayout linear28;
	private LinearLayout linear29;
	private TextView btn_edit_slot_amount;
	private TextView textview2;
	private TextView tv_per_slot_amount;
	private LinearLayout linear127;
	private LinearLayout linear128;
	private CardView cardview24;
	private TextView tv_my_payment_label;
	private TextView tv_my_payment_amount;
	private TextView tv_my_payment_due;
	private TextView btn_pay_now;
	private TextView textview8;
	private TextView tv_fixed_progress;
	private RecyclerView recycler_fixed_days;
	private TextView textview9;
	private TextView textview72;
	private TextView tv_round_paid_count;
	
	private DatabaseReference groupdb = _firebase.getReference("groupdb");
	private ChildEventListener _groupdb_child_listener;
	private SharedPreferences groupsp;
	private DatabaseReference userdb = _firebase.getReference("userdb");
	private ChildEventListener _userdb_child_listener;
	private FirebaseAuth fbauth;
	private OnCompleteListener<AuthResult> _fbauth_create_user_listener;
	private OnCompleteListener<AuthResult> _fbauth_sign_in_listener;
	private OnCompleteListener<Void> _fbauth_reset_password_listener;
	private OnCompleteListener<Void> fbauth_updateEmailListener;
	private OnCompleteListener<Void> fbauth_updatePasswordListener;
	private OnCompleteListener<Void> fbauth_emailVerificationSentListener;
	private OnCompleteListener<Void> fbauth_deleteUserListener;
	private OnCompleteListener<Void> fbauth_updateProfileListener;
	private OnCompleteListener<AuthResult> fbauth_phoneAuthListener;
	private OnCompleteListener<AuthResult> fbauth_googleSignInListener;
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.group_payment_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		linear126 = _view.findViewById(R.id.linear126);
		card_group_wallet = _view.findViewById(R.id.card_group_wallet);
		cardview23 = _view.findViewById(R.id.cardview23);
		panel_fixed_days = _view.findViewById(R.id.panel_fixed_days);
		panel_type_board = _view.findViewById(R.id.panel_type_board);
		layout_type_board = _view.findViewById(R.id.layout_type_board);
		tv_type_board_title = _view.findViewById(R.id.tv_type_board_title);
		tv_type_board_subtitle = _view.findViewById(R.id.tv_type_board_subtitle);
		tv_type_board_legend = _view.findViewById(R.id.tv_type_board_legend);
		linear129 = _view.findViewById(R.id.linear129);
		progress_round_paid = _view.findViewById(R.id.progress_round_paid);
		recycler_payments = _view.findViewById(R.id.recycler_payments);
		tv_payments_empty = _view.findViewById(R.id.tv_payments_empty);
		linear3 = _view.findViewById(R.id.linear3);
		textview7 = _view.findViewById(R.id.textview7);
		tv_group_wallet_balance = _view.findViewById(R.id.tv_group_wallet_balance);
		tv_group_wallet_ngn = _view.findViewById(R.id.tv_group_wallet_ngn);
		panel_per_slot_amount = _view.findViewById(R.id.panel_per_slot_amount);
		view27 = _view.findViewById(R.id.view27);
		linear28 = _view.findViewById(R.id.linear28);
		linear29 = _view.findViewById(R.id.linear29);
		btn_edit_slot_amount = _view.findViewById(R.id.btn_edit_slot_amount);
		textview2 = _view.findViewById(R.id.textview2);
		tv_per_slot_amount = _view.findViewById(R.id.tv_per_slot_amount);
		linear127 = _view.findViewById(R.id.linear127);
		linear128 = _view.findViewById(R.id.linear128);
		cardview24 = _view.findViewById(R.id.cardview24);
		tv_my_payment_label = _view.findViewById(R.id.tv_my_payment_label);
		tv_my_payment_amount = _view.findViewById(R.id.tv_my_payment_amount);
		tv_my_payment_due = _view.findViewById(R.id.tv_my_payment_due);
		btn_pay_now = _view.findViewById(R.id.btn_pay_now);
		textview8 = _view.findViewById(R.id.textview8);
		tv_fixed_progress = _view.findViewById(R.id.tv_fixed_progress);
		recycler_fixed_days = _view.findViewById(R.id.recycler_fixed_days);
		textview9 = _view.findViewById(R.id.textview9);
		textview72 = _view.findViewById(R.id.textview72);
		tv_round_paid_count = _view.findViewById(R.id.tv_round_paid_count);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();
		
		btn_pay_now.setOnClickListener(_v -> _onPayClicked());
		
		_groupdb_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {
				
			}
			
			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();
				
			}
		};
		groupdb.addChildEventListener(_groupdb_child_listener);
		
		_userdb_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {
				
			}
			
			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();
				
			}
		};
		userdb.addChildEventListener(_userdb_child_listener);
		
		fbauth_updateEmailListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_updatePasswordListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_emailVerificationSentListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_deleteUserListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_phoneAuthListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		fbauth_updateProfileListener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		fbauth_googleSignInListener = task -> {
			final boolean _success = task.isSuccessful();
			final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
			
		};
		
		_fbauth_create_user_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_sign_in_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
			
		};
		
		_fbauth_reset_password_listener = _param1 -> {
			final boolean _success = _param1.isSuccessful();
			
		};
	}
	
	private void initializeLogic() {
		groupId = getArguments() != null ? getArguments().getString("groupId", "") : "";
		myUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		monthKey = FixedPaymentEngine.currentMonthKey();
		if (btn_pay_now != null) btn_pay_now.setOnClickListener(v -> _onPayClicked());
		_loadAll();
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (!groupId.isEmpty()) _loadAll();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (countdownTimer != null) countdownTimer.cancel();
	}
	

	public void _MORE() {
	}
	private boolean hasPaidThisCycle = false;
	private double contributionAmount = 0;
	private String groupWalletAddress = "";
	private String groupId = "";
	private int mySlot = 0;
	private static final String PREF_PAY = "harvanna_group_pay_state";
	private String myUid = "";
	private String groupType = "";
	private long totalSlots = 1;
	private long currentRound = 1;
	private int currentPayerSlot = 1;
	private boolean iHavePaidThisRound = false;
	private String frequency = "Weekly";
	private String monthKey = FixedPaymentEngine.currentMonthKey();
	private List<Integer> fixedDaysPaid = new ArrayList<>();
	private CountDownTimer countdownTimer;
	private final ArrayList<HashMap<String, Object>> paymentRows = new ArrayList<>();
	private FixedDayGridAdapter dayAdapter;
	private List<Map<String, Object>> boardMembers = new ArrayList<>();
	private String linkedNodeId = "";
	private String groupCreatorId = "";
	private boolean viewerIsAdmin = false;
	
	private void _applyCachedPayContext() {
		Map<String, Object> cached = com.harvanna.app.offline.OfflineCacheManager.load(
		getContext(), "group_" + groupId);
		if (cached != null) {
			if (contributionAmount <= 0) contributionAmount = num(cached, "contributionAmount");
			if (totalSlots <= 1) totalSlots = Math.max(1, (long) num(cached, "totalSlots"));
			currentRound = Math.max(1, (long) num(cached, "currentRound"));
			frequency = str(cached, "frequency", frequency);
			groupType = str(cached, "groupType", groupType);
			groupWalletAddress = str(cached, "groupWalletAddress",
			str(cached, "group_wallet", groupWalletAddress));
			currentPayerSlot = (int) ((currentRound - 1) % Math.max(1, totalSlots)) + 1;
		}
		List<HashMap<String, Object>> cachedMembers =
		com.harvanna.app.offline.OfflineCacheManager.loadList(getContext(), "members_" + groupId);
		if (cachedMembers != null) {
			for (HashMap<String, Object> m : cachedMembers) {
				if (str(m, "memberUid", "").equals(myUid)) {
					mySlot = (int) num(m, "slot");
					break;
				}
			}
		}
		_updatePayButtonState();
	}
	
	private void _loadAll() {
		if (groupId.isEmpty() || getContext() == null) return;
		_applyCachedPayContext();
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getGroup(groupId, new com.harvanna.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				contributionAmount = num(data, "contributionAmount");
				totalSlots = Math.max(1, (long) num(data, "totalSlots"));
				currentRound = Math.max(1, (long) num(data, "currentRound"));
				frequency = str(data, "frequency", "Weekly");
				groupType = str(data, "groupType", "");
				groupWalletAddress = str(data, "groupWalletAddress", str(data, "group_wallet", ""));
				currentPayerSlot = (int) ((currentRound - 1) % totalSlots) + 1;
				groupCreatorId = str(data, "creatorId", "");
				if (myUid != null && myUid.equals(groupCreatorId)) viewerIsAdmin = true;
				linkedNodeId = com.harvanna.app.group.GroupKinds.nodeId(data);
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (tv_my_payment_amount != null) {
						// APPLIED FIX
						tv_my_payment_amount.setText(_formatUIBalance(contributionAmount));
					}
					boolean isFixed = groupType != null && groupType.toLowerCase(Locale.US).contains("fixed");
					if (panel_fixed_days != null) {
						panel_fixed_days.setVisibility(View.GONE);
					}
					if (!linkedNodeId.isEmpty()) {
						_hideLegacyPaymentChrome();
						_embedNodeRecord(linkedNodeId);
					} else if (isFixed) {
						_setupFixedGrid();
					}
					_loadMyMemberThenPayments();
					_loadGroupWalletBalance();
				});
			}
			@Override public void onError(String error) { _loadPaymentsOnly(); }
		});
	}
	
	private void _setupFixedGrid() {
		if (recycler_fixed_days == null || getContext() == null) return;
		if (dayAdapter == null) {
			dayAdapter = new FixedDayGridAdapter();
			recycler_fixed_days.setLayoutManager(new GridLayoutManager(getContext(), 7));
			recycler_fixed_days.setAdapter(dayAdapter);
			dayAdapter.setListener((day, alreadyPaid) -> {
				if (alreadyPaid) {
					SketchwareUtil.showMessage(getContext(), "Day " + day + " already paid");
				} else if (day == FixedPaymentEngine.todayDayOfMonth()) {
					_onPayClicked();
				} else {
					SketchwareUtil.showMessage(getContext(), "You can only pay for today (day "
					+ FixedPaymentEngine.todayDayOfMonth() + ")");
				}
			});
		}
		dayAdapter.setPaidDays(fixedDaysPaid);
		if (tv_fixed_progress != null) {
			tv_fixed_progress.setText(FixedPaymentEngine.paidCount(fixedDaysPaid) + " / 31 days · month " + monthKey
			+ " · admin fee 1 day at close");
		}
	}
	
	private void _loadMyMemberThenPayments() {
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getMembers(groupId, new com.harvanna.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> members) {
				mySlot = 0;
				fixedDaysPaid = new ArrayList<>();
				boardMembers = new ArrayList<>();
				if (members != null) {
					for (Map<String, Object> m : members) {
						boardMembers.add(m);
						if (str(m, "memberUid", "").equals(myUid)) {
							mySlot = (int) num(m, "slot");
							fixedDaysPaid = FixedPaymentEngine.daysPaidFromMember(m, monthKey);
							String role = str(m, "role", "");
							if ("ADMIN".equalsIgnoreCase(role) || "CREATOR".equalsIgnoreCase(role)) viewerIsAdmin = true;
						}
					}
				}
				if (!linkedNodeId.isEmpty()) _embedNodeRecord(linkedNodeId);
				if (getActivity() != null) {
					getActivity().runOnUiThread(() -> {
						if (dayAdapter != null) dayAdapter.setPaidDays(fixedDaysPaid);
						if (tv_fixed_progress != null && groupType.toLowerCase(Locale.US).contains("fixed")) {
							tv_fixed_progress.setText(FixedPaymentEngine.paidCount(fixedDaysPaid)
							+ " / 31 days paid · " + monthKey);
						}
					});
				}
				_loadPaymentsOnly();
			}
			@Override public void onError(String error) { _loadPaymentsOnly(); }
		});
	}
	
	private void _processPaymentsList(List<Map<String, Object>> items, boolean isCached) {
		paymentRows.clear();
		iHavePaidThisRound = false;
		long myLatestPaymentTs = 0;
		final List<Map<String, Object>> stalePending = new ArrayList<>();
		
		if (items != null) {
			for (Map<String, Object> p : items) {
				paymentRows.add(new HashMap<>(p));
				String st = str(p, "status", "");
				boolean confirmed = "CONFIRMED".equalsIgnoreCase(st) || "PAID".equalsIgnoreCase(st);
				boolean pending = "PENDING".equalsIgnoreCase(st);
				long round = (long) num(p, "round");
				if (round <= 0) round = currentRound;
				
				if (confirmed && str(p, "memberUid", "").equals(myUid) && round == currentRound) {
					iHavePaidThisRound = true;
					long ts = (long) num(p, "timestamp");
					if (ts > myLatestPaymentTs) myLatestPaymentTs = ts;
				}
				
				String txRef = str(p, "txRef", "");
				String docId = str(p, "_docId", "");
				if (!isCached && pending && !txRef.isEmpty() && !docId.isEmpty()) {
					stalePending.add(p);
				}
			}
		}
		
		if (iHavePaidThisRound && myLatestPaymentTs > 0) {
			getContext().getSharedPreferences(PREF_PAY, Activity.MODE_PRIVATE)
			.edit().putLong(groupId + "_" + myUid + "_paidAt", myLatestPaymentTs).apply();
		}
		
		if (!isCached) {
			com.harvanna.app.offline.OfflineCacheManager.saveList(getContext(), "payments_" + groupId, paymentRows);
		}
		
		_renderPaymentRows();
		
		if (!isCached && !stalePending.isEmpty()) {
			_reconcilePendingPayments(stalePending);
		}
	}
	
	private void _loadPaymentsOnly() {
		List<HashMap<String, Object>> cached = com.harvanna.app.offline.OfflineCacheManager.loadList(
		getContext(), "payments_" + groupId);
		if (cached != null && !cached.isEmpty() && paymentRows.isEmpty()) {
			_processPaymentsList(new ArrayList<>(cached), true);
		}
		
		com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getPayments(groupId, new com.harvanna.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> items) {
				if (getActivity() == null) return;
				_processPaymentsList(items, false);
			}
			@Override public void onError(String error) {
				if (getActivity() != null) getActivity().runOnUiThread(() -> _updatePayButtonState());
			}
		});
	}
	
	private void _renderPaymentRows() {
		java.util.Collections.sort(paymentRows, (a, b) -> Long.compare((long) num(b, "timestamp"), (long) num(a, "timestamp")));
		
		int paidThisRound = 0;
		for (Map<String, Object> p : paymentRows) {
			String st = str(p, "status", "");
			boolean ok = "CONFIRMED".equalsIgnoreCase(st) || "PAID".equalsIgnoreCase(st);
			long round = (long) num(p, "round");
			if (round <= 0) round = currentRound;
			if (ok && round == currentRound) paidThisRound++;
		}
		final int fPaid = paidThisRound;
		
		if (getActivity() == null) return;
		getActivity().runOnUiThread(() -> {
			if (tv_round_paid_count != null) {
				if (groupType.toLowerCase(Locale.US).contains("fixed")) {
					int paidDays = FixedPaymentEngine.paidCount(fixedDaysPaid);
					tv_round_paid_count.setText(paidDays + " / 31 days");
					if (progress_round_paid != null) {
						progress_round_paid.setProgress((int) Math.round((paidDays / 31.0) * 100));
					}
				} else {
					tv_round_paid_count.setText(fPaid + " / " + totalSlots + " paid this round");
					if (progress_round_paid != null) {
						int percent = totalSlots > 0 ? (int) Math.round((fPaid / (double) totalSlots) * 100) : 0;
						progress_round_paid.setProgress(percent);
					}
				}
			}
			if (textview72 != null) textview72.setText("PAYMENT RECORDS");
			_bindTypeBoard();
			if (tv_payments_empty != null) {
				tv_payments_empty.setVisibility(paymentRows.isEmpty() ? View.VISIBLE : View.GONE);
			}
			if (recycler_payments != null) {
				if (recycler_payments.getLayoutManager() == null) {
					recycler_payments.setLayoutManager(new LinearLayoutManager(getContext()));
				}
				recycler_payments.setAdapter(new PayAdapter(paymentRows));
			}
			_updatePayButtonState();
		});
	}
	
	private void _reconcilePendingPayments(List<Map<String, Object>> stalePending) {
		if (getContext() == null) return;
		final int[] remaining = {stalePending.size()};
		final boolean[] anyChanged = {false};
		
		for (Map<String, Object> p : stalePending) {
			String txRef = str(p, "txRef", "");
			String docId = str(p, "_docId", "");
			
			com.harvanna.app.sync.HarvannaNodeClient.getTxStatus(txRef, new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
				@Override
				public void onSuccess(org.json.JSONObject json) {
					String status = json.optString("status", "");
					int confirmations = json.optInt("confirmations", 0);
					boolean nowConfirmed = "CONFIRMED".equalsIgnoreCase(status) || confirmations >= 1;
					
					if (nowConfirmed) {
						Map<String, Object> update = new HashMap<>();
						update.put("status", "CONFIRMED");
						com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
						.updatePaymentStatus(groupId, docId, update,
						new com.harvanna.app.firestore.GroupFirestoreManager.SimpleCallback() {
							@Override public void onSuccess() { anyChanged[0] = true; _checkReconcileDone(); }
							@Override public void onError(String error) { _checkReconcileDone(); }
						});
					} else {
						_checkReconcileDone();
					}
				}
				@Override
				public void onError(String error) { _checkReconcileDone(); }
				
				private void _checkReconcileDone() {
					remaining[0]--;
					if (remaining[0] <= 0 && anyChanged[0] && getActivity() != null) {
						getActivity().runOnUiThread(() -> _loadAll());
					}
				}
			});
		}
	}
	
	private void _loadGroupWalletBalance() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		
		Map<String, Object> cachedBalance = com.harvanna.app.offline.OfflineCacheManager.load(
		getContext(), "wallet_balance_" + groupWalletAddress);
		if (cachedBalance != null && cachedBalance.containsKey("show")) {
			double cachedHarv = num(cachedBalance, "show");
			if (tv_group_wallet_balance != null) {
				// APPLIED FIX
				tv_group_wallet_balance.setText(_formatUIBalance(cachedHarv));
			}
			if (tv_group_wallet_ngn != null) tv_group_wallet_ngn.setVisibility(View.GONE); // no real rate: show nothing rather than a made-up one
		}
		
		HarvannaNodeClient.getWallet(groupWalletAddress, new HarvannaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				double harv = HarvannaNodeClient.toHarvDisplay(json.optLong("balance", 0L));
				Map<String, Object> toCache = new HashMap<>();
				toCache.put("show", harv);
				com.harvanna.app.offline.OfflineCacheManager.save(getContext(), "wallet_balance_" + groupWalletAddress, toCache);
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (tv_group_wallet_balance != null) {
						// APPLIED FIX
						tv_group_wallet_balance.setText(_formatUIBalance(harv));
					}
					if (tv_group_wallet_ngn != null) tv_group_wallet_ngn.setVisibility(View.GONE);
				});
			}
			@Override
			public void onError(String error) {}
		});
	}
	
	private void _bindTypeBoard() {
		if (panel_type_board == null || layout_type_board == null || getContext() == null) return;
		if (linkedNodeId != null && !linkedNodeId.isEmpty()) return;
		Map<String, Object> group = new HashMap<>();
		group.put("groupType", groupType);
		group.put("totalSlots", totalSlots);
		group.put("currentRound", currentRound);
		group.put("frequency", frequency);
		group.put("contributionAmount", contributionAmount);
		List<Map<String, Object>> pays = new ArrayList<>();
		pays.addAll(paymentRows);
		boolean accum = com.harvanna.app.group.GroupTypeBoard.ACCUMULATIVE.equals(
				com.harvanna.app.group.GroupTypeBoard.kindOf(groupType));
		boolean personalBook = !accum || !viewerIsAdmin;
		com.harvanna.app.group.GroupTypeBoard.Board board =
			com.harvanna.app.group.GroupTypeBoard.fromFirestore(
				group, boardMembers, pays, myUid, personalBook, System.currentTimeMillis(), java.util.TimeZone.getDefault());
		if (board.kind == null || board.kind.isEmpty()) {
			panel_type_board.setVisibility(View.GONE);
			return;
		}
		panel_type_board.setVisibility(View.VISIBLE);
		if (tv_type_board_title != null) tv_type_board_title.setText(board.title);
		if (tv_type_board_subtitle != null) tv_type_board_subtitle.setText(board.subtitle);
		if (tv_type_board_legend != null) tv_type_board_legend.setText(board.legend);
		com.harvanna.app.group.GroupTypeBoardView.fill(layout_type_board, board, cell -> {
			if (cell.payToday && btn_pay_now != null && btn_pay_now.isEnabled()) {
				_onPayClicked();
			} else if (getContext() != null) {
				com.harvanna.app.group.GroupTypeBoardView.showDetail(getContext(), cell);
			}
		});
	}

	/** On-chain groups show the savings card here, not a second screen. */
	private int nodeLoadGen = 0;
	private com.google.android.material.button.MaterialButton nodeActionButton;

	private void _hideLegacyPaymentChrome() {
		if (card_group_wallet != null) card_group_wallet.setVisibility(View.GONE);
		if (cardview23 != null) cardview23.setVisibility(View.GONE);
		if (linear129 != null) linear129.setVisibility(View.GONE);
		if (panel_fixed_days != null) panel_fixed_days.setVisibility(View.GONE);
	}

	private void _embedNodeRecord(String nodeId) {
		if (nodeId == null || nodeId.isEmpty() || getContext() == null) return;
		final int gen = ++nodeLoadGen;
		final String family = com.harvanna.app.sync.NodeGroupClient.familyOf(nodeId);
		final String me = com.harvanna.app.wallet.WalletManager.getActiveWalletAddress(getContext());
		com.harvanna.app.sync.NodeGroupClient.Callback onGroup = new com.harvanna.app.sync.NodeGroupClient.Callback() {
			@Override public void onSuccess(org.json.JSONObject json) {
				if (gen != nodeLoadGen) return;
				Map<String, Object> group = com.harvanna.app.utils.JsonMaps.toMap(json);
				boolean admin = viewerIsAdmin || (me != null && me.equalsIgnoreCase(str(group, "creatorAddress", "")));
				if (admin) viewerIsAdmin = true;
				_loadMyNodePosition(gen, family, nodeId, me, group, admin);
			}
			@Override public void onError(String e) { }
		};
		if ("CIRCLE".equals(family)) com.harvanna.app.sync.NodeGroupClient.getCircle(nodeId, onGroup);
		else if ("ACCUM".equals(family)) com.harvanna.app.sync.NodeGroupClient.getAccumGroup(nodeId, onGroup);
		else if ("FIXED".equals(family)) com.harvanna.app.sync.NodeGroupClient.getFixedGroup(nodeId, onGroup);
	}

	private void _loadMyNodePosition(int gen, String family, String nodeId, String me,
			Map<String, Object> group, boolean admin) {
		if (me == null || me.isEmpty()) {
			_finishNodeRecord(gen, family, group, null, null, me, admin);
			return;
		}
		com.harvanna.app.sync.NodeGroupClient.Callback cb = new com.harvanna.app.sync.NodeGroupClient.Callback() {
			@Override public void onSuccess(org.json.JSONObject json) {
				if (gen != nodeLoadGen) return;
				Map<String, Object> member = com.harvanna.app.utils.JsonMaps.toMap(json);
				if (!"ACCUM".equals(family)) {
					_finishNodeRecord(gen, family, group, member, null, me, admin);
					return;
				}
				Object lid = member.get("currentLineId");
				if (lid == null || String.valueOf(lid).isEmpty()) {
					_finishNodeRecord(gen, family, group, member, null, me, admin);
					return;
				}
				com.harvanna.app.sync.NodeGroupClient.getAccumLine(nodeId, String.valueOf(lid), new com.harvanna.app.sync.NodeGroupClient.Callback() {
					@Override public void onSuccess(org.json.JSONObject lineJson) {
						if (gen != nodeLoadGen) return;
						_finishNodeRecord(gen, family, group, member, com.harvanna.app.utils.JsonMaps.toMap(lineJson), me, admin);
					}
					@Override public void onError(String e) {
						_finishNodeRecord(gen, family, group, member, null, me, admin);
					}
				});
			}
			@Override public void onError(String e) {
				_finishNodeRecord(gen, family, group, null, null, me, admin);
			}
		};
		if ("CIRCLE".equals(family)) com.harvanna.app.sync.NodeGroupClient.getCircleMember(nodeId, me, cb);
		else if ("ACCUM".equals(family)) com.harvanna.app.sync.NodeGroupClient.getAccumMember(nodeId, me, cb);
		else com.harvanna.app.sync.NodeGroupClient.getFixedMember(nodeId, me, cb);
	}

	private void _finishNodeRecord(int gen, String family, Map<String, Object> group,
			Map<String, Object> member, Map<String, Object> line, String me, boolean admin) {
		if (!admin || !"ACCUM".equals(family)) {
			_paintNodeRecord(gen, family, group, member, line, me, false, null);
			return;
		}
		final List<Object[]> others = new ArrayList<>();
		List<Map<String, Object>> targets = new ArrayList<>();
		if (boardMembers != null) {
			for (Map<String, Object> m : boardMembers) {
				String wallet = str(m, "walletAddress", str(m, "wallet_address", str(m, "wallet", "")));
				if (wallet.isEmpty() || (me != null && wallet.equalsIgnoreCase(me))) continue;
				targets.add(m);
			}
		}
		if (targets.isEmpty()) {
			_paintNodeRecord(gen, family, group, member, line, me, true, others);
			return;
		}
		final java.util.concurrent.atomic.AtomicInteger left = new java.util.concurrent.atomic.AtomicInteger(targets.size());
		for (Map<String, Object> m : targets) {
			final String wallet = str(m, "walletAddress", str(m, "wallet_address", str(m, "wallet", "")));
			final String name = str(m, "displayName", str(m, "memberName", str(m, "name", _shortWallet(wallet))));
			com.harvanna.app.sync.NodeGroupClient.getAccumMember(linkedNodeId, wallet, new com.harvanna.app.sync.NodeGroupClient.Callback() {
				@Override public void onSuccess(org.json.JSONObject json) {
					Map<String, Object> mem = com.harvanna.app.utils.JsonMaps.toMap(json);
					Object lid = mem.get("currentLineId");
					if (lid == null || String.valueOf(lid).isEmpty()) {
						if (left.decrementAndGet() == 0) _paintNodeRecord(gen, family, group, member, line, me, true, others);
						return;
					}
					com.harvanna.app.sync.NodeGroupClient.getAccumLine(linkedNodeId, String.valueOf(lid), new com.harvanna.app.sync.NodeGroupClient.Callback() {
						@Override public void onSuccess(org.json.JSONObject lineJson) {
							synchronized (others) { others.add(new Object[]{name, com.harvanna.app.utils.JsonMaps.toMap(lineJson)}); }
							if (left.decrementAndGet() == 0) _paintNodeRecord(gen, family, group, member, line, me, true, others);
						}
						@Override public void onError(String e) {
							if (left.decrementAndGet() == 0) _paintNodeRecord(gen, family, group, member, line, me, true, others);
						}
					});
				}
				@Override public void onError(String e) {
					if (left.decrementAndGet() == 0) _paintNodeRecord(gen, family, group, member, line, me, true, others);
				}
			});
		}
	}

	private void _paintNodeRecord(int gen, String family, Map<String, Object> group,
			Map<String, Object> member, Map<String, Object> line, String me, boolean admin, List<Object[]> others) {
		if (gen != nodeLoadGen || getActivity() == null) return;
		getActivity().runOnUiThread(() -> {
			if (gen != nodeLoadGen || getContext() == null || layout_type_board == null || panel_type_board == null) return;
			_hideLegacyPaymentChrome();
			long now = System.currentTimeMillis();
			java.util.TimeZone zone = java.util.TimeZone.getDefault();
			com.harvanna.app.group.GroupTypeBoard.Board mine =
					com.harvanna.app.group.GroupTypeBoard.fromNode(family, group, member, line, me, now, zone);
			boolean accum = "ACCUM".equals(family);
			panel_type_board.setVisibility(View.VISIBLE);
			if (tv_type_board_title != null) {
				tv_type_board_title.setText(accum ? (admin ? "Members' savings cards" : "Your savings card") : mine.title);
			}
			if (tv_type_board_subtitle != null) {
				tv_type_board_subtitle.setText(accum && admin
						? "You see every member. Each member only sees their own card."
						: mine.subtitle);
			}
			if (tv_type_board_legend != null) tv_type_board_legend.setText(mine.legend);
			layout_type_board.removeAllViews();
			_addRecordSection(accum && admin ? "You" : null, mine, cell -> _onNodeCell(cell));
			if (accum && admin && others != null) {
				for (Object[] row : others) {
					@SuppressWarnings("unchecked") Map<String, Object> theirLine = (Map<String, Object>) row[1];
					com.harvanna.app.group.GroupTypeBoard.Board theirs =
							com.harvanna.app.group.GroupTypeBoard.fromNode("ACCUM", group, null, theirLine, "", now, zone);
					_addRecordSection(String.valueOf(row[0]), theirs, cell -> {
						if (getContext() != null) com.harvanna.app.group.GroupTypeBoardView.showDetail(getContext(), cell);
					});
				}
			}
			_ensureNodeActionButton(family);
		});
	}

	private void _addRecordSection(String label, com.harvanna.app.group.GroupTypeBoard.Board board,
			com.harvanna.app.group.GroupTypeBoardView.Click click) {
		if (getContext() == null || layout_type_board == null || board == null) return;
		if (label != null && !label.isEmpty()) {
			TextView t = new TextView(getContext());
			t.setText(label);
			t.setTextSize(12);
			t.setTypeface(null, android.graphics.Typeface.BOLD);
			t.setTextColor(com.harvanna.app.utils.UiColors.textPrimary());
			t.setPadding(0, (int) (10 * getResources().getDisplayMetrics().density), 0, 0);
			layout_type_board.addView(t);
		}
		LinearLayout host = new LinearLayout(getContext());
		host.setOrientation(LinearLayout.VERTICAL);
		layout_type_board.addView(host, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		com.harvanna.app.group.GroupTypeBoardView.fill(host, board, click);
	}

	private void _onNodeCell(com.harvanna.app.group.GroupTypeBoard.Cell cell) {
		if (cell != null && cell.payToday) {
			_openNodeActions();
			return;
		}
		if (getContext() != null) com.harvanna.app.group.GroupTypeBoardView.showDetail(getContext(), cell);
	}

	private void _ensureNodeActionButton(String family) {
		if (getContext() == null || panel_type_board == null) return;
		if (nodeActionButton == null) {
			nodeActionButton = new com.google.android.material.button.MaterialButton(getContext());
			nodeActionButton.setAllCaps(false);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
			panel_type_board.addView(nodeActionButton, lp);
		}
		String label = "ACCUM".equals(family) ? "Save or withdraw"
				: "FIXED".equals(family) ? "Deposit or request a payout" : "Pay and bond";
		nodeActionButton.setText(label);
		nodeActionButton.setOnClickListener(v -> _openNodeActions());
	}

	private void _openNodeActions() {
		if (getContext() == null || linkedNodeId == null || linkedNodeId.isEmpty()) return;
		Intent i = new Intent(getContext(), com.harvanna.app.group.NodeGroupActivity.class);
		i.putExtra("nodeGroupId", linkedNodeId);
		i.putExtra("firestoreGroupId", groupId);
		startActivity(i);
	}

	private static String _shortWallet(String wallet) {
		if (wallet == null || wallet.length() < 10) return wallet == null ? "Member" : wallet;
		return wallet.substring(0, 6) + "…" + wallet.substring(wallet.length() - 4);
	}

	private void _updatePayButtonState() {
		if (btn_pay_now == null) return;
		if (countdownTimer != null) { countdownTimer.cancel(); countdownTimer = null; }
		
		boolean typeKnown = groupType != null && !groupType.isEmpty();
		if (!typeKnown || contributionAmount <= 0) {
			btn_pay_now.setVisibility(View.GONE);
			btn_pay_now.setEnabled(false);
			return;
		}
		
		boolean fixed = groupType.toLowerCase(Locale.US).contains("fixed");
		boolean rotating = groupType.toLowerCase(Locale.US).contains("rotating");
		
		if (tv_my_payment_label != null) {
			tv_my_payment_label.setText(mySlot > 0 ? "MY PAYMENT · SLOT " + mySlot : "MY PAYMENT");
		}
		
		if (mySlot <= 0) {
			btn_pay_now.setVisibility(View.GONE);
			btn_pay_now.setEnabled(false);
			if (tv_my_payment_due != null) tv_my_payment_due.setText("Join the group to pay");
			return;
		}
		
		btn_pay_now.setVisibility(View.VISIBLE);
		
		if (fixed) {
			if (FixedPaymentEngine.canPayToday(fixedDaysPaid)) {
				btn_pay_now.setEnabled(true);
				// APPLIED FIX
				btn_pay_now.setText("Pay today · " + _formatUIBalance(contributionAmount));
				if (tv_my_payment_due != null) tv_my_payment_due.setText("Day " + FixedPaymentEngine.todayDayOfMonth() + " · " + monthKey);
			} else if (FixedPaymentEngine.isDayPaid(fixedDaysPaid, FixedPaymentEngine.todayDayOfMonth())) {
				btn_pay_now.setEnabled(false);
				btn_pay_now.setText("Today paid ✓");
				if (tv_my_payment_due != null) tv_my_payment_due.setText("You have paid today's contribution");
			} else {
				btn_pay_now.setEnabled(false);
				btn_pay_now.setText("No payment due");
			}
			return;
		}
		
		if (iHavePaidThisRound) {
			long cycleMs = _frequencyToMillis(frequency);
			long paidAt = getContext().getSharedPreferences(PREF_PAY, Activity.MODE_PRIVATE)
			.getLong(groupId + "_" + myUid + "_paidAt", System.currentTimeMillis());
			long remain = paidAt + cycleMs - System.currentTimeMillis();
			if (remain > 0) {
				btn_pay_now.setEnabled(false);
				if (tv_my_payment_due != null) {
					tv_my_payment_due.setText("You have paid for Round " + currentRound);
				}
				_startCountdown(remain);
				return;
			}
			iHavePaidThisRound = false;
		}
		
		if (rotating) {
			if (mySlot != currentPayerSlot) {
				btn_pay_now.setEnabled(false);
				btn_pay_now.setText("Waiting · Slot " + currentPayerSlot + " pays");
				if (tv_my_payment_due != null) {
					tv_my_payment_due.setText("Your slot " + mySlot + " · not this round");
				}
				return;
			}
			btn_pay_now.setEnabled(true);
			// APPLIED FIX
			btn_pay_now.setText("Pay slot " + mySlot + " · "
			+ _formatUIBalance(contributionAmount));
			if (tv_my_payment_due != null) {
				tv_my_payment_due.setText("Your turn · Round " + currentRound);
			}
			return;
		}
		
		btn_pay_now.setEnabled(true);
		// APPLIED FIX
		btn_pay_now.setText("Pay " + _formatUIBalance(contributionAmount));
		if (tv_my_payment_due != null) {
			tv_my_payment_due.setText("Pay anytime · " + frequency);
		}
	}
	
	private void _startCountdown(long millis) {
		countdownTimer = new CountDownTimer(millis, 1000) {
			@Override public void onTick(long ms) {
				long h = TimeUnit.MILLISECONDS.toHours(ms);
				long m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
				long s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
				if (btn_pay_now != null) {
					btn_pay_now.setText(String.format(Locale.US, "Next in %02d:%02d:%02d", h, m, s));
				}
			}
			@Override public void onFinish() {
				iHavePaidThisRound = false;
				_updatePayButtonState();
			}
		}.start();
	}
	
	private long _frequencyToMillis(String freq) {
		if (freq == null) return TimeUnit.DAYS.toMillis(7);
		String f = freq.toLowerCase(Locale.US);
		if (f.contains("daily")) return TimeUnit.DAYS.toMillis(1);
		if (f.contains("month")) return TimeUnit.DAYS.toMillis(30);
		if (f.contains("bi") || f.contains("fortnight")) return TimeUnit.DAYS.toMillis(14);
		return TimeUnit.DAYS.toMillis(7);
	}
	
	private void _onPayClicked() {
		if (btn_pay_now == null || !btn_pay_now.isEnabled()) return;
		if (contributionAmount <= 0) {
			SketchwareUtil.showMessage(getContext(), "Contribution not loaded");
			return;
		}
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) {
			SketchwareUtil.showMessage(getContext(), "Group wallet not ready");
			return;
		}
		SharedPreferences prefs = getContext().getSharedPreferences("wallet", Activity.MODE_PRIVATE);
		String from = prefs.getString("wallet_address", "").trim();
		if (from.isEmpty()) {
			prefs = getContext().getSharedPreferences("harv_wallet_prefs", Activity.MODE_PRIVATE);
			from = prefs.getString("active_wallet_address", "").trim();
		}
		try {
			String via = com.harvanna.app.wallet.WalletManager.getActiveWalletAddress(getContext());
			if (via != null && !via.isEmpty()) from = via;
		} catch (Throwable ignored) {}
		if (from.isEmpty() || !from.startsWith("HV0x")) {
			SketchwareUtil.showMessage(getContext(), ("Link your " + com.harvanna.app.CoinInfo.ticker() + " wallet first"));
			return;
		}
		final String fromAddr = from;
		
		com.harvanna.app.group.PaymentConfirmSheet.show(getContext(), "Confirm Contribution",
		contributionAmount, groupWalletAddress, null, "Calculated on send",
		() -> _submitPayment(fromAddr));
	}
	
	private void _submitPayment(String fromAddr) {
		try {
			if (!com.harvanna.app.group.PendingSpendGuard.tryReserve(getContext(), fromAddr, contributionAmount + 0.1)) {
				SketchwareUtil.showMessage(getContext(), "A transfer is still in progress. Wait a moment, then try again.");
				return;
			}
		} catch (Throwable ignored) {}
		
		btn_pay_now.setEnabled(false);
		btn_pay_now.setText("Paying…");
		final com.harvanna.app.group.TransactionProgressSheet progress =
		com.harvanna.app.group.TransactionProgressSheet.show(getActivity(), "Sending Payment", contributionAmount);
		final boolean isFixed = groupType != null && groupType.toLowerCase(Locale.US).contains("fixed");
		final int payDay = FixedPaymentEngine.todayDayOfMonth();
		
		progress.advanceTo(2);
		
		com.harvanna.app.wallet.PinPromptHelper.verifyPinThenProceed(getActivity(), () -> {
			com.harvanna.app.tx.TransactionRouter.routeTransaction(
			getActivity(), fromAddr, groupWalletAddress,
			BigDecimal.valueOf(contributionAmount), null,
			"Group contribution - Round " + currentRound,
			new com.harvanna.app.tx.TransactionRouter.RouterCallback() {
				@Override
				public void onSuccess(String txHash) {
					try {
						com.harvanna.app.group.PendingSpendGuard.markBroadcast(
						getContext(), fromAddr, contributionAmount + 0.1, txHash);
					} catch (Throwable ignored) {}
					
					_recordPayment(fromAddr, txHash, isFixed, payDay, (paymentDocId) -> {
						if (!isAdded() || getActivity() == null) return;
						getActivity().runOnUiThread(() -> {
							if (!isAdded()) return;
							progress.advanceTo(3);
							iHavePaidThisRound = true;
							
							if (getContext() != null) {
								getContext().getSharedPreferences(PREF_PAY, Activity.MODE_PRIVATE)
								.edit().putLong(groupId + "_" + myUid + "_paidAt", System.currentTimeMillis()).apply();
							}
							if (isFixed) {
								fixedDaysPaid = new ArrayList<>(fixedDaysPaid);
								if (!fixedDaysPaid.contains(payDay)) fixedDaysPaid.add(payDay);
							}
							
							com.harvanna.app.wallet.WalletNodeBalanceHelper.watchConfirmation(txHash, 30,
							() -> {
								if (!isAdded() || getActivity() == null || getContext() == null) return;
								getActivity().runOnUiThread(() -> {
									if (!isAdded()) return;
									try {
										com.harvanna.app.group.PendingSpendGuard.release(getContext(), fromAddr);
									} catch (Throwable ignored) {}
									progress.showConfirmed();
									
									com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
									.postSystemNotice(groupId, "Contribution received",
									// APPLIED FIX
									"A member paid " + _formatUIBalance(contributionAmount)
									+ (isFixed ? "" : " · Round " + currentRound));
									
									Map<String, Object> walletUpdate = new HashMap<>();
									walletUpdate.put("walletAddress", fromAddr);
									com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
									.updateMemberFields(groupId, myUid, walletUpdate,
									new com.harvanna.app.firestore.GroupFirestoreManager.SimpleCallback() {
										@Override public void onSuccess() {}
										@Override public void onError(String error) {}
									});
									
									if (paymentDocId != null) {
										GroupPaymentHelper.confirmPayment(
										getContext(), groupId, paymentDocId, txHash,
										new GroupPaymentHelper.Callback() {
											@Override public void onSuccess(String id) { if (isAdded()) _loadAll(); }
											@Override public void onError(String error) { if (isAdded()) _loadAll(); }
										});
									} else {
										if (isAdded()) _loadAll();
									}
								});
							},
							() -> {
								try {
									if (getContext() != null) com.harvanna.app.group.PendingSpendGuard.release(getContext(), fromAddr);
								} catch (Throwable ignored) {}
								if (isAdded() && getActivity() != null) {
									getActivity().runOnUiThread(() -> { if (isAdded()) _loadAll(); });
								}
							});
						});
					});
				}
				@Override
				public void onError(String code, String msg) {
					try {
						if (getContext() != null) com.harvanna.app.group.PendingSpendGuard.release(getContext(), fromAddr);
					} catch (Throwable ignored) {}
					if (!isAdded() || getActivity() == null) return;
					getActivity().runOnUiThread(() -> {
						if (!isAdded()) return;
						progress.showError("Pay failed: " + msg);
						_updatePayButtonState();
					});
				}
			});
		});
	}
	
	private void _recordPayment(String from, String txHash, boolean isFixed, int day, androidx.core.util.Consumer<String> done) {
		_resolveMemberName(name -> {
			if (isFixed) {
				GroupPaymentHelper.recordFixedDailyPending(
				getContext(), groupId, myUid, name,
				contributionAmount, day, monthKey,
				from, groupWalletAddress, txHash,
				fixedDaysPaid,
				new GroupPaymentHelper.Callback() {
					@Override public void onSuccess(String paymentDocId) {
						if (done != null) done.accept(paymentDocId);
					}
					@Override public void onError(String error) {
						if (done != null) done.accept(null);
					}
				});
			} else {
				GroupPaymentHelper.recordContributionPending(
				getContext(), groupId, myUid, mySlot,
				contributionAmount, from, groupWalletAddress, txHash,
				new GroupPaymentHelper.Callback() {
					@Override public void onSuccess(String paymentDocId) {
						Map<String, Object> update = new HashMap<>();
						update.put("memberName", name);
						com.harvanna.app.firestore.GroupFirestoreManager.getInstance(getContext())
						.updatePaymentStatus(groupId, paymentDocId, update, new com.harvanna.app.firestore.GroupFirestoreManager.SimpleCallback() {
							@Override public void onSuccess() {}
							@Override public void onError(String error) {}
						});
						if (done != null) done.accept(paymentDocId);
					}
					@Override public void onError(String error) {
						if (com.harvanna.app.offline.NetworkStatus.isConnectivityError(error)) {
							java.util.Map<String, Object> payload = new java.util.HashMap<>();
							payload.put("groupId", groupId);
							payload.put("memberUid", myUid);
							payload.put("memberName", name);
							payload.put("slot", mySlot);
							payload.put("amountHarv", contributionAmount);
							payload.put("from", from);
							payload.put("groupWallet", groupWalletAddress);
							payload.put("txRef", txHash);
							com.harvanna.app.offline.OfflineActionQueue.enqueue(getContext(), "CONTRIBUTION", payload);
						}
						if (done != null) done.accept(null);
					}
				});
			}
		});
	}
	
	private void _resolveMemberName(androidx.core.util.Consumer<String> done) {
		if (myUid == null || myUid.isEmpty()) {
			if (done != null) done.accept("Member");
			return;
		}
		com.harvanna.app.firestore.UserProfileManager.getInstance(getContext()).getProfile(myUid,
		new com.harvanna.app.firestore.UserProfileManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				Object fObj = data.get("first_name");
				Object lObj = data.get("last_name");
				String f = fObj != null ? fObj.toString() : "";
				String l = lObj != null ? lObj.toString() : "";
				String full = (f + " " + l).trim();
				if (done != null) done.accept(full.isEmpty() ? "Member" : full);
			}
			@Override
			public void onError(String error) {
				if (done != null) done.accept("Member");
			}
		});
	}
	
	private class PayAdapter extends RecyclerView.Adapter<PayAdapter.VH> {
		private final List<HashMap<String, Object>> data;
		PayAdapter(List<HashMap<String, Object>> data) { this.data = data; }
		@NonNull @Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			int lid = getResources().getIdentifier("payment_row_card", "layout", getContext().getPackageName());
			View v = lid != 0
			? LayoutInflater.from(parent.getContext()).inflate(lid, parent, false)
			: LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
			return new VH(v);
		}
		@Override public void onBindViewHolder(@NonNull VH h, int pos) {
			HashMap<String, Object> p = data.get(pos);
			String memberUid = str(p, "memberUid", "");
			String amountFallback = str(p, "memberName", "Member");
			double amt = num(p, "amountHarv");
			long ts = (long) num(p, "timestamp");
			String time = ts > 0 ? new SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(new Date(ts)) : "—";
			String st = str(p, "status", "PENDING");
			if (h.name != null) {
				com.harvanna.app.group.MemberNameResolver.resolveInto(getContext(), memberUid, amountFallback, h.name);
			}
			if (h.meta != null) {
				String day = str(p, "day", "");
				h.meta.setText(time + (day.isEmpty() ? "" : " · day " + day));
			}
			// APPLIED FIX
			if (h.amount != null) h.amount.setText(_formatUIBalance(amt));
			if (h.status != null) {
				boolean confirmed = "CONFIRMED".equalsIgnoreCase(st) || "PAID".equalsIgnoreCase(st);
				h.status.setText(confirmed ? "Confirmed" : "Pending");
				h.status.setBackgroundResource(confirmed
				? getResources().getIdentifier("bg_chip_confirmed", "drawable", getContext().getPackageName())
				: getResources().getIdentifier("bg_chip_pending", "drawable", getContext().getPackageName()));
			}
		}
		@Override public int getItemCount() { return data.size(); }
		class VH extends RecyclerView.ViewHolder {
			TextView name, meta, amount, status;
			VH(View v) {
				super(v);
				int n = v.getResources().getIdentifier("tv_pay_name", "id", v.getContext().getPackageName());
				int m = v.getResources().getIdentifier("tv_pay_meta", "id", v.getContext().getPackageName());
				int a = v.getResources().getIdentifier("tv_pay_amount", "id", v.getContext().getPackageName());
				int s = v.getResources().getIdentifier("tv_pay_status", "id", v.getContext().getPackageName());
				if (n != 0) name = v.findViewById(n);
				if (m != 0) meta = v.findViewById(m);
				if (a != 0) amount = v.findViewById(a);
				if (s != 0) status = v.findViewById(s);
				if (name == null) name = v.findViewById(android.R.id.text1);
				if (meta == null) meta = v.findViewById(android.R.id.text2);
			}
		}
	}
	
	private static String str(Map<String, Object> m, String k, String d) {
		Object v = m.get(k);
		if (v == null) return d;
		String s = v.toString().trim();
		return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
	}
	
	private static double num(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		if (v != null) try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
		return 0;
	}
	
	// NEW UI FORMATTER HELPER
	private String _formatUIBalance(double v) {
		try {
			java.text.DecimalFormat uiDf = new java.text.DecimalFormat("#,##0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			return uiDf.format(v);
		} catch (Exception e) {
			return String.valueOf(v);
		}
	}
	{
	}
	
}
