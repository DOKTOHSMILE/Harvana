package com.harvana.app;

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
import androidx.core.widget.NestedScrollView;
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

public class GroupLoansFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private NestedScrollView vscroll2;
	private LinearLayout linear53;
	private LinearLayout linear54;
	private MaterialCardView cardview14;
	private MaterialCardView card_apply_loan;
	private TextView textview20;
	private RecyclerView recycler_loans;
	private TextView tv_loans_empty;
	private MaterialCardView cardview11;
	private MaterialCardView cardview12;
	private MaterialCardView cardview13;
	private LinearLayout linear55;
	private TextView textview15;
	private TextView tv_total_loaned;
	private LinearLayout linear56;
	private TextView textview16;
	private TextView tv_total_repaid;
	private LinearLayout linear57;
	private TextView textview17;
	private TextView tv_total_outstanding;
	private LinearLayout linear58;
	private TextView textview18;
	private TextView tv_loan_terms;
	private TextView tv_wallet_available;
	private LinearLayout linear59;
	private TextView textview19;
	private TextView btn_apply_loan;
	
	private SharedPreferences groupsp;
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
		View _view = _inflater.inflate(R.layout.group_loans_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		vscroll2 = _view.findViewById(R.id.vscroll2);
		linear53 = _view.findViewById(R.id.linear53);
		linear54 = _view.findViewById(R.id.linear54);
		cardview14 = _view.findViewById(R.id.cardview14);
		card_apply_loan = _view.findViewById(R.id.card_apply_loan);
		textview20 = _view.findViewById(R.id.textview20);
		recycler_loans = _view.findViewById(R.id.recycler_loans);
		com.harvana.app.utils.Shimmer.hold(recycler_loans, com.harvana.app.utils.Shimmer.cards(recycler_loans.getContext(), 3), 4000, "GroupLoansFragmentActivity");
		tv_loans_empty = _view.findViewById(R.id.tv_loans_empty);
		cardview11 = _view.findViewById(R.id.cardview11);
		cardview12 = _view.findViewById(R.id.cardview12);
		cardview13 = _view.findViewById(R.id.cardview13);
		linear55 = _view.findViewById(R.id.linear55);
		textview15 = _view.findViewById(R.id.textview15);
		tv_total_loaned = _view.findViewById(R.id.tv_total_loaned);
		linear56 = _view.findViewById(R.id.linear56);
		textview16 = _view.findViewById(R.id.textview16);
		tv_total_repaid = _view.findViewById(R.id.tv_total_repaid);
		linear57 = _view.findViewById(R.id.linear57);
		textview17 = _view.findViewById(R.id.textview17);
		tv_total_outstanding = _view.findViewById(R.id.tv_total_outstanding);
		linear58 = _view.findViewById(R.id.linear58);
		textview18 = _view.findViewById(R.id.textview18);
		tv_loan_terms = _view.findViewById(R.id.tv_loan_terms);
		tv_wallet_available = _view.findViewById(R.id.tv_wallet_available);
		linear59 = _view.findViewById(R.id.linear59);
		textview19 = _view.findViewById(R.id.textview19);
		btn_apply_loan = _view.findViewById(R.id.btn_apply_loan);
		groupsp = getContext().getSharedPreferences("groupsp", Activity.MODE_PRIVATE);
		fbauth = FirebaseAuth.getInstance();
		
		card_apply_loan.setOnClickListener(_v -> _showApplyDialog());
		
		
		
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
		if (groupId.isEmpty()) return;
		_loadGroupTerms();
		_loadLoans();
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (!groupId.isEmpty()) {
			_loadGroupTerms();
			_loadLoans();
		}
		
	}
	public void _MORE() {
	}
	private String groupId = "";
	private double loanInterestRate = 0;
	private boolean isAdmin = false;
	private String groupWalletAddress = "";
	private String creatorId = "";
	private double groupWalletAvailable = 0;
	private String loanInterestPeriod = "MONTHLY";
	private String protocolId = "";
	// The group's own coin. Drives the suffix on every amount on this tab; without it we would always show HARV
	// even on a stable-coin group, which used to confuse the total loaned / total repaid cards.
	private String groupCoin = "";
	
	private ArrayList<HashMap<String, Object>> loanRows = new ArrayList<>();
	
	private void _loadGroupTerms() {
		if (getContext() == null) return;
		Map<String, Object> cachedGroup = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "group_" + groupId);
		if (cachedGroup != null && !cachedGroup.isEmpty()) {
			_applyGroupTerms(cachedGroup);
		}
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getGroup(groupId, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				com.harvana.app.offline.OfflineCacheManager.save(getContext(), "group_" + groupId, data);
				_applyGroupTerms(data);
			}
			@Override
			public void onError(String error) {}
		});
	}
	
	private void _applyGroupTerms(Map<String, Object> data) {
		Object rateObj = data.get("loanInterestRate");
		double storedRate = rateObj instanceof Number ? ((Number) rateObj).doubleValue() : 0;
		Object periodObj = data.get("loanInterestPeriod");
		String storedPeriod = periodObj != null ? periodObj.toString().trim() : "";
		// Interest is always shown and charged per day. A group on the network already stores a daily rate; an older app-only group
		// with a monthly or yearly rate has it spread over 30 or 365 days.
		loanInterestRate = com.harvana.app.group.LoanInterest.dailyPercent(storedRate, storedPeriod, com.harvana.app.group.GroupKinds.isOnChain(data));
		loanInterestPeriod = "DAILY";
		Object walletObj = data.get("groupWalletAddress");
		groupWalletAddress = walletObj != null ? walletObj.toString() : "";
		Object creatorObj = data.get("creatorId");
		creatorId = creatorObj != null ? creatorObj.toString() : "";
		String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
		? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
		isAdmin = !creatorId.isEmpty() && creatorId.equals(currentUid);
		protocolId = com.harvana.app.group.GroupKinds.nodeId(data);
		Object coinObj = data.get("asset");
		groupCoin = coinObj != null ? coinObj.toString().trim() : "";
		
		if (getActivity() == null) return;
		getActivity().runOnUiThread(() -> {
			if (tv_loan_terms != null) {
				String periodLabel = "DAILY".equals(loanInterestPeriod) ? "day"
				: "ANNUALLY".equals(loanInterestPeriod) ? "year" : "month";
				tv_loan_terms.setText("Interest " + loanInterestRate + "% per " + periodLabel + " · "
				+ _safe(data, "withdrawalRule", "See group rules"));
			}
			_loadWalletAvailable();
		});
	}
	
	private void _loadWalletAvailable() {
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) return;
		
		Map<String, Object> cachedBalance = com.harvana.app.offline.OfflineCacheManager.load(
		getContext(), "wallet_balance_" + groupWalletAddress);
		if (cachedBalance != null && cachedBalance.get("show") instanceof Number) {
			groupWalletAvailable = ((Number) cachedBalance.get("show")).doubleValue();
			if (getActivity() != null) {
				getActivity().runOnUiThread(() -> {
					if (tv_wallet_available != null) {
						// APPLIED FIX
						tv_wallet_available.setText("Group wallet available: "
						+ _formatUIBalance(groupWalletAvailable));
					}
				});
			}
		}
		
		com.harvana.app.sync.HarvanaNodeClient.getWallet(groupWalletAddress, new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject json) {
				groupWalletAvailable = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(json.optLong("balance", 0L));
				Map<String, Object> toCache = new java.util.HashMap<>();
				toCache.put("show", groupWalletAvailable);
				com.harvana.app.offline.OfflineCacheManager.save(getContext(), "wallet_balance_" + groupWalletAddress, toCache);
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (tv_wallet_available != null) {
						// APPLIED FIX
						tv_wallet_available.setText("Group wallet available: "
						+ _formatUIBalance(groupWalletAvailable));
					}
				});
			}
			@Override
			public void onError(String error) {}
		});
	}
	
	/** A node loan's status and amounts replace the copy. The copy is only the list of ids. */
	private void _refreshNodeLoansThenRender(List<Map<String, Object>> loans, List<Map<String, Object>> payments) {
		if (protocolId.isEmpty() || loans == null || loans.isEmpty()) {
			_renderLoansWithPayments(loans, payments);
			return;
		}
		List<Map<String, Object>> nodeLoans = new ArrayList<>();
		for (Map<String, Object> loan : loans) {
			String id = _safe(loan, "_docId", "");
			if ("node".equalsIgnoreCase(_safe(loan, "source", "")) || id.startsWith("GENLOAN-")) nodeLoans.add(loan);
		}
		if (nodeLoans.isEmpty()) {
			_renderLoansWithPayments(loans, payments);
			return;
		}
		java.util.concurrent.atomic.AtomicInteger left = new java.util.concurrent.atomic.AtomicInteger(nodeLoans.size());
		for (Map<String, Object> loan : nodeLoans) {
			String id = _safe(loan, "_docId", "");
			com.harvana.app.sync.NodeGroupClient.getLoan(protocolId, id, new com.harvana.app.sync.NodeGroupClient.Callback() {
				@Override public void onSuccess(org.json.JSONObject json) {
					loan.put("status", json.optString("status", _safe(loan, "status", "")));
					if (json.has("principalRaw")) loan.put("principalRaw", json.optLong("principalRaw"));
					if (json.has("repaidRaw")) loan.put("repaidRaw", json.optLong("repaidRaw"));
					loan.put("source", "node");
					if (left.decrementAndGet() == 0) _renderLoansWithPayments(loans, payments);
				}
				@Override public void onError(String error) {
					if (left.decrementAndGet() == 0) _renderLoansWithPayments(loans, payments);
				}
			});
		}
	}

	private void _loadLoans() {
		if (getContext() == null || groupId.isEmpty()) return;
		
		List<HashMap<String, Object>> cachedLoans = com.harvana.app.offline.OfflineCacheManager.loadList(
		getContext(), "loans_" + groupId);
		List<HashMap<String, Object>> cachedPayments = com.harvana.app.offline.OfflineCacheManager.loadList(
		getContext(), "loan_payments_" + groupId);
		
		if (cachedLoans != null && !cachedLoans.isEmpty()) {
			List<Map<String, Object>> loansAsMap = new ArrayList<>(cachedLoans);
			List<Map<String, Object>> paymentsAsMap = cachedPayments != null ? new ArrayList<>(cachedPayments) : null;
			_renderLoansWithPayments(loansAsMap, paymentsAsMap);
		}
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getLoans(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> loans) {
				com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
				.getPayments(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
					@Override
					public void onSuccess(List<Map<String, Object>> payments) {
						com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "loans_" + groupId, loans);
						com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "loan_payments_" + groupId, payments);
						_refreshNodeLoansThenRender(loans, payments);
					}
					@Override
					public void onError(String error) {
						com.harvana.app.offline.OfflineCacheManager.saveList(getContext(), "loans_" + groupId, loans);
						List<Map<String, Object>> fallbackPayments = cachedPayments != null ? new ArrayList<>(cachedPayments) : null;
						_renderLoansWithPayments(loans, fallbackPayments);
					}
				});
			}
			@Override
			public void onError(String error) {
				if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) return;
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() ->
				SketchwareUtil.showMessage(getContext(), "Failed to load loans: " + error));
			}
		});
	}
	
	private void _renderLoansWithPayments(List<Map<String, Object>> loans,
	List<Map<String, Object>> payments) {
		java.util.HashMap<String, Double> repaidByLoan = new java.util.HashMap<>();
		double totalLoaned = 0, totalRepaid = 0;
		if (payments != null) {
			for (Map<String, Object> p : payments) {
				String type = _safe(p, "type", "");
				String st = _safe(p, "status", "");
				boolean ok = "CONFIRMED".equalsIgnoreCase(st) || st.isEmpty();
				double amt = _num(p, "amountHarv");
				String loanId = _safe(p, "loanId", "");
				if ("LOAN_DISBURSE".equalsIgnoreCase(type) && ok) {
					totalLoaned += amt;
				}
				if ("LOAN_REPAY".equalsIgnoreCase(type) && ok) {
					totalRepaid += amt;
					if (!loanId.isEmpty()) {
						Double prev = repaidByLoan.get(loanId);
						repaidByLoan.put(loanId, (prev != null ? prev : 0) + amt);
					}
				}
			}
		}
		
		loanRows.clear();
		if (loans != null) {
			for (Map<String, Object> loan : loans) {
				HashMap<String, Object> row = new HashMap<>();
				row.putAll(loan);
				String loanId = _safe(loan, "_docId", "");
				boolean fromNode = "node".equalsIgnoreCase(_safe(loan, "source", "")) || loanId.startsWith("GENLOAN-");
				double fromDoc = _num(loan, "repaidAmountHarv");
				double fromLedger = !fromNode && repaidByLoan.containsKey(loanId) ? repaidByLoan.get(loanId) : 0;
				double repaidSoFar = fromNode && loan.get("repaidRaw") instanceof Number
						? com.harvana.app.wallet.BalanceFormatter.rawToHarv(((Number) loan.get("repaidRaw")).longValue())
						: Math.max(fromDoc, fromLedger);
				if (fromNode && loan.get("principalRaw") instanceof Number) {
					row.put("principalHarv", com.harvana.app.wallet.BalanceFormatter.rawToHarv(((Number) loan.get("principalRaw")).longValue()));
				}
				row.put("repaidAmountHarv", repaidSoFar);
				loanRows.add(row);
				
				if (payments == null || payments.isEmpty()) {
					String status = _safe(loan, "status", "");
					double principal = _num(loan, "principalHarv");
					if ("ACTIVE".equalsIgnoreCase(status) || "REPAID".equalsIgnoreCase(status)
					|| "APPROVED".equalsIgnoreCase(status)) {
						totalLoaned += principal;
					}
					if ("REPAID".equalsIgnoreCase(status)) {
						totalRepaid += principal;
					} else {
						totalRepaid += repaidSoFar;
					}
				}
			}
		}
		
		final double fLoaned = totalLoaned, fRepaid = totalRepaid;
		if (getActivity() == null) return;
		getActivity().runOnUiThread(() -> {
			// APPLIED FIX
			if (tv_total_loaned != null) tv_total_loaned.setText(_formatUIBalance(fLoaned));
			if (tv_total_repaid != null) tv_total_repaid.setText(_formatUIBalance(fRepaid));
			if (tv_total_outstanding != null) {
				tv_total_outstanding.setText(_formatUIBalance(Math.max(0, fLoaned - fRepaid)));
			}
			if (tv_loans_empty != null) tv_loans_empty.setVisibility(loanRows.isEmpty() ? View.VISIBLE : View.GONE);
			if (recycler_loans != null) {
				if (recycler_loans.getLayoutManager() == null) {
					recycler_loans.setLayoutManager(new LinearLayoutManager(getContext()));
				}
				recycler_loans.setAdapter(new LoanAdapter(loanRows));
			}
		});
	}
	
	private void _showApplyDialog() {
		if (getContext() == null) return;
		String myWallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext());
		if (myWallet == null || myWallet.isEmpty()) {
			SketchwareUtil.showMessage(getContext(), ("Link your " + com.harvana.app.CoinInfo.ticker() + " wallet first."));
			return;
		}
		
		int layoutId = getResources().getIdentifier(
		"bottom_sheet_loan_apply", "layout", getContext().getPackageName());
		if (layoutId == 0) {
			SketchwareUtil.showMessage(getContext(), "Loan application layout missing.");
			return;
		}
		
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet =
		new com.harvana.app.ui.AppSheet(getContext());
		View root = LayoutInflater.from(getContext()).inflate(layoutId, null);
		sheet.setContentView(root);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(sheet, root);
		
		TextView tvSubtitle = root.findViewById(getResources().getIdentifier(
		"tv_apply_subtitle", "id", getContext().getPackageName()));
		TextView tvInterestLine = root.findViewById(getResources().getIdentifier(
		"tv_apply_interest_line", "id", getContext().getPackageName()));
		TextView tvFeeEstimate = root.findViewById(getResources().getIdentifier(
		"tv_apply_fee_estimate", "id", getContext().getPackageName()));
		TextView tvPeriodsLabel = root.findViewById(getResources().getIdentifier(
		"textview7", "id", getContext().getPackageName()));
		final EditText amountInput = root.findViewById(getResources().getIdentifier(
		"et_apply_amount", "id", getContext().getPackageName()));
		final EditText noteInput = root.findViewById(getResources().getIdentifier(
		"et_apply_reason", "id", getContext().getPackageName()));
		final EditText periodsInput = root.findViewById(getResources().getIdentifier(
		"et_apply_periods", "id", getContext().getPackageName()));
		final TextView tvCalcEmpty = root.findViewById(getResources().getIdentifier(
		"tv_apply_calc_empty", "id", getContext().getPackageName()));
		final View calcRowsLayout = root.findViewById(getResources().getIdentifier(
		"layout_apply_calc_rows", "id", getContext().getPackageName()));
		final TextView tvCalcAfter = root.findViewById(getResources().getIdentifier(
		"tv_apply_calc_after", "id", getContext().getPackageName()));
		final TextView tvCalcPrincipal = root.findViewById(getResources().getIdentifier(
		"tv_apply_calc_principal", "id", getContext().getPackageName()));
		final TextView tvCalcInterest = root.findViewById(getResources().getIdentifier(
		"tv_apply_calc_interest", "id", getContext().getPackageName()));
		final TextView tvCalcTotal = root.findViewById(getResources().getIdentifier(
		"tv_apply_calc_total", "id", getContext().getPackageName()));
		final TextView tvDisbursementWallet = root.findViewById(getResources().getIdentifier(
		"tv_apply_disbursement_wallet", "id", getContext().getPackageName()));
		View btnCancel = root.findViewById(getResources().getIdentifier(
		"btn_apply_cancel", "id", getContext().getPackageName()));
		View btnReview = root.findViewById(getResources().getIdentifier(
		"btn_apply_review", "id", getContext().getPackageName()));
		
		if (tvDisbursementWallet != null) {
			tvDisbursementWallet.setText(_shortAddr(myWallet));
		}
		
		if (tvPeriodsLabel != null) {
			String unitPlural = "DAILY".equals(loanInterestPeriod) ? "DAYS"
			: "ANNUALLY".equals(loanInterestPeriod) ? "YEARS" : "MONTHS";
			tvPeriodsLabel.setText("I PLAN TO KEEP IT FOR (1 TO 30 DAYS)");
		}
		
		android.text.TextWatcher calcWatcher = new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
			@Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
			@Override
			public void afterTextChanged(android.text.Editable s) {
				double amt = com.harvana.app.wallet.BalanceFormatter.parseUserInput(
				amountInput != null ? amountInput.getText().toString() : "");
				int periods = 1;
				try {
					String raw = periodsInput != null ? periodsInput.getText().toString().trim() : "1";
					periods = raw.isEmpty() ? 1 : com.harvana.app.group.LoanInterest.clampDays(Integer.parseInt(raw));
				} catch (Exception ignored) {}
				
				if (amt <= 0) {
					if (tvCalcEmpty != null) tvCalcEmpty.setVisibility(View.VISIBLE);
					if (calcRowsLayout != null) calcRowsLayout.setVisibility(View.GONE);
					return;
				}
				
				double interest = com.harvana.app.group.LoanInterest.interest(amt, loanInterestRate, periods);
				double total = amt + interest;
				String periodUnit = "DAILY".equals(loanInterestPeriod) ? "day"
				: "ANNUALLY".equals(loanInterestPeriod) ? "year" : "month";
				String periodLabel = periodUnit + (periods > 1 ? "s" : "");
				
				if (tvCalcEmpty != null) tvCalcEmpty.setVisibility(View.GONE);
				if (calcRowsLayout != null) calcRowsLayout.setVisibility(View.VISIBLE);
				if (tvCalcAfter != null) tvCalcAfter.setText("If held " + periods + " " + periodLabel
				+ " at " + loanInterestRate + "% a day. Repay earlier and you pay interest only for the days held.");
				
				// APPLIED FIX
				if (tvCalcPrincipal != null) tvCalcPrincipal.setText(_formatUIBalance(amt));
				if (tvCalcInterest != null) tvCalcInterest.setText(_formatUIBalance(interest));
				if (tvCalcTotal != null) tvCalcTotal.setText(_formatUIBalance(total));
			}
		};
		if (amountInput != null) amountInput.addTextChangedListener(calcWatcher);
		if (periodsInput != null) periodsInput.addTextChangedListener(calcWatcher);
		
		if (tvSubtitle != null) {
			// APPLIED FIX
			tvSubtitle.setText(groupWalletAvailable > 0
			? "Group wallet has " + _formatUIBalance(groupWalletAvailable) + " available."
			: "Group wallet balance not loaded yet — you may not be able to apply.");
		}
		if (tvInterestLine != null) {
			tvInterestLine.setText(loanInterestRate > 0
			? String.format(java.util.Locale.US, "%.2f%% / %s", loanInterestRate, loanInterestPeriod.toLowerCase())
			: "0% (no interest)");
		}
		if (tvFeeEstimate != null) {
			String previewFrom = !groupWalletAddress.isEmpty() ? groupWalletAddress : myWallet;
			com.harvana.app.sync.HarvanaNodeClient.getFee(previewFrom, myWallet, 0L,
			new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
				@Override
				public void onSuccess(org.json.JSONObject feeJson) {
					if (getActivity() == null) return;
					long rawFee = feeJson.optLong("estimatedGas", 1000L);
					double feeHarv = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(rawFee);
					// APPLIED FIX
					getActivity().runOnUiThread(() -> tvFeeEstimate.setText(_formatUIBalance(feeHarv)));
				}
				@Override
				public void onError(String error) {}
			});
		}
		
		if (btnCancel != null) btnCancel.setOnClickListener(v -> sheet.dismiss());
		
		if (btnReview != null) {
			btnReview.setOnClickListener(v -> {
				double amount = com.harvana.app.wallet.BalanceFormatter.parseUserInput(
				amountInput != null ? amountInput.getText().toString() : "");
				if (amount <= 0) {
					SketchwareUtil.showMessage(getContext(), "Enter a valid amount.");
					return;
				}
				if (amount > groupWalletAvailable) {
					// APPLIED FIX
					SketchwareUtil.showMessage(getContext(), "Can't request more than the group wallet holds ("
					+ _formatUIBalance(groupWalletAvailable) + " available).");
					return;
				}
				String note = noteInput != null ? noteInput.getText().toString().trim() : "";
				int days = 1;
				try {
					String rawDays = periodsInput != null ? periodsInput.getText().toString().trim() : "1";
					days = rawDays.isEmpty() ? 1 : Integer.parseInt(rawDays);
				} catch (Exception ignored) {}
				if (days < com.harvana.app.group.LoanInterest.MIN_DAYS || days > com.harvana.app.group.LoanInterest.MAX_DAYS) {
					SketchwareUtil.showMessage(getContext(), "Choose 1 to 30 days.");
					return;
				}
				final int termDays = days;
				sheet.dismiss();
				com.harvana.app.group.PaymentConfirmSheet.show(getContext(), "Confirm Loan Application",
				amount, groupWalletAddress, note.isEmpty() ? null : note, "No fee to apply",
				() -> _submitApplication(myWallet, amount, note, termDays));
			});
		}
		
		sheet.show();
	}
	
	private void _submitApplication(String myWallet, double amount, String note, int termDays) {
		com.harvana.app.group.GroupLoanManager.applyForLoan(getContext(), groupId, myWallet,
		amount, loanInterestRate, loanInterestPeriod, note, termDays, new com.harvana.app.group.GroupLoanManager.Callback() {
			@Override
			public void onSuccess(String message) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					SketchwareUtil.showMessage(getContext(), message);
					_loadLoans();
				});
			}
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> SketchwareUtil.showMessage(getContext(), error));
			}
		});
	}
	
	private void _approveLoan(String loanId, String borrowerWallet, double principal) {
		if (getContext() == null || groupWalletAddress.isEmpty()) return;
		
		long rawAmount = com.harvana.app.sync.HarvanaNodeClient.toRawUnits(principal);
		com.harvana.app.sync.HarvanaNodeClient.getFee(groupWalletAddress, borrowerWallet, rawAmount,
		new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(org.json.JSONObject feeJson) {
				if (getActivity() == null) return;
				long rawFee = feeJson.optLong("estimatedGas", 1000L);
				double feeHarv = com.harvana.app.sync.HarvanaNodeClient.toHarvDisplay(rawFee);
				getActivity().runOnUiThread(() -> _showApproveLoanSheet(loanId, borrowerWallet, principal, feeHarv));
			}
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> SketchwareUtil.showMessage(getContext(),
				"Could not fetch tFare from node: " + error));
			}
		});
	}
	
	private void _showApproveLoanSheet(String loanId, String borrowerWallet, double principal, double feeHarv) {
		if (getContext() == null) return;
		double netToBorrower = principal - feeHarv;
		if (netToBorrower <= 0) {
			SketchwareUtil.showMessage(getContext(), "Loan amount is too small to cover the tFare.");
			return;
		}
		
		String feeDisplay = com.harvana.app.wallet.BalanceFormatter.fee(feeHarv);
		java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
		rows.add(new String[]{"For", "Disburse this approved loan from the group wallet"});
		rows.add(new String[]{"To", borrowerWallet});
		rows.add(new String[]{"Borrower receives", _formatUIBalance(netToBorrower)});
		rows.add(new String[]{"tFare", feeDisplay + ", taken from the principal"});
		rows.add(new String[]{"Group wallet debit", _formatUIBalance(principal)});
		com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(getActivity(),
		"Approve and disburse", _formatUIBalance(principal), rows,
		() -> _executeLoanDisbursement(loanId, borrowerWallet, principal, feeHarv));
	}
	
	private void _executeLoanDisbursement(String loanId, String borrowerWallet, double principal, double feeHarv) {
		com.harvana.app.group.GroupPayoutManager.GroupWalletTransfer transferExecutor =
		(from, to, amountHarv, cb) -> {
			double netAmount = amountHarv - feeHarv;
			if (netAmount <= 0) {
				cb.onError("Loan amount too small to cover the tFare.");
				return;
			}
			String escrowKey = com.harvana.app.wallet.WalletManager.loadPrivateKey(getActivity(), from);
			if (escrowKey == null) {
				cb.onError("This circle's wallet is held on the network, not on this phone. Accepting can't sign that wallet from here, so the loan was not sent. Reject still works because it moves no money.");
				return;
			}
			com.harvana.app.tx.TransactionRouter.routeTransaction(
			getActivity(), from, to, java.math.BigDecimal.valueOf(netAmount), null,
			"Loan disbursement (tFare deducted from principal)",
			new com.harvana.app.tx.TransactionRouter.RouterCallback() {
				@Override public void onSuccess(String txHash) { cb.onSuccess(txHash); }
				@Override public void onError(String code, String msg) { cb.onError(msg); }
			});
		};
		
		com.harvana.app.group.GroupLoanManager.approveAndDisburse(getContext(), groupId, loanId,
		groupWalletAddress, borrowerWallet, principal, transferExecutor,
		new com.harvana.app.group.GroupLoanManager.Callback() {
			@Override
			public void onSuccess(String message) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					SketchwareUtil.showMessage(getContext(), message);
					_loadLoans();
					_loadWalletAvailable();
				});
			}
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> SketchwareUtil.showMessage(getContext(), error));
			}
		});
	}
	
	private void _rejectLoan(String loanId) {
		if (getContext() == null) return;
		com.harvana.app.group.GroupLoanManager.rejectLoan(getContext(), groupId, loanId,
		new com.harvana.app.group.GroupLoanManager.Callback() {
			@Override
			public void onSuccess(String message) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					SketchwareUtil.showMessage(getContext(), message);
					_loadLoans();
				});
			}
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> SketchwareUtil.showMessage(getContext(), error));
			}
		});
	}
	
	private void _repayLoan(String loanId, String borrowerWallet,
	double principal, double interestRate, String interestPeriod,
	long disbursedAt, double remainingDue, double alreadyRepaid) {
		if (getContext() == null || getActivity() == null) return;
		if (groupWalletAddress == null || !groupWalletAddress.startsWith("HV0x")) {
			SketchwareUtil.showMessage(getContext(), "Group wallet not loaded.");
			return;
		}
		String myWallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext());
		if (myWallet == null || !myWallet.startsWith("HV0x")) {
			SketchwareUtil.showMessage(getContext(), ("Link your " + com.harvana.app.CoinInfo.ticker() + " wallet first."));
			return;
		}
		if (remainingDue <= 0.0001) {
			_executeLoanRepayment(loanId, myWallet, 0.0001, true);
			return;
		}
		_showLoanAmountSheet(loanId, myWallet, remainingDue, alreadyRepaid, false);
	}
	
	private void _showLoanAmountSheet(String loanId, String fromWallet,
	double remainingDue, double alreadyRepaid,
	boolean partialMode) {
		if (getContext() == null || getActivity() == null) return;
		
		int layoutId = getResources().getIdentifier(
		"bottom_sheet_payment_confirm", "layout", getContext().getPackageName());
		if (layoutId == 0) {
			SketchwareUtil.showMessage(getContext(), "Payment confirm layout missing.");
			return;
		}
		
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet =
		new com.harvana.app.ui.AppSheet(getContext());
		View root = LayoutInflater.from(getContext()).inflate(layoutId, null);
		sheet.setContentView(root);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(sheet, root);
		
		TextView tvTitle = root.findViewById(getResources().getIdentifier(
		"tv_confirm_title", "id", getContext().getPackageName()));
		TextView tvAmount = root.findViewById(getResources().getIdentifier(
		"tv_confirm_amount", "id", getContext().getPackageName()));
		TextView tvRecipient = root.findViewById(getResources().getIdentifier(
		"tv_confirm_recipient", "id", getContext().getPackageName()));
		TextView tvFee = root.findViewById(getResources().getIdentifier(
		"tv_confirm_fee", "id", getContext().getPackageName()));
		TextView tvMemo = root.findViewById(getResources().getIdentifier(
		"tv_confirm_memo", "id", getContext().getPackageName()));
		View rowMemo = root.findViewById(getResources().getIdentifier(
		"row_confirm_memo", "id", getContext().getPackageName()));
		View btnCancel = root.findViewById(getResources().getIdentifier(
		"btn_confirm_cancel", "id", getContext().getPackageName()));
		View btnSend = root.findViewById(getResources().getIdentifier(
		"btn_confirm_send", "id", getContext().getPackageName()));
		
		if (tvTitle != null) {
			tvTitle.setText(partialMode ? "Partial repayment" : "Repay loan");
		}
		if (tvRecipient != null) {
			String addr = groupWalletAddress;
			tvRecipient.setText(addr.length() > 12
			? addr.substring(0, 8) + "…" + addr.substring(addr.length() - 4) : addr);
		}
		if (tvFee != null) tvFee.setText("Calculated on send");
		
		// APPLIED FIX
		String memoText = alreadyRepaid > 0
		? "Already paid: " + _formatUIBalance(alreadyRepaid)
		: "Repayment to group wallet";
		if (rowMemo != null) rowMemo.setVisibility(View.VISIBLE);
		if (tvMemo != null) tvMemo.setText(memoText);
		
		final EditText amountInput;
		if (partialMode) {
			amountInput = new com.google.android.material.textfield.TextInputEditText(getContext());
			amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
			| android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
			// APPLIED FIX
			amountInput.setHint("Amount (max "
			+ _formatUIBalance(remainingDue) + ")");
			amountInput.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
			amountInput.setTextSize(20);
			amountInput.setBackground(null);
			if (tvAmount != null) {
				amountInput.setTextColor(tvAmount.getCurrentTextColor());
				if (tvAmount.getParent() instanceof ViewGroup) {
					ViewGroup parent = (ViewGroup) tvAmount.getParent();
					int idx = parent.indexOfChild(tvAmount);
					tvAmount.setVisibility(View.GONE);
					parent.addView(amountInput, idx + 1,
					new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
				}
			}
		} else {
			amountInput = null;
			if (tvAmount != null) {
				// APPLIED FIX
				tvAmount.setText(_formatUIBalance(remainingDue));
			}
		}
		
		if (btnCancel != null) {
			if (btnCancel instanceof TextView) {
				((TextView) btnCancel).setText(partialMode ? "Back" : "Partial…");
			}
			btnCancel.setOnClickListener(v -> {
				sheet.dismiss();
				if (partialMode) {
					_showLoanAmountSheet(loanId, fromWallet, remainingDue, alreadyRepaid, false);
				} else {
					_showLoanAmountSheet(loanId, fromWallet, remainingDue, alreadyRepaid, true);
				}
			});
		}
		
		if (btnSend != null) {
			if (btnSend instanceof TextView) {
				((TextView) btnSend).setText(partialMode ? "Pay amount" : "Repay & close");
			}
			btnSend.setOnClickListener(v -> {
				double amount;
				boolean fully;
				if (partialMode) {
					String raw = amountInput != null ? amountInput.getText().toString().trim() : "";
					try {
						amount = Double.parseDouble(raw);
					} catch (Exception e) {
						SketchwareUtil.showMessage(getContext(), "Enter a valid amount.");
						return;
					}
					if (amount <= 0) {
						SketchwareUtil.showMessage(getContext(), "Amount must be greater than zero.");
						return;
					}
					if (amount > remainingDue + 0.0001) {
						// APPLIED FIX
						SketchwareUtil.showMessage(getContext(), "Amount can't exceed what's owed ("
						+ _formatUIBalance(remainingDue) + ").");
						return;
					}
					fully = amount >= remainingDue - 0.0001;
				} else {
					amount = remainingDue;
					fully = true;
				}
				sheet.dismiss();
				final double payAmount = amount;
				final boolean payFully = fully;
				java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
				rows.add(new String[]{"For", payFully
						? "Pays the rest of this loan and closes it"
						: "Pays part of what is still owed on this loan"});
				rows.add(new String[]{"To", "Group wallet"});
				rows.add(new String[]{"Wallet", groupWalletAddress});
				rows.add(new String[]{"From", fromWallet});
				if (alreadyRepaid > 0) rows.add(new String[]{"Already paid", _formatUIBalance(alreadyRepaid)});
				rows.add(new String[]{"Still owed before this", _formatUIBalance(remainingDue)});
				rows.add(new String[]{"tFare", "Taken from your wallet when this is sent"});
				com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(getActivity(),
				payFully ? "Repay and close this loan" : "Part-pay this loan",
				_formatUIBalance(payAmount), rows,
				() -> _executeLoanRepayment(loanId, fromWallet, payAmount, payFully));
			});
		}
		
		sheet.show();
	}
	
	private void _executeLoanRepayment(String loanId, String fromWallet,
	double amountHarv, boolean markFullyRepaid) {
		if (getActivity() == null) return;
		if (!protocolId.isEmpty()) {
			if (loanId == null || !loanId.startsWith("GENLOAN-")) {
				SketchwareUtil.showMessage(getContext(), "This loan is only in the app. The network does not have it, so it cannot be repaid here.");
				return;
			}
			final com.harvana.app.group.TransactionProgressSheet progress =
					com.harvana.app.group.TransactionProgressSheet.show(getActivity(), "Repaying on the network", amountHarv);
			progress.advanceTo(2);
			long raw = com.harvana.app.wallet.BalanceFormatter.harvToRaw(amountHarv);
			com.harvana.app.sync.NodeGroupClient.repayLoan(getContext(), fromWallet, protocolId, loanId, raw,
					new com.harvana.app.sync.NodeGroupClient.Callback() {
						@Override public void onSuccess(org.json.JSONObject json) {
							if (getActivity() == null) return;
							getActivity().runOnUiThread(() -> {
								progress.showConfirmed();
								SketchwareUtil.showMessage(getContext(), "The network accepted the repayment.");
								_loadLoans();
							});
						}
						@Override public void onError(String error) {
							if (getActivity() == null) return;
							getActivity().runOnUiThread(() -> progress.showError(error));
						}
					});
			return;
		}
		final com.harvana.app.group.TransactionProgressSheet progress =
		com.harvana.app.group.TransactionProgressSheet.show(
		getActivity(), markFullyRepaid ? "Repaying loan" : "Partial repayment", amountHarv);
		progress.advanceTo(2);
		
		try {
				com.harvana.app.tx.TransactionRouter.routeTransaction(
				getActivity(),
				fromWallet,
				groupWalletAddress,
				java.math.BigDecimal.valueOf(amountHarv),
				null,
				"Loan repayment",
				new com.harvana.app.tx.TransactionRouter.RouterCallback() {
					@Override
					public void onSuccess(String txHash) {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() -> {
							progress.advanceTo(3);
							com.harvana.app.group.GroupLoanManager.recordRepayment(
							getContext(), groupId, loanId,
							fromWallet, groupWalletAddress, amountHarv, txHash,
							markFullyRepaid,
							new com.harvana.app.group.GroupLoanManager.Callback() {
								@Override
								public void onSuccess(String message) {
									if (getActivity() == null) return;
									getActivity().runOnUiThread(() -> {
										progress.showConfirmed();
										SketchwareUtil.showMessage(getContext(), message);
										_loadLoans();
									});
								}
								
								@Override
								public void onError(String error) {
									if (getActivity() == null) return;
									if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)) {
										java.util.Map<String, Object> payload = new java.util.HashMap<>();
										payload.put("groupId", groupId);
										payload.put("loanId", loanId);
										payload.put("fromWallet", fromWallet);
										payload.put("groupWallet", groupWalletAddress);
										payload.put("amountHarv", amountHarv);
										payload.put("txRef", txHash);
										payload.put("markFullyRepaid", markFullyRepaid);
										com.harvana.app.offline.OfflineActionQueue.enqueue(getContext(), "LOAN_REPAY", payload);
									}
									getActivity().runOnUiThread(() -> {
										progress.showConfirmed();
										SketchwareUtil.showMessage(getContext(),
										"Transfer sent. Ledger: " + error);
										_loadLoans();
									});
								}
							});
						});
					}
					
					@Override
					public void onError(String code, String msg) {
						if (getActivity() == null) return;
						getActivity().runOnUiThread(() ->
						progress.showError("Repayment failed: " + msg));
					}
				});
			} catch (Exception e) {
				progress.showError(e.getMessage() != null ? e.getMessage() : "Repayment failed");
			}
	}
	
	private String _safe(Map<String, Object> d, String k, String fb) {
		Object v = d.get(k);
		return v != null ? v.toString() : fb;
	}
	
	private double _num(Map<String, Object> d, String k) {
		Object v = d.get(k);
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
	}
	
	private String _shortAddr(String a) {
		if (a == null || a.length() < 12) return a != null ? a : "";
		return a.substring(0, 8) + "…" + a.substring(a.length() - 4);
	}
	
	private class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.VH> {
		private final ArrayList<HashMap<String, Object>> data;
		LoanAdapter(ArrayList<HashMap<String, Object>> data) { this.data = data; }
		
		@NonNull @Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.loan_row_card, parent, false);
			return new VH(v);
		}
		
		@Override
		public void onBindViewHolder(@NonNull VH h, int pos) {
			HashMap<String, Object> loan = data.get(pos);
			String status = _safe(loan, "status", "PENDING");
			double principal = _num(loan, "principalHarv");
			if (principal == 0) principal = _num(loan, "amountHarv");
			final String loanId = _safe(loan, "_docId", "");
			final String borrowerWallet = _safe(loan, "borrowerWallet", "");
			
			if (h.status != null) h.status.setText(status);
			boolean fromNode = "node".equalsIgnoreCase(_safe(loan, "source", "")) || loanId.startsWith("GENLOAN-");
			if (fromNode && "PENDING_APPROVAL".equalsIgnoreCase(status) && h.status != null) {
				h.status.setText("Waiting for guardians");
			}
			// APPLIED FIX
			if (h.amount != null) h.amount.setText(_formatUIBalance(principal));
			double repaidSoFar = _num(loan, "repaidAmountHarv");
			if (h.meta != null) {
				long disbursedAt = (long) _num(loan, "disbursedAt");
				String interestPeriod = _safe(loan, "interestPeriod", "MONTHLY");
				double interestRate = _num(loan, "interestRate");
				if (disbursedAt > 0 && !fromNode) {
					double accrued = com.harvana.app.group.GroupLoanManager.computeAccruedTotalDue(
					principal, interestRate, interestPeriod, disbursedAt);
					double remaining = Math.max(0, accrued - repaidSoFar);
					if (repaidSoFar > 0) {
						// APPLIED FIX
						h.meta.setText("Due: " + _formatUIBalance(remaining)
						+ " · paid " + _formatUIBalance(repaidSoFar));
					} else {
						// APPLIED FIX
						h.meta.setText("Total due: " + _formatUIBalance(accrued) + " (accruing)");
					}
				} else if (fromNode && "PENDING_APPROVAL".equalsIgnoreCase(status)) {
					h.meta.setText("Released only after enough guardians sign. A group admin cannot pay this out.");
				} else if (fromNode) {
					h.meta.setText("Paid " + _formatUIBalance(repaidSoFar) + " of " + _formatUIBalance(principal));
				} else {
					h.meta.setText(_shortAddr(borrowerWallet));
				}
			}
			
			String borrowerUid = _safe(loan, "borrowerUid", "");
			if (h.borrower != null) {
				h.borrower.setTag(borrowerUid);
				com.harvana.app.utils.Shimmer.loading(h.borrower);
			}
			
			String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
			? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
			String myWallet = "";
			try { myWallet = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext()); } catch (Throwable ignored) {}
			boolean isMyActiveLoan = fromNode
					? (("ACTIVE".equalsIgnoreCase(status) || "LATE".equalsIgnoreCase(status))
						&& myWallet != null && myWallet.equalsIgnoreCase(borrowerWallet))
					: ("ACTIVE".equalsIgnoreCase(status) && !borrowerUid.isEmpty() && borrowerUid.equals(currentUid));
			boolean showAdminActions = !fromNode && isAdmin && "PENDING".equalsIgnoreCase(status);
			
			if (h.adminRow != null) {
				h.adminRow.setVisibility((showAdminActions || isMyActiveLoan) ? View.VISIBLE : View.GONE);
			}
			final double fPrincipal = principal;
			final long fDisbursedAt = (long) _num(loan, "disbursedAt");
			final double fInterestRate = _num(loan, "interestRate");
			final String fInterestPeriod = _safe(loan, "interestPeriod", "MONTHLY");
			
			if (showAdminActions) {
				if (h.approve != null) {
					h.approve.setVisibility(View.VISIBLE);
					h.approve.setText("Approve");
					h.approve.setOnClickListener(v -> _approveLoan(loanId, borrowerWallet, fPrincipal));
				}
				if (h.reject != null) {
					h.reject.setVisibility(View.VISIBLE);
					h.reject.setText("Reject");
					h.reject.setOnClickListener(v -> _rejectLoan(loanId));
				}
			} else if (isMyActiveLoan) {
				double remaining;
				if (fromNode) {
					remaining = Math.max(0, principal - repaidSoFar);
				} else {
					double accrued = com.harvana.app.group.GroupLoanManager.computeAccruedTotalDue(
					fPrincipal, fInterestRate, fInterestPeriod, fDisbursedAt);
					repaidSoFar = _num(loan, "repaidAmountHarv");
					remaining = Math.max(0, accrued - repaidSoFar);
				}
				if (h.approve != null) {
					h.approve.setVisibility(View.VISIBLE);
					h.approve.setText(remaining <= 0.0001 ? "Close" : "Repay");
					final double fRemaining = remaining;
					final double fRepaid = repaidSoFar;
					h.approve.setOnClickListener(v -> _repayLoan(loanId, borrowerWallet,
					fPrincipal, fInterestRate, fInterestPeriod, fDisbursedAt,
					fRemaining, fRepaid));
				}
				if (h.reject != null) {
					h.reject.setVisibility(View.GONE);
				}
			} else {
				if (h.approve != null) h.approve.setVisibility(View.GONE);
				if (h.reject != null) h.reject.setVisibility(View.GONE);
			}
			
			if (!borrowerUid.isEmpty() && h.borrower != null) {
				com.harvana.app.group.MemberNameResolver.resolve(getContext(), borrowerUid, "Member", full -> {
					if (h.borrower != null && borrowerUid.equals(h.borrower.getTag())) {
						h.borrower.setText(full);
					}
				});
			}
			
			h.itemView.setOnClickListener(v -> _showLoanDetailSheet(loan, loanId));
		}
		
		@Override public int getItemCount() { return data.size(); }
		
		class VH extends RecyclerView.ViewHolder {
			TextView borrower, status, amount, meta, approve, reject;
			View adminRow;
			VH(View v) {
				super(v);
				borrower = v.findViewById(R.id.tv_loan_borrower);
				status = v.findViewById(R.id.tv_loan_status);
				amount = v.findViewById(R.id.tv_loan_amount);
				meta = v.findViewById(R.id.tv_loan_meta);
				adminRow = v.findViewById(R.id.row_loan_admin_actions);
				approve = v.findViewById(R.id.btn_loan_approve);
				reject = v.findViewById(R.id.btn_loan_reject);
			}
		}
	}
	
	private void _showLoanDetailSheet(HashMap<String, Object> loan, String loanId) {
		if (getContext() == null) return;
		
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet =
		new com.harvana.app.ui.AppSheet(getContext());
		View root = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_loan_detail, null);
		sheet.setContentView(root);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(sheet, root);
		
		TextView tvStatus = root.findViewById(R.id.tv_loan_detail_status);
		TextView tvBorrower = root.findViewById(R.id.tv_loan_detail_borrower);
		TextView tvId = root.findViewById(R.id.tv_loan_detail_id);
		TextView tvDisbursed = root.findViewById(R.id.tv_loan_detail_disbursed);
		TextView tvPrincipal = root.findViewById(R.id.tv_loan_detail_principal);
		TextView tvRate = root.findViewById(R.id.tv_loan_detail_rate);
		TextView tvInterestAccrued = root.findViewById(R.id.tv_loan_detail_interest_accrued);
		TextView tvRepaid = root.findViewById(R.id.tv_loan_detail_repaid);
		TextView tvTotalDue = root.findViewById(R.id.tv_loan_detail_total_due);
		LinearLayout historyLayout = root.findViewById(R.id.layout_loan_repayment_history);
		TextView tvNoRepayments = root.findViewById(R.id.tv_loan_no_repayments);
		View btnClose = root.findViewById(R.id.btn_loan_detail_close);
		if (btnClose != null) btnClose.setOnClickListener(v -> sheet.dismiss());
		
		String status = _safe(loan, "status", "PENDING");
		double principal = _num(loan, "principalHarv");
		if (principal == 0) principal = _num(loan, "amountHarv");
		double interestRate = _num(loan, "interestRate");
		String interestPeriod = _safe(loan, "interestPeriod", "MONTHLY");
		long disbursedAt = (long) _num(loan, "disbursedAt");
		double repaidSoFar = _num(loan, "repaidAmountHarv");
		String borrowerUid = _safe(loan, "borrowerUid", "");
		
		double accruedDue = disbursedAt > 0
		? com.harvana.app.group.GroupLoanManager.computeAccruedTotalDue(principal, interestRate, interestPeriod, disbursedAt)
		: principal;
		double interestAccrued = Math.max(0, accruedDue - principal);
		double totalDue = Math.max(0, accruedDue - repaidSoFar);
		
		if (tvStatus != null) tvStatus.setText(status);
		if (tvId != null) tvId.setText(loanId.isEmpty() ? "—" : loanId);
		if (tvDisbursed != null) {
			tvDisbursed.setText(disbursedAt > 0
			? new java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(new java.util.Date(disbursedAt))
			: "Not yet disbursed");
		}
		// APPLIED FIX
		if (tvPrincipal != null) tvPrincipal.setText(_formatUIBalance(principal));
		if (tvRate != null) {
			String periodLabel = "DAILY".equals(interestPeriod) ? "day" : "ANNUALLY".equals(interestPeriod) ? "year" : "month";
			tvRate.setText(interestRate + "% per " + periodLabel);
		}
		// APPLIED FIX
		if (tvInterestAccrued != null) tvInterestAccrued.setText(_formatUIBalance(interestAccrued));
		if (tvRepaid != null) tvRepaid.setText(_formatUIBalance(repaidSoFar));
		if (tvTotalDue != null) tvTotalDue.setText(_formatUIBalance(totalDue));
		
		if (tvBorrower != null) {
			com.harvana.app.utils.Shimmer.loading(tvBorrower);
			if (!borrowerUid.isEmpty()) {
				com.harvana.app.group.MemberNameResolver.resolve(getContext(), borrowerUid, "Member", name -> {
					if (tvBorrower != null) tvBorrower.setText(name);
				});
			} else {
				tvBorrower.setText("Member");
			}
		}
		
		// Immediately show cached history
		List<HashMap<String, Object>> cachedPayments = com.harvana.app.offline.OfflineCacheManager.loadList(getContext(), "loan_payments_" + groupId);
		if (cachedPayments != null) {
			List<Map<String, Object>> matching = new ArrayList<>();
			for (Map<String, Object> p : cachedPayments) {
				if (!"LOAN_REPAY".equalsIgnoreCase(_safe(p, "type", ""))) continue;
				if (!loanId.equals(_safe(p, "loanId", ""))) continue;
				matching.add(p);
			}
			Collections.sort(matching, (a, b) -> {
				long ta = a.get("timestamp") instanceof Number ? ((Number) a.get("timestamp")).longValue() : 0L;
				long tb = b.get("timestamp") instanceof Number ? ((Number) b.get("timestamp")).longValue() : 0L;
				return Long.compare(tb, ta);
			});
			_renderRepaymentHistory(historyLayout, tvNoRepayments, matching);
		}
		
		com.harvana.app.firestore.GroupFirestoreManager.getInstance(getContext())
		.getPayments(groupId, new com.harvana.app.firestore.GroupFirestoreManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> payments) {
				List<Map<String, Object>> matching = new ArrayList<>();
				if (payments != null) {
					for (Map<String, Object> p : payments) {
						if (!"LOAN_REPAY".equalsIgnoreCase(_safe(p, "type", ""))) continue;
						if (!loanId.equals(_safe(p, "loanId", ""))) continue;
						matching.add(p);
					}
					Collections.sort(matching, (a, b) -> {
						long ta = a.get("timestamp") instanceof Number ? ((Number) a.get("timestamp")).longValue() : 0L;
						long tb = b.get("timestamp") instanceof Number ? ((Number) b.get("timestamp")).longValue() : 0L;
						return Long.compare(tb, ta);
					});
				}
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> _renderRepaymentHistory(historyLayout, tvNoRepayments, matching));
			}
			
			@Override
			public void onError(String error) {
				if (getActivity() == null) return;
				getActivity().runOnUiThread(() -> {
					if (tvNoRepayments != null && (cachedPayments == null || cachedPayments.isEmpty())) {
						tvNoRepayments.setText("Couldn't load repayment history.");
						tvNoRepayments.setVisibility(View.VISIBLE);
					}
				});
			}
		});
		
		sheet.show();
	}
	
	private void _renderRepaymentHistory(LinearLayout container, TextView emptyState, List<Map<String, Object>> repayments) {
		if (container == null) return;
		container.removeAllViews();
		if (repayments.isEmpty()) {
			if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
			return;
		}
		if (emptyState != null) emptyState.setVisibility(View.GONE);
		
		for (Map<String, Object> payment : repayments) {
			double amount = payment.get("amountHarv") instanceof Number ? ((Number) payment.get("amountHarv")).doubleValue() : 0;
			long timestamp = payment.get("timestamp") instanceof Number ? ((Number) payment.get("timestamp")).longValue() : 0L;
			
			LinearLayout row = new LinearLayout(getContext());
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setPadding(0, 16, 0, 16);
			LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			row.setLayoutParams(rowParams);
			
			TextView tvDate = new TextView(getContext());
			tvDate.setText(timestamp > 0
			? new java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(new java.util.Date(timestamp))
			: "—");
			tvDate.setTextSize(13f);
			LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
			tvDate.setLayoutParams(dateParams);
			
			TextView tvAmount = new TextView(getContext());
			// APPLIED FIX
			tvAmount.setText(_formatUIBalance(amount));
			tvAmount.setTextSize(13f);
			tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
			
			row.addView(tvDate);
			row.addView(tvAmount);
			container.addView(row);
		}
	}
	
	// NEW UI FORMATTER HELPER
	private String _formatUIBalance(double v) {
		try {
			java.text.DecimalFormat uiDf = new java.text.DecimalFormat("#,##0.########", new java.text.DecimalFormatSymbols(java.util.Locale.US));
			String suffix = _coinSymbol();
			String display = uiDf.format(v);
			return display.endsWith(" " + suffix) ? display : display + " " + suffix;
		} catch (Exception e) {
			return String.valueOf(v) + " " + _coinSymbol();
		}
	}

	/** The group's own coin: stable coins carry their own symbol, everything else falls back to HARV. */
	private String _coinSymbol() {
		if (com.harvana.app.husd.Stable.isStable(groupCoin)) return com.harvana.app.husd.Stable.symbol(groupCoin);
		return com.harvana.app.CoinInfo.ticker();
	}
	
	{
	}
	
}
