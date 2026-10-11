package com.harvana.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.harvana.app.earn.StakingPanel;

public class StakingDashboardActivity extends AppCompatActivity {

	private Toolbar _toolbar;
	private StakingPanel panel;
	private final View[] pages = new View[2];
	private int pagesReady;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_staking_dashboard);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setTitle("Staking Dashboard");
		}
		_toolbar.setNavigationOnClickListener(v -> onBackPressed());

		View scroll = findViewById(R.id.staking_scroll);
		pages[0] = findViewById(R.id.page_stake);
		pages[1] = findViewById(R.id.page_positions);
		ViewGroup column = (ViewGroup) ((ViewGroup) scroll).getChildAt(0);
		column.removeView(pages[0]);
		column.removeView(pages[1]);
		column.removeView(findViewById(R.id.staking_pill));
		pages[0].setVisibility(View.VISIBLE);
		pages[1].setVisibility(View.VISIBLE);

		ViewGroup parent = (ViewGroup) scroll.getParent();
		int at = parent.indexOfChild(scroll);
		ViewGroup.LayoutParams old = scroll.getLayoutParams();
		parent.removeView(scroll);

		LinearLayout host = new LinearLayout(this);
		host.setOrientation(LinearLayout.VERTICAL);
		int side = Math.round(16 * getResources().getDisplayMetrics().density);
		host.setPadding(side, side, side, 0);
		TabLayout tabs = (TabLayout) getLayoutInflater().inflate(R.layout.view_toggle_tabs, host, false);
		com.harvana.app.utils.Toggles.pill(tabs);
		ViewPager2 pager = new ViewPager2(this);
		pager.setId(View.generateViewId());
		pager.setOffscreenPageLimit(1);
		pager.setAdapter(new FragmentStateAdapter(this) {
			@Override public int getItemCount() { return 2; }
			@NonNull @Override public Fragment createFragment(int position) { return StakingPage.newInstance(position); }
		});
		new TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(position == 0 ? "Staking" : "My Positions")).attach();
		host.addView(tabs);
		LinearLayout.LayoutParams pagerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
		pagerLp.topMargin = Math.round(8 * getResources().getDisplayMetrics().density);
		host.addView(pager, pagerLp);
		parent.addView(host, at, old);
	}

	View takePage(int index) { return pages[index]; }

	void pageReady() {
		if (++pagesReady == 2 && panel == null) {
			panel = new StakingPanel(this);
			panel.reload();
			_refreshStableShare();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (panel != null) panel.reload();
		_refreshStableShare();
	}

	/** HARV stakers' share of stable-coin fees, on the Staking page (only once a stable coin is on). */
	private void _refreshStableShare() {
		if (pages[0] instanceof LinearLayout) com.harvana.app.husd.StakerShareCard.refresh(this, (LinearLayout) pages[0]);
	}

	public static final class StakingPage extends Fragment {
		static StakingPage newInstance(int index) {
			StakingPage page = new StakingPage();
			Bundle args = new Bundle();
			args.putInt("page", index);
			page.setArguments(args);
			return page;
		}

		@Override
		public View onCreateView(@NonNull android.view.LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			StakingDashboardActivity host = (StakingDashboardActivity) requireActivity();
			View page = host.takePage(getArguments() == null ? 0 : getArguments().getInt("page"));
			if (page.getParent() instanceof ViewGroup) ((ViewGroup) page.getParent()).removeView(page);
			NestedScrollView scroll = new NestedScrollView(requireContext());
			scroll.setFillViewport(true);
			scroll.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			return scroll;
		}

		@Override
		public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
			((StakingDashboardActivity) requireActivity()).pageReady();
		}
	}
}
