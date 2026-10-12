package com.harvana.app;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.util.Calendar;

/**
 * About Harvana: what the app is, what it does, how it keeps money safe, who made it (Capital Oracle Projects),
 * and the legal pages and open-source credits.
 */
public class AboutActivity extends AppCompatActivity {

	private LinearLayout root;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ScrollView sv = new ScrollView(this);
		sv.setBackgroundColor(UiColors.background());
		root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(dp(16), dp(16), dp(16), dp(32));
		sv.addView(root);

		header();

		section("What Harvana is",
				"Harvana is a community money app built on its own blockchain, the Harvana Protocol. It brings together "
				+ "the things people already do with money among friends, family and colleagues, saving together, "
				+ "lending, buying and selling, and makes them safe, open and fair, with every naira and every coin "
				+ "accounted for on the chain.");

		features();

		section("How your money is kept safe",
				"• Your keys stay on your phone. Your recovery phrase never leaves it, and nobody at Harvana can move your money.\n"
				+ "• Group money and P2P trades are held in escrow on the chain and released only by the rules everyone agreed to.\n"
				+ "• Every transaction pays a small transport fare, like any other blockchain, so the network stays secure and can't be flooded.\n"
				+ "• Stable coins are backed one for one by the same currency held in a vault, and the reserves can be checked by anyone.");

		section("Our mission",
				"To give every person a fair, transparent way to save, borrow and grow money together, in their own "
				+ "currency, from their own phone, without needing a bank to trust them first.");

		maker();
		links();

		TextView foot = text("© " + Calendar.getInstance().get(Calendar.YEAR) + " Capital Oracle Projects. All rights reserved.\n"
				+ "Harvana and the Harvana Protocol are products of Capital Oracle Projects.", 12, false, UiColors.textTertiary());
		foot.setGravity(Gravity.CENTER);
		foot.setPadding(0, dp(20), 0, 0);
		root.addView(foot);

		AppBar.setContent(this, sv, "About", "Harvana");
	}

	private void header() {
		LinearLayout card = card();
		card.setGravity(Gravity.CENTER_HORIZONTAL);
		ImageView logo = new ImageView(this);
		logo.setImageResource(R.drawable.ic_harv_glow_logo);
		card.addView(logo, new LinearLayout.LayoutParams(dp(72), dp(72)));
		TextView name = text("Harvana", 26, true, UiColors.textPrimary());
		name.setGravity(Gravity.CENTER);
		card.addView(name);
		TextView tag = text("Save together. Trade fairly. Own your money.", 14, false, UiColors.textSecondary());
		tag.setGravity(Gravity.CENTER);
		card.addView(tag);
		TextView ver = text(version(), 12, false, UiColors.textTertiary());
		ver.setGravity(Gravity.CENTER);
		ver.setPadding(0, dp(8), 0, 0);
		card.addView(ver);
		TextView by = text("Created by Capital Oracle Projects", 13, true, UiColors.accent());
		by.setGravity(Gravity.CENTER);
		by.setPadding(0, dp(6), 0, 0);
		card.addView(by);
		root.addView(card);
	}

	private void features() {
		title("What you can do");
		LinearLayout card = card();
		String[][] items = {
				{"Multi-chain wallet", "HARV, Bitcoin, Ethereum, BNB, Solana, Tron, Litecoin, Dogecoin, Pi and more, all from one recovery phrase."},
				{"Stable coins", "hUSD, hNGN, hEUR and other currencies: money that keeps its value, sent instantly to anyone on Harvana."},
				{"Savings groups", "Rotating circles (ajo, esusu, susu), shared savings goals and group loans, with the rules enforced on the chain."},
				{"P2P market", "Buy and sell with people near you. The coins sit in escrow until the seller confirms they were paid."},
				{"Staking", "Lock HARV to help secure the network and earn rewards, plus a share of stable-coin fees."},
				{"Mining", "Earn HARV by helping produce blocks with the Harvana miner."},
				{"Learn and Discover", "Guides, news and communities to help you use money well."},
		};
		for (String[] it : items) {
			card.addView(text(it[0], 15, true, UiColors.textPrimary()));
			TextView d = text(it[1], 13, false, UiColors.textSecondary());
			d.setPadding(0, 0, 0, dp(10));
			card.addView(d);
		}
		root.addView(card);
	}

	private void maker() {
		title("Who made Harvana");
		LinearLayout card = card();
		card.addView(text("Capital Oracle Projects", 16, true, UiColors.textPrimary()));
		card.addView(text("Harvana, the Harvana Protocol blockchain, the node and the miner are designed and built by Capital "
				+ "Oracle Projects, an independent fintech studio building open, community-first financial tools.",
				13, false, UiColors.textSecondary()));
		root.addView(card);
	}

	private void links() {
		title("Legal and help");
		LinearLayout card = card();
		card.addView(row("Privacy policy", v -> startActivity(new Intent(this, DocumentViewerActivity.class).putExtra("doc_type", "privacy"))));
		card.addView(row("Terms of service", v -> startActivity(new Intent(this, DocumentViewerActivity.class).putExtra("doc_type", "terms"))));
		card.addView(row("Help and support", v -> startActivity(new Intent(this, HelpSupportActivity.class))));
		card.addView(row("Website: harvannaprotocol.com", v -> {
			try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://harvannaprotocol.com"))); } catch (Exception ignored) { }
		}));
		card.addView(row("Open-source licences", v -> licences()));
		root.addView(card);
	}

	/** The open-source libraries the app is built with (app/module.toml), and their licences. */
	private void licences() {
		String text = "Harvana is built with these open-source libraries. Thank you to their authors.\n\n"
				+ "• AndroidX (AppCompat, RecyclerView, ViewPager2, Biometric, WorkManager and others): Apache 2.0\n"
				+ "• Material Components for Android: Apache 2.0\n"
				+ "• Kotlin standard library: Apache 2.0\n"
				+ "• OkHttp (Square): Apache 2.0\n"
				+ "• Gson (Google): Apache 2.0\n"
				+ "• Glide (Bumptech): BSD, part MIT and Apache 2.0\n"
				+ "• Bouncy Castle: MIT-style Bouncy Castle licence\n"
				+ "• Code Scanner (Yuriy Budiyev): MIT\n"
				+ "• CircleImageView (Henning Dodenhof): Apache 2.0\n"
				+ "• MPAndroidChart (Philipp Jahoda): Apache 2.0\n"
				+ "• ZXing (QR codes): Apache 2.0\n"
				+ "• Firebase and Google Play services: Google APIs Terms of Service\n\n"
				+ "Full licence texts: apache.org/licenses/LICENSE-2.0, opensource.org/licenses/MIT and bouncycastle.org/licence.html.";
		com.harvana.app.p2p.ThemedDialogs.show(com.harvana.app.p2p.ThemedDialogs.alert(this)
				.setTitle("Open-source licences").setMessage(text).setPositiveButton("Close", null));
	}

	private String version() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			return "Version " + info.versionName;
		} catch (Exception e) {
			return "";
		}
	}

	// ── small builders ───────────────────────────────────────────────────────

	private void section(String title, String body) {
		title(title);
		LinearLayout card = card();
		card.addView(text(body, 14, false, UiColors.textSecondary()));
		root.addView(card);
	}

	private void title(String s) {
		TextView t = text(s, 15, true, UiColors.textPrimary());
		t.setPadding(dp(4), dp(18), 0, dp(6));
		root.addView(t);
	}

	private View row(String label, View.OnClickListener l) {
		TextView t = text(label + "  ›", 15, false, UiColors.textPrimary());
		t.setPadding(0, dp(12), 0, dp(12));
		t.setOnClickListener(l);
		return t;
	}

	private LinearLayout card() {
		LinearLayout c = new LinearLayout(this);
		c.setOrientation(LinearLayout.VERTICAL);
		c.setPadding(dp(16), dp(16), dp(16), dp(16));
		GradientDrawable g = new GradientDrawable();
		g.setCornerRadius(dp(18));
		g.setColor(UiColors.surface());
		c.setBackground(g);
		return c;
	}

	private TextView text(String s, float sp, boolean bold, int color) {
		TextView t = new TextView(this);
		t.setText(s);
		t.setTextSize(sp);
		t.setTextColor(color);
		t.setLineSpacing(0, 1.15f);
		if (bold) t.setTypeface(null, Typeface.BOLD);
		t.setPadding(0, dp(2), 0, dp(2));
		return t;
	}

	private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
