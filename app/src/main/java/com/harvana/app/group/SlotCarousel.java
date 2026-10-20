package com.harvana.app.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.harvana.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Every savings slot (30-day line) of one member as pages swiped left and right, oldest first; it opens on the newest.
 * The current slot's page is drawn by the caller (it can be paid on); finished ones are read-only boards.
 */
public final class SlotCarousel {

    /** Draws the current slot's board (with paying) into {@code host}. */
    public interface CurrentPage { void draw(LinearLayout host); }

    private SlotCarousel() { }

    public static View create(Context ctx, List<Map<String, Object>> lines, String currentLineId, CurrentPage current,
                              GroupTypeBoardView.Click readOnlyClick) {
        View root = LayoutInflater.from(ctx).inflate(R.layout.view_slot_carousel, null, false);
        RecyclerView rv = root.findViewById(R.id.rv_slots);
        LinearLayout dots = root.findViewById(R.id.layout_slot_dots);
        LinearLayoutManager lm = new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false);
        rv.setLayoutManager(lm);
        rv.setAdapter(new Pages(lines, currentLineId, current, readOnlyClick));
        new PagerSnapHelper().attachToRecyclerView(rv);
        int last = Math.max(0, lines.size() - 1);
        rv.scrollToPosition(last);
        paintDots(ctx, dots, lines.size(), last);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView r, int state) {
                if (state != RecyclerView.SCROLL_STATE_IDLE) return;
                int p = lm.findFirstCompletelyVisibleItemPosition();
                if (p >= 0) paintDots(ctx, dots, lines.size(), p);
            }
        });
        return root;
    }

    private static void paintDots(Context ctx, LinearLayout dots, int n, int on) {
        dots.removeAllViews();
        if (n < 2) return;
        float d = ctx.getResources().getDisplayMetrics().density;
        for (int i = 0; i < n; i++) {
            View dot = new View(ctx);
            int w = (int) ((i == on ? 16 : 6) * d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, (int) (6 * d));
            lp.setMargins((int) (3 * d), 0, (int) (3 * d), 0);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setCornerRadius(3 * d);
            g.setColor(i == on ? com.harvana.app.utils.UiColors.textPrimary() : 0x55888888);
            dot.setBackground(g);
            dots.addView(dot, lp);
        }
    }

    private static final class Pages extends RecyclerView.Adapter<Pages.Holder> {
        private final List<Map<String, Object>> lines;
        private final String currentLineId;
        private final CurrentPage current;
        private final GroupTypeBoardView.Click click;

        Pages(List<Map<String, Object>> lines, String currentLineId, CurrentPage current, GroupTypeBoardView.Click click) {
            this.lines = new ArrayList<>(lines);
            this.currentLineId = currentLineId == null ? "" : currentLineId;
            this.current = current;
            this.click = click;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView name, status, range, saved;
            final LinearLayout board;
            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.tv_slot_name);
                status = v.findViewById(R.id.tv_slot_status);
                range = v.findViewById(R.id.tv_slot_range);
                saved = v.findViewById(R.id.tv_slot_saved);
                board = v.findViewById(R.id.layout_slot_board);
            }
        }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot_page, parent, false);
            v.setLayoutParams(new RecyclerView.LayoutParams(parent.getMeasuredWidth() > 0 ? parent.getMeasuredWidth()
                    : ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new Holder(v);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            Map<String, Object> line = lines.get(position);
            String id = String.valueOf(line.get("lineId"));
            boolean isCurrent = id.equals(currentLineId);
            String status = String.valueOf(line.get("status"));
            h.name.setText("Slot " + (position + 1) + " of " + lines.size());
            h.status.setText(isCurrent ? "CURRENT" : status);
            h.status.setTextColor(0xFFFFFFFF);
            android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
            pill.setCornerRadius(24 * h.itemView.getResources().getDisplayMetrics().density);
            pill.setColor(isCurrent ? 0xFF2FA36B : 0xFF6B7280);
            h.status.setBackground(pill);
            long start = num(line.get("startTimestamp"));
            long end = num(line.get("endTimestamp"));
            if (end <= 0) end = num(line.get("naturalEndTimestamp"));
            java.text.DateFormat f = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM);
            h.range.setText((start > 0 ? f.format(new java.util.Date(start)) : "—") + "  →  " + (end > 0 ? f.format(new java.util.Date(end)) : "—"));
            String coin = com.harvana.app.engine.Amounts.assetOf(line);
            long paid = num(line.get("amountPaidRaw"));
            h.saved.setText(num(line.get("daysPaid")) + " days · " + (coin.isEmpty()
                    ? String.format(java.util.Locale.US, "%,.2f HARV", paid / 1e8)
                    : com.harvana.app.engine.Amounts.money(paid, coin)));
            h.board.removeAllViews();
            if (isCurrent && current != null) {
                current.draw(h.board);
            } else {
                GroupTypeBoard.Board b = GroupTypeBoard.fromNode("ACCUM", null, null, line, "",
                        System.currentTimeMillis(), TimeZone.getDefault());
                GroupTypeBoardView.fill(h.board, b, click);
            }
        }

        @Override public int getItemCount() { return lines.size(); }

        private static long num(Object o) {
            if (o instanceof Number) return ((Number) o).longValue();
            try { return o == null ? 0 : Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
        }
    }
}
