package com.harvana.app;

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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.card.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;



public class WalletEarnFragmentActivity extends Fragment {
	
	private static final String PREF_MINING = "harvana_mining_rewards";
	private static final String KEY_TOTAL_MINED = "total_mined_harv";
	private String walletAddress = "";
	
	private NestedScrollView vscroll1;
	private LinearLayout linear1;
	private MaterialCardView cardview4;
	private MaterialCardView cardview5;
	private LinearLayout linear7;
	private CardView cardview7;
	private LinearLayout linear13;
	private ImageView imageview1;
	private TextView textview16;
	private LinearLayout linear8;
	private TextView textview10;
	private TextView textview8;
	private TextView tvEarnApr;
	private TextView tvLearnMore;
	private com.google.android.material.button.MaterialButton btnOpenStaking;
	
	private Intent intent = new Intent();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.wallet_earn_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		vscroll1 = _view.findViewById(R.id.vscroll1);
		linear1 = _view.findViewById(R.id.linear1);
		cardview4 = _view.findViewById(R.id.cardview4);
		cardview5 = _view.findViewById(R.id.cardview5);
		linear7 = _view.findViewById(R.id.linear7);
		cardview7 = _view.findViewById(R.id.cardview7);
		linear13 = _view.findViewById(R.id.linear13);
		imageview1 = _view.findViewById(R.id.imageview1);
		textview16 = _view.findViewById(R.id.textview16);
		linear8 = _view.findViewById(R.id.linear8);
		textview10 = _view.findViewById(R.id.textview10);
		textview8 = _view.findViewById(R.id.textview8);
		tvEarnApr = _view.findViewById(R.id.tv_earn_apr);
		tvLearnMore = _view.findViewById(R.id.tv_learn_more);
		btnOpenStaking = _view.findViewById(R.id.btn_open_staking);

		btnOpenStaking.setOnClickListener(_v -> {
			if (getContext() == null) return;
			startActivity(new Intent(getContext(), StakingDashboardActivity.class));
		});
		tvLearnMore.setOnClickListener(_v -> showHowItWorks());
		
		cardview7.setOnClickListener(_v -> {
			Context targetContext = getContext() != null ? getContext() : (getActivity() != null ? getActivity() : null);
			boolean isInstalled = false;
			
			if (targetContext != null) {
				try {
					targetContext.getPackageManager().getPackageInfo("com.harvanna.mine", 0);
					isInstalled = true;
				} catch (Exception e) {
					isInstalled = false;
				}
			}
			
			if (isInstalled && targetContext != null) {
				Intent launchIntent = targetContext.getPackageManager().getLaunchIntentForPackage("com.harvanna.mine");
				if (launchIntent != null) {
					try {
						startActivity(launchIntent);
					} catch (Exception e) {
						Toast.makeText(targetContext, "Unable to launch Harvana Node Miner", Toast.LENGTH_LONG).show();
					}
				}
			} else {
				if (targetContext != null) {
					Toast.makeText(targetContext, "Harvana Node Miner app is not installed — opening download link", Toast.LENGTH_LONG).show();
				}
				String downloadUrl = "https://t.me/+yay9a20_6M4wZGZk";
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
				try {
					startActivity(browserIntent);
				} catch (Exception e) {
					// Fallback handled
				}
			}
		});
	}
	
	private void initializeLogic() {
		if (getActivity() != null) com.harvana.app.earn.StakingPanel.fillHeadline(getActivity(), tvEarnApr);
	}

	private void showHowItWorks() {
		if (getContext() == null) return;
		com.google.android.material.bottomsheet.BottomSheetDialog dialog = com.harvana.app.p2p.ThemedDialogs.sheet(getContext());
		android.widget.ScrollView scroll = new android.widget.ScrollView(getContext());
		LinearLayout box = new LinearLayout(getContext());
		box.setOrientation(LinearLayout.VERTICAL);
		int p = (int) (20 * getResources().getDisplayMetrics().density);
		box.setPadding(p, p, p, p);
		box.addView(sheetTitle("HARV PoW Reward Program"));
		box.addView(sheetBody("The HARV Proof-of-Work reward program pays people who secure the chain. Solo miners take the full block when they find it. Pool miners split that block by the valid shares they submitted. A reward stays pending until the block has enough confirmations, then it is credited to the wallet.\n\nDifficulty, hashrate, battery protection and share checks are there so one phone cannot pretend to be a farm."));
		box.addView(sheetTitle("How to earn more"));
		box.addView(sheetBody("Stay online in longer sessions, keep a stable connection, and cut rejected shares by updating the app. A stronger CPU raises hashrate, but the red thread setting can overheat the phone. Staking is the other path: lock HARV and take a share of every block in proportion to your stake. Unstaking stops earning immediately and returns after 7 days."));
		scroll.addView(box);
		dialog.setContentView(scroll);
		dialog.show();
	}

	private TextView sheetTitle(String value) {
		TextView t = new TextView(getContext());
		t.setText(value);
		t.setTextSize(18);
		t.setTypeface(null, android.graphics.Typeface.BOLD);
		t.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
		t.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, 0);
		return t;
	}

	private TextView sheetBody(String value) {
		TextView t = new TextView(getContext());
		t.setText(value);
		t.setTextSize(14);
		t.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
		t.setPadding(0, (int) (8 * getResources().getDisplayMetrics().density), 0, 0);
		return t;
	}
	

	public void _MORE() {
	}
	{
	}
	
}
