package com.harvana.app;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.util.Locale;
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
public class CreateGroupWizardActivity extends AppCompatActivity {

    private enum Kind { ROTATING, ACCUM, FIXED }

    private static final long FEE_RAW = 100_000_000L;
    private static final int DESC_MAX = 300;

    private Kind kind = Kind.ROTATING;
    private String typeLabel = "Rotating Pool";
    private int step;
    private String[] stepNames;

    private LinearLayout page, dots;
    private MaterialButton back, next;
    private ScrollView scroll;

    // basics
    private String name = "", description = "", visibility = "Public", country = "", state = "", city = "";
    private final List<String> rules = new ArrayList<>();
    private final List<Map<String, Object>> chatLinks = new ArrayList<>();
    private LocationHelper location;

    // rotating
    private String contribution = "";
    private int members = 5;
    private int cycleDays = 7;

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

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String type = getIntent() == null ? null : getIntent().getStringExtra("groupType");
        if ("Accumulative Funds".equals(type)) { kind = Kind.ACCUM; typeLabel = type; }
        else if ("Fixed Contribution".equals(type)) { kind = Kind.FIXED; typeLabel = type; }
        else { kind = Kind.ROTATING; typeLabel = "Rotating Pool"; }
        stepNames = new String[] {"Basics", kind == Kind.ROTATING ? "The rounds" : kind == Kind.ACCUM ? "Savings rules" : "The goal", "People", "Review"};
        location = new LocationHelper(this);
        country = firstOr(location.getCountryNames(), "Nigeria");
        if (location.getCountryNames().contains("Nigeria")) country = "Nigeria";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.background());

        dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, dp(12), 0, dp(4));
        root.addView(dots);

        scroll = new ScrollView(this);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(8), dp(16), dp(24));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));
        bar.setBackgroundColor(UiColors.surface());
        back = button("Back", true);
        back.setOnClickListener(v -> go(step - 1));
        bar.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        next = button("Next", false);
        next.setOnClickListener(v -> onNext());
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f);
        np.leftMargin = dp(8);
        bar.addView(next, np);
        root.addView(bar);

        AppBar.setContent(this, root, kind == Kind.ROTATING ? "New rotating group" : kind == Kind.ACCUM ? "New savings group" : "New goal group", null);
        go(0);
    }

    // ── navigation ─────────────────────────────────────────────────────────

    private void go(int to) {
        if (to < 0) { finish(); return; }
        step = Math.min(to, stepNames.length - 1);
        dots.removeAllViews();
        for (int i = 0; i < stepNames.length; i++) {
            TextView d = text(String.valueOf(i + 1), 12, true, i <= step ? 0xFF1A1208 : UiColors.textSecondary());
            d.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i <= step ? UiColors.accent() : (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
            d.setBackground(g);
            dots.addView(d, new LinearLayout.LayoutParams(dp(26), dp(26)));
            if (i < stepNames.length - 1) {
                View line = new View(this);
                line.setBackgroundColor(i < step ? UiColors.accent() : (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(2));
                lp.setMargins(dp(4), 0, dp(4), 0);
                dots.addView(line, lp);
            }
        }
        page.removeAllViews();
        page.addView(text(stepNames[step], 22, true, UiColors.textPrimary()));
        switch (step) {
            case 0: basics(); break;
            case 1: if (kind == Kind.ROTATING) rotating(); else if (kind == Kind.ACCUM) savings(); else goal(); break;
            case 2: people(); break;
            default: review(); break;
        }
        back.setText(step == 0 ? "Cancel" : "Back");
        next.setText(step == stepNames.length - 1 ? "Create group" : "Next");
        scroll.post(() -> scroll.scrollTo(0, 0));
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
        if (kind == Kind.ROTATING) {
            if (CircleUi.toRaw(contribution) <= 0) return "Enter how much each member pays every round.";
            if (members < 3 || members > 12) return "A rotating group needs 3 to 12 members.";
            return null;
        }
        if (kind == Kind.ACCUM) {
            if (allowLoans) {
                try { double r = Double.parseDouble(interest.trim()); if (r < 0 || r > 50) return "Interest must be between 0 and 50%."; }
                catch (Exception e) { return "Enter the interest rate, from 0 to 50%."; }
            }
            return null;
        }
        if (CircleUi.toRaw(target) <= 0) return "Enter the amount the group is aiming for.";
        if (purpose != 3 && !beneficiaryIsMe && !(beneficiary.startsWith("HV0x") && beneficiary.length() == 44)) return "Enter the full wallet address of who receives the money.";
        return null;
    }

    // ── step 1: basics ─────────────────────────────────────────────────────

    private void basics() {
        page.addView(text(kind == Kind.ROTATING ? "A circle where everyone pays in each round and one member takes the pot, until all have had a turn."
                : kind == Kind.ACCUM ? "A group where each member saves in their own line and earns interest set by the group."
                : "A group saving together toward one target, where money leaves only by the rule you pick.", 13, false, UiColors.textSecondary()));
        page.addView(label("Group name"));
        EditText n = edit("e.g. Market women ajo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, name);
        n.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(40)});
        n.addTextChangedListener(watch(s -> name = s));
        page.addView(n);

        page.addView(label("What is it about? (optional)"));
        EditText d = edit("A line or two members will read before they join", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, description);
        d.setMinLines(3);
        d.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(DESC_MAX)});
        d.addTextChangedListener(watch(s -> description = s));
        page.addView(d);

        page.addView(label("Who can find it"));
        page.addView(chips(new String[] {"Public", "Private"}, "Public".equals(visibility) ? 0 : 1, i -> visibility = i == 0 ? "Public" : "Private"));
        page.addView(hint("Public: anyone can find the group and ask to join. Private: only people you invite can see it."));

        page.addView(label("Where is it based"));
        Spinner sc = spinner(), ss = spinner(), sy = spinner();
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
        page.addView(sc); page.addView(ss); page.addView(sy);

        page.addView(label("Group rules (optional)"));
        LinearLayout ruleList = new LinearLayout(this);
        ruleList.setOrientation(LinearLayout.VERTICAL);
        page.addView(ruleList);
        paintRules(ruleList);
        EditText r = edit("e.g. Pay before 6pm on the due day", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, "");
        page.addView(r);
        MaterialButton addRule = button("Add rule", true);
        addRule.setOnClickListener(v -> {
            String t = r.getText().toString().trim();
            if (t.isEmpty()) return;
            if (rules.size() >= 12) { Toast.makeText(this, "Keep it to 12 rules.", Toast.LENGTH_SHORT).show(); return; }
            rules.add(t);
            r.setText("");
            paintRules(ruleList);
        });
        page.addView(addRule);

        page.addView(label("Outside chat (optional)"));
        LinearLayout chatList = new LinearLayout(this);
        chatList.setOrientation(LinearLayout.VERTICAL);
        page.addView(chatList);
        paintChats(chatList);
        MaterialButton addChat = button("Add a WhatsApp, Telegram or other chat link", true);
        addChat.setOnClickListener(v -> {
            if (chatLinks.size() >= 8) { Toast.makeText(this, "A group can have 8 chats.", Toast.LENGTH_SHORT).show(); return; }
            GroupChatSheet.pickOne(this, link -> runOnUiThread(() -> {
                Map<String, Object> copy = new HashMap<>();
                if (link != null) copy.putAll(link);
                chatLinks.add(copy);
                paintChats(chatList);
            }));
        });
        page.addView(addChat);
    }

    private void paintRules(LinearLayout box) {
        box.removeAllViews();
        for (int i = 0; i < rules.size(); i++) {
            final int at = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text((i + 1) + ".  " + rules.get(i), 13, false, UiColors.textPrimary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView x = text("Remove", 12, true, UiColors.accent());
            x.setPadding(dp(8), dp(8), dp(8), dp(8));
            x.setOnClickListener(v -> { rules.remove(at); paintRules(box); });
            row.addView(x);
            box.addView(row);
        }
    }

    private void paintChats(LinearLayout box) {
        box.removeAllViews();
        for (int i = 0; i < chatLinks.size(); i++) {
            final int at = i;
            Object label = chatLinks.get(i).get("label");
            Object url = chatLinks.get(i).get("url");
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text((label == null ? "Chat" : String.valueOf(label)) + (url == null ? "" : "  " + url), 13, false, UiColors.textPrimary()),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView x = text("Remove", 12, true, UiColors.accent());
            x.setPadding(dp(8), dp(8), dp(8), dp(8));
            x.setOnClickListener(v -> { chatLinks.remove(at); paintChats(box); });
            row.addView(x);
            box.addView(row);
        }
    }

    // ── step 2a: rotating ──────────────────────────────────────────────────

    private void rotating() {
        page.addView(label("Each member pays, every round (HARV)"));
        EditText c = edit("0.00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, contribution);
        final TextView summary = text("", 13, false, UiColors.textPrimary());
        c.addTextChangedListener(watch(s -> { contribution = s; refreshRotating(summary); }));
        page.addView(c);

        page.addView(label("How many members (3 to 12)"));
        page.addView(stepper(members, 3, 12, v -> { members = v; refreshRotating(summary); }));

        page.addView(label("How often is a round"));
        int sel = cycleDays == 1 ? 0 : cycleDays == 7 ? 1 : cycleDays == 14 ? 2 : cycleDays == 30 ? 3 : 4;
        final LinearLayout custom = new LinearLayout(this);
        custom.setOrientation(LinearLayout.VERTICAL);
        page.addView(chips(new String[] {"Daily", "Weekly", "2 weeks", "Monthly", "Other"}, sel, i -> {
            int[] days = {1, 7, 14, 30, 0};
            if (days[i] > 0) { cycleDays = days[i]; custom.removeAllViews(); }
            else {
                custom.removeAllViews();
                custom.addView(hint("Days between rounds, 1 to 30"));
                EditText ed = edit("e.g. 10", InputType.TYPE_CLASS_NUMBER, "");
                ed.addTextChangedListener(watch(s -> {
                    try { int d = Integer.parseInt(s.trim()); if (d >= 1 && d <= 30) cycleDays = d; } catch (Exception ignored) { }
                    refreshRotating(summary);
                }));
                custom.addView(ed);
            }
            refreshRotating(summary);
        }));
        page.addView(custom);

        LinearLayout card = card();
        card.addView(text("What this adds up to", 14, true, UiColors.textPrimary()));
        card.addView(summary);
        page.addView(card);
        page.addView(hint("Payout order is the order members join, and you join first. A member who misses a round has 24 hours' grace, then a small penalty is taken from their bond. "
                + "The group must fill within 72 hours or it is cancelled and every bond is returned."));
        refreshRotating(summary);
    }

    private void refreshRotating(TextView out) {
        long c = CircleUi.toRaw(contribution);
        if (c <= 0) { out.setText("Enter the amount to see the pot, the bond and how long it runs."); return; }
        long pot = c * (members - 1);
        out.setText("Pot each round:  " + CircleUi.harv(pot) + "  (the other " + (members - 1) + " members pay in)\n"
                + "Each member locks:  " + CircleUi.harv(pot) + "  when they join, as a bond. It comes back when the group finishes cleanly.\n"
                + "Runs for:  " + members + " rounds of " + cycleDays + (cycleDays == 1 ? " day" : " days") + "  =  about " + (members * cycleDays) + " days\n"
                + "Everyone gets one turn to take the pot.");
    }

    // ── step 2b: savings ───────────────────────────────────────────────────

    private void savings() {
        page.addView(text("Each member starts their own savings line inside the group: a daily amount, flexible or locked, a lump sum or changing deposits. A line lasts 30 days.", 13, false, UiColors.textSecondary()));

        page.addView(label("Lending"));
        SwitchMaterial loans = new SwitchMaterial(this);
        loans.setText("Members can borrow from the group's savings");
        loans.setTextColor(UiColors.textPrimary());
        loans.setChecked(allowLoans);
        page.addView(loans);
        final LinearLayout rate = new LinearLayout(this);
        rate.setOrientation(LinearLayout.VERTICAL);
        page.addView(rate);
        Runnable paintRate = () -> {
            rate.removeAllViews();
            if (!allowLoans) { rate.addView(hint("No lending. Savings stay in the group until members take them out.")); interest = "0"; return; }
            rate.addView(label("Interest on a loan (0 to 50%)"));
            EditText e = edit("e.g. 5", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, "0".equals(interest) ? "" : interest);
            e.addTextChangedListener(watch(s -> interest = s));
            rate.addView(e);
            rate.addView(hint("Borrowers repay with this interest. It is set once, now, and every member sees it before they save."));
        };
        loans.setOnCheckedChangeListener((v, on) -> { allowLoans = on; paintRate.run(); });
        paintRate.run();

        page.addView(label("When can savings be taken out"));
        page.addView(chips(new String[] {"Any time", "Only when the line ends"}, cycleEndOnly ? 1 : 0, i -> cycleEndOnly = i == 1));
        page.addView(hint("Any time: a member asks and you, as admin, approve the withdrawal. Only when the line ends: nothing can be taken out before its 30 days are up."));

        page.addView(label("Expected number of members (optional)"));
        EditText m = edit("e.g. 10", InputType.TYPE_CLASS_NUMBER, expectedMembers);
        m.addTextChangedListener(watch(s -> expectedMembers = s));
        page.addView(m);
        page.addView(label("Suggested daily saving, HARV (optional)"));
        EditText a = edit("e.g. 1", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, suggestedAmount);
        a.addTextChangedListener(watch(s -> suggestedAmount = s));
        page.addView(a);
        page.addView(hint("These two only help members decide. Each member chooses their own amount when they start a line."));
    }

    // ── step 2c: goal ──────────────────────────────────────────────────────

    private static final String[] PURPOSES = {"One-off purchase or event", "A cause the admin pays out", "A project in stages", "Emergency fund"};
    private static final String[] RULE_CODES = {"ALL_AT_ONCE_AT_GOAL", "ADMIN_TRIGGERED", "MILESTONE_BASED", "EMERGENCY_GRANT"};
    private static final String[] RULE_NAMES = {"All at once at goal", "Admin-Triggered", "Released in Milestones", "Emergency Grants"};

    private void goal() {
        page.addView(label("What is the group saving for"));
        final LinearLayout explain = new LinearLayout(this);
        explain.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout bene = new LinearLayout(this);
        bene.setOrientation(LinearLayout.VERTICAL);
        page.addView(chips(PURPOSES, purpose, i -> { purpose = i; paintGoalRule(explain, bene); }, true));
        page.addView(explain);

        page.addView(label("Target amount (HARV)"));
        EditText t = edit("0.00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, target);
        t.addTextChangedListener(watch(s -> target = s));
        page.addView(t);

        page.addView(bene);
        paintGoalRule(explain, bene);

        page.addView(label("Expected number of members (optional)"));
        EditText m = edit("e.g. 10", InputType.TYPE_CLASS_NUMBER, expectedMembers);
        m.addTextChangedListener(watch(s -> expectedMembers = s));
        page.addView(m);
        page.addView(label("Suggested deposit per member, HARV (optional)"));
        EditText a = edit("e.g. 20", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, suggestedAmount);
        a.addTextChangedListener(watch(s -> suggestedAmount = s));
        page.addView(a);
        page.addView(hint("Deposits close when the target is reached. Members can deposit any amount until then."));
    }

    private void paintGoalRule(LinearLayout explain, LinearLayout bene) {
        explain.removeAllViews();
        String why = purpose == 0 ? "When the target is reached the whole pot is sent, automatically and with no vote, to the wallet you name below. Members see that wallet before they deposit."
                : purpose == 1 ? "Nothing is sent when the target is reached. You ask for a payout and the members vote. It is paid only if the vote passes."
                : purpose == 2 ? "Money is released in parts. You ask for each part and say what it is for, and the members vote on every one."
                : "Nobody is named in advance. A member asks for a grant, says why, and the group votes. If it passes, that member is paid.";
        LinearLayout c = card();
        c.addView(text(RULE_NAMES[purpose], 14, true, UiColors.textPrimary()));
        c.addView(text(why, 12.5f, false, UiColors.textSecondary()));
        explain.addView(c);

        bene.removeAllViews();
        if (purpose == 3) return;
        bene.addView(label(purpose == 0 ? "Who receives the pot" : purpose == 1 ? "Who is paid if the vote passes" : "Who receives each approved part"));
        bene.addView(chips(new String[] {"Me, the admin", "Another wallet"}, beneficiaryIsMe ? 0 : 1, i -> { beneficiaryIsMe = i == 0; paintGoalRule(explain, bene); }));
        if (beneficiaryIsMe) {
            LinearLayout warn = card();
            warn.addView(text("Members will see that the money goes to you.", 13, true, UiColors.textPrimary()));
            warn.addView(text("Say so in the description so people know who they are paying and why. Choose another wallet, such as a supplier or a venue, if the money is not for you.", 12.5f, false, UiColors.textSecondary()));
            bene.addView(warn);
        } else {
            EditText w = edit("HV0x… full wallet address", InputType.TYPE_CLASS_TEXT, beneficiary);
            w.addTextChangedListener(watch(s -> beneficiary = s.trim()));
            bene.addView(w);
        }
    }

    // ── step 3: people ─────────────────────────────────────────────────────

    private void people() {
        page.addView(text("Invite people now or later. They get the group's rules and decide for themselves.", 13, false, UiColors.textSecondary()));
        page.addView(PeoplePicker.section(this, invite));

        page.addView(label("Joining"));
        if (kind == Kind.ROTATING) {
            page.addView(hint("Anyone can join until the group is full. Joining locks their bond, so there is no approval step."));
        } else {
            SwitchMaterial ap = new SwitchMaterial(this);
            ap.setText("New members need approval");
            ap.setTextColor(UiColors.textPrimary());
            ap.setChecked(requireApproval);
            page.addView(ap);
            final LinearLayout how = new LinearLayout(this);
            how.setOrientation(LinearLayout.VERTICAL);
            page.addView(how);
            Runnable paint = () -> {
                how.removeAllViews();
                if (!requireApproval) return;
                how.addView(chips(new String[] {"Admin decides", "Members vote"}, memberVote ? 1 : 0, i -> memberVote = i == 1));
                how.addView(hint(memberVote ? "Members vote on each request. 60% must agree." : "You approve or decline each request."));
            };
            ap.setOnCheckedChangeListener((v, on) -> { requireApproval = on; paint.run(); });
            paint.run();
        }
        SwitchMaterial mi = new SwitchMaterial(this);
        mi.setText("Members can invite people");
        mi.setTextColor(UiColors.textPrimary());
        mi.setChecked(membersCanInvite);
        mi.setOnCheckedChangeListener((v, on) -> membersCanInvite = on);
        page.addView(mi);
        SwitchMaterial il = new SwitchMaterial(this);
        il.setText("Anyone with the link can ask to join");
        il.setTextColor(UiColors.textPrimary());
        il.setChecked(inviteViaLink);
        il.setOnCheckedChangeListener((v, on) -> inviteViaLink = on);
        page.addView(il);
    }

    // ── step 4: review ─────────────────────────────────────────────────────

    private void review() {
        LinearLayout c = card();
        c.addView(text(name.trim().isEmpty() ? "Your group" : name.trim(), 17, true, UiColors.textPrimary()));
        c.addView(text(visibility + "  ·  " + city + ", " + state, 12.5f, false, UiColors.textSecondary()));
        page.addView(c);
        LinearLayout t = card();
        t.addView(text("The terms", 14, true, UiColors.textPrimary()));
        for (String[] row : termRows()) t.addView(pair(row[0], row[1]));
        page.addView(t);
        LinearLayout cost = card();
        cost.addView(text("What it costs you", 14, true, UiColors.textPrimary()));
        cost.addView(pair("Network fee", CircleUi.harv(FEE_RAW)));
        cost.addView(pair("Locked now", "Nothing"));
        cost.addView(pair("Group wallet", "Made and held by the network. This phone never creates or holds one."));
        if (kind == Kind.ROTATING) cost.addView(pair("Then you join", "You join first and lock the same bond as everyone else."));
        page.addView(cost);
        if (invite.count() > 0) page.addView(hint("Invitations will go to " + invite.count() + (invite.count() == 1 ? " person." : " people.")));
        String why = null;
        if (kind == Kind.FIXED && purpose != 3 && beneficiaryIsMe) why = "The money will go to your wallet. Members are shown that before they deposit.";
        if (why != null) page.addView(hint(why));
    }

    private List<String[]> termRows() {
        List<String[]> rows = new ArrayList<>();
        if (kind == Kind.ROTATING) {
            long c = CircleUi.toRaw(contribution);
            rows.add(new String[] {"Each member pays", CircleUi.harv(c) + " every " + (cycleDays == 1 ? "day" : cycleDays + " days")});
            rows.add(new String[] {"Pot each round", CircleUi.harv(c * (members - 1))});
            rows.add(new String[] {"Members", String.valueOf(members)});
            rows.add(new String[] {"Bond each member locks", CircleUi.harv(c * (members - 1))});
            rows.add(new String[] {"Payout order", "Order of joining"});
        } else if (kind == Kind.ACCUM) {
            rows.add(new String[] {"Lending", allowLoans ? "Yes, " + interest.trim() + "% interest" : "No"});
            rows.add(new String[] {"Savings can be taken out", cycleEndOnly ? "Only when the 30-day line ends" : "Any time, with your approval"});
        } else {
            rows.add(new String[] {"For", PURPOSES[purpose]});
            rows.add(new String[] {"Target", CircleUi.harv(CircleUi.toRaw(target))});
            rows.add(new String[] {"How money leaves", RULE_NAMES[purpose]});
            if (purpose != 3) rows.add(new String[] {"Receiver", beneficiaryIsMe ? "You, the admin" : beneficiary});
        }
        if (kind != Kind.ROTATING) rows.add(new String[] {"Joining", requireApproval ? (memberVote ? "Members vote" : "Admin approves") : "Open"});
        return rows;
    }

    // ── create ─────────────────────────────────────────────────────────────

    private void create() {
        final String wallet = WalletManager.getActiveWalletAddress(this);
        if (wallet == null || wallet.isEmpty() || WalletManager.loadPrivateKey(this, wallet) == null) {
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("You need a wallet first")
                    .setMessage("The network fee is paid from your wallet. Create or restore one, then try again.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        List<String[]> rows = new ArrayList<>(termRows());
        rows.add(new String[] {"Network fee", CircleUi.harv(FEE_RAW)});
        rows.add(new String[] {"Locked now", "Nothing"});
        rows.add(new String[] {"Group wallet", "Made and held by the network."});
        PaymentConfirmSheet.reviewThenAuthorize(this, "Create this group", CircleUi.harv(FEE_RAW), rows, () -> {
            final ProgressDialog pd = ProgressDialog.show(this, null, "Creating on the network…", true, false);
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
                NodeGroupClient.createCircle(getApplicationContext(), wallet, CircleUi.toRaw(contribution), members, cycleDays * 24L, done);
            } else if (kind == Kind.ACCUM) {
                double pct = allowLoans ? parse(interest) : 0.0;
                NodeGroupClient.createAccumGroup(getApplicationContext(), wallet, Math.round(pct * 100.0), cycleEndOnly ? "CYCLE_END_ONLY" : "ANYTIME", done);
            } else {
                String rule = RULE_CODES[purpose];
                String who = purpose == 3 ? "" : beneficiaryIsMe ? wallet : beneficiary;
                NodeGroupClient.createFixedGroup(getApplicationContext(), wallet, CircleUi.toRaw(target), rule, who, 0L, name.trim(), description.trim(), done);
            }
        });
    }

    /** The group's profile in Firestore: names, photos, rules, chat links. The network holds the money; this is what people read. */
    private void save(String escrow) {
        com.harvana.app.trust.AppActivity.note(this, "GROUP", "Created a group");
        final String id = java.util.UUID.randomUUID().toString();
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> g = new HashMap<>();
        g.put("groupId", id);
        g.put("firestoreDocId", id);
        g.put("timestamp", String.valueOf(System.currentTimeMillis()));
        g.put("groupName", name.trim());
        g.put("groupType", typeLabel);
        g.put("groupDescription", description.trim());
        g.put("frequency", kind == Kind.ROTATING ? (cycleDays == 1 ? "Daily" : cycleDays == 7 ? "Weekly" : cycleDays == 14 ? "Bi-weekly" : cycleDays == 30 ? "Monthly" : "Every " + cycleDays + " days") : "Daily");
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
            g.put("loanInterestPeriod", "MONTHLY");
            g.put("withdrawalRule", cycleEndOnly ? "Cycle-End Only" : "Anytime");
        } else {
            g.put("contributionAmount", parse(suggestedAmount));
            g.put("totalSlots", (int) parse(expectedMembers));
            g.put("targetGoalAmount", parse(target));
            g.put("disbursementRule", RULE_NAMES[purpose]);
        }
        if (pendingNodeId != null) {
            g.put("nodeGroupId", pendingNodeId);
            g.put("family", GroupKinds.familyOf(pendingNodeId));
            if (pendingNodeId.startsWith("CIRC-")) g.put("circleId", pendingNodeId);
            g.put("onChain", true);
        }
        if (!chatLinks.isEmpty()) g.put("chatLinks", new ArrayList<>(chatLinks));

        final ProgressDialog saving = ProgressDialog.show(this, null, "Saving your group…", true, false);
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
    private interface Count { void on(int v); }

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

    private Spinner spinner() {
        Spinner s = new Spinner(this);
        s.setMinimumHeight(dp(48));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        s.setLayoutParams(lp);
        return s;
    }

    private View chips(String[] options, int selected, Pick pick) { return chips(options, selected, pick, false); }

    /** A row of choices, one picked. {@code column} stacks them for long labels. */
    private View chips(String[] options, int selected, Pick pick, boolean column) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(column ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        final MaterialButton[] all = new MaterialButton[options.length];
        for (int i = 0; i < options.length; i++) {
            final int at = i;
            MaterialButton b = button(options[i], i != selected);
            b.setTextSize(12.5f);
            b.setOnClickListener(v -> {
                for (int k = 0; k < all.length; k++) styleChip(all[k], k == at);
                pick.on(at);
            });
            all[i] = b;
            LinearLayout.LayoutParams lp = column ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), dp(4), dp(2), dp(4));
            row.addView(b, lp);
        }
        return row;
    }

    private void styleChip(MaterialButton b, boolean on) {
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(on ? UiColors.accent() : 0x00000000));
        b.setTextColor(on ? 0xFF1A1208 : UiColors.accent());
    }

    private View stepper(int value, int min, int max, Count c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final TextView n = text(String.valueOf(value), 22, true, UiColors.textPrimary());
        n.setGravity(Gravity.CENTER);
        final int[] cur = {value};
        MaterialButton minus = button("−", true), plus = button("+", true);
        minus.setOnClickListener(v -> { if (cur[0] > min) { cur[0]--; n.setText(String.valueOf(cur[0])); c.on(cur[0]); } });
        plus.setOnClickListener(v -> { if (cur[0] < max) { cur[0]++; n.setText(String.valueOf(cur[0])); c.on(cur[0]); } });
        row.addView(minus);
        row.addView(n, new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(plus);
        return row;
    }

    private EditText edit(String hint, int inputType, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setText(value);
        e.setTextColor(UiColors.textPrimary());
        e.setHintTextColor(UiColors.textTertiary());
        return e;
    }

    private TextView label(String s) {
        TextView t = text(s, 13, true, UiColors.textPrimary());
        t.setPadding(0, dp(16), 0, dp(2));
        return t;
    }

    private TextView hint(String s) {
        TextView t = text(s, 12, false, UiColors.textSecondary());
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private View pair(String k, String v) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(5), 0, dp(5));
        row.addView(text(k, 13, false, UiColors.textSecondary()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView value = text(v, 13, true, UiColors.textPrimary());
        value.setGravity(Gravity.END);
        row.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
        return row;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(14);
        l.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiColors.surface());
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        l.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        l.setLayoutParams(lp);
        return l;
    }

    private MaterialButton button(String label, boolean outlined) {
        MaterialButton b = outlined ? new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle) : new MaterialButton(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
