package com.harvana.app;


import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.circle.CircleUi;
import com.harvana.app.group.GroupChatSheet;
import com.harvana.app.group.GroupKinds;
import com.harvana.app.group.GroupSaver;
import com.harvana.app.group.PaymentConfirmSheet;
import com.harvana.app.social.PeoplePicker;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creating a group, one screen per kind. Rotating, savings and goal groups are different things on the network, so each one asks
 * for different things, in its own words, and shows what they add up to before anything is signed:
 *
 *   Rotating  contribution, number of members, round length; the pot, the bond each member locks, how long it runs.
 *   Savings   whether members can borrow and at what interest, when savings can be taken out.
 *   Goal      what it is for, the target, how money leaves (and who votes), who receives it.
 *
 * Creating costs the admin only the 1 HARV network fee. Nothing is locked, and the group wallet is made by the network, not
 * by this phone. The kind comes from the entry screen (CreateRotatingGroupActivity and the others set it).
 */
public class CreateGroupWizardActivity extends AppCompatActivity implements com.harvana.app.group.create.CreateGroupStepFragment.Host {

    private enum Kind { ROTATING, ACCUM, FIXED }

    private static final long FEE_RAW = 100_000_000L;
    private static final int DESC_MAX = 300;

    private Kind kind = Kind.ROTATING;
    private String typeLabel = "Rotating Pool";
    private int step;
    private String[] stepNames;

    /** The page being drawn: the current step fragment's column, set while a step-builder runs. */
    private LinearLayout page, dots;
    private MaterialButton back, next;

    // basics
    private String name = "", description = "", visibility = "Public", country = "", state = "", city = "";
    private String photoUrl = "";
    private android.net.Uri photoUri;          // the cropped picture, waiting on the phone until Create group
    private String groupDocId;                  // the group's Firestore id, fixed early so the picture can be stored under it

    private boolean idChecked;

    private String groupDocId() { if (groupDocId == null) groupDocId = java.util.UUID.randomUUID().toString(); return groupDocId; }
    private static final int RC_PHOTO = 721;
    private final List<String> rules = new ArrayList<>();
    private final List<Map<String, Object>> chatLinks = new ArrayList<>();
    private LocationHelper location;

    // rotating
    private String contribution = "";
    private int members = 5;
    private int cycleDays = 7;
    /** Days after a round's due time before an unpaid member defaults (node: graceHours). */
    private int graceDays = 1;

    // savings
    private boolean allowLoans = false;
    private String interest = "0";
    private boolean cycleEndOnly = false;

    // goal
    private int purpose = 0; // 0 one-off purchase, 1 admin decides, 2 project in stages, 3 emergency fund
    private String target = "";
    private boolean beneficiaryIsMe = true;
    private String beneficiary = "";

    // optional display numbers for savings and goal groups (the network does not need them)
    private String expectedMembers = "";
    private String suggestedAmount = "";

    // people
    private final PeoplePicker.Selection invite = new PeoplePicker.Selection();
    private boolean membersCanInvite = true, inviteViaLink = true;
    private boolean requireApproval = false;
    private boolean memberVote = false;

    private String pendingNodeId = null;

    // the group's coin (owner, 12 Oct 2026): every amount of the group is in it; HARV pays only the tFare
    // The member chooses it; nothing is picked for them (owner, 13 Oct 2026).
    private String coin = "";
    private final List<String> coins = new ArrayList<>(java.util.Arrays.asList("HNGN"));

    private String sym() { return coin.isEmpty() ? "coin" : com.harvana.app.husd.Stable.symbol(coin); }
    private long raw(String typed) { return com.harvana.app.group.NodePayActions.coinRaw(typed); }
    private String money(long raw) {
        return coin.isEmpty() ? java.math.BigDecimal.valueOf(raw, 6).stripTrailingZeros().toPlainString()
                : com.harvana.app.engine.Amounts.money(raw, coin);
    }

    /** The coins the network has switched on; hNGN first. */
    private void loadCoins() {
        com.harvana.app.husd.HusdClient.list(new com.harvana.app.husd.HusdClient.Cb() {
            @Override public void ok(org.json.JSONObject r) {
                org.json.JSONArray arr = r.optJSONArray("assets");
                List<String> got = new ArrayList<>();
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject o = arr.optJSONObject(i);
                    String a = o == null ? "" : o.optString("asset");
                    if (o != null && o.optBoolean("active") && com.harvana.app.husd.Stable.isStable(a) && !got.contains(a)) got.add(a);
                }
                runOnUiThread(() -> {
                    if (isFinishing() || got.isEmpty() || got.equals(coins)) return;
                    coins.clear(); coins.addAll(got);
                    if (step == 1) go(step);
                });
            }
            @Override public void fail(String e) { }
        });
    }

    /** The coin the group saves in, chosen by the creator from a bottom sheet (nothing is chosen for them): btn_create_coin. */
    private void coinPicker() {
        MaterialButton pick = page.findViewById(R.id.btn_create_coin);
        if (coin.isEmpty()) {
            pick.setText("Choose a coin");
            pick.setIcon(null);
        } else {
            pick.setText(com.harvana.app.husd.Stable.symbol(coin) + "  ·  " + com.harvana.app.husd.Stable.name(coin));
            pick.setIcon(com.harvana.app.husd.StableIcon.drawable(this, coin));
            pick.setIconTint(null);
            pick.setIconSize(dp(22));
        }
        pick.setOnClickListener(v -> {
            String[] names = new String[coins.size()];
            int sel = -1;
            for (int i = 0; i < names.length; i++) {
                names[i] = com.harvana.app.husd.Stable.symbol(coins.get(i)) + "  ·  " + com.harvana.app.husd.Stable.name(coins.get(i));
                if (coins.get(i).equals(coin)) sel = i;
            }
            new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Stable coin for savings")
                    .setSingleChoiceItems(names, sel, (d, which) -> { coin = coins.get(which); d.dismiss(); go(step); })
                    .setNegativeButton("Cancel", null).show();
        });
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        loadCoins();
        String type = getIntent() == null ? null : getIntent().getStringExtra("groupType");
        if ("Accumulative Funds".equals(type)) { kind = Kind.ACCUM; typeLabel = type; }
        else if ("Fixed Contribution".equals(type)) { kind = Kind.FIXED; typeLabel = type; }
        else { kind = Kind.ROTATING; typeLabel = "Rotating Pool"; }
        stepNames = new String[] {"Basics", kind == Kind.ROTATING ? "The rounds" : kind == Kind.ACCUM ? "Savings rules" : "The goal", "People", "Review"};
        location = new LocationHelper(this);
        country = firstOr(location.getCountryNames(), "Nigeria");
        if (location.getCountryNames().contains("Nigeria")) country = "Nigeria";

        AppBar.setContent(this, R.layout.activity_create_group_wizard,
                kind == Kind.ROTATING ? "New rotating group" : kind == Kind.ACCUM ? "New savings group" : "New goal group", null);
        dots = findViewById(R.id.create_group_dots);
        back = findViewById(R.id.btn_create_back);
        back.setOnClickListener(v -> go(step - 1));
        next = findViewById(R.id.btn_create_next);
        next.setOnClickListener(v -> onNext());
        go(0);
    }

    @Override
    public void onBackPressed() { go(step - 1); }

    // ── the step fragments ─────────────────────────────────────────────────

    private Class<? extends com.harvana.app.group.create.CreateGroupStepFragment> stepClass(int s) {
        switch (s) {
            case 0: return com.harvana.app.group.create.BasicsStepFragment.class;
            case 1: return kind == Kind.ROTATING ? com.harvana.app.group.create.RoundsStepFragment.class
                    : kind == Kind.ACCUM ? com.harvana.app.group.create.SavingsRulesStepFragment.class
                    : com.harvana.app.group.create.GoalStepFragment.class;
            case 2: return com.harvana.app.group.create.PeopleStepFragment.class;
            default: return com.harvana.app.group.create.ReviewStepFragment.class;
        }
    }

    private com.harvana.app.group.create.CreateGroupStepFragment current() {
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.create_group_step);
        return f instanceof com.harvana.app.group.create.CreateGroupStepFragment ? (com.harvana.app.group.create.CreateGroupStepFragment) f : null;
    }

    /** Draws one step into its fragment's page, under the step's title. */
    private void draw(int s, LinearLayout into, Runnable fields) {
        page = into;
        ((TextView) page.findViewById(R.id.tv_step_title)).setText(stepNames[s]);
        fields.run();
    }

    @Override public void buildBasics(LinearLayout p) { draw(0, p, this::basics); }
    @Override public void buildRounds(LinearLayout p) { draw(1, p, () -> { coinPicker(); rotating(); }); }
    @Override public void buildSavingsRules(LinearLayout p) { draw(1, p, () -> { coinPicker(); savings(); }); }
    @Override public void buildGoal(LinearLayout p) { draw(1, p, () -> { coinPicker(); goal(); }); }
    @Override public void buildPeople(LinearLayout p) { draw(2, p, this::people); }
    @Override public void buildReview(LinearLayout p) { draw(3, p, this::review); }
    @Override public void pageShown(com.harvana.app.group.create.CreateGroupStepFragment f) { f.toTop(); }

    // ── navigation ─────────────────────────────────────────────────────────

    private void go(int to) {
        if (to < 0) { finish(); return; }
        step = Math.min(to, stepNames.length - 1);
        dots.removeAllViews();
        for (int i = 0; i < stepNames.length; i++) {
            TextView d = (TextView) getLayoutInflater().inflate(R.layout.item_step_dot, dots, false);
            d.setText(String.valueOf(i + 1));
            d.setBackgroundResource(i <= step ? R.drawable.bg_step_dot_on : R.drawable.bg_step_dot_off);
            d.setTextColor(i <= step ? UiColors.onAccent() : UiColors.textSecondary());
            dots.addView(d);
            if (i < stepNames.length - 1) {
                View line = getLayoutInflater().inflate(R.layout.item_step_line, dots, false);
                if (i < step) line.setBackgroundColor(UiColors.accent());
                dots.addView(line);
            }
        }
        back.setText(step == 0 ? "Cancel" : "Back");
        next.setText(step == stepNames.length - 1 ? "Create group" : "Next");
        com.harvana.app.group.create.CreateGroupStepFragment shown = current();
        if (shown != null && shown.getClass() == stepClass(step)) { shown.redraw(); return; }   // same page: drawn again in place
        boolean forward = shown == null || stepOf(shown) < step;
        try {
            getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(forward ? R.anim.slide_in_right : android.R.anim.slide_in_left,
                        forward ? R.anim.slide_out_left : android.R.anim.slide_out_right)
                .replace(R.id.create_group_step, stepClass(step).newInstance())
                .commitNow();
        } catch (Exception e) {
            Toast.makeText(this, String.valueOf(e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private int stepOf(com.harvana.app.group.create.CreateGroupStepFragment f) {
        for (int i = 0; i < stepNames.length; i++) if (stepClass(i) == f.getClass()) return i;
        return 0;
    }

    private void onNext() {
        String problem = problemWithStep(step);
        if (problem != null) { Toast.makeText(this, problem, Toast.LENGTH_LONG).show(); return; }
        if (step == stepNames.length - 1) { create(); return; }
        go(step + 1);
    }

    private String problemWithStep(int s) {
        if (s == 0) {
            if (name.trim().length() < 3) return "Give the group a name of at least 3 letters.";
            if (state.isEmpty() || city.isEmpty()) return "Choose the state and city where the group is based.";
            return null;
        }
        if (s != 1) return null;
        if (coin.isEmpty()) return "Choose the stable coin this group saves in.";
        if (kind == Kind.ROTATING) {
            if (raw(contribution) <= 0) return "Enter how much each member pays every round.";
            if (members < 3 || members > 12) return "A rotating group needs 3 to 12 members.";
            return null;
        }
        if (kind == Kind.ACCUM) {
            if (allowLoans) {
                try { double r = Double.parseDouble(interest.trim()); if (r < 0 || r > 5) return "Interest per day must be between 0 and 5%."; }
                catch (Exception e) { return "Enter the interest per day, from 0 to 5%."; }
            }
            return null;
        }
        if (raw(target) <= 0) return "Enter the amount the group is aiming for.";
        if (purpose != 3 && !beneficiaryIsMe && !(beneficiary.startsWith("HV0x") && beneficiary.length() == 44)) return "Enter the full wallet address of who receives the money.";
        return null;
    }

    // ── step 1: basics ─────────────────────────────────────────────────────

    /** The optional group picture (iv_create_photo): a round preview, Add / Change, and Remove. Picked and cropped square by GroupPhotoPickerActivity. */
    private void photoRow() {
        android.widget.ImageView face = page.findViewById(R.id.iv_create_photo);
        com.harvana.app.utils.GroupRing.apply(face, typeLabel, 1f, 0x00000000);   // ring colour = group type
        if (photoUri != null) {
            com.bumptech.glide.Glide.with(this).load(photoUri).circleCrop().into(face);
        } else if (photoUrl.isEmpty()) {
            face.setImageResource(R.drawable.ic_coin_harv);
            face.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
            face.setPadding(dp(20), dp(20), dp(20), dp(20));
        } else {
            com.bumptech.glide.Glide.with(this).load(photoUrl).circleCrop().into(face);
        }
        boolean has = photoUri != null || !photoUrl.isEmpty();
        MaterialButton pick = page.findViewById(R.id.btn_create_photo_pick);
        pick.setText(has ? "Change" : "Add picture");
        pick.setOnClickListener(v -> startActivityForResult(com.harvana.app.utils.GroupPhotoPickerActivity.intent(this), RC_PHOTO));
        View remove = page.findViewById(R.id.btn_create_photo_remove);
        remove.setVisibility(has ? View.VISIBLE : View.GONE);
        remove.setOnClickListener(v -> { photoUrl = ""; photoUri = null; go(step); });
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req == RC_PHOTO && res == RESULT_OK && data != null && data.getData() != null) {
            photoUri = data.getData();          // kept on the phone; it uploads when Create group is pressed
            photoUrl = "";
            go(step);
        }
    }

    private void basics() {
        photoRow();
        EditText n = text(R.id.et_create_name, name, s -> name = s);
        EditText d = text(R.id.et_create_description, description, s -> description = s);
        d.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(DESC_MAX)});
        toggle(R.id.tg_create_visibility, "Public".equals(visibility) ? 0 : 1, i -> visibility = i == 0 ? "Public" : "Private");

        Spinner sc = page.findViewById(R.id.sp_create_country), ss = page.findViewById(R.id.sp_create_state), sy = page.findViewById(R.id.sp_create_city);
        List<String> countries = location.getCountryNames();
        location.setSpinnerData(sc, countries);
        sc.setSelection(Math.max(0, countries.indexOf(country)));
        sc.setOnItemSelectedListener(sel(v -> {
            country = v;
            List<String> st = location.getStateNames(country);
            location.setSpinnerData(ss, st);
            if (!st.isEmpty()) { int at = Math.max(0, st.indexOf(state)); ss.setSelection(at); }
        }));
        ss.setOnItemSelectedListener(sel(v -> {
            state = v;
            List<String> ct = location.getCityNames(country, state);
            location.setSpinnerData(sy, ct);
            if (!ct.isEmpty()) { int at = Math.max(0, ct.indexOf(city)); sy.setSelection(at); }
        }));
        sy.setOnItemSelectedListener(sel(v -> city = v));

        LinearLayout ruleList = page.findViewById(R.id.create_rules);
        paintRules(ruleList);
        EditText r = page.findViewById(R.id.et_create_rule);
        page.findViewById(R.id.btn_create_add_rule).setOnClickListener(v -> {
            String t = r.getText().toString().trim();
            if (t.isEmpty()) return;
            if (rules.size() >= 12) { Toast.makeText(this, "Keep it to 12 rules.", Toast.LENGTH_SHORT).show(); return; }
            rules.add(t);
            r.setText("");
            paintRules(ruleList);
        });

        LinearLayout chatList = page.findViewById(R.id.create_chats);
        paintChats(chatList);
        page.findViewById(R.id.btn_create_add_chat).setOnClickListener(v -> {
            if (chatLinks.size() >= 8) { Toast.makeText(this, "A group can have 8 chats.", Toast.LENGTH_SHORT).show(); return; }
            GroupChatSheet.pickOne(this, link -> runOnUiThread(() -> {
                Map<String, Object> copy = new HashMap<>();
                if (link != null) copy.putAll(link);
                chatLinks.add(copy);
                paintChats(chatList);
            }));
        });
    }

    /** One added rule or chat: res/layout/item_create_list_row.xml. */
    private View listRow(LinearLayout box, String text, android.graphics.drawable.Drawable icon, Runnable remove) {
        View row = getLayoutInflater().inflate(R.layout.item_create_list_row, box, false);
        ((TextView) row.findViewById(R.id.tv_create_row_text)).setText(text);
        if (icon != null) {
            android.widget.ImageView iv = row.findViewById(R.id.iv_create_row_icon);
            iv.setImageDrawable(icon);
            iv.setVisibility(View.VISIBLE);
        }
        row.findViewById(R.id.tv_create_row_remove).setOnClickListener(v -> remove.run());
        return row;
    }

    private void paintRules(LinearLayout box) {
        box.removeAllViews();
        for (int i = 0; i < rules.size(); i++) {
            final int at = i;
            box.addView(listRow(box, (i + 1) + ".  " + rules.get(i), null, () -> { rules.remove(at); paintRules(box); }));
        }
    }

    private void paintChats(LinearLayout box) {
        box.removeAllViews();
        for (int i = 0; i < chatLinks.size(); i++) {
            final int at = i;
            Object label = chatLinks.get(i).get("label");
            Object url = chatLinks.get(i).get("url");
            Object kindOf = chatLinks.get(i).get("kind");
            android.graphics.drawable.Drawable badge = com.harvana.app.group.ChatPlatforms.badge(getResources(),
                    com.harvana.app.group.ChatPlatforms.of(kindOf == null ? "" : String.valueOf(kindOf)), 24);
            box.addView(listRow(box, (label == null ? "Chat" : String.valueOf(label)) + (url == null ? "" : "  " + url), badge,
                    () -> { chatLinks.remove(at); paintChats(box); }));
        }
    }

    // ── step 2a: rotating ──────────────────────────────────────────────────

    private void rotating() {
        ((TextView) page.findViewById(R.id.tv_create_contribution_label)).setText("Each member pays, every round (" + sym() + ")");
        final LinearLayout summary = page.findViewById(R.id.create_rounds_summary);
        text(R.id.et_create_contribution, contribution, s -> { contribution = s; refreshRotating(summary); });

        TextView count = page.findViewById(R.id.tv_create_members);
        count.setText(String.valueOf(members));
        page.findViewById(R.id.btn_create_members_minus).setOnClickListener(v -> {
            if (members > 3) { members--; count.setText(String.valueOf(members)); refreshRotating(summary); }
        });
        page.findViewById(R.id.btn_create_members_plus).setOnClickListener(v -> {
            if (members < 12) { members++; count.setText(String.valueOf(members)); refreshRotating(summary); }
        });

        int sel = cycleDays == 1 ? 0 : cycleDays == 7 ? 1 : cycleDays == 14 ? 2 : cycleDays == 30 ? 3 : 4;
        EditText custom = page.findViewById(R.id.et_create_cycle_days);
        custom.setVisibility(sel == 4 ? View.VISIBLE : View.GONE);
        if (sel == 4) custom.setText(String.valueOf(cycleDays));
        custom.addTextChangedListener(watch(s -> {
            try { int d = Integer.parseInt(s.trim()); if (d >= 1 && d <= 30) cycleDays = d; } catch (Exception ignored) { }
            refreshRotating(summary);
        }));
        toggle(R.id.tg_create_cycle, sel, i -> {
            int[] days = {1, 7, 14, 30, 0};
            if (days[i] > 0) { cycleDays = days[i]; custom.setVisibility(View.GONE); }
            else custom.setVisibility(View.VISIBLE);
            refreshRotating(summary);
        });
        int[] graceOptions = {0, 1, 2, 3, 7};
        int gSel = 1;
        for (int i = 0; i < graceOptions.length; i++) if (graceOptions[i] == graceDays) gSel = i;
        toggle(R.id.tg_create_grace, gSel, i -> { graceDays = graceOptions[i]; refreshRotating(summary); });
        refreshRotating(summary);
    }

    /** What the rounds add up to, as a table: pot, bond, rounds and how long. */
    private void refreshRotating(LinearLayout out) {
        out.removeAllViews();
        long c = raw(contribution);
        com.harvana.app.utils.InfoTable t = new com.harvana.app.utils.InfoTable(this);
        long pot = c * (members - 1);
        t.row(com.harvana.app.group.CoinLogo.of(this, coin), "Pot each round", c <= 0 ? "—" : money(pot));
        t.row(R.drawable.icon_lock_round_line, "Bond each member locks", c <= 0 ? "—" : money(pot));
        t.row(R.drawable.ic_groups_line, "Rounds", String.valueOf(members));
        t.row(R.drawable.ic_calendar, "Each round", cycleDays + (cycleDays == 1 ? " day" : " days"));
        t.row(R.drawable.ic_calendar, "Runs for", "about " + (members * cycleDays) + " days");
        t.row(R.drawable.ic_calendar, "Grace period", graceDays == 0 ? "None" : graceDays + (graceDays == 1 ? " day" : " days"));
        out.addView(t.view());
    }

    // ── step 2b: savings ───────────────────────────────────────────────────

    private void savings() {
        SwitchMaterial loans = page.findViewById(R.id.sw_create_loans);
        View rateLabel = page.findViewById(R.id.tv_create_interest_label);
        EditText rate = text(R.id.et_create_interest, "0".equals(interest) ? "" : interest, s -> interest = s);
        Runnable paintRate = () -> {
            rateLabel.setVisibility(allowLoans ? View.VISIBLE : View.GONE);
            rate.setVisibility(allowLoans ? View.VISIBLE : View.GONE);
            if (!allowLoans) interest = "0";
        };
        loans.setChecked(allowLoans);
        loans.setOnCheckedChangeListener((v, on) -> { allowLoans = on; paintRate.run(); });
        paintRate.run();
        toggle(R.id.tg_create_withdraw, cycleEndOnly ? 1 : 0, i -> cycleEndOnly = i == 1);
        text(R.id.et_create_expected, expectedMembers, s -> expectedMembers = s);
        ((TextView) page.findViewById(R.id.tv_create_suggested_label)).setText("Suggested daily saving, " + sym() + " (optional)");
        text(R.id.et_create_suggested, suggestedAmount, s -> suggestedAmount = s);
    }

    // ── step 2c: goal ──────────────────────────────────────────────────────

    private static final String[] PURPOSES = {"One-off purchase or event", "A cause the admin pays out", "A project in stages", "Emergency fund"};
    private static final String[] RULE_CODES = {"ALL_AT_ONCE_AT_GOAL", "ADMIN_TRIGGERED", "MILESTONE_BASED", "EMERGENCY_GRANT"};
    private static final String[] RULE_NAMES = {"All at once at goal", "Admin-Triggered", "Released in Milestones", "Emergency Grants"};

    private void goal() {
        LinearLayout rule = page.findViewById(R.id.create_goal_rule);
        toggle(R.id.tg_create_purpose, purpose, i -> { purpose = i; paintGoalRule(rule); });
        ((TextView) page.findViewById(R.id.tv_create_target_label)).setText("Target amount (" + sym() + ")");
        text(R.id.et_create_target, target, s -> target = s);
        text(R.id.et_create_beneficiary, beneficiary, s -> beneficiary = s.trim());
        toggle(R.id.tg_create_beneficiary, beneficiaryIsMe ? 0 : 1, i -> { beneficiaryIsMe = i == 0; paintGoalRule(rule); });
        paintGoalRule(rule);
        text(R.id.et_create_expected, expectedMembers, s -> expectedMembers = s);
        ((TextView) page.findViewById(R.id.tv_create_suggested_label)).setText("Suggested deposit per member, " + sym() + " (optional)");
        text(R.id.et_create_suggested, suggestedAmount, s -> suggestedAmount = s);
    }

    /** How money leaves for the chosen purpose (a table row), and who receives it (hidden for an emergency fund). */
    private void paintGoalRule(LinearLayout rule) {
        rule.removeAllViews();
        com.harvana.app.utils.InfoTable t = new com.harvana.app.utils.InfoTable(this);
        t.row(R.drawable.ic_check_circle_line, "How money leaves", RULE_NAMES[purpose]);
        t.row(R.drawable.ic_groups_line, "Vote before paying", purpose == 0 ? "No" : "Yes");
        ((LinearLayout.LayoutParams) rule.getLayoutParams()).topMargin = dp(10);
        rule.addView(t.view());
        page.findViewById(R.id.create_beneficiary).setVisibility(purpose == 3 ? View.GONE : View.VISIBLE);
        ((TextView) page.findViewById(R.id.tv_create_beneficiary_label)).setText(purpose == 0 ? "Who receives the pot"
                : purpose == 1 ? "Who is paid if the vote passes" : "Who receives each approved part");
        page.findViewById(R.id.et_create_beneficiary).setVisibility(beneficiaryIsMe ? View.GONE : View.VISIBLE);
    }

    // ── step 3: people ─────────────────────────────────────────────────────

    private void people() {
        ((LinearLayout) page.findViewById(R.id.create_people_picker)).addView(PeoplePicker.section(this, invite));
        View approval = page.findViewById(R.id.create_approval_section);
        approval.setVisibility(kind == Kind.ROTATING ? View.GONE : View.VISIBLE);
        SwitchMaterial ap = page.findViewById(R.id.sw_create_approval);
        View how = page.findViewById(R.id.tg_create_approval_how);
        ap.setChecked(requireApproval);
        how.setVisibility(requireApproval ? View.VISIBLE : View.GONE);
        ap.setOnCheckedChangeListener((v, on) -> { requireApproval = on; how.setVisibility(on ? View.VISIBLE : View.GONE); });
        toggle(R.id.tg_create_approval_how, memberVote ? 1 : 0, i -> memberVote = i == 1);
        SwitchMaterial mi = page.findViewById(R.id.sw_create_members_invite);
        mi.setChecked(membersCanInvite);
        mi.setOnCheckedChangeListener((v, on) -> membersCanInvite = on);
        SwitchMaterial il = page.findViewById(R.id.sw_create_invite_link);
        il.setChecked(inviteViaLink);
        il.setOnCheckedChangeListener((v, on) -> inviteViaLink = on);
    }

    // ── step 4: review ─────────────────────────────────────────────────────

    /** The group, its terms and what it costs, as tables (create_review). */
    private void review() {
        LinearLayout box = page.findViewById(R.id.create_review);
        com.harvana.app.utils.InfoTable g = new com.harvana.app.utils.InfoTable(this);
        g.section("The group");
        g.row(R.drawable.ic_groups_line, "Name", name.trim().isEmpty() ? "—" : name.trim());
        g.row(R.drawable.ic_visibility_line, "Who can find it", visibility);
        g.row(R.drawable.icon_flag_round_line, "Place", city + ", " + state);
        if (invite.count() > 0) g.row(R.drawable.ic_person_line, "Invitations", String.valueOf(invite.count()));
        box.addView(g.view());

        com.harvana.app.utils.InfoTable t = new com.harvana.app.utils.InfoTable(this);
        t.section("The terms");
        for (String[] row : termRows()) {
            if ("Coin".equals(row[0])) t.row(com.harvana.app.group.CoinLogo.of(this, coin), row[0], row[1]);
            else t.row(row[0], row[1]);
        }
        View tv = t.view();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        box.addView(tv, lp);

        com.harvana.app.utils.InfoTable cost = new com.harvana.app.utils.InfoTable(this);
        cost.section("What it costs you");
        cost.row(R.drawable.ic_harv_coin, "tFare", CircleUi.harv(FEE_RAW));
        cost.row(R.drawable.icon_lock_round_line, "Locked now", "Nothing");
        cost.row(R.drawable.ic_wallet_line, "Group wallet", "Held by the network");
        if (kind == Kind.ROTATING) cost.row(R.drawable.ic_person_line, "Your place", "You join first, same bond");
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = dp(12);
        box.addView(cost.view(), lp2);
    }

    private List<String[]> termRows() {
        List<String[]> rows = new ArrayList<>();
        if (kind == Kind.ROTATING) {
            rows.add(new String[] {"Coin", sym()});
            long c = raw(contribution);
            rows.add(new String[] {"Each member pays", money(c) + " every " + (cycleDays == 1 ? "day" : cycleDays + " days")});
            rows.add(new String[] {"Pot each round", money(c * (members - 1))});
            rows.add(new String[] {"Members", String.valueOf(members)});
            rows.add(new String[] {"Bond each member locks", money(c * (members - 1))});
            rows.add(new String[] {"Payout order", "Order of joining"});
        } else if (kind == Kind.ACCUM) {
            rows.add(new String[] {"Coin", sym()});
            rows.add(new String[] {"Lending", allowLoans ? "Yes, " + interest.trim() + "% interest per day" : "No"});
            rows.add(new String[] {"Savings can be taken out", cycleEndOnly ? "Only when the 30-day line ends" : "Any time, with your approval"});
        } else {
            rows.add(new String[] {"For", PURPOSES[purpose]});
            rows.add(new String[] {"Coin", sym()});
            rows.add(new String[] {"Target", money(raw(target))});
            rows.add(new String[] {"How money leaves", RULE_NAMES[purpose]});
            if (purpose != 3) rows.add(new String[] {"Receiver", beneficiaryIsMe ? "You, the admin" : beneficiary});
        }
        if (kind != Kind.ROTATING) rows.add(new String[] {"Joining", requireApproval ? (memberVote ? "Members vote" : "Admin approves") : "Open"});
        return rows;
    }

    // ── create ─────────────────────────────────────────────────────────────

    private void create() {
        // A group id is never used twice: if a record (or a deleted group's marker) already has this id, take a fresh one.
        if (!idChecked) {
            com.harvana.app.firestore.GroupFirestoreManager.getInstance(this).getGroup(groupDocId(), new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
                @Override public void onSuccess(java.util.Map<String, Object> existing) {
                    if (existing != null && !existing.isEmpty()) groupDocId = null;
                    runOnUiThread(() -> { idChecked = true; create(); });
                }
                @Override public void onError(String e) { runOnUiThread(() -> { idChecked = true; create(); }); }   // not found: the id is free
            });
            return;
        }
        final String wallet = WalletManager.getActiveWalletAddress(this);
        if (wallet == null || wallet.isEmpty() || WalletManager.loadPrivateKey(this, wallet) == null) {
            new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("You need a wallet first")
                    .setMessage("The tFare is paid from your wallet. Create or restore one, then try again.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        // The picture uploads now, before the group is made. If it fails nothing has been created and no fee has been spent; the same
        // name is reused on a retry, so a failed attempt never leaves extra pictures behind.
        if (photoUri != null && photoUrl.isEmpty()) {
            final com.harvana.app.ui.ProgressSheet up = com.harvana.app.ui.ProgressSheet.show(this, null, "Uploading the group picture…", true, false);
            com.harvana.app.utils.GroupPhotoPickerActivity.upload(this, photoUri, groupDocId(), new com.harvana.app.utils.ImageHost.Callback() {
                @Override public void onSuccess(String url) { up.dismiss(); photoUrl = url; photoUri = null; create(); }
                @Override public void onError(String message) { up.dismiss(); toast("Couldn't upload the group picture: " + message + " The group was not created. Try again, or remove the picture."); }
            });
            return;
        }
        List<String[]> rows = new ArrayList<>(termRows());
        rows.add(new String[] {"tFare", CircleUi.harv(FEE_RAW)});
        rows.add(new String[] {"Locked now", "Nothing"});
        rows.add(new String[] {"Group wallet", "Made and held by the network."});
        PaymentConfirmSheet.reviewThenAuthorize(this, "Create this group", CircleUi.harv(FEE_RAW), rows, () -> {
            final com.harvana.app.ui.ProgressSheet pd = com.harvana.app.ui.ProgressSheet.show(this, null, "Creating on the network…", true, false);
            NodeGroupClient.Callback done = new NodeGroupClient.Callback() {
                @Override public void onSuccess(JSONObject json) {
                    String id = json.optString("circleId", json.optString("groupId", ""));
                    if (id.isEmpty()) { runOnUiThread(() -> { pd.dismiss(); toast("The network accepted it but returned no id."); }); return; }
                    pendingNodeId = id;
                    NodeGroupClient.Callback got = new NodeGroupClient.Callback() {
                        @Override public void onSuccess(JSONObject g) { runOnUiThread(() -> { pd.dismiss(); save(g.optString("escrowAddress", "")); }); }
                        @Override public void onError(String e) { runOnUiThread(() -> { pd.dismiss(); save(""); }); }
                    };
                    if (kind == Kind.ROTATING) NodeGroupClient.getCircle(id, got);
                    else if (kind == Kind.ACCUM) NodeGroupClient.getAccumGroup(id, got);
                    else NodeGroupClient.getFixedGroup(id, got);
                }
                @Override public void onError(String error) { runOnUiThread(() -> { pd.dismiss(); toast(CircleUi.friendly(error)); }); }
            };
            if (kind == Kind.ROTATING) {
                NodeGroupClient.createCircle(getApplicationContext(), wallet, raw(contribution), members, cycleDays * 24L, coin, graceDays * 24L, done);
            } else if (kind == Kind.ACCUM) {
                double pct = allowLoans ? parse(interest) : 0.0;
                NodeGroupClient.createAccumGroup(getApplicationContext(), wallet, Math.round(pct * 100.0), cycleEndOnly ? "CYCLE_END_ONLY" : "ANYTIME", coin, done);
            } else {
                String rule = RULE_CODES[purpose];
                String who = purpose == 3 ? "" : beneficiaryIsMe ? wallet : beneficiary;
                NodeGroupClient.createFixedGroup(getApplicationContext(), wallet, raw(target), rule, who, 0L, name.trim(), description.trim(), coin, done);
            }
        });
    }

    /** The group's profile in Firestore: names, photos, rules, chat links. The network holds the money; this is what people read. */
    private void save(String escrow) {
        com.harvana.app.trust.AppActivity.note(this, "GROUP", "Created a group");
        final String id = groupDocId();
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> g = new HashMap<>();
        g.put("groupId", id);
        g.put("firestoreDocId", id);
        g.put("timestamp", String.valueOf(System.currentTimeMillis()));
        g.put("groupName", name.trim());
        g.put("groupType", typeLabel);
        g.put("groupDescription", description.trim());
        if (!photoUrl.isEmpty()) g.put("groupPhoto", photoUrl);
        g.put("frequency", kind == Kind.ROTATING ? (cycleDays == 1 ? "Daily" : cycleDays == 7 ? "Weekly" : cycleDays == 14 ? "Bi-weekly" : cycleDays == 30 ? "Monthly" : "Every " + cycleDays + " days") : "Daily");
        if (kind == Kind.ROTATING) g.put("gracePeriodDays", String.valueOf(graceDays));       // the node holds it (graceHours); shown in settings
        g.put("visibility", visibility);
        g.put("country", country);
        g.put("state", state);
        g.put("city", city);
        g.put("distributionMethod", kind == Kind.ROTATING ? "Order of joining" : "");
        g.put("creatorId", uid);
        g.put("status", "Active");
        g.put("currentRound", 1);
        g.put("membersCount", 1);
        g.put("totalCollected", 0.0);
        g.put("trustScore", 0);
        List<Map<String, Object>> ruleMaps = new ArrayList<>();
        for (String r : rules) { Map<String, Object> m = new HashMap<>(); m.put("rules", r); ruleMaps.add(m); }
        g.put("groupRules", ruleMaps);
        g.put("groupWalletAddress", escrow);
        g.put("asset", coin);
        g.put("requireApprovalToJoin", kind != Kind.ROTATING && requireApproval);
        g.put("joinApprovalMethod", kind != Kind.ROTATING && requireApproval ? (memberVote ? "MEMBER_VOTE" : "ADMIN") : "NONE");
        g.put("membersCanInvite", membersCanInvite);
        g.put("membersCanEdit", false);
        g.put("inviteViaLinkEnabled", inviteViaLink);
        g.put("allowLoans", kind == Kind.ACCUM && allowLoans);
        if (kind == Kind.ROTATING) {
            g.put("contributionAmount", parse(contribution));
            g.put("totalSlots", members);
        } else if (kind == Kind.ACCUM) {
            g.put("contributionAmount", parse(suggestedAmount));
            g.put("totalSlots", (int) parse(expectedMembers));
            g.put("loanInterestRate", allowLoans ? parse(interest) : 0.0);
            g.put("loanInterestPeriod", "DAILY");
            g.put("withdrawalRule", cycleEndOnly ? "Cycle-End Only" : "Anytime");
        } else {
            g.put("contributionAmount", parse(suggestedAmount));
            g.put("totalSlots", (int) parse(expectedMembers));
            g.put("targetGoalAmount", parse(target));
            g.put("disbursementRule", RULE_NAMES[purpose]);
            g.put("beneficiaryAddress", purpose == 3 ? "" : beneficiaryIsMe ? String.valueOf(WalletManager.getActiveWalletAddress(this)) : beneficiary);
        }
        if (pendingNodeId != null) {
            g.put("nodeGroupId", pendingNodeId);
            g.put("family", GroupKinds.familyOf(pendingNodeId));
            if (pendingNodeId.startsWith("CIRC-")) g.put("circleId", pendingNodeId);
            g.put("onChain", true);
        }
        if (!chatLinks.isEmpty()) g.put("chatLinks", new ArrayList<>(chatLinks));

        final com.harvana.app.ui.ProgressSheet saving = com.harvana.app.ui.ProgressSheet.show(this, null, "Saving your group…", true, false);
        final boolean onNode = pendingNodeId != null;
        if (onNode) GroupSaver.queue(getApplicationContext(), uid, id, g);
        GroupSaver.save(getApplicationContext(), uid, id, g, error -> runOnUiThread(() -> {
            if (!isFinishing() && saving.isShowing()) saving.dismiss();
            if (error != null) {
                toast(onNode ? "Your group exists on the network, but saving its details failed. Group ID: " + pendingNodeId : error);
                if (onNode) finish();
                return;
            }
            if (onNode) GroupSaver.unqueue(getApplicationContext(), id);
            PeoplePicker.sendAll(this, id, g, invite, "", (sent, failed) -> { });
            android.content.Intent open = new android.content.Intent(getApplicationContext(), GroupDetailsActivity.class);
            open.putExtra("groupId", id);
            startActivity(open);
            finish();
        }));
    }

    // ── small building blocks ──────────────────────────────────────────────

    private interface Text { void on(String s); }
    private interface Pick { void on(int i); }
    private interface Chosen { void on(String s); }

    private static double parse(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private static String firstOr(List<String> l, String d) { return l == null || l.isEmpty() ? d : l.get(0); }

    private android.text.TextWatcher watch(Text t) {
        return new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) { t.on(e.toString()); }
        };
    }

    private AdapterView.OnItemSelectedListener sel(Chosen c) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { Object o = p.getItemAtPosition(pos); if (o != null) c.on(o.toString()); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        };
    }

    /** Fills a field from the wizard's state and keeps the state in step with what is typed. */
    private EditText text(int id, String value, Text t) {
        EditText e = page.findViewById(id);
        e.setText(value);
        e.addTextChangedListener(watch(t));
        return e;
    }

    /** A row of choices (MaterialButtonToggleGroup in the layout), {@code selected} checked; {@code pick} gets the index tapped. */
    private void toggle(int id, int selected, Pick pick) {
        com.google.android.material.button.MaterialButtonToggleGroup g = page.findViewById(id);
        if (selected >= 0 && selected < g.getChildCount()) g.check(g.getChildAt(selected).getId());
        g.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            int at = group.indexOfChild(group.findViewById(checkedId));
            if (at >= 0) pick.on(at);
        });
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
