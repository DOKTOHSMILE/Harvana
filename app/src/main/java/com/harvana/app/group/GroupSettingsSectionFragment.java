package com.harvana.app.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        return inflater.inflate(com.harvana.app.R.layout.fragment_settings_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        if (getActivity() instanceof Host) ((Host) getActivity()).attachSection(section(), view.findViewById(com.harvana.app.R.id.settings_section_page));
    }

    @Override
    public void onDestroyView() {
        if (getActivity() instanceof Host) ((Host) getActivity()).detachSection(section());
        super.onDestroyView();
    }
}
