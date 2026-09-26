package com.ajoo.app.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ajoo.app.firestore.GroupFirestoreManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Logic for MembersFragmentActivity */
public class MembersFragmentLogic {

    private final Context ctx;
    private String groupId = "";
    private boolean isAdmin = false;
    private String adminUid = "";

    public MembersFragmentLogic(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public void setGroupId(String id) { this.groupId = id != null ? id : ""; }
    public void setAdmin(boolean a) { isAdmin = a; }
    public void setAdminUid(String uid) { adminUid = uid != null ? uid : ""; }

    public void load(RecyclerView recycler, TextView countView, TextView emptyView, TextView inviteBtn, int cardLayoutId) {
        if (inviteBtn != null) inviteBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (groupId.isEmpty()) return;

        GroupFirestoreManager.getInstance(ctx).getMembers(groupId, new GroupFirestoreManager.ListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> items) {
                List<Map<String, Object>> list = items != null ? items : new ArrayList<>();
                if (recycler.getHandler() != null) {
                    recycler.post(() -> bindList(recycler, countView, emptyView, list, cardLayoutId));
                } else {
                    bindList(recycler, countView, emptyView, list, cardLayoutId);
                }
            }
            @Override public void onError(String error) {}
        });
    }

    private void bindList(RecyclerView recycler, TextView countView, TextView emptyView,
                          List<Map<String, Object>> list, int cardLayoutId) {
        if (countView != null) {
            countView.setText("Members · " + list.size());
        }
        if (emptyView != null) {
            emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (recycler.getLayoutManager() == null) {
            recycler.setLayoutManager(new LinearLayoutManager(ctx));
        }
        recycler.setAdapter(new MemberAdapter(list, cardLayoutId, adminUid));
    }

    private static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        private final List<Map<String, Object>> items;
        private final int layoutId;
        private final String adminUid;

        MemberAdapter(List<Map<String, Object>> items, int layoutId, String adminUid) {
            this.items = items;
            this.layoutId = layoutId;
            this.adminUid = adminUid;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map<String, Object> m = items.get(position);
            String name = str(m, "displayName", str(m, "name", str(m, "memberUid", "Member")));
            String wallet = str(m, "walletAddress", str(m, "wallet", ""));
            String role = str(m, "role", "");
            int slot = (int) num(m, "slot", 0);
            String status = str(m, "paymentStatus", "Pending");
            String uid = str(m, "memberUid", str(m, "_docId", ""));

            if (h.name != null) h.name.setText(name);
            if (h.wallet != null) h.wallet.setText(shorten(wallet));
            if (h.slot != null) h.slot.setText(slot > 0 ? "Slot " + slot : "—");
            if (h.status != null) h.status.setText(status);
            if (h.initials != null) {
                h.initials.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.US));
            }
            if (h.role != null) {
                boolean show = "admin".equalsIgnoreCase(role) || uid.equals(adminUid);
                h.role.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, wallet, slot, status, role, initials, balance;
            VH(View v) {
                super(v);
                name = find(v, "tv_member_name");
                wallet = find(v, "tv_member_wallet");
                slot = find(v, "tv_member_slot");
                status = find(v, "tv_member_status");
                role = find(v, "tv_member_role");
                initials = find(v, "tv_member_initials");
                balance = find(v, "tv_member_balance");
            }
            TextView find(View v, String id) {
                int res = v.getResources().getIdentifier(id, "id", v.getContext().getPackageName());
                return res != 0 ? v.findViewById(res) : null;
            }
        }

        static String str(Map<String, Object> m, String k, String d) {
            Object v = m.get(k);
            if (v == null) return d;
            String s = String.valueOf(v).trim();
            return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
        }
        static double num(Map<String, Object> m, String k, double d) {
            Object v = m.get(k);
            if (v instanceof Number) return ((Number) v).doubleValue();
            try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return d; }
        }
        static String shorten(String a) {
            if (a == null || a.length() < 12) return a != null ? a : "—";
            return a.substring(0, 10) + "…" + a.substring(a.length() - 4);
        }
    }
}
