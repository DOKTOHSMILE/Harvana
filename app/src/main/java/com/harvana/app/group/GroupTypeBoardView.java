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

    /** Which missed days the member has picked to pay back. A tap on a red box toggles it; a long press starts picking. */
    public static final class Selection {
        public final java.util.TreeSet<String> picked = new java.util.TreeSet<>();
        /** Called after every change so the screen can update its "Pay N days" bar. */
        public Runnable onChange;
        void toggle(String iso) { if (!picked.remove(iso)) picked.add(iso); }
        /** Days ahead are paid in order from today, so picking one picks every unpaid day up to it; unpicking one drops the later ones. */
        void toggleForward(GroupTypeBoard.Board board, GroupTypeBoard.Cell cell) {
            if (picked.contains(cell.isoDate)) {
                for (GroupTypeBoard.Cell c : board.cells) if (c.forwardPayable && c.isoDate.compareTo(cell.isoDate) >= 0) picked.remove(c.isoDate);
            } else {
                for (GroupTypeBoard.Cell c : board.cells) if (c.forwardPayable && c.isoDate.compareTo(cell.isoDate) <= 0) picked.add(c.isoDate);
            }
        }
        public void clear() { picked.clear(); }
    }

    public static void fill(LinearLayout host, GroupTypeBoard.Board board, Click click) {
        fill(host, board, click, null);
    }

    public static void fill(LinearLayout host, GroupTypeBoard.Board board, Click click, Selection sel) {
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
            final boolean pickable = sel != null && (cell.backpayable || cell.forwardPayable);
            if (pickable && sel.picked.contains(cell.isoDate)) markPicked(c, box, cell, dense);
            int height = dp(c, dense ? 34 : 46);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
            int m = dp(c, dense ? 2 : 3);
            lp.setMargins(m, m, m, m);
            box.setOnClickListener(v -> {
                // Picking several starts with a long press; while days are picked a tap adds or removes one. A lone tap pays that day.
                if (pickable && !sel.picked.isEmpty()) { toggleCell(sel, board, cell); fill(host, board, click, sel); if (sel.onChange != null) sel.onChange.run(); return; }
                if (click != null) click.onCell(cell);
            });
            if (pickable) box.setOnLongClickListener(v -> {
                toggleCell(sel, board, cell);
                fill(host, board, click, sel);
                if (sel.onChange != null) sel.onChange.run();
                return true;
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

    private static void toggleCell(Selection sel, GroupTypeBoard.Board board, GroupTypeBoard.Cell cell) {
        if (cell.forwardPayable) sel.toggleForward(board, cell); else sel.toggle(cell.isoDate);
    }

    public static void showDetail(Context c, GroupTypeBoard.Cell cell) {
        if (c == null || cell == null) return;
        new com.harvana.app.ui.SheetDialog.Builder(c)
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

    /** A picked missed day keeps its red but gains a thick accent ring and a tick, so it reads as chosen. */
    private static void markPicked(Context c, LinearLayout box, GroupTypeBoard.Cell cell, boolean dense) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, dense ? 6 : 8));
        d.setColor(color(c, R.color.colorError));
        d.setStroke(dp(c, 3), color(c, R.color.colorAccent));
        box.setBackground(d);
        TextView tick = new TextView(c);
        tick.setText("\u2713");
        tick.setTextSize(dense ? 10 : 12);
        tick.setTypeface(null, Typeface.BOLD);
        tick.setTextColor(0xFFFFFFFF);
        tick.setGravity(Gravity.CENTER);
        box.addView(tick);
    }

    private static int color(Context c, int res) {
        return androidx.core.content.ContextCompat.getColor(c, res);
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
