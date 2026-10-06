package com.harvana.app.group;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.utils.UiColors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * External chats for a group. The conversation stays in WhatsApp, Telegram,
 * or a Facebook group. Harvana only keeps the links the admin chose.
 */
public final class GroupChatSheet {

    private static final int MAX_LINKS = 8;
    private static final String[] KINDS = {"WHATSAPP", "TELEGRAM", "FACEBOOK", "OTHER"};

    private GroupChatSheet() {}

    public interface OnLink {
        void onLink(Map<String, Object> link);
    }

    /** One link, not saved yet. Used while a group is still being created. */
    public static void pickOne(Activity activity, OnLink onLink) {
        askAdd(activity, null, new ArrayList<Link>(), onLink);
    }

    public static void show(Activity activity, String groupId) {
        if (activity == null || groupId == null || groupId.isEmpty()) return;
        GroupFirestoreManager.getInstance(activity).getGroup(groupId, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> data) {
                String uid = FirebaseAuth.getInstance().getCurrentUser() == null
                        ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();
                String creator = str(data == null ? null : data.get("creatorId"));
                boolean admin = !uid.isEmpty() && uid.equals(creator);
                List<Link> links = read(data);
                activity.runOnUiThread(() -> present(activity, groupId, admin, links));
            }
            @Override public void onError(String error) {
                activity.runOnUiThread(() -> toast(activity, "Could not load the group chats."));
            }
        });
    }

    private static void present(Activity activity, String groupId, boolean admin, List<Link> links) {
        if (activity.isFinishing()) return;
        BottomSheetDialog sheet = new com.harvana.app.ui.AppSheet(activity, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        float d = activity.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, (int) (28 * d));
        curve(root, sheet, (int) (22 * d));

        TextView title = text(activity, "Group chats", 18, true, UiColors.textPrimary());
        root.addView(title);
        TextView sub = text(activity, "These open outside Harvana. The conversation is not inside the app.", 13, false, UiColors.textSecondary());
        sub.setPadding(0, (int) (6 * d), 0, (int) (14 * d));
        root.addView(sub);

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        if (links.isEmpty()) {
            TextView empty = text(activity, admin
                    ? "No chats yet. Add WhatsApp, Telegram, or a Facebook group so members can talk outside the app."
                    : "The admin has not added a chat yet.", 14, false, UiColors.textSecondary());
            empty.setPadding(0, 0, 0, (int) (12 * d));
            list.addView(empty);
        }
        for (Link link : links) {
            list.addView(row(activity, sheet, groupId, admin, links, link, d));
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(list);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scroll.setLayoutParams(scrollLp);
        root.addView(scroll);

        if (admin) {
            TextView add = text(activity, links.size() >= MAX_LINKS ? "This group already has " + MAX_LINKS + " chats" : "Add a chat", 15, true, UiColors.onAccent());
            add.setGravity(Gravity.CENTER);
            add.setPadding(0, (int) (14 * d), 0, (int) (14 * d));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(14 * d);
            bg.setColor(links.size() >= MAX_LINKS ? UiColors.textTertiary() : UiColors.accent());
            add.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (16 * d);
            add.setLayoutParams(lp);
            if (links.size() < MAX_LINKS) {
                add.setOnClickListener(v -> {
                    sheet.dismiss();
                    askAdd(activity, groupId, links, null);
                });
            }
            root.addView(add);
        }
        sheet.setContentView(root);
        sheet.show();
    }

    private static View row(Activity activity, BottomSheetDialog sheet, String groupId, boolean admin, List<Link> links, Link link, float d) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (10 * d), 0, (int) (10 * d));

        TextView mark = text(activity, link.mark(), 14, true, UiColors.onAccent());
        mark.setGravity(Gravity.CENTER);
        GradientDrawable bubble = new GradientDrawable();
        bubble.setShape(GradientDrawable.OVAL);
        bubble.setColor(UiColors.accent());
        mark.setBackground(bubble);
        int size = (int) (36 * d);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(size, size);
        markLp.setMarginEnd((int) (12 * d));
        mark.setLayoutParams(markLp);
        row.addView(mark);

        LinearLayout words = new LinearLayout(activity);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        words.addView(text(activity, link.label, 15, true, UiColors.textPrimary()));
        words.addView(text(activity, link.kindName(), 12, false, UiColors.textSecondary()));
        row.addView(words);

        row.setOnClickListener(v -> confirmLeave(activity, link));

        if (admin) {
            TextView remove = text(activity, "Remove", 13, true, UiColors.error());
            remove.setPadding((int) (12 * d), (int) (8 * d), 0, (int) (8 * d));
            remove.setOnClickListener(v -> {
                v.setClickable(false);
                new com.harvana.app.ui.SheetDialog.Builder(activity)
                        .setTitle("Remove this chat?")
                        .setMessage(link.label + " will no longer show for members.")
                        .setNegativeButton("Keep", (d1, w) -> v.setClickable(true))
                        .setPositiveButton("Remove", (d1, w) -> {
                            List<Link> next = new ArrayList<>();
                            for (Link each : links) if (!each.id.equals(link.id)) next.add(each);
                            save(activity, groupId, next, () -> {
                                sheet.dismiss();
                                present(activity, groupId, true, next);
                            });
                        })
                        .setOnCancelListener(d1 -> v.setClickable(true))
                        .show();
            });
            row.addView(remove);
        }
        return row;
    }

    private static void confirmLeave(Activity activity, Link link) {
        new com.harvana.app.ui.SheetDialog.Builder(activity)
                .setTitle("You are leaving Harvana")
                .setMessage("This opens " + link.kindName() + " outside the app so the group can talk there. Harvana cannot see that conversation.")
                .setNegativeButton("Stay", null)
                .setPositiveButton("Continue", (d, w) -> open(activity, link.url))
                .show();
    }

    private static void open(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            toast(activity, "No app on this phone can open that link.");
        }
    }

    private static void askAdd(Activity activity, String groupId, List<Link> existing, OnLink onLink) {
        if (activity.isFinishing()) return;
        BottomSheetDialog sheet = new com.harvana.app.ui.AppSheet(activity, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        float d = activity.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, (int) (28 * d));
        curve(root, sheet, (int) (22 * d));

        root.addView(text(activity, "Add a chat", 18, true, UiColors.textPrimary()));
        TextView hint = text(activity, "Paste the group invite. Members will be warned before they leave Harvana.", 13, false, UiColors.textSecondary());
        hint.setPadding(0, (int) (6 * d), 0, (int) (14 * d));
        root.addView(hint);

        final String[] kind = {KINDS[0]};
        EditText label = field(activity, "Name members will see");
        EditText url = field(activity, urlHint(kind[0]));
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        LinearLayout kinds = new LinearLayout(activity);
        kinds.setOrientation(LinearLayout.HORIZONTAL);
        TextView[] chips = new TextView[KINDS.length];
        for (int i = 0; i < KINDS.length; i++) {
            final int index = i;
            TextView chip = text(activity, shortKind(KINDS[i]), 12, true, UiColors.textPrimary());
            chip.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart((int) (8 * d));
            chip.setLayoutParams(lp);
            chips[i] = chip;
            chip.setOnClickListener(v -> {
                kind[0] = KINDS[index];
                paintChips(chips, kind[0], d);
                url.setHint(urlHint(kind[0]));
            });
            kinds.addView(chip);
        }
        paintChips(chips, kind[0], d);
        HorizontalScrollView scroller = new HorizontalScrollView(activity);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.addView(kinds);
        root.addView(scroller);

        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldLp.topMargin = (int) (12 * d);
        label.setLayoutParams(fieldLp);
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        urlLp.topMargin = (int) (8 * d);
        url.setLayoutParams(urlLp);
        root.addView(label);
        root.addView(url);

        TextView save = text(activity, "Save chat", 15, true, UiColors.onAccent());
        save.setGravity(Gravity.CENTER);
        save.setPadding(0, (int) (14 * d), 0, (int) (14 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14 * d);
        bg.setColor(UiColors.accent());
        save.setBackground(bg);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveLp.topMargin = (int) (16 * d);
        save.setLayoutParams(saveLp);
        save.setOnClickListener(v -> {
            String cleaned = cleanUrl(kind[0], url.getText().toString());
            if (cleaned == null) {
                toast(activity, "Use an https link for " + kindName(kind[0]) + ".");
                return;
            }
            String name = label.getText().toString().trim();
            if (name.isEmpty()) name = kindName(kind[0]);
            if (name.length() > 40) name = name.substring(0, 40);
            List<Link> next = new ArrayList<>(existing);
            Link made = new Link(String.valueOf(System.currentTimeMillis()), kind[0], name, cleaned);
            next.add(made);
            if (onLink != null) {
                sheet.dismiss();
                onLink.onLink(made.toMap());
                return;
            }
            v.setEnabled(false);
            save(activity, groupId, next, () -> {
                sheet.dismiss();
                present(activity, groupId, true, next);
            });
        });
        root.addView(save);
        sheet.setContentView(root);
        sheet.show();
    }

    private static void paintChips(TextView[] chips, String selected, float d) {
        for (int i = 0; i < chips.length; i++) {
            boolean on = KINDS[i].equals(selected);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(20 * d);
            bg.setColor(on ? UiColors.accent() : UiColors.background());
            chips[i].setBackground(bg);
            chips[i].setTextColor(on ? UiColors.onAccent() : UiColors.textSecondary());
        }
    }

    private static void save(Activity activity, String groupId, List<Link> links, Runnable after) {
        List<Map<String, Object>> raw = new ArrayList<>();
        for (Link link : links) raw.add(link.toMap());
        Map<String, Object> patch = new HashMap<>();
        patch.put("chatLinks", raw);
        GroupFirestoreManager.getInstance(activity).updateGroupFields(groupId, patch, new GroupFirestoreManager.SimpleCallback() {
            @Override public void onSuccess() {
                activity.runOnUiThread(after);
            }
            @Override public void onError(String error) {
                activity.runOnUiThread(() -> toast(activity, "Only the admin can change the group chats."));
            }
        });
    }

    static List<Link> read(Map<String, Object> group) {
        List<Link> out = new ArrayList<>();
        if (group == null) return out;
        Object raw = group.get("chatLinks");
        if (!(raw instanceof List)) return out;
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) item;
            String url = str(map.get("url"));
            String kind = str(map.get("kind")).toUpperCase(Locale.US);
            if (cleanUrl(kind, url) == null) continue;
            String id = str(map.get("id"));
            if (id.isEmpty()) id = url;
            String label = str(map.get("label"));
            if (label.isEmpty()) label = kindName(kind);
            out.add(new Link(id, kind, label, url.trim()));
            if (out.size() >= MAX_LINKS) break;
        }
        return out;
    }

    static String cleanUrl(String kind, String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.length() < 12 || value.length() > 300) return null;
        if (!value.toLowerCase(Locale.US).startsWith("https://")) return null;
        Uri uri = Uri.parse(value);
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null) return null;
        host = host.toLowerCase(Locale.US);
        if (host.startsWith("www.")) host = host.substring(4);
        if (!hostAllowed(kind, host)) return null;
        return value;
    }

    private static boolean hostAllowed(String kind, String host) {
        if ("WHATSAPP".equals(kind)) {
            return host.equals("chat.whatsapp.com") || host.equals("wa.me") || host.equals("whatsapp.com") || host.equals("api.whatsapp.com");
        }
        if ("TELEGRAM".equals(kind)) {
            return host.equals("t.me") || host.equals("telegram.me") || host.equals("telegram.org");
        }
        if ("FACEBOOK".equals(kind)) {
            return host.equals("facebook.com") || host.equals("m.facebook.com") || host.equals("fb.com") || host.equals("fb.me") || host.endsWith(".facebook.com");
        }
        return "OTHER".equals(kind) && host.contains(".");
    }

    private static String shortKind(String kind) {
        if ("WHATSAPP".equals(kind)) return "WhatsApp";
        if ("TELEGRAM".equals(kind)) return "Telegram";
        if ("FACEBOOK".equals(kind)) return "Facebook";
        return "Other";
    }

    private static String urlHint(String kind) {
        if ("WHATSAPP".equals(kind)) return "https://chat.whatsapp.com/…";
        if ("TELEGRAM".equals(kind)) return "https://t.me/…";
        if ("FACEBOOK".equals(kind)) return "https://facebook.com/groups/…";
        return "https://…";
    }

    private static String kindName(String kind) {
        if ("WHATSAPP".equals(kind)) return "WhatsApp";
        if ("TELEGRAM".equals(kind)) return "Telegram";
        if ("FACEBOOK".equals(kind)) return "Facebook group";
        return "Other link";
    }

    private static void curve(LinearLayout root, BottomSheetDialog sheet, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(UiColors.surface());
        bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        root.setBackground(bg);
        sheet.setOnShowListener(dialog -> {
            View panel = ((BottomSheetDialog) dialog).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (panel != null) panel.setBackgroundColor(0x00000000);
        });
    }

    private static EditText field(Activity activity, String hint) {
        EditText box = new EditText(activity);
        box.setHint(hint);
        box.setSingleLine(true);
        box.setTextColor(UiColors.textPrimary());
        box.setHintTextColor(UiColors.textTertiary());
        box.setBackgroundColor(UiColors.background());
        int p = (int) (12 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(p, p, p, p);
        return box;
    }

    private static TextView text(Activity activity, String value, int sp, boolean bold, int color) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private static void toast(Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static final class Link {
        final String id;
        final String kind;
        final String label;
        final String url;

        Link(String id, String kind, String label, String url) {
            this.id = id;
            this.kind = kind;
            this.label = label;
            this.url = url;
        }

        String kindName() { return GroupChatSheet.kindName(kind); }

        String mark() {
            if ("WHATSAPP".equals(kind)) return "W";
            if ("TELEGRAM".equals(kind)) return "T";
            if ("FACEBOOK".equals(kind)) return "F";
            return "·";
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("kind", kind);
            map.put("label", label);
            map.put("url", url);
            return map;
        }
    }
}
