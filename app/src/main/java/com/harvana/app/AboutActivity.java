package com.harvana.app;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.utils.AppBar;

import java.util.Calendar;

/**
 * About Harvana: what the app is, what it does, how it keeps money safe, who made it (Capital Oracle Projects),
 * and the legal pages and open-source credits.
 */
public class AboutActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AppBar.setContent(this, R.layout.activity_about, "About", "Harvana");
		((TextView) findViewById(R.id.tv_about_version)).setText(version());
		((TextView) findViewById(R.id.tv_about_footer)).setText("© " + Calendar.getInstance().get(Calendar.YEAR) + " Capital Oracle Projects. All rights reserved.\n"
				+ "Harvana and the Harvana Protocol are products of Capital Oracle Projects.");
		findViewById(R.id.row_about_privacy).setOnClickListener(v -> startActivity(new Intent(this, DocumentViewerActivity.class).putExtra("doc_type", "privacy")));
		findViewById(R.id.row_about_terms).setOnClickListener(v -> startActivity(new Intent(this, DocumentViewerActivity.class).putExtra("doc_type", "terms")));
		findViewById(R.id.row_about_help).setOnClickListener(v -> startActivity(new Intent(this, HelpSupportActivity.class)));
		findViewById(R.id.row_about_website).setOnClickListener(v -> {
			try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://harvannaprotocol.com"))); } catch (Exception ignored) { }
		});
		findViewById(R.id.row_about_licences).setOnClickListener(v -> licences());
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
}
