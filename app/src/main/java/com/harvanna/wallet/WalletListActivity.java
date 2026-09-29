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
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import com.google.android.material.card.MaterialCardView;
import com.harvanna.wallet.wallet.WalletListManager;
import java.util.List;


public class WalletListActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private FloatingActionButton _fab;
	
	private RecyclerView recycler_wallets;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.wallet_list);
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
		_fab = findViewById(R.id._fab);
		recycler_wallets = findViewById(R.id.recycler_wallets);
		
		_fab.setOnClickListener(_v -> showAddWalletChooser());
	}
	
	private void initializeLogic() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final Window window = this.getWindow(); window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS); window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS); window.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
		}
		androidx.appcompat.app.ActionBar ab = getSupportActionBar();
		if (ab != null) {
			int backgroundColor = getResources().getColor(R.color.colorPrimary);
			ab.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(backgroundColor));
		}
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("My wallet list");
			getSupportActionBar().setSubtitle("");
		}
		recycler_wallets.setLayoutManager(new LinearLayoutManager(this));
		refreshList();
	}
	
	public void _MORE() {
	}
	private static final int RC_CREATE_WALLET = 9101;
	private static final int RC_RESTORE_WALLET = 9102;
	private WalletAdapter adapter;
	private void refreshList() {
		List<WalletListManager.WalletEntry> wallets = WalletListManager.getWalletList(this);
		String activeAddress = WalletListManager.getActiveWallet(this);
		
		// First-run backfill: if the list is empty but a wallet is already
		// active (created before this feature existed), add it so it isn't
		// invisible to the person who already has a working wallet.
		if (wallets.isEmpty() && activeAddress != null && !activeAddress.trim().isEmpty()) {
			WalletListManager.addWalletToList(this, activeAddress, "My Wallet");
			wallets = WalletListManager.getWalletList(this);
		}
		
		adapter = new WalletAdapter(wallets, activeAddress);
		recycler_wallets.setAdapter(adapter);
	}
	
	private void showAddWalletChooser() {
		new AlertDialog.Builder(this)
		.setTitle("Add a Wallet")
		.setItems(new String[]{"Create New Wallet", "Restore Existing Wallet"}, (dialog, which) -> {
			if (which == 0) {
				startActivityForResult(new Intent(this, CreateWalletActivity.class), RC_CREATE_WALLET);
			} else {
				Intent restoreIntent = new Intent(this, RestoreWalletActivity.class);
				restoreIntent.putExtra(RestoreWalletActivity.EXTRA_EMBEDDED, true);
				startActivityForResult(restoreIntent, RC_RESTORE_WALLET);
			}
		})
		.show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == RC_CREATE_WALLET || requestCode == RC_RESTORE_WALLET) && resultCode == Activity.RESULT_OK) {
			refreshList();
			setResult(RESULT_OK); // bubble up so WalletAssetFragmentActivity knows to refresh too
		}
	}
	
	private void switchToWallet(WalletListManager.WalletEntry entry) {
		WalletListManager.setActiveWallet(this, entry.address);
		SketchwareUtil.showMessage(getApplicationContext(), "Switched to " + entry.label);
		refreshList();
		setResult(RESULT_OK);
	}
	
	private void showWalletOptions(WalletListManager.WalletEntry entry) {
		new AlertDialog.Builder(this)
		.setTitle(entry.label)
		.setItems(new String[]{"Rename", "Remove from list"}, (dialog, which) -> {
			if (which == 0) {
				showRenameDialog(entry);
			} else {
				confirmRemove(entry);
			}
		})
		.show();
	}
	
	private void showRenameDialog(WalletListManager.WalletEntry entry) {
		final EditText input = new EditText(this);
		input.setText(entry.label);
		input.setSelection(input.getText().length());
		new AlertDialog.Builder(this)
		.setTitle("Rename Wallet")
		.setView(input)
		.setPositiveButton("Save", (d, w) -> {
			String newLabel = input.getText().toString().trim();
			if (!newLabel.isEmpty()) {
				WalletListManager.renameWallet(this, entry.address, newLabel);
				refreshList();
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	private void confirmRemove(WalletListManager.WalletEntry entry) {
		List<WalletListManager.WalletEntry> allWallets = WalletListManager.getWalletList(this);
		String activeAddress = WalletListManager.getActiveWallet(this);
		
		if (allWallets.size() <= 1) {
			SketchwareUtil.showMessage(getApplicationContext(),
			"You need at least one wallet — switch to another wallet before removing this one, or this is your only wallet.");
			return;
		}
		if (entry.address.equals(activeAddress)) {
			SketchwareUtil.showMessage(getApplicationContext(),
			"This is your active wallet. Switch to a different wallet first, then remove this one.");
			return;
		}
		
		new AlertDialog.Builder(this)
		.setTitle("Remove from list?")
		.setMessage("This only removes " + entry.label + " from this list — its funds and keys are not affected, and you can add it back anytime by restoring it again.")
		.setPositiveButton("Remove", (d, w) -> {
			WalletListManager.removeFromList(this, entry.address);
			refreshList();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	// ── Adapter ──────────────────────────────────────────────────────────
	
	private class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.ViewHolder> {
		
		private final List<WalletListManager.WalletEntry> data;
		private final String activeAddress;
		
		WalletAdapter(List<WalletListManager.WalletEntry> data, String activeAddress) {
			this.data = data;
			this.activeAddress = activeAddress;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_wallet_list_item, parent, false);
			return new ViewHolder(v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			WalletListManager.WalletEntry entry = data.get(position);
			boolean isActive = entry.address.equals(activeAddress);
			
			holder.tvLabel.setText(entry.label);
			holder.tvAddress.setText(AddressUtil.truncateAddress(entry.address));
			holder.tvActiveBadge.setVisibility(isActive ? View.VISIBLE : View.GONE);
			holder.cardRoot.setStrokeColor(isActive
			? androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.colorAccent)
			: androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.colorCardStroke));
			
			holder.cardRoot.setOnClickListener(v -> {
				if (!isActive) switchToWallet(entry);
			});
			holder.btnOptions.setOnClickListener(v -> showWalletOptions(entry));
		}
		
		@Override
		public int getItemCount() {
			return data.size();
		}
		
		class ViewHolder extends RecyclerView.ViewHolder {
			MaterialCardView cardRoot;
			TextView tvLabel;
			TextView tvAddress;
			TextView tvActiveBadge;
			ImageButton btnOptions;
			
			ViewHolder(View itemView) {
				super(itemView);
				cardRoot = itemView.findViewById(R.id.card_wallet_row);
				tvLabel = itemView.findViewById(R.id.tv_wallet_label);
				tvAddress = itemView.findViewById(R.id.tv_wallet_address);
				tvActiveBadge = itemView.findViewById(R.id.tv_active_badge);
				btnOptions = itemView.findViewById(R.id.btn_wallet_options);
			}
		}
	}
	
	{
	}
	
}