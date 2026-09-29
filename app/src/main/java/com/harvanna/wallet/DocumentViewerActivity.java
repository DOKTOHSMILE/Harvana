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
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.ImageView;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.core.content.FileProvider;

public class DocumentViewerActivity extends AppCompatActivity {
	
	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	
	private MaterialCardView cardview1;
	private TextView tvDocTitle;
	private TextView tvDocDescription;
	private MaterialButton btnOpenDocument;
	private TextView tvNoViewerHint;
	private ImageView imageview1;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.document_viewer);
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
		cardview1 = findViewById(R.id.cardview1);
		tvDocTitle = findViewById(R.id.tvDocTitle);
		tvDocDescription = findViewById(R.id.tvDocDescription);
		btnOpenDocument = findViewById(R.id.btnOpenDocument);
		tvNoViewerHint = findViewById(R.id.tvNoViewerHint);
		imageview1 = findViewById(R.id.imageview1);
	}
	
	private void initializeLogic() {
		String docType = getIntent().getStringExtra("doc_type");
		if (docType == null) docType = "terms";
		
		if ("privacy".equals(docType)) {
			assetFileName = "Ajoo-Privacy-Policy.docx";
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle("Privacy Policy");
				getSupportActionBar().setSubtitle("");
			}
			getSupportActionBar().setTitle("Privacy Policy");
		} else {
			assetFileName = "Ajoo-Terms-of-Service.docx";
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle("Terms of Service");
				getSupportActionBar().setSubtitle("");
			}
			getSupportActionBar().setTitle("Terms of Service");
		}
		btnOpenDocument.setOnClickListener(v -> _openDocument());
	}
	
	public void _MORE() {
	}
	private String assetFileName;
	private String docMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private void _openDocument() {
		try {
			File docsDir = new File(getCacheDir(), "docs");
			if (!docsDir.exists()) docsDir.mkdirs();
			File outFile = new File(docsDir, assetFileName);
			
			// Re-copy every time rather than checking if it already exists —
			// the asset is small and this guarantees the viewer always sees
			// the current bundled version, not a stale cached copy from an
			// older app install.
			try (InputStream in = getAssets().open(assetFileName);
			OutputStream out = new FileOutputStream(outFile)) {
				byte[] buffer = new byte[4096];
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
			}
			
			Uri contentUri = FileProvider.getUriForFile(this,
			"com.harvanna.wallet.fileprovider", outFile);
			
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setDataAndType(contentUri, docMimeType);
			viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			startActivity(viewIntent);
			tvNoViewerHint.setVisibility(View.GONE);
			
		} catch (ActivityNotFoundException e) {
			tvNoViewerHint.setVisibility(View.VISIBLE);
		} catch (Exception e) {
			Toast.makeText(this, "Couldn't open the document: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	{
	}
	
}