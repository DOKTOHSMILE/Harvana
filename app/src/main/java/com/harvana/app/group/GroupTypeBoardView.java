package com.harvana.app.group;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.harvana.app.R;

/** Paints a {@link GroupTypeBoard.Board} as tappable date boxes. */
public final class GroupTypeBoardView {

    public interface Click {
        void onCell(GroupTypeBoard.Cell cell);
    }

    private GroupTypeBoardView() {}

    public static void fill(LinearLayout host, GroupTypeBoard.Board board, Click click) {
        if (host == null) return;
        host.removeAllViews();
        if (board == null || board.cells.isEmpty()) return;
        Context c = host.getContext();
        int cols = Math.max(1, board.columns);
        LinearLayout row = null;
        for (int i = 0; i < board.cells.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                host.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            final GroupTypeBoard.Cell cell = board.cells.get(i);
            boolean dense = cols >= 6;
            LinearLayout box = cellBox(c, cell, dense);
            int height = dp(c, dense ? 34 : 46);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
            int m = dp(c, dense ? 2 : 3);
            lp.setMargins(m, m, m, m);
            box.setOnClickListener(v -> {
                if (click != null) click.onCell(cell);
            });
            row.addView(box, lp);
        }
        if (row != null) {
            int rem = board.cells.size() % cols;
            if (rem != 0) {
                for (int i = rem; i < cols; i++) {
                    View spacer = new View(c);
                    row.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));
                }
            }
        }
    }

    public static void showDetail(Context c, GroupTypeBoard.Cell cell) {
        if (c == null || cell == null) return;
        new AlertDialog.Builder(c)
                .setTitle(cell.title.isEmpty() ? "Payment" : cell.title)
                .setMessage(cell.body)
                .setPositiveButton("OK", null)
                .show();
    }

    private static LinearLayout cellBox(Context c, GroupTypeBoard.Cell cell, boolean dense) {
        int bg;
        int fg;
        switch (cell.state) {
            case GroupTypeBoard.PAID:
                bg = color(c, R.color.colorSuccess);
                fg = 0xFFFFFFFF;
                break;
            case GroupTypeBoard.PENDING:
                bg = color(c, R.color.colorWarning);
                fg = color(c, R.color.colorTextPrimary);
                break;
            case GroupTypeBoard.MISSED:
                bg = color(c, R.color.colorError);
                fg = 0xFFFFFFFF;
                break;
            case GroupTypeBoard.TODAY:
                bg = color(c, R.color.colorInfo);
                fg = 0xFFFFFFFF;
                break;
            case GroupTypeBoard.TURN:
                bg = color(c, R.color.colorAccent);
                fg = color(c, R.color.colorTextPrimary);
                break;
            default:
                bg = color(c, R.color.colorSurfaceAlt);
                fg = color(c, R.color.colorTextPrimary);
                break;
        }
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, dense ? 6 : 8));
        d.setColor(bg);
        box.setBackground(d);
        box.setClickable(true);
        box.setFocusable(true);

        TextView top = new TextView(c);
        top.setText(cell.top);
        top.setTextSize(dense ? 11 : 13);
        top.setTypeface(null, Typeface.BOLD);
        top.setTextColor(fg);
        top.setGravity(Gravity.CENTER);
        box.addView(top);

        if (cell.bottom != null && !cell.bottom.isEmpty()) {
            TextView bottom = new TextView(c);
            bottom.setText(cell.bottom);
            bottom.setTextSize(dense ? 8 : 9);
            bottom.setTextColor(fg);
            bottom.setGravity(Gravity.CENTER);
            box.addView(bottom);
        }
        return box;
    }

    private static int color(Context c, int res) {
        return androidx.core.content.ContextCompat.getColor(c, res);
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
