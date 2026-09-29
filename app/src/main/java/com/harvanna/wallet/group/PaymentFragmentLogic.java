package com.harvanna.wallet.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvanna.wallet.firestore.GroupFirestoreManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Logic for PaymentFragmentActivity */
public class PaymentFragmentLogic {

    private final Context ctx;
    private String groupId = "";
    private double contributionAmount = 0;
    private int mySlot = 0;

    public PaymentFragmentLogic(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public void setGroupId(String id) { groupId = id != null ? id : ""; }
    public void setContribution(double ajc) { contributionAmount = ajc; }
    public void setMySlot(int slot) { mySlot = slot; }

    public void load(TextView myLabel, TextView myAmount, TextView myDue,
                     TextView roundCount, RecyclerView recycler, TextView empty, int rowLayoutId) {
        if (myAmount != null) {
            myAmount.setText(com.harvanna.wallet.wallet.BalanceFormatter.smart(contributionAmount));
        }
        if (myLabel != null) {
            myLabel.setText(mySlot > 0 ? "MY PAYMENT · SLOT " + mySlot : "MY PAYMENT");
        }
        if (groupId.isEmpty()) return;

        GroupFirestoreManager.getInstance(ctx).getPayments(groupId, new GroupFirestoreManager.ListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> items) {
                List<Map<String, Object>> list = items != null ? items : new ArrayList<>();
                int paid = 0, pending = 0;
                for (Map<String, Object> p : list) {
                    String st = String.valueOf(p.get("status"));
                    if ("CONFIRMED".equalsIgnoreCase(st) || "PAID".equalsIgnoreCase(st)) paid++;
                    else pending++;
                }
                final int fPaid = paid, fPending = pending;
                if (recycler != null) {
                    recycler.post(() -> {
                        if (roundCount != null) {
                            roundCount.setText(fPaid + " paid · " + fPending + " pending");
                        }
                        if (empty != null) empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                        if (recycler.getLayoutManager() == null) {
                            recycler.setLayoutManager(new LinearLayoutManager(ctx));
                        }
                        recycler.setAdapter(new PayAdapter(list, rowLayoutId));
                    });
                }
            }
            @Override public void onError(String error) {}
        });
    }

    private static class PayAdapter extends RecyclerView.Adapter<PayAdapter.VH> {
        private final List<Map<String, Object>> items;
        private final int layoutId;
        PayAdapter(List<Map<String, Object>> items, int layoutId) {
            this.items = items;
            this.layoutId = layoutId;
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, Object> p = items.get(pos);
            if (h.name != null) h.name.setText(str(p, "to", str(p, "memberName", "Member")));
            if (h.meta != null) h.meta.setText(str(p, "type", "PAYMENT") + " · " + str(p, "status", ""));
            double amt = num(p, "amountAjc", 0);
            if (h.amount != null) h.amount.setText(com.harvanna.wallet.wallet.BalanceFormatter.smart(amt));
            if (h.status != null) h.status.setText(str(p, "status", "—"));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView name, meta, amount, status;
            VH(View v) {
                super(v);
                name = id(v, "tv_pay_name");
                meta = id(v, "tv_pay_meta");
                amount = id(v, "tv_pay_amount");
                status = id(v, "tv_pay_status");
            }
            static TextView id(View v, String n) {
                int r = v.getResources().getIdentifier(n, "id", v.getContext().getPackageName());
                return r != 0 ? v.findViewById(r) : null;
            }
        }
        static String str(Map<String, Object> m, String k, String d) {
            Object v = m.get(k);
            return v != null ? String.valueOf(v) : d;
        }
        static double num(Map<String, Object> m, String k, double d) {
            Object v = m.get(k);
            if (v instanceof Number) return ((Number) v).doubleValue();
            try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return d; }
        }
    }
}
