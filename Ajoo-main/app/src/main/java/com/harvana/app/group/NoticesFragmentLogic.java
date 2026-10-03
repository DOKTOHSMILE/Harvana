package com.harvana.app.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Logic for NoticesFragmentActivity */
public class NoticesFragmentLogic {

    private final Context ctx;
    private final Activity activity;
    private String groupId = "";
    private boolean isAdmin = false;

    public NoticesFragmentLogic(Activity activity) {
        this.activity = activity;
        this.ctx = activity.getApplicationContext();
    }

    public void setGroupId(String id) { groupId = id != null ? id : ""; }
    public void setAdmin(boolean a) { isAdmin = a; }

    public void load(RecyclerView recycler, TextView empty, TextView postBtn, int cardLayoutId) {
        if (postBtn != null) {
            postBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            postBtn.setOnClickListener(v -> showPostDialog());
        }
        if (groupId.isEmpty()) return;

        GroupFirestoreManager.getInstance(ctx).getNotices(groupId, new GroupFirestoreManager.ListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> items) {
                List<Map<String, Object>> list = items != null ? items : new ArrayList<>();
                activity.runOnUiThread(() -> {
                    if (empty != null) empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    if (recycler.getLayoutManager() == null) {
                        recycler.setLayoutManager(new LinearLayoutManager(ctx));
                    }
                    recycler.setAdapter(new NoticeAdapter(list, cardLayoutId));
                });
            }
            @Override public void onError(String error) {}
        });
    }

    private void showPostDialog() {
        EditText title = new EditText(activity);
        title.setHint("Title");
        EditText body = new EditText(activity);
        body.setHint("Message");
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(48, 24, 48, 0);
        box.addView(title);
        box.addView(body);
        new AlertDialog.Builder(activity)
                .setTitle("Post notice")
                .setView(box)
                .setPositiveButton("Post", (d, w) -> {
                    Map<String, Object> fields = new HashMap<>();
                    fields.put("title", title.getText().toString().trim());
                    fields.put("message", body.getText().toString().trim());
                    fields.put("priority", "Normal");
                    fields.put("sender", "Admin");
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                    fields.put("creatorId", uid);
                    fields.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    GroupFirestoreManager.getInstance(ctx).addNotice(groupId, fields,
                            new GroupFirestoreManager.IdCallback() {
                                @Override public void onSuccess(String id) {
                                    activity.runOnUiThread(() ->
                                            android.widget.Toast.makeText(ctx, "Notice posted", android.widget.Toast.LENGTH_SHORT).show());
                                }
                                @Override public void onError(String error) {
                                    activity.runOnUiThread(() ->
                                            android.widget.Toast.makeText(ctx, error, android.widget.Toast.LENGTH_SHORT).show());
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.VH> {
        private final List<Map<String, Object>> items;
        private final int layoutId;
        NoticeAdapter(List<Map<String, Object>> items, int layoutId) {
            this.items = items;
            this.layoutId = layoutId;
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, Object> n = items.get(pos);
            if (h.title != null) h.title.setText(str(n, "title", str(n, "sender", "Notice")));
            if (h.body != null) h.body.setText(str(n, "message", str(n, "body", "")));
            if (h.author != null) h.author.setText("By " + str(n, "sender", "Admin"));
            if (h.date != null) h.date.setText(str(n, "timestamp", ""));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView title, body, author, date;
            VH(View v) {
                super(v);
                title = id(v, "tv_notice_title");
                body = id(v, "tv_notice_body");
                author = id(v, "tv_notice_author");
                date = id(v, "tv_notice_date");
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
    }
}
