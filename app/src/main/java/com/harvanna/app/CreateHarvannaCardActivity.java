package com.harvanna.app;

import com.harvanna.app.utils.UiColors;
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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.*;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import com.harvanna.app.wallet.*;
import com.budiyev.android.codescanner.*;
import com.bumptech.glide.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.card.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.*;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Map;
import java.util.regex.*;
import org.json.*;

public class CreateHarvannaCardActivity extends AppCompatActivity {

  private AppBarLayout app_bar_layout1;
  private NestedScrollView vscroll1;
  private Toolbar toolbar;
  private LinearLayout linear2;
  private TextView tvSubtitle;
  private TextView textview5;
  private MaterialCardView cardview1;
  private HorizontalScrollView hscroll1;
  private TextView textview1;
  private TextView textview2;
  private TextView textview3;
  private TextInputLayout textinputlayout1;
  private TextInputLayout textinputlayout2;
  private TextView tv_char_count;
  private TextView tv_error;
  private MaterialButton btn_generate;
  private LinearLayout linear5;
  private MaterialButton btn_done;
  private HorizontalScrollView hscroll3;
  private LinearLayout linear3;
  private ImageView swatch_classic;
  private ImageView swatch_obsidian;
  private ImageView swatch_gold;
  private ImageView swatch_oasis;
  private ImageView swatch_node;
  private ImageView swatch_cyber;
  private ImageView swatch_mint;
  private LinearLayout linear4;
  private MaterialCardView cardview2;
  private MaterialCardView cardview3;
  private ImageView iv_card_front;
  private ImageView iv_card_back;
  private TextInputEditText et_sentence;
  private TextInputEditText et_confirm_sentence;
  private MaterialButton btn_save_images;
  private MaterialButton btn_save_pdf;

  @Override
  protected void onCreate(Bundle _savedInstanceState) {
    super.onCreate(_savedInstanceState);
		getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE); // no screenshots or recents thumbnails of secrets
    setContentView(R.layout.create_harvanna_card);
    initialize(_savedInstanceState);
    FirebaseApp.initializeApp(this);
    initializeLogic();
  }

  private void initialize(Bundle _savedInstanceState) {
    app_bar_layout1 = findViewById(R.id.app_bar_layout1);
    vscroll1 = findViewById(R.id.vscroll1);
    toolbar = findViewById(R.id.toolbar);
    linear2 = findViewById(R.id.linear2);
    tvSubtitle = findViewById(R.id.tvSubtitle);
    textview5 = findViewById(R.id.textview5);
    cardview1 = findViewById(R.id.cardview1);
    hscroll1 = findViewById(R.id.hscroll1);
    textview1 = findViewById(R.id.textview1);
    textview2 = findViewById(R.id.textview2);
    textview3 = findViewById(R.id.textview3);
    textinputlayout1 = findViewById(R.id.textinputlayout1);
    textinputlayout2 = findViewById(R.id.textinputlayout2);
    tv_char_count = findViewById(R.id.tv_char_count);
    tv_error = findViewById(R.id.tv_error);
    btn_generate = findViewById(R.id.btn_generate);
    linear5 = findViewById(R.id.linear5);
    btn_done = findViewById(R.id.btn_done);
    hscroll3 = findViewById(R.id.hscroll3);
    linear3 = findViewById(R.id.linear3);
    swatch_classic = findViewById(R.id.swatch_classic);
    swatch_obsidian = findViewById(R.id.swatch_obsidian);
    swatch_gold = findViewById(R.id.swatch_gold);
    swatch_oasis = findViewById(R.id.swatch_oasis);
    swatch_node = findViewById(R.id.swatch_node);
    swatch_cyber = findViewById(R.id.swatch_cyber);
    swatch_mint = findViewById(R.id.swatch_mint);
    linear4 = findViewById(R.id.linear4);
    cardview2 = findViewById(R.id.cardview2);
    cardview3 = findViewById(R.id.cardview3);
    iv_card_front = findViewById(R.id.iv_card_front);
    iv_card_back = findViewById(R.id.iv_card_back);
    et_sentence = findViewById(R.id.et_sentence);
    et_confirm_sentence = findViewById(R.id.et_confirm_sentence);
    btn_save_images = findViewById(R.id.btn_save_images);
    btn_save_pdf = findViewById(R.id.btn_save_pdf);
  }

  private void initializeLogic() {
    cardview2.setCardBackgroundColor(Color.parseColor("#99FFFFFF"));
    cardview3.setCardBackgroundColor(Color.parseColor("#99FFFFFF"));
    setSupportActionBar(toolbar);

    mandatory = getIntent() != null && getIntent().getBooleanExtra(EXTRA_MANDATORY, false);

    if (mandatory) {
      if (getSupportActionBar() != null)
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      toolbar.setNavigationIcon(null);
    } else {
      if (getSupportActionBar() != null)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    String label = getIntent() != null ? getIntent().getStringExtra(EXTRA_LABEL) : null;

    if (mandatory) {
      walletAddress = getIntent() != null ? getIntent().getStringExtra(EXTRA_WALLET_ADDRESS) : null;
      if (getIntent() != null) getIntent().removeExtra(EXTRA_RAW_MNEMONIC);
      mnemonic = com.harvanna.app.wallet.PhraseHandoff.peek(walletAddress);
      if (tvSubtitle != null) {
        tvSubtitle.setText("Required step: back up "
            + (label != null && !label.trim().isEmpty() ? label : "this wallet")
            + " before it's created. This can't be skipped.");
      }
    } else {
      String requestedAddress = getIntent() != null
          ? getIntent().getStringExtra(EXTRA_WALLET_ADDRESS)
          : null;
      if (requestedAddress != null && !requestedAddress.trim().isEmpty()) {
        walletAddress = requestedAddress.trim();
        mnemonic = SecureWalletStorage.loadMnemonic(this, walletAddress);
        if (tvSubtitle != null) {
          tvSubtitle.setText(label != null && !label.trim().isEmpty() ? "Backing up: " + label
              : "Backing up wallet " + shortAddress(walletAddress));
        }
      } else {
        walletAddress = WalletManager.getActiveWalletAddress(this);
        mnemonic = SecureWalletStorage.getActiveMnemonic(this);
        if (tvSubtitle != null)
          tvSubtitle.setText("Backing up your personal wallet");
      }
    }

    if (mnemonic == null || mnemonic.trim().isEmpty() || walletAddress == null
        || walletAddress.trim().isEmpty()) {
      SketchwareUtil.showMessage(getApplicationContext(), "No wallet found to back up.");
      finish();
      return;
    }

    // Fetch User Name for the Card Owner property
    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
      String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

      // Instantly load from cache so it's ready for the first render
      Map<String, Object> cached = com.harvanna.app.offline.OfflineCacheManager.load(this,
          "profile_" + uid);
      if (cached != null) {
        String fName = cached.get("first_name") != null ? cached.get("first_name").toString() : "";
        String lName = cached.get("last_name") != null ? cached.get("last_name").toString() : "";
        cardOwnerName = (fName + " " + lName).trim();
      }

      // Fetch from Firestore to ensure it's up to date
      com.harvanna.app.firestore.UserProfileManager.getInstance(this).getProfile(uid,
          new com.harvanna.app.firestore.UserProfileManager.DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
              if (data != null) {
                String fName = data.get("first_name") != null ? data.get("first_name").toString()
                    : "";
                String lName = data.get("last_name") != null ? data.get("last_name").toString()
                    : "";
                String newName = (fName + " " + lName).trim();

                // If the name changed from the cache, update it and re-render the card
                if (!newName.equals(cardOwnerName)) {
                  cardOwnerName = newName;
                  if (generatedFrontBitmap != null) {
                    runOnUiThread(() -> onGenerateClicked());
                  }
                }
              }
            }

            @Override
            public void onError(String error) {
            }
          });
    }

    // Set up click listeners for all 7 swatches
    swatch_classic.setOnClickListener(v -> selectTemplate(CardTemplate.CLASSIC));
    swatch_obsidian.setOnClickListener(v -> selectTemplate(CardTemplate.OBSIDIAN_BLACK));
    swatch_gold.setOnClickListener(v -> selectTemplate(CardTemplate.GOLD_ELITE));
    swatch_oasis.setOnClickListener(v -> selectTemplate(CardTemplate.OASIS));
    swatch_node.setOnClickListener(v -> selectTemplate(CardTemplate.NODE));
    swatch_cyber.setOnClickListener(v -> selectTemplate(CardTemplate.CYBER));
    swatch_mint.setOnClickListener(v -> selectTemplate(CardTemplate.MINT));

    tintSwatches();
    selectTemplate(CardTemplate.CLASSIC);

    TextWatcher watcher = new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateGenerateButtonState();
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    };
    et_sentence.addTextChangedListener(watcher);
    et_confirm_sentence.addTextChangedListener(watcher);

    btn_generate.setOnClickListener(v -> onGenerateClicked());
    btn_save_images.setOnClickListener(v -> onSaveImagesClicked());
    btn_save_pdf.setOnClickListener(v -> onSavePdfClicked());

    if (btn_done != null) {
      btn_done.setVisibility(mandatory ? View.VISIBLE : View.GONE);
      btn_done.setEnabled(false);
      btn_done.setAlpha(0.4f);
      btn_done.setOnClickListener(v -> finishSuccessfully());
    }

    updateGenerateButtonState();

  }

  public void _MORE() {
  }

  public static final String EXTRA_WALLET_ADDRESS = "wallet_address";
  public static final String EXTRA_LABEL = "label";
  public static final String EXTRA_MANDATORY = "mandatory";
  /** Unused. The phrase is passed in memory via {@link com.harvanna.app.wallet.PhraseHandoff}, never on an Intent. */
  public static final String EXTRA_RAW_MNEMONIC = "raw_mnemonic";
  /** When set, the encrypted card is also published privately to the group's members. */
  public static final String EXTRA_GROUP_ID = "group_id";
  public static final String EXTRA_RESULT_QR = "result_qr_payload";
  public static final String EXTRA_RESULT_DIGITS = "result_digit_code";
  public static final String EXTRA_RESULT_TEMPLATE = "result_template";
  public static final String EXTRA_RESULT_CARD_SAVED = "card_saved";

  private String mnemonic;
  private String walletAddress;
  private String cardOwnerName = "";
  private boolean mandatory;
  private boolean cardHasBeenSaved = false;
  private CardTemplate selectedTemplate = CardTemplate.CLASSIC;

  private String lastQrPayload;
  private String lastDigitCode;

  private Bitmap generatedFrontBitmap;
  private Bitmap generatedBackBitmap;

  private void tintSwatches() {
    swatch_classic.setColorFilter(Color.parseColor("#111827"), PorterDuff.Mode.SRC_IN);
    swatch_obsidian.setColorFilter(Color.parseColor("#0A0A0A"), PorterDuff.Mode.SRC_IN);
    swatch_gold.setColorFilter(Color.parseColor("#FFC107"), PorterDuff.Mode.SRC_IN);
    swatch_oasis.setColorFilter(Color.parseColor("#FFD180"), PorterDuff.Mode.SRC_IN);
    swatch_node.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_IN);
    swatch_cyber.setColorFilter(Color.parseColor("#6200EA"), PorterDuff.Mode.SRC_IN);
    swatch_mint.setColorFilter(Color.parseColor("#00BFA5"), PorterDuff.Mode.SRC_IN);
  }

  @Override
  public void onBackPressed() {
    if (!mandatory) {
      super.onBackPressed();
      return;
    }
    if (cardHasBeenSaved) {
      finishSuccessfully();
      return;
    }
    new AlertDialog.Builder(this).setTitle("Skip the backup card?").setMessage(
        "Without a saved Harvanna Access Card, this wallet has no backup yet. If you lose this device, the funds are gone. Are you sure you want to cancel wallet creation?")
        .setPositiveButton("Cancel Wallet Creation", (d, w) -> {
          com.harvanna.app.wallet.PhraseHandoff.clear();
          setResult(RESULT_CANCELED);
          super.onBackPressed();
        }).setNegativeButton("Keep Going", null).show();
  }

  private void finishSuccessfully() {
    Intent result = new Intent();
    result.putExtra(EXTRA_RESULT_CARD_SAVED, cardHasBeenSaved);
    result.putExtra(EXTRA_RESULT_QR, lastQrPayload);
    result.putExtra(EXTRA_RESULT_DIGITS, lastDigitCode);
    result.putExtra(EXTRA_RESULT_TEMPLATE, selectedTemplate.name());
    setResult(RESULT_OK, result);
    com.harvanna.app.wallet.PhraseHandoff.clear();
    finish();
  }

  private String shortAddress(String address) {
    if (address == null || address.length() < 10)
      return address;
    return address.substring(0, 10) + "…";
  }

  // Updates alpha state across all 7 swatches
  private void selectTemplate(CardTemplate template) {
    selectedTemplate = template;
    swatch_classic.setAlpha(template == CardTemplate.CLASSIC ? 1f : 0.4f);
    swatch_obsidian.setAlpha(template == CardTemplate.OBSIDIAN_BLACK ? 1f : 0.4f);
    swatch_gold.setAlpha(template == CardTemplate.GOLD_ELITE ? 1f : 0.4f);
    swatch_oasis.setAlpha(template == CardTemplate.OASIS ? 1f : 0.4f);
    swatch_node.setAlpha(template == CardTemplate.NODE ? 1f : 0.4f);
    swatch_cyber.setAlpha(template == CardTemplate.CYBER ? 1f : 0.4f);
    swatch_mint.setAlpha(template == CardTemplate.MINT ? 1f : 0.4f);

    if (generatedFrontBitmap != null)
      onGenerateClicked();
  }

  private void updateGenerateButtonState() {
    String password = et_sentence.getText().toString();
    int len = password.trim().length();
    tv_char_count.setText(len + " / " + HarvannaCardCrypto.MIN_PASSWORD_LENGTH + " characters minimum");

    boolean longEnough = len >= HarvannaCardCrypto.MIN_PASSWORD_LENGTH;
    boolean matches = password.equals(et_confirm_sentence.getText().toString())
        && !et_confirm_sentence.getText().toString().isEmpty();

    btn_generate.setEnabled(longEnough && matches);

    if (longEnough) {
      tv_char_count.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
    } else {
      tv_char_count.setTextColor(UiColors.textTertiary());
    }

    if (!longEnough) {
      tv_error.setText("Keep going — needs to be at least " + HarvannaCardCrypto.MIN_PASSWORD_LENGTH
          + " characters.");
      tv_error.setVisibility(View.VISIBLE);
    } else if (!matches) {
      tv_error.setText("Passwords don't match yet.");
      tv_error.setVisibility(View.VISIBLE);
    } else {
      tv_error.setVisibility(View.GONE);
    }
  }

  private void onGenerateClicked() {
    String password = et_sentence.getText().toString();
    btn_generate.setEnabled(false);
    btn_generate.setText("ENCRYPTING…");

    new Thread(() -> {
      try {
        String qrPayload = HarvannaCardCrypto.encryptToQrString(this, mnemonic, password);
        String digitCode = HarvannaCardCrypto.encryptToDigitCode(this, mnemonic, password);

        Bitmap front = HarvannaCardRenderer.renderFront(this, selectedTemplate, walletAddress,
            cardOwnerName);
        Bitmap back = HarvannaCardRenderer.renderBack(this, selectedTemplate, qrPayload, digitCode);

        runOnUiThread(() -> {
          generatedFrontBitmap = front;
          generatedBackBitmap = back;
          iv_card_front.setImageBitmap(front);
          iv_card_back.setImageBitmap(back);
          iv_card_front.setVisibility(View.VISIBLE);
          iv_card_back.setVisibility(View.VISIBLE);
          btn_save_images.setVisibility(View.VISIBLE);
          btn_save_pdf.setVisibility(View.VISIBLE);
          btn_generate.setEnabled(true);
          btn_generate.setText("REGENERATE CARD");
          lastQrPayload = qrPayload;
          lastDigitCode = digitCode;
          publishToGroup(qrPayload, digitCode);
          SketchwareUtil.showMessage(getApplicationContext(),
              "Card ready. Save it now — you'll need the password to restore later.");
        });
      } catch (HarvannaCardCrypto.ValidationException ve) {
        runOnUiThread(() -> {
          btn_generate.setEnabled(true);
          btn_generate.setText("GENERATE CARD");
          SketchwareUtil.showMessage(getApplicationContext(), ve.getMessage());
        });
      } catch (Exception e) {
        Log.e("CreateHarvannaCard", "Card generation failed", e);
        final String detail = e.getMessage() != null ? e.getMessage()
            : e.getClass().getSimpleName();
        runOnUiThread(() -> {
          btn_generate.setEnabled(true);
          btn_generate.setText("GENERATE CARD");
          SketchwareUtil.showMessage(getApplicationContext(),
              "Couldn't create the card: " + detail);
        });
      }
    }).start();
  }

  private void publishToGroup(String qrPayload, String digitCode) {
    String gid = getIntent().getStringExtra(EXTRA_GROUP_ID);
    if (gid == null || gid.isEmpty()) return;
    com.google.firebase.auth.FirebaseUser u =
        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
    com.harvanna.app.group.GroupCardStore.publish(this, gid, walletAddress,
        getIntent().getStringExtra(EXTRA_LABEL), selectedTemplate.name(), qrPayload, digitCode,
        u == null ? "" : u.getUid(), published -> runOnUiThread(() ->
            SketchwareUtil.showMessage(getApplicationContext(), published
                ? "Encrypted card shared with group members as a backup."
                : "Couldn't share with members yet — will retry automatically.")));
  }

  private void markSavedAndUnlockDone() {
    cardHasBeenSaved = true;
    if (btn_done != null) {
      btn_done.setEnabled(true);
      btn_done.setAlpha(1f);
    }
  }

  private void onSaveImagesClicked() {
    if (generatedFrontBitmap == null || generatedBackBitmap == null)
      return;
    long stamp = System.currentTimeMillis();

    HarvannaCardRenderer.saveAsPng(this, generatedFrontBitmap, "harvanna-card-front-" + stamp,
        new HarvannaCardRenderer.ExportCallback() {
          @Override
          public void onSuccess(android.net.Uri savedUri) {
            runOnUiThread(() -> {
              SketchwareUtil.showMessage(getApplicationContext(), "Front saved to Pictures/Harvanna");
              markSavedAndUnlockDone();
            });
          }

          @Override
          public void onError(String error) {
            runOnUiThread(() -> SketchwareUtil.showMessage(getApplicationContext(),
                "Couldn't save front: " + error));
          }
        });

    HarvannaCardRenderer.saveAsPng(this, generatedBackBitmap, "harvanna-card-back-" + stamp,
        new HarvannaCardRenderer.ExportCallback() {
          @Override
          public void onSuccess(android.net.Uri savedUri) {
            runOnUiThread(() -> {
              SketchwareUtil.showMessage(getApplicationContext(), "Back saved to Pictures/Harvanna");
              markSavedAndUnlockDone();
            });
          }

          @Override
          public void onError(String error) {
            runOnUiThread(() -> SketchwareUtil.showMessage(getApplicationContext(),
                "Couldn't save back: " + error));
          }
        });
  }

  private void onSavePdfClicked() {
    if (generatedFrontBitmap == null || generatedBackBitmap == null)
      return;
    String name = "harvanna-access-card-" + System.currentTimeMillis();
    HarvannaCardRenderer.saveAsPdf(this, generatedFrontBitmap, generatedBackBitmap, name,
        new HarvannaCardRenderer.ExportCallback() {
          @Override
          public void onSuccess(android.net.Uri savedUri) {
            runOnUiThread(() -> {
              SketchwareUtil.showMessage(getApplicationContext(),
                  "Saved to Downloads/Harvanna — 2 pages");
              markSavedAndUnlockDone();
            });
          }

          @Override
          public void onError(String error) {
            runOnUiThread(() -> SketchwareUtil.showMessage(getApplicationContext(),
                "Couldn't save PDF: " + error));
          }
        });
  }

  {
  }

}
