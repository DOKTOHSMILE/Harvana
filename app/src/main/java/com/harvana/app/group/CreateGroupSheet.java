package com.harvana.app.group;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.CreateAccumulativeGroupActivity;
import com.harvana.app.CreateFixedGroupActivity;
import com.harvana.app.CreateRotatingGroupActivity;
import com.harvana.app.R;

/**
 * The single "Create a group" entry. Pick what the group is FOR; each choice opens its own screen,
 * with only the fields that belong to that type:
 *  - Rotating circle  -> CreateRotatingGroupActivity
 *  - Accumulative     -> CreateAccumulativeGroupActivity
 *  - Fixed goal       -> CreateFixedGroupActivity
 */
public final class CreateGroupSheet {

    private CreateGroupSheet() {}

    public static void show(Context ctx) {
        final BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        final float d = ctx.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (20 * d), (int) (18 * d), (int) (20 * d), (int) (24 * d));

        root.addView(text(ctx, "Create a group", 20, R.color.colorTextPrimary, true));
        TextView sub = text(ctx, "What will this group be for?", 13.5f, R.color.colorTextSecondary, false);
        sub.setPadding(0, (int) (2 * d), 0, (int) (6 * d));
        root.addView(sub);

        root.addView(option(ctx, R.drawable.ic_type_circle, "Rotating group",
                "Everyone pays the same amount each round. Each round one member takes the whole pot, until everyone has had a turn. Best for getting a large sum at once.",
                "3–12 members  ·  Runs on the network  ·  Bond-backed", v -> {
                    dialog.dismiss();
                    open(ctx, CreateRotatingGroupActivity.class);
                }));

        root.addView(option(ctx, R.drawable.ic_type_savings, "Accumulative savings",
                "Each member saves at their own pace in a personal savings line and earns interest set by the group. Best for building savings over time.",
                "Flexible or locked  ·  Daily saving  ·  Interest", v -> {
                    dialog.dismiss();
                    open(ctx, CreateAccumulativeGroupActivity.class);
                }));

        root.addView(option(ctx, R.drawable.ic_type_goal, "Fixed goal",
                "The group saves together toward one target, such as a project or an emergency fund. Money leaves only when the group votes. Best for shared goals.",
                "Target amount  ·  Group votes  ·  Milestones", v -> {
                    dialog.dismiss();
                    open(ctx, CreateFixedGroupActivity.class);
                }));

        TextView note = text(ctx, "Each group type has its own screen. Rotating groups keep their money on the Harvana network.", 11.5f, R.color.colorTextTertiary, false);
        note.setPadding(0, (int) (12 * d), 0, 0);
        root.addView(note);

        dialog.setContentView(root);
        dialog.show();
    }

    private static void open(Context ctx, Class<?> activity) {
        Intent i = new Intent(ctx, activity);
        ctx.startActivity(i);
    }

    private static View option(Context ctx, int iconRes, String title, String body, String facts, View.OnClickListener l) {
        final float d = ctx.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.bg_quick_action);
        row.setPadding((int) (14 * d), (int) (14 * d), (int) (14 * d), (int) (14 * d));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(title + ". " + body);
        row.setOnClickListener(l);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = (int) (10 * d);
        row.setLayoutParams(rlp);

        ImageView icon = new ImageView(ctx);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(ctx, R.color.colorTextOnAccent));
        icon.setBackgroundResource(R.drawable.bg_quick_action_icon);
        int pad = (int) (10 * d);
        icon.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams((int) (44 * d), (int) (44 * d));
        ilp.rightMargin = (int) (14 * d);
        row.addView(icon, ilp);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(ctx, title, 16, R.color.colorTextPrimary, true));
        TextView b = text(ctx, body, 13, R.color.colorTextSecondary, false);
        b.setPadding(0, (int) (2 * d), 0, (int) (6 * d));
        col.addView(b);
        col.addView(text(ctx, facts, 11.5f, R.color.colorAccentStrong, true));
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private static TextView text(Context ctx, String s, float sp, int colorRes, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(ContextCompat.getColor(ctx, colorRes));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.START);
        return t;
    }
}
