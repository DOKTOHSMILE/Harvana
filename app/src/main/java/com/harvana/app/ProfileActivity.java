package com.harvana.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.social.PublicProfileActivity;

/**
 * The old personal profile screen. There is one profile screen now (the same page other members see, with my own details,
 * edit and settings added), so this only forwards to it and closes. It stays so older shortcuts and intents still work.
 */
public class ProfileActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		com.google.firebase.auth.FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
		if (me != null) {
			startActivity(new Intent(this, PublicProfileActivity.class).putExtra(PublicProfileActivity.EXTRA_UID, me.getUid()));
		}
		finish();
	}
}
