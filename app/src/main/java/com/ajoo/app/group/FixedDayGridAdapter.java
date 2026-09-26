package com.ajoo.app.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 31 day cells for Fixed Contribution groups.
 * Paid = green (app's real accent color, not an unrelated teal).
 * Missed (past, unpaid) = red — the actual signal a member needs to see.
 * Today, unpaid = blue — deliberately distinct from "missed," since it's
 * still actionable today, not yet a problem.
 * Future = muted/neutral — not reddish, since a day that isn't due yet
 * hasn't actually been missed. Collapsing this into "unpaid = red" would
 * make every future day look alarming for no reason.
 */
public class FixedDayGridAdapter extends RecyclerView.Adapter<FixedDayGridAdapter.VH> {

    public interface Listener {
        void onDayClick(int day, boolean alreadyPaid);
    }

    private final List<Integer> paidDays = new ArrayList<>();
    private Listener listener;
    private int today = FixedPaymentEngine.todayDayOfMonth();

    private static final int COLOR_PAID = 0xFF10B881;      // app's real accent green
    private static final int COLOR_MISSED = 0xFFEF4444;    // clean red for actually-missed days
    private static final int COLOR_TODAY = 0xFF3498DB;     // distinct blue — actionable, not yet a problem
    private static final int COLOR_FUTURE_BG = 0xFF1E2733; // muted, matches colorSurfaceAlt's dark-mode value
    private static final int COLOR_FUTURE_FG = 0xFF8B95A5; // muted grey text
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    public void setPaidDays(List<Integer> days) {
        paidDays.clear();
        if (days != null) paidDays.addAll(days);
        notifyDataSetChanged();
    }

    public void setListener(Listener l) { this.listener = l; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(parent.getResources().getIdentifier(
                        "item_fixed_day_cell", "layout", parent.getContext().getPackageName()),
                        parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final int day = position + 1;
        boolean paid = paidDays.contains(day);
        boolean isToday = day == today;

        if (h.label != null) h.label.setText(String.valueOf(day));

        int bg;
        int fg = COLOR_WHITE;
        if (paid) {
            bg = COLOR_PAID;
        } else if (isToday) {
            bg = COLOR_TODAY;
        } else if (day < today) {
            bg = COLOR_MISSED;
        } else {
            bg = COLOR_FUTURE_BG;
            fg = COLOR_FUTURE_FG;
        }

        // CardView needs its own setter — setBackgroundColor() on the raw
        // itemView doesn't interact correctly with its rounded-corner
        // rendering, and can draw incorrectly or get ignored entirely.
        if (h.card != null) {
            h.card.setCardBackgroundColor(bg);
        } else {
            h.itemView.setBackgroundColor(bg);
        }
        if (h.label != null) h.label.setTextColor(fg);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(day, paid);
        });
    }

    @Override
    public int getItemCount() {
        return FixedPaymentEngine.DAYS_IN_GRID;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView label;
        CardView card;
        VH(View v) {
            super(v);
            int labelId = v.getResources().getIdentifier("tv_day", "id", v.getContext().getPackageName());
            if (labelId != 0) label = v.findViewById(labelId);
            else if (v instanceof TextView) label = (TextView) v;

            if (v instanceof CardView) {
                card = (CardView) v;
            } else {
                int cardId = v.getResources().getIdentifier("card_day", "id", v.getContext().getPackageName());
                if (cardId != 0) {
                    View found = v.findViewById(cardId);
                    if (found instanceof CardView) card = (CardView) found;
                }
            }
        }
    }
}
