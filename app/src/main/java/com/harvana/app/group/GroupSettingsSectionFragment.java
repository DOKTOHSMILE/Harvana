package com.harvana.app.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

/**
 * One section of Group settings (General, Access, Members, Money & rules, Network, More) as its own page. The page is an
 * empty scrolling column; the settings screen puts that section's cards in it ({@link Host#attachSection}).
 */
public class GroupSettingsSectionFragment extends Fragment {

    /** The settings screen: fills a section's page with its cards. */
    public interface Host {
        void attachSection(int section, LinearLayout page);
        void detachSection(int section);
    }

    private static final String ARG_SECTION = "section";

    public static GroupSettingsSectionFragment newInstance(int section) {
        GroupSettingsSectionFragment f = new GroupSettingsSectionFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_SECTION, section);
        f.setArguments(b);
        return f;
    }

    private int section() { return getArguments() == null ? 0 : getArguments().getInt(ARG_SECTION); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        float d = getResources().getDisplayMetrics().density;
        NestedScrollView scroll = new NestedScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout page = new LinearLayout(requireContext());
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding((int) (16 * d), (int) (4 * d), (int) (16 * d), (int) (24 * d));
        scroll.addView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        if (getActivity() instanceof Host) ((Host) getActivity()).attachSection(section(), (LinearLayout) ((ViewGroup) view).getChildAt(0));
    }

    @Override
    public void onDestroyView() {
        if (getActivity() instanceof Host) ((Host) getActivity()).detachSection(section());
        super.onDestroyView();
    }
}
